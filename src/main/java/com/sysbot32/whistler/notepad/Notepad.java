package com.sysbot32.whistler.notepad;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Getter
@NoArgsConstructor
public class Notepad {
    private String content = "";
    private boolean edited = false;
    private Path path = null;

    public Notepad(final Path path) {
        try {
            this.open(path);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createNew() {
        this.content = "";
        this.path = null;
        this.edited = false;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setEdited(final boolean edited) {
        this.edited = edited;
    }

    public String open(final Path path) throws IOException {
        final String loaded = Files.readString(path);
        this.path = path;
        if (isLogDocument(loaded)) {
            this.content = appendLogStamp(loaded);
            this.edited = true;
        } else {
            this.content = loaded;
            this.edited = false;
        }
        return this.content;
    }

    public void save(final Path path) throws IOException {
        Files.writeString(path, this.content);
        this.edited = false;
        this.path = path;
    }

    /**
     * Search for {@code str} starting at {@code fromIndex}.
     * Down: first match at/after fromIndex. Up: last match strictly before fromIndex.
     */
    public int find(
            final String str,
            final int fromIndex,
            final boolean matchCase,
            final SearchDirection direction
    ) {
        if (Objects.isNull(str) || str.isEmpty()) {
            return -1;
        }
        final String haystack = matchCase ? this.content : this.content.toLowerCase(Locale.ROOT);
        final String needle = matchCase ? str : str.toLowerCase(Locale.ROOT);
        if (direction == SearchDirection.UP) {
            final int end = Math.min(Math.max(fromIndex, 0), haystack.length());
            if (end <= 0) {
                return -1;
            }
            return haystack.lastIndexOf(needle, end - 1);
        }
        return haystack.indexOf(needle, Math.max(fromIndex, 0));
    }

    public int replaceAll(final String find, final String replace, final boolean matchCase) {
        if (Objects.isNull(find) || find.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        final StringBuilder builder = new StringBuilder(this.content);
        while (true) {
            final int index = matchCase
                    ? builder.indexOf(find, from)
                    : builder.toString().toLowerCase(Locale.ROOT).indexOf(find.toLowerCase(Locale.ROOT), from);
            if (index < 0) {
                break;
            }
            builder.replace(index, index + find.length(), replace);
            from = index + replace.length();
            count++;
        }
        if (count > 0) {
            this.content = builder.toString();
            this.edited = true;
        }
        return count;
    }

    /**
     * Classic Notepad: first line is exactly {@code .LOG} (optional trailing CR).
     */
    public static boolean isLogDocument(final String content) {
        if (Objects.isNull(content) || content.isEmpty()) {
            return false;
        }
        int end = content.indexOf('\n');
        if (end < 0) {
            end = content.length();
        }
        String firstLine = content.substring(0, end);
        if (firstLine.endsWith("\r")) {
            firstLine = firstLine.substring(0, firstLine.length() - 1);
        }
        return ".LOG".equals(firstLine);
    }

    /**
     * Append a time/date stamp after the document body (classic .LOG open behavior).
     */
    public static String appendLogStamp(final String content) {
        return appendLogStamp(content, getTimeDate());
    }

    public static String appendLogStamp(final String content, final String stamp) {
        final String body = Objects.requireNonNullElse(content, "");
        final String time = Objects.requireNonNullElse(stamp, getTimeDate());
        if (body.isEmpty() || body.endsWith("\n") || body.endsWith("\r")) {
            return body + time + System.lineSeparator();
        }
        return body + System.lineSeparator() + time + System.lineSeparator();
    }

    public static String getTimeDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm yyyy-MM-dd"));
    }
}
