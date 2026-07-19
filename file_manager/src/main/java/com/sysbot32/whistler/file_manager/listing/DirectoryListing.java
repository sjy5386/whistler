package com.sysbot32.whistler.file_manager.listing;

import com.sysbot32.whistler.file_manager.model.FileEntry;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Lists a disk directory for the file table.
 */
@UtilityClass
public final class DirectoryListing {
    public static List<FileEntry> list(final Path directory, final boolean showHidden) throws IOException {
        final Path dir = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }

        final List<FileEntry> entries = new ArrayList<>();
        if (dir.getParent() != null) {
            entries.add(FileEntry.upLink());
        }

        try (final Stream<Path> stream = Files.list(dir)) {
            final List<Path> children = stream
                    .filter(p -> showHidden || !isHidden(p))
                    .sorted(directoryThenName())
                    .toList();
            for (final Path child : children) {
                try {
                    entries.add(toEntry(child));
                } catch (final IOException ex) {
                    final String name = child.getFileName() != null
                            ? child.getFileName().toString()
                            : child.toString();
                    entries.add(new FileEntry(
                            name, Files.isDirectory(child), false, 0L, null,
                            null, null, null, "?"
                    ));
                }
            }
        }
        return entries;
    }

    private static Comparator<Path> directoryThenName() {
        return Comparator
                .comparing((Path p) -> !Files.isDirectory(p))
                .thenComparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);
    }

    private static boolean isHidden(final Path path) {
        try {
            if (Files.isHidden(path)) {
                return true;
            }
        } catch (final IOException ignored) {
            // fall through
        }
        final String name = path.getFileName() != null ? path.getFileName().toString() : "";
        return name.startsWith(".");
    }

    static FileEntry toEntry(final Path path) throws IOException {
        final String name = path.getFileName() != null ? path.getFileName().toString() : path.toString();
        final boolean directory = Files.isDirectory(path);
        long size = 0L;
        Instant modified = null;
        Instant created = null;
        Instant accessed = null;
        String attributes = "";

        try {
            final BasicFileAttributes basic = Files.readAttributes(path, BasicFileAttributes.class);
            size = directory ? 0L : basic.size();
            modified = toInstant(basic.lastModifiedTime());
            created = toInstant(basic.creationTime());
            accessed = toInstant(basic.lastAccessTime());
            attributes = formatAttributes(path, basic, directory);
        } catch (final IOException ex) {
            attributes = directory ? "D" : "A";
        }

        return new FileEntry(name, directory, false, size, null, modified, created, accessed, attributes);
    }

    private static Instant toInstant(final java.nio.file.attribute.FileTime time) {
        if (time == null) {
            return null;
        }
        return time.toInstant();
    }

    private static String formatAttributes(
            final Path path,
            final BasicFileAttributes basic,
            final boolean directory
    ) {
        try {
            final DosFileAttributes dos = Files.readAttributes(path, DosFileAttributes.class);
            final StringBuilder sb = new StringBuilder(4);
            if (directory) {
                sb.append('D');
            }
            if (dos.isReadOnly()) {
                sb.append('R');
            }
            if (dos.isHidden()) {
                sb.append('H');
            }
            if (dos.isSystem()) {
                sb.append('S');
            }
            if (dos.isArchive()) {
                sb.append('A');
            }
            return sb.toString();
        } catch (final UnsupportedOperationException | IOException ignored) {
            // not DOS
        }

        try {
            final PosixFileAttributes posix = Files.readAttributes(path, PosixFileAttributes.class);
            final Set<PosixFilePermission> perms = posix.permissions();
            final StringBuilder sb = new StringBuilder(10);
            sb.append(directory ? 'd' : '-');
            sb.append(perms.contains(PosixFilePermission.OWNER_READ) ? 'r' : '-');
            sb.append(perms.contains(PosixFilePermission.OWNER_WRITE) ? 'w' : '-');
            sb.append(perms.contains(PosixFilePermission.OWNER_EXECUTE) ? 'x' : '-');
            sb.append(perms.contains(PosixFilePermission.GROUP_READ) ? 'r' : '-');
            sb.append(perms.contains(PosixFilePermission.GROUP_WRITE) ? 'w' : '-');
            sb.append(perms.contains(PosixFilePermission.GROUP_EXECUTE) ? 'x' : '-');
            sb.append(perms.contains(PosixFilePermission.OTHERS_READ) ? 'r' : '-');
            sb.append(perms.contains(PosixFilePermission.OTHERS_WRITE) ? 'w' : '-');
            sb.append(perms.contains(PosixFilePermission.OTHERS_EXECUTE) ? 'x' : '-');
            return sb.toString();
        } catch (final UnsupportedOperationException | IOException ignored) {
            // not POSIX
        }

        if (directory) {
            return "D";
        }
        if (!basic.isRegularFile()) {
            return "";
        }
        return "A";
    }

    /**
     * Whether {@code path} is a regular file that a registered archive backend can open.
     * Delegates to {@link com.sysbot32.whistler.file_manager.archive.Archives#isOpenableArchive}
     * so UI checks stay aligned with {@link com.sysbot32.whistler.file_manager.archive.ArchiveOperations#supports}.
     */
    public static boolean looksLikeZip(final Path path) {
        return com.sysbot32.whistler.file_manager.archive.Archives.isOpenableArchive(path);
    }
}
