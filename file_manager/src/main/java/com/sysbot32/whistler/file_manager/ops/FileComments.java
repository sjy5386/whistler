package com.sysbot32.whistler.file_manager.ops;


import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-file comments: user-defined attribute {@code comment} when supported,
 * else a sidecar {@code .filename.comment.txt}.
 */
@UtilityClass
public final class FileComments {
    public static final String ATTR_NAME = "comment";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    public static Optional<String> get(final Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        final Optional<String> fromAttr = readAttr(path);
        if (fromAttr.isPresent()) {
            return fromAttr;
        }
        final Path sidecar = sidecarPath(path);
        if (Files.isRegularFile(sidecar)) {
            return Optional.of(Files.readString(sidecar, CHARSET));
        }
        return Optional.empty();
    }

    public static void set(final Path path, final String comment) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(comment, "comment");
        if (!Files.exists(path)) {
            throw new IOException("Missing: " + path);
        }
        if (writeAttr(path, comment)) {
            final Path sidecar = sidecarPath(path);
            Files.deleteIfExists(sidecar);
            return;
        }
        Files.writeString(sidecarPath(path), comment, CHARSET);
    }

    public static void clear(final Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try {
            final UserDefinedFileAttributeView view =
                    Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
            if (view != null && view.list().contains(ATTR_NAME)) {
                view.delete(ATTR_NAME);
            }
        } catch (final UnsupportedOperationException | IOException ignored) {
            // fall through to sidecar
        }
        Files.deleteIfExists(sidecarPath(path));
    }

    static Path sidecarPath(final Path path) {
        final Path parent = path.getParent();
        final String name = path.getFileName().toString();
        final Path side = Path.of("." + name + ".comment.txt");
        return parent == null ? side : parent.resolve(side);
    }

    private static Optional<String> readAttr(final Path path) {
        try {
            final UserDefinedFileAttributeView view =
                    Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
            if (view == null || !view.list().contains(ATTR_NAME)) {
                return Optional.empty();
            }
            final ByteBuffer buf = ByteBuffer.allocate(view.size(ATTR_NAME));
            view.read(ATTR_NAME, buf);
            buf.flip();
            final byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            return Optional.of(new String(bytes, CHARSET));
        } catch (final Exception ex) {
            return Optional.empty();
        }
    }

    private static boolean writeAttr(final Path path, final String comment) {
        try {
            final UserDefinedFileAttributeView view =
                    Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
            if (view == null) {
                return false;
            }
            final byte[] bytes = comment.getBytes(CHARSET);
            view.write(ATTR_NAME, ByteBuffer.wrap(bytes));
            return true;
        } catch (final Exception ex) {
            return false;
        }
    }
}
