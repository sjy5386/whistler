package com.sysbot32.whistler.file_manager.ops;


import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 7zFM-style volume split/combine: {@code name.001}, {@code name.002}, …
 */
@UtilityClass
public final class FileSplitCombine {
    private static final int BUFFER = 64 * 1024;
    private static final Pattern PART_SUFFIX = Pattern.compile("^(.*)\\.(\\d{3,})$");
    /**
     * Split {@code source} into volumes of at most {@code volumeSize} bytes under {@code destDir}.
     * Parts are named {@code &lt;filename&gt;.001}, {@code .002}, …
     *
     * @return paths of created volume files
     */
    public static List<Path> split(
            final Path source,
            final Path destDir,
            final long volumeSize,
            final TransferControl control
    ) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destDir, "destDir");
        if (!Files.isRegularFile(source)) {
            throw new IOException("Not a regular file: " + source);
        }
        if (volumeSize <= 0) {
            throw new IOException("Volume size must be positive");
        }
        final long total = Files.size(source);
        if (volumeSize >= total) {
            throw new IOException("Volume size must be smaller than size of original file");
        }
        Files.createDirectories(destDir);
        final String base = source.getFileName().toString();
        final int volumes = (int) ((total + volumeSize - 1) / volumeSize);
        if (control != null) {
            control.begin(volumes);
        }
        final List<Path> parts = new ArrayList<>();
        try (final InputStream in = Files.newInputStream(source)) {
            final byte[] buf = new byte[BUFFER];
            long remainingTotal = total;
            int index = 1;
            while (remainingTotal > 0) {
                if (control != null) {
                    control.throwIfCancelled();
                }
                final long thisVol = Math.min(volumeSize, remainingTotal);
                final Path part = destDir.resolve(base + "." + formatIndex(index));
                try (final OutputStream out = Files.newOutputStream(part)) {
                    long left = thisVol;
                    while (left > 0) {
                        final int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                        if (n < 0) {
                            throw new IOException("Unexpected EOF while splitting");
                        }
                        out.write(buf, 0, n);
                        left -= n;
                    }
                }
                parts.add(part);
                if (control != null) {
                    control.advance(part.getFileName().toString());
                }
                remainingTotal -= thisVol;
                index++;
            }
        }
        return parts;
    }

    /**
     * Combine a split sequence starting at {@code firstPart} (must end with {@code .001} style)
     * into {@code destFile}.
     *
     * @return number of parts combined
     */
    public static int combine(
            final Path firstPart,
            final Path destFile,
            final TransferControl control
    ) throws IOException {
        Objects.requireNonNull(firstPart, "firstPart");
        Objects.requireNonNull(destFile, "destFile");
        if (!Files.isRegularFile(firstPart)) {
            throw new IOException("Not a regular file: " + firstPart);
        }
        final String name = firstPart.getFileName().toString();
        final Matcher m = PART_SUFFIX.matcher(name);
        if (!m.matches()) {
            throw new IOException("Cannot detect file as part of split file: " + name);
        }
        final String prefix = m.group(1);
        final String digits = m.group(2);
        if (!"001".equals(digits) && Integer.parseInt(digits) != 1) {
            // 7zFM expects first part (.001); still allow .1 padded forms that parse to 1
            if (Integer.parseInt(digits) != 1) {
                throw new IOException("Select only first part of split file (.001)");
            }
        }
        final Path dir = firstPart.getParent() != null ? firstPart.getParent() : Path.of(".");
        final List<Path> parts = new ArrayList<>();
        int index = 1;
        while (true) {
            final Path part = dir.resolve(prefix + "." + formatIndex(index, digits.length()));
            if (!Files.isRegularFile(part)) {
                break;
            }
            parts.add(part);
            index++;
        }
        if (parts.size() < 2) {
            throw new IOException("Cannot find more than one part of split file");
        }
        final Path parent = destFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (control != null) {
            control.begin(parts.size());
        }
        try (final OutputStream out = Files.newOutputStream(destFile)) {
            final byte[] buf = new byte[BUFFER];
            for (final Path part : parts) {
                if (control != null) {
                    control.throwIfCancelled();
                }
                try (final InputStream in = Files.newInputStream(part)) {
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }
                if (control != null) {
                    control.advance(part.getFileName().toString());
                }
            }
        }
        return parts.size();
    }

    /** Detect whether name looks like a split volume ({@code *.NNN}). */
    public static boolean looksLikePart(final String fileName) {
        if (fileName == null) {
            return false;
        }
        final Matcher m = PART_SUFFIX.matcher(fileName);
        return m.matches();
    }

    public static boolean isFirstPart(final String fileName) {
        if (!looksLikePart(fileName)) {
            return false;
        }
        final Matcher m = PART_SUFFIX.matcher(fileName);
        m.matches();
        return Integer.parseInt(m.group(2)) == 1;
    }

    static String formatIndex(final int index) {
        return formatIndex(index, 3);
    }

    static String formatIndex(final int index, final int width) {
        final int w = Math.max(3, width);
        return String.format("%0" + w + "d", index);
    }
}
