package com.sysbot32.whistler.file_manager.archive;

import com.sysbot32.whistler.file_manager.ops.TransferControl;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link ArchiveOperations} backends. Zip is the only registered format today;
 * add future formats by registering another implementation (no Frame if-else sprawl).
 */
@UtilityClass
public class Archives {
    private static final List<ArchiveOperations> BACKENDS = new ArrayList<>();

    static {
        register(new ZipArchiveOperations());
    }

    /** Register a backend (idempotent by format — replaces existing of same format). */
    public static synchronized void register(final ArchiveOperations ops) {
        if (ops == null) {
            return;
        }
        BACKENDS.removeIf(b -> b.format() == ops.format());
        BACKENDS.add(ops);
    }

    public static synchronized List<ArchiveOperations> backends() {
        return List.copyOf(BACKENDS);
    }

    public static ArchiveOperations zip() {
        return require(ArchiveFormat.ZIP);
    }

    public static ArchiveOperations require(final ArchiveFormat format) {
        return find(format).orElseThrow(() -> new IllegalStateException("No backend for " + format));
    }

    public static Optional<ArchiveOperations> find(final ArchiveFormat format) {
        for (final ArchiveOperations b : backends()) {
            if (b.format() == format) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    /**
     * Whether any registered backend claims this path by name/extension
     * (file need not exist yet — used when creating a new archive).
     */
    public static boolean isSupportedArchiveName(final Path archiveFile) {
        if (archiveFile == null) {
            return false;
        }
        for (final ArchiveOperations b : backends()) {
            if (b.supports(archiveFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Regular file that a registered backend can open (UI “looks like archive” check).
     */
    public static boolean isOpenableArchive(final Path path) {
        return path != null && Files.isRegularFile(path) && isSupportedArchiveName(path);
    }

    /**
     * Resolve backend for an archive file path (by registered {@link ArchiveOperations#supports}).
     */
    public static ArchiveOperations forArchive(final Path archiveFile) throws IOException {
        if (archiveFile == null) {
            throw new IOException("No archive path");
        }
        for (final ArchiveOperations b : backends()) {
            if (b.supports(archiveFile)) {
                return b;
            }
        }
        throw new IOException("Unsupported archive format: " + archiveFile.getFileName());
    }

    /**
     * Create a new archive, or add into it when the file already exists.
     */
    public static void createOrAdd(
            final Path archive,
            final String destInternalDir,
            final List<Path> sources,
            final TransferControl control
    ) throws IOException {
        createOrAdd(archive, destInternalDir, sources, control, ArchiveWriteOptions.defaults());
    }

    public static void createOrAdd(
            final Path archive,
            final String destInternalDir,
            final List<Path> sources,
            final TransferControl control,
            final ArchiveWriteOptions options
    ) throws IOException {
        final ArchiveOperations backend = forArchive(archive);
        final ArchiveWriteOptions opts = options == null ? ArchiveWriteOptions.defaults() : options;
        if (Files.isRegularFile(archive)) {
            backend.add(archive, destInternalDir == null ? "" : destInternalDir, sources, control, opts);
        } else {
            backend.create(archive, sources, control, opts);
        }
    }
}
