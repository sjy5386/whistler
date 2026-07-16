package com.sysbot32.whistler.file_manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP as a virtual folder: list, navigate, extract, create.
 * Blocks zip-slip on extract.
 */
public final class ZipArchiveFs {
    private static final int BUFFER_SIZE = 64 * 1024;

    private ZipArchiveFs() {
    }

    public static List<FileEntry> list(final Path zipFile, final String internalDir) throws IOException {
        final String prefix = normalizeDirPrefix(internalDir);
        final Map<String, FileEntry> children = new LinkedHashMap<>();

        try (final ZipFile zip = new ZipFile(zipFile.toFile())) {
            final var entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String name = normalizeEntryName(entry.getName());
                if (name.isEmpty() || name.startsWith("../") || name.contains("/../") || name.equals("..")) {
                    continue;
                }
                if (!name.startsWith(prefix)) {
                    continue;
                }
                final String remainder = name.substring(prefix.length());
                if (remainder.isEmpty()) {
                    continue;
                }

                final int slash = remainder.indexOf('/');
                if (slash < 0) {
                    // file in this directory
                    children.putIfAbsent(remainder, toFileEntry(remainder, entry, false));
                } else if (slash == remainder.length() - 1) {
                    // explicit directory entry "foo/"
                    final String dirName = remainder.substring(0, slash);
                    children.putIfAbsent(dirName, toFileEntry(dirName, entry, true));
                } else {
                    // nested: "foo/bar" → child folder "foo"
                    final String dirName = remainder.substring(0, slash);
                    children.putIfAbsent(dirName, directoryPlaceholder(dirName, entry));
                }
            }
        }

        final List<FileEntry> result = new ArrayList<>();
        result.add(FileEntry.upLink());
        children.values().stream()
                .sorted(Comparator
                        .comparing(FileEntry::directory).reversed()
                        .thenComparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(result::add);
        return result;
    }

    private static FileEntry toFileEntry(final String name, final ZipEntry entry, final boolean directory) {
        final long size = directory ? 0L : entry.getSize();
        final long packed = directory ? 0L : entry.getCompressedSize();
        Instant modified = null;
        if (entry.getLastModifiedTime() != null) {
            modified = entry.getLastModifiedTime().toInstant();
        }
        final String attr = directory ? "D" : "A";
        return new FileEntry(
                name,
                directory,
                false,
                size < 0 ? 0L : size,
                packed < 0 ? null : packed,
                modified,
                null,
                null,
                attr
        );
    }

    private static FileEntry directoryPlaceholder(final String name, final ZipEntry sample) {
        Instant modified = null;
        if (sample.getLastModifiedTime() != null) {
            modified = sample.getLastModifiedTime().toInstant();
        }
        return new FileEntry(name, true, false, 0L, 0L, modified, null, null, "D");
    }

