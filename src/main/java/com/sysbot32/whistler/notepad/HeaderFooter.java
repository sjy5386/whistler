package com.sysbot32.whistler.notepad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Classic Notepad header/footer codes: &amp;f &amp;p &amp;d &amp;t and &amp;l &amp;c &amp;r.
 */
public final class HeaderFooter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private HeaderFooter() {
    }

    /**
     * Expand a classic header/footer template.
     *
     * @param template  raw template (e.g. {@code &f}, {@code Page &p})
     * @param fileName  document name for {@code &f}
     * @param pageNumber 1-based page for {@code &p}
     * @param dateTime  clock used for {@code &d}/{@code &t}
     */
    public static String expand(
            final String template,
            final String fileName,
            final int pageNumber,
            final LocalDateTime dateTime
    ) {
        if (Objects.isNull(template) || template.isEmpty()) {
            return "";
        }
        final LocalDateTime now = Objects.requireNonNullElseGet(dateTime, LocalDateTime::now);
        final String name = Objects.requireNonNullElse(fileName, "");
        final StringBuilder out = new StringBuilder(template.length() + 16);
        for (int i = 0; i < template.length(); i++) {
            final char ch = template.charAt(i);
            if (ch == '&' && i + 1 < template.length()) {
                final char code = Character.toLowerCase(template.charAt(i + 1));
                switch (code) {
                    case 'f' -> {
                        out.append(name);
                        i++;
                    }
                    case 'p' -> {
                        out.append(pageNumber);
                        i++;
                    }
                    case 'd' -> {
                        out.append(LocalDate.from(now).format(DATE));
                        i++;
                    }
                    case 't' -> {
                        out.append(LocalTime.from(now).format(TIME));
                        i++;
                    }
                    case 'l', 'c', 'r' -> {
                        // Alignment markers: classic Notepad treats them as layout hints.
                        // We drop them in the expanded plain string (layout is printer-side).
                        i++;
                    }
                    case '&' -> {
                        out.append('&');
                        i++;
                    }
                    default -> out.append(ch);
                }
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    /**
     * Convert classic codes to a {@link java.text.MessageFormat} pattern for Swing printing,
     * where {@code {0}} is the page number.
     */
    public static String toPrintMessageFormat(final String template, final String fileName, final LocalDateTime dateTime) {
        final String withMeta = expandKeepingPagePlaceholder(template, fileName, dateTime);
        return withMeta.replace("'", "''");
    }

    private static String expandKeepingPagePlaceholder(
            final String template,
            final String fileName,
            final LocalDateTime dateTime
    ) {
        if (Objects.isNull(template) || template.isEmpty()) {
            return "";
        }
        final LocalDateTime now = Objects.requireNonNullElseGet(dateTime, LocalDateTime::now);
        final String name = Objects.requireNonNullElse(fileName, "");
        final StringBuilder out = new StringBuilder(template.length() + 16);
        for (int i = 0; i < template.length(); i++) {
            final char ch = template.charAt(i);
            if (ch == '&' && i + 1 < template.length()) {
                final char code = Character.toLowerCase(template.charAt(i + 1));
                switch (code) {
                    case 'f' -> {
                        out.append(name.replace("'", "''"));
                        i++;
                    }
                    case 'p' -> {
                        out.append("{0}");
                        i++;
                    }
                    case 'd' -> {
                        out.append(LocalDate.from(now).format(DATE));
                        i++;
                    }
                    case 't' -> {
                        out.append(LocalTime.from(now).format(TIME));
                        i++;
                    }
                    case 'l', 'c', 'r' -> i++;
                    case '&' -> {
                        out.append('&');
                        i++;
                    }
                    default -> out.append(ch);
                }
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
