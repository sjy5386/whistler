package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Portable stand-in for 7zFM “Alternate streams”: lists user-defined attributes
 * (and reports when unsupported). On macOS/Linux this is xattr-backed UDF view.
 */
@UtilityClass
public final class AlternateStreams {
    public record StreamInfo(String name, int sizeBytes) {
    }

    public static boolean isSupported(final Path path) {
        try {
            return Files.getFileAttributeView(path, UserDefinedFileAttributeView.class) != null;
        } catch (final Exception ex) {
            return false;
        }
    }

    public static List<StreamInfo> list(final Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        final UserDefinedFileAttributeView view =
                Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        if (view == null) {
            return List.of();
        }
        final List<StreamInfo> out = new ArrayList<>();
        for (final String name : view.list()) {
            final int size = view.size(name);
            out.add(new StreamInfo(name, size));
        }
        return out;
    }

    public static Map<String, String> listAsTextPreview(final Path path, final int maxValueChars)
            throws IOException {
        final UserDefinedFileAttributeView view =
                Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        final Map<String, String> map = new LinkedHashMap<>();
        if (view == null) {
            return map;
        }
        for (final String name : view.list()) {
            final int size = view.size(name);
            final ByteBuffer buf = ByteBuffer.allocate(size);
            view.read(name, buf);
            buf.flip();
            final byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > maxValueChars) {
                text = text.substring(0, maxValueChars) + "…";
            }
            // hide non-printable for display
            if (!isMostlyText(bytes)) {
                text = "<binary " + size + " bytes>";
            }
            map.put(name, text);
        }
        return map;
    }

    public static void write(final Path path, final String streamName, final String value)
            throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(streamName, "streamName");
        Objects.requireNonNull(value, "value");
        if (streamName.isBlank() || streamName.contains("/") || streamName.contains("\\")) {
            throw new IOException("Invalid stream name: " + streamName);
        }
        final UserDefinedFileAttributeView view =
                Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        if (view == null) {
            throw new IOException("Alternate streams / user attributes not supported on this path");
        }
        view.write(streamName, ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static void delete(final Path path, final String streamName) throws IOException {
        final UserDefinedFileAttributeView view =
                Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        if (view == null) {
            throw new IOException("Alternate streams not supported");
        }
        view.delete(streamName);
    }

    private static boolean isMostlyText(final byte[] bytes) {
        if (bytes.length == 0) {
            return true;
        }
        int bad = 0;
        for (final byte b : bytes) {
            final int v = b & 0xff;
            if (v == 0 || (v < 0x09) || (v > 0x0d && v < 0x20 && v != 0x1b)) {
                bad++;
            }
        }
        return bad * 10 < bytes.length;
    }
}
