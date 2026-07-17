package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Recursive (flat) disk listing for 7zFM Flat View: every file/folder under root
 * appears as a single row with a relative path name.
 */
@UtilityClass
public final class FlatListing {
    public static List<FileEntry> list(final Path root, final boolean showHidden) throws IOException {
        final Path base = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(base)) {
            throw new IOException("Not a directory: " + base);
        }
        final List<FileEntry> entries = new ArrayList<>();
        if (base.getParent() != null) {
            entries.add(FileEntry.upLink());
        }

        final List<FileEntry> found = new ArrayList<>();
        Files.walkFileTree(base, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                if (dir.equals(base)) {
                    return FileVisitResult.CONTINUE;
                }
                if (!showHidden && isHiddenName(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                found.add(toRelativeEntry(base, dir, true, attrs));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (!showHidden && isHiddenName(file)) {
                    return FileVisitResult.CONTINUE;
                }
                found.add(toRelativeEntry(base, file, false, attrs));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        found.sort(Comparator
                .comparing(FileEntry::directory).reversed()
                .thenComparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER));
        entries.addAll(found);
        return entries;
    }

    private static FileEntry toRelativeEntry(
            final Path base,
            final Path path,
            final boolean directory,
            final BasicFileAttributes attrs
    ) {
        String rel = base.relativize(path).toString().replace('\\', '/');
        if (directory && !rel.endsWith("/")) {
            // display without trailing slash in name; directory flag carries type
        }
        final long size = directory ? 0L : attrs.size();
        final Instant modified = attrs.lastModifiedTime() != null
                ? attrs.lastModifiedTime().toInstant()
                : null;
        final Instant created = attrs.creationTime() != null
                ? attrs.creationTime().toInstant()
                : null;
        final Instant accessed = attrs.lastAccessTime() != null
                ? attrs.lastAccessTime().toInstant()
                : null;
        final String attr = directory ? "D" : "A";
        return new FileEntry(rel, directory, false, size, null, modified, created, accessed, attr);
    }

    private static boolean isHiddenName(final Path path) {
        final String name = path.getFileName() != null ? path.getFileName().toString() : "";
        if (name.startsWith(".")) {
            return true;
        }
        try {
            return Files.isHidden(path);
        } catch (final IOException e) {
            return false;
        }
    }

    /** Resolve a flat-view relative name under the current disk folder. */
    public static Path resolveRelative(final Path root, final String relativeName) {
        String rel = relativeName.replace('\\', '/');
        while (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        Path p = root.toAbsolutePath().normalize();
        for (final String part : rel.split("/")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                throw new IllegalArgumentException("Illegal relative path: " + relativeName);
            }
            p = p.resolve(part);
        }
        return p.normalize();
    }
}