    /**
     * Extract selected internal paths (files or directories) to {@code destDir}.
     * Empty selection extracts the whole archive when {@code extractAll} is true.
     */
    public static int extract(
            final Path zipFile,
            final List<String> internalPaths,
            final Path destDir,
            final boolean extractAll
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(destDir, "destDir");
        Files.createDirectories(destDir);
        final Path destRoot = destDir.toAbsolutePath().normalize();

        final Set<String> wanted = new LinkedHashSet<>();
        if (!extractAll) {
            for (final String p : internalPaths) {
                final String n = normalizeEntryName(p);
                if (!n.isEmpty()) {
                    wanted.add(n);
                }
            }
            if (wanted.isEmpty()) {
                return 0;
            }
        }

        int count = 0;
        try (final ZipFile zip = new ZipFile(zipFile.toFile())) {
            final var entries = zip.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String name = normalizeEntryName(entry.getName());
                if (name.isEmpty()) {
                    continue;
                }
                if (!extractAll && !matchesSelection(name, wanted)) {
                    continue;
                }
                final Path target = resolveSafe(destRoot, name);
                if (entry.isDirectory() || name.endsWith("/")) {
                    Files.createDirectories(target);
                } else {
                    final Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (final InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (entry.getLastModifiedTime() != null) {
                        Files.setLastModifiedTime(target, entry.getLastModifiedTime());
                    }
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean matchesSelection(final String entryName, final Set<String> wanted) {
        for (final String w : wanted) {
            if (entryName.equals(w)) {
                return true;
            }
            final String dirPrefix = w.endsWith("/") ? w : w + "/";
            if (entryName.startsWith(dirPrefix) || (w.endsWith("/") && entryName.equals(w.substring(0, w.length() - 1)))) {
                return true;
            }
            // selected directory without trailing slash
            if (!w.endsWith("/") && entryName.startsWith(w + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a new zip archive containing the given files/directories.
     */
    public static void createZip(final Path zipFile, final List<Path> sources) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(sources, "sources");
        if (sources.isEmpty()) {
            throw new IOException("Nothing to add to archive");
        }
        final Path parent = zipFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (final OutputStream fileOut = Files.newOutputStream(zipFile);
             final ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            for (final Path source : sources) {
                final Path abs = source.toAbsolutePath().normalize();
                if (!Files.exists(abs)) {
                    throw new IOException("Missing: " + abs);
                }
                final String baseName = abs.getFileName() != null ? abs.getFileName().toString() : "item";
                if (Files.isDirectory(abs)) {
                    addDirectory(zipOut, abs, baseName);
                } else {
                    addFile(zipOut, abs, baseName);
                }
            }
        }
    }

    private static void addDirectory(final ZipOutputStream zipOut, final Path dir, final String entryPrefix)
            throws IOException {
        final String dirEntry = entryPrefix.endsWith("/") ? entryPrefix : entryPrefix + "/";
        final ZipEntry dirZip = new ZipEntry(dirEntry);
        dirZip.setLastModifiedTime(FileTime.from(Files.getLastModifiedTime(dir).toInstant()));
        zipOut.putNextEntry(dirZip);
        zipOut.closeEntry();

        try (final var stream = Files.list(dir)) {
            for (final Path child : stream.sorted().toList()) {
                final String childName = dirEntry + child.getFileName().toString();
                if (Files.isDirectory(child)) {
                    addDirectory(zipOut, child, childName);
                } else {
                    addFile(zipOut, child, childName);
                }
            }
        }
    }

    private static void addFile(final ZipOutputStream zipOut, final Path file, final String entryName)
            throws IOException {
        final ZipEntry entry = new ZipEntry(entryName.replace('\\', '/'));
        entry.setLastModifiedTime(FileTime.from(Files.getLastModifiedTime(file).toInstant()));
        zipOut.putNextEntry(entry);
        try (final InputStream in = Files.newInputStream(file)) {
            in.transferTo(zipOut);
        }
        zipOut.closeEntry();
    }

    /**
     * Test that the archive can be fully read (basic integrity check).
     */
    public static int test(final Path zipFile) throws IOException {
        int count = 0;
        try (final InputStream fileIn = Files.newInputStream(zipFile);
             final ZipInputStream zipIn = new ZipInputStream(fileIn)) {
            ZipEntry entry;
            final byte[] buffer = new byte[BUFFER_SIZE];
            while ((entry = zipIn.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    while (zipIn.read(buffer) != -1) {
                        // drain
                    }
                    count++;
                }
                zipIn.closeEntry();
            }
        }
        return count;
    }

    /**
     * Resolve {@code entryName} under {@code destRoot}, rejecting zip-slip paths.
     */
    public static Path resolveSafe(final Path destRoot, final String entryName) throws IOException {
        final Path root = destRoot.toAbsolutePath().normalize();
        final String normalized = normalizeEntryName(entryName);
        if (normalized.isEmpty()) {
            throw new IOException("Empty archive entry name");
        }
        if (normalized.contains("..")) {
            // reject any .. segment
            final String[] parts = normalized.split("/");
            for (final String part : parts) {
                if ("..".equals(part)) {
                    throw new IOException("Blocked zip-slip path: " + entryName);
                }
            }
        }
        Path target = root;
        for (final String part : normalized.split("/")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            target = target.resolve(part);
        }
        target = target.normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Blocked zip-slip path: " + entryName);
        }
        return target;
    }

    static String normalizeEntryName(final String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\\', '/');
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        // strip trailing slash for comparison helpers; keep for directory detection at call sites
        return s;
    }

    static String normalizeDirPrefix(final String internalDir) {
        if (internalDir == null || internalDir.isBlank() || "/".equals(internalDir)) {
            return "";
        }
        String s = normalizeEntryName(internalDir);
        if (!s.isEmpty() && !s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }

}
