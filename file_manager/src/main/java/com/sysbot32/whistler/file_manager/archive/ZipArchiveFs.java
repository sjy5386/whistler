package com.sysbot32.whistler.file_manager.archive;

import com.sysbot32.whistler.file_manager.model.FileEntry;
import com.sysbot32.whistler.file_manager.ops.TransferControl;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP as a virtual folder: list, navigate, extract, create, and in-archive mutations.
 * Blocks zip-slip on extract. Mutations rewrite via a temp file then replace.
 */
@UtilityClass
public final class ZipArchiveFs {
    private static final int BUFFER_SIZE = 64 * 1024;
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
                    children.putIfAbsent(remainder, toFileEntry(remainder, entry, false));
                } else if (slash == remainder.length() - 1) {
                    final String dirName = remainder.substring(0, slash);
                    children.putIfAbsent(dirName, toFileEntry(dirName, entry, true));
                } else {
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
        return extract(zipFile, internalPaths, destDir, extractAll, null, ArchiveExtractOptions.defaults());
    }

    public static int extract(
            final Path zipFile,
            final List<String> internalPaths,
            final Path destDir,
            final boolean extractAll,
            final TransferControl control
    ) throws IOException {
        return extract(zipFile, internalPaths, destDir, extractAll, control, ArchiveExtractOptions.defaults());
    }

    public static int extract(
            final Path zipFile,
            final List<String> internalPaths,
            final Path destDir,
            final boolean extractAll,
            final TransferControl control,
            final ArchiveExtractOptions options
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(destDir, "destDir");
        final ArchiveExtractOptions opts = options == null ? ArchiveExtractOptions.defaults() : options;
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
        int planned = 0;
        try (final ZipFile zip = new ZipFile(zipFile.toFile())) {
            final String stripRoot = opts.eliminateDuplicationOfRootFolder()
                    ? detectSingleRootFolder(zip, wanted, extractAll)
                    : "";

            final Enumeration<? extends ZipEntry> scan = zip.entries();
            while (scan.hasMoreElements()) {
                final ZipEntry entry = scan.nextElement();
                final String name = normalizeEntryName(entry.getName());
                if (name.isEmpty()) {
                    continue;
                }
                if (!extractAll && !matchesSelection(name, wanted)) {
                    continue;
                }
                final String mapped = mapExtractRelative(name, stripRoot, opts.pathMode());
                if (mapped == null || mapped.isEmpty()) {
                    continue;
                }
                if (!entry.isDirectory() && !name.endsWith("/")) {
                    planned++;
                }
            }
            if (control != null) {
                control.begin(planned);
            }

            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (control != null) {
                    control.throwIfCancelled();
                }
                final ZipEntry entry = entries.nextElement();
                final String name = normalizeEntryName(entry.getName());
                if (name.isEmpty()) {
                    continue;
                }
                if (!extractAll && !matchesSelection(name, wanted)) {
                    continue;
                }
                final boolean isDir = entry.isDirectory() || name.endsWith("/");
                final String mapped = mapExtractRelative(name, stripRoot, opts.pathMode());
                if (mapped == null || mapped.isEmpty()) {
                    continue;
                }
                // Flat mode: skip pure directory entries (no leaf name).
                if (isDir && opts.pathMode() == ArchiveExtractOptions.PathMode.NO_PATHNAMES) {
                    continue;
                }
                Path target = resolveSafe(destRoot, mapped);
                if (isDir) {
                    Files.createDirectories(target);
                } else {
                    if (Files.exists(target)) {
                        switch (opts.overwriteMode()) {
                            case SKIP -> {
                                if (control != null) {
                                    control.advance("Skipped " + name);
                                }
                                continue;
                            }
                            case AUTO_RENAME -> target = uniqueExtractTarget(target);
                            case OVERWRITE -> { /* replace below */ }
                        }
                    }
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
                    if (control != null) {
                        control.advance("Extracted " + name);
                    }
                }
            }
        }
        return count;
    }

    /**
     * When every matching entry lives under one top-level folder (and no root files),
     * return that folder prefix including trailing {@code /}; otherwise empty.
     */
    private static String detectSingleRootFolder(
            final ZipFile zip,
            final Set<String> wanted,
            final boolean extractAll
    ) {
        final Set<String> tops = new LinkedHashSet<>();
        boolean hasRootFile = false;
        final Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            final ZipEntry entry = entries.nextElement();
            final String name = normalizeEntryName(entry.getName());
            if (name.isEmpty()) {
                continue;
            }
            if (!extractAll && !matchesSelection(name, wanted)) {
                continue;
            }
            final int slash = name.indexOf('/');
            if (slash < 0) {
                if (!entry.isDirectory()) {
                    hasRootFile = true;
                }
                tops.add(name);
            } else {
                tops.add(name.substring(0, slash + 1));
            }
        }
        if (hasRootFile || tops.size() != 1) {
            return "";
        }
        final String only = tops.iterator().next();
        return only.endsWith("/") ? only : "";
    }

    /**
     * @return relative output path, empty if the entry is only the stripped root folder,
     *         or {@code null} if mapping discarded the path.
     */
    private static String mapExtractRelative(
            final String entryName,
            final String stripRoot,
            final ArchiveExtractOptions.PathMode pathMode
    ) {
        String relative = entryName;
        if (stripRoot != null && !stripRoot.isEmpty()) {
            if (!relative.startsWith(stripRoot)) {
                return relative;
            }
            relative = relative.substring(stripRoot.length());
            if (relative.isEmpty()) {
                return "";
            }
        }
        if (pathMode == ArchiveExtractOptions.PathMode.NO_PATHNAMES) {
            if (relative.endsWith("/")) {
                return "";
            }
            final int slash = relative.lastIndexOf('/');
            return slash < 0 ? relative : relative.substring(slash + 1);
        }
        return relative;
    }

    /** {@code file.txt} → {@code file_1.txt}, {@code file_2.txt}, … */
    private static Path uniqueExtractTarget(final Path target) {
        if (!Files.exists(target)) {
            return target;
        }
        final Path parent = target.getParent();
        final String fileName = target.getFileName() != null ? target.getFileName().toString() : "file";
        final int dot = fileName.lastIndexOf('.');
        final String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        final String ext = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 1; i < 10_000; i++) {
            final Path candidate = parent == null
                    ? Path.of(stem + "_" + i + ext)
                    : parent.resolve(stem + "_" + i + ext);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
        return target;
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
        createZip(zipFile, sources, null, ArchiveWriteOptions.defaults());
    }

    public static void createZip(
            final Path zipFile,
            final List<Path> sources,
            final TransferControl control
    ) throws IOException {
        createZip(zipFile, sources, control, ArchiveWriteOptions.defaults());
    }

    public static void createZip(
            final Path zipFile,
            final List<Path> sources,
            final TransferControl control,
            final ArchiveWriteOptions options
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(sources, "sources");
        final ArchiveWriteOptions opts = options == null ? ArchiveWriteOptions.defaults() : options;
        if (sources.isEmpty()) {
            throw new IOException("Nothing to add to archive");
        }
        final Path parent = zipFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (control != null) {
            control.begin(sources.size());
        }

        try (final OutputStream fileOut = Files.newOutputStream(zipFile);
             final ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
            zipOut.setLevel(opts.compressionLevel());
            for (final Path source : sources) {
                if (control != null) {
                    control.throwIfCancelled();
                }
                final Path abs = source.toAbsolutePath().normalize();
                if (!Files.exists(abs)) {
                    throw new IOException("Missing: " + abs);
                }
                final String baseName = abs.getFileName() != null ? abs.getFileName().toString() : "item";
                if (Files.isDirectory(abs)) {
                    addDirectory(zipOut, abs, baseName, control);
                } else {
                    addFile(zipOut, abs, baseName);
                }
                if (control != null) {
                    control.advance("Added " + baseName);
                }
            }
        }
    }

    /**
     * Add disk files/directories into an existing zip under {@code destInternalDir}
     * (empty = archive root). Existing entries with the same paths are replaced
     * according to {@link ArchiveWriteOptions#updateMode()}.
     */
    public static void addToZip(
            final Path zipFile,
            final String destInternalDir,
            final List<Path> sources,
            final TransferControl control
    ) throws IOException {
        addToZip(zipFile, destInternalDir, sources, control, ArchiveWriteOptions.defaults());
    }

    public static void addToZip(
            final Path zipFile,
            final String destInternalDir,
            final List<Path> sources,
            final TransferControl control,
            final ArchiveWriteOptions options
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(sources, "sources");
        final ArchiveWriteOptions opts = options == null ? ArchiveWriteOptions.defaults() : options;
        if (!Files.isRegularFile(zipFile)) {
            throw new IOException("Not an existing zip: " + zipFile);
        }
        if (sources.isEmpty()) {
            throw new IOException("Nothing to add to archive");
        }
        final String destPrefix = normalizeDirPrefix(destInternalDir);

        // Existing entry mtimes for update-mode decisions (file paths, no trailing slash)
        final Map<String, Long> existingMtimes = new LinkedHashMap<>();
        try (final ZipFile zip = new ZipFile(zipFile.toFile())) {
            final Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                final ZipEntry e = en.nextElement();
                final String n = normalizeEntryName(e.getName());
                if (n.isEmpty() || n.endsWith("/")) {
                    continue;
                }
                long ms = 0L;
                if (e.getLastModifiedTime() != null) {
                    ms = e.getLastModifiedTime().toMillis();
                }
                existingMtimes.put(n, ms);
            }
        }

        final List<Path> toWrite = new ArrayList<>();
        final Set<String> dropRoots = new LinkedHashSet<>();
        for (final Path source : sources) {
            final Path abs = source.toAbsolutePath().normalize();
            if (!Files.exists(abs)) {
                throw new IOException("Missing: " + abs);
            }
            final String baseName = abs.getFileName() != null ? abs.getFileName().toString() : "item";
            final String rootFile = destPrefix + baseName;
            final String rootDir = rootFile.endsWith("/") ? rootFile : rootFile + "/";
            final boolean existsInZip = existingMtimes.containsKey(rootFile)
                    || existingMtimes.keySet().stream().anyMatch(k -> k.startsWith(rootDir));

            switch (opts.updateMode()) {
                case FRESHEN -> {
                    if (!existsInZip) {
                        continue;
                    }
                }
                case UPDATE_AND_ADD -> {
                    if (existsInZip && Files.isRegularFile(abs)) {
                        final long archMs = existingMtimes.getOrDefault(rootFile, 0L);
                        final long srcMs = Files.getLastModifiedTime(abs).toMillis();
                        if (srcMs <= archMs) {
                            continue; // archive is same or newer
                        }
                    }
                }
                case ADD_AND_REPLACE -> {
                    // always write
                }
            }
            toWrite.add(abs);
            dropRoots.add(Files.isDirectory(abs) ? rootDir : rootFile);
            dropRoots.add(rootFile.endsWith("/") ? rootFile.substring(0, rootFile.length() - 1) : rootFile);
        }
        if (toWrite.isEmpty()) {
            if (control != null) {
                control.begin(0);
            }
            return;
        }
        if (control != null) {
            control.begin(toWrite.size());
            control.setDetail("Updating archive…");
        }
        rewriteZip(
                zipFile,
                (name, entry, zip) -> {
                    for (final String root : dropRoots) {
                        if (name.equals(root)
                                || name.startsWith(root.endsWith("/") ? root : root + "/")) {
                            return RewriteAction.ofDrop();
                        }
                    }
                    return RewriteAction.ofKeep(name);
                },
                zipOut -> {
                    zipOut.setLevel(opts.compressionLevel());
                    for (final Path abs : toWrite) {
                        if (control != null) {
                            control.throwIfCancelled();
                        }
                        final String baseName = abs.getFileName() != null
                                ? abs.getFileName().toString() : "item";
                        final String entryPrefix = destPrefix + baseName;
                        if (Files.isDirectory(abs)) {
                            addDirectory(zipOut, abs, entryPrefix, control);
                        } else {
                            addFile(zipOut, abs, entryPrefix);
                        }
                        if (control != null) {
                            control.advance("Added " + baseName);
                        }
                    }
                },
                control
        );
    }

    /**
     * Delete selected internal paths (files or directory trees) from the archive.
     */
    public static int deleteEntries(final Path zipFile, final List<String> internalPaths) throws IOException {
        return deleteEntries(zipFile, internalPaths, null);
    }

    public static int deleteEntries(
            final Path zipFile,
            final List<String> internalPaths,
            final TransferControl control
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        final Set<String> wanted = toWantedSet(internalPaths);
        if (wanted.isEmpty()) {
            return 0;
        }
        if (control != null) {
            control.begin(1);
            control.setDetail("Deleting from archive…");
        }
        final int[] removed = {0};
        rewriteZip(zipFile, (name, entry, zip) -> {
            if (matchesSelection(name, wanted)) {
                removed[0]++;
                return RewriteAction.ofDrop();
            }
            return RewriteAction.ofKeep(name);
        }, null, control);
        if (control != null) {
            control.advance("Deleted " + removed[0] + " entr(y/ies)");
        }
        return removed[0];
    }

    /**
     * Rename a single entry (file or folder leaf) inside the archive.
     * {@code fromInternal} is the full internal path; {@code newLeafName} has no slashes.
     */
    public static void renameEntry(
            final Path zipFile,
            final String fromInternal,
            final String newLeafName
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(fromInternal, "fromInternal");
        Objects.requireNonNull(newLeafName, "newLeafName");
        if (newLeafName.isBlank() || newLeafName.contains("/") || newLeafName.contains("\\")
                || "..".equals(newLeafName)) {
            throw new IOException("Invalid name: " + newLeafName);
        }
        final String from = normalizeEntryName(fromInternal);
        if (from.isEmpty()) {
            throw new IOException("Empty archive path");
        }
        final boolean wasDir = from.endsWith("/") || fromInternal.endsWith("/");
        final String fromKey = wasDir && !from.endsWith("/") ? from + "/" : from;
        final String fromNoSlash = fromKey.endsWith("/") ? fromKey.substring(0, fromKey.length() - 1) : fromKey;

        final int slash = fromNoSlash.lastIndexOf('/');
        final String parent = slash < 0 ? "" : fromNoSlash.substring(0, slash + 1);
        final String toNoSlash = parent + newLeafName;
        final String toDir = toNoSlash + "/";

        final boolean[] found = {false};
        rewriteZip(zipFile, (name, entry, zip) -> {
            if (name.equals(fromNoSlash) || name.equals(fromKey)
                    || (wasDir && name.startsWith(fromKey.endsWith("/") ? fromKey : fromKey + "/"))
                    || (!wasDir && name.startsWith(fromNoSlash + "/"))) {
                found[0] = true;
                String mapped;
                if (name.equals(fromNoSlash) || name.equals(fromKey)) {
                    mapped = (entry.isDirectory() || name.endsWith("/")) ? toDir : toNoSlash;
                } else {
                    final String prefix = fromKey.endsWith("/") ? fromKey : fromNoSlash + "/";
                    final String rest = name.startsWith(prefix) ? name.substring(prefix.length()) : name;
                    mapped = toDir + rest;
                }
                return RewriteAction.ofKeep(mapped);
            }
            return RewriteAction.ofKeep(name);
        }, null, null);
        if (!found[0]) {
            throw new IOException("Entry not found: " + fromInternal);
        }
    }

    /**
     * Create an empty directory entry under {@code parentInternalDir}.
     */
    public static void mkdir(final Path zipFile, final String parentInternalDir, final String folderName)
            throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(folderName, "folderName");
        if (folderName.isBlank() || folderName.contains("/") || folderName.contains("\\")
                || "..".equals(folderName)) {
            throw new IOException("Invalid folder name: " + folderName);
        }
        final String parent = normalizeDirPrefix(parentInternalDir);
        final String dirEntry = parent + folderName + "/";

        // Fail if something already occupies that name
        try (final ZipFile zip = new ZipFile(zipFile.toFile())) {
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                final String name = normalizeEntryName(entries.nextElement().getName());
                if (name.equals(dirEntry) || name.equals(parent + folderName)
                        || name.startsWith(dirEntry)) {
                    throw new IOException("Already exists: " + folderName);
                }
            }
        }

        rewriteZip(zipFile, (name, entry, zip) -> RewriteAction.ofKeep(name), zos -> {
            final ZipEntry dirZip = new ZipEntry(dirEntry);
            dirZip.setLastModifiedTime(FileTime.from(Instant.now()));
            zos.putNextEntry(dirZip);
            zos.closeEntry();
        }, null);
    }

    /**
     * Copy selected zip entries out to a disk directory (same as partial extract).
     * When {@code move} is true, also delete them from the archive.
     */
    public static int copyOrMoveToDisk(
            final Path zipFile,
            final List<String> internalPaths,
            final Path destDir,
            final boolean move,
            final TransferControl control
    ) throws IOException {
        final int count = extract(zipFile, internalPaths, destDir, false, control);
        if (move && !internalPaths.isEmpty()) {
            deleteEntries(zipFile, internalPaths, control);
        }
        return count;
    }

    private static Set<String> toWantedSet(final List<String> internalPaths) {
        final Set<String> wanted = new LinkedHashSet<>();
        if (internalPaths == null) {
            return wanted;
        }
        for (final String p : internalPaths) {
            final String n = normalizeEntryName(p);
            if (!n.isEmpty()) {
                wanted.add(n);
            }
        }
        return wanted;
    }

    @FunctionalInterface
    private interface EntryMapper {
        RewriteAction map(String normalizedName, ZipEntry entry, ZipFile zip) throws IOException;
    }

    @FunctionalInterface
    private interface EntryAppender {
        void append(ZipOutputStream zos) throws IOException;
    }

    private record RewriteAction(boolean shouldDrop, String newName) {
        static RewriteAction ofDrop() {
            return new RewriteAction(true, null);
        }

        static RewriteAction ofKeep(final String name) {
            return new RewriteAction(false, name);
        }
    }

    private static void rewriteZip(
            final Path zipFile,
            final EntryMapper mapper,
            final EntryAppender appender,
            final TransferControl control
    ) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        final Path parent = zipFile.getParent();
        final Path temp = parent == null
                ? Files.createTempFile("zipmut-", ".zip")
                : Files.createTempFile(parent, "zipmut-", ".zip");
        try {
            try (final ZipFile zip = new ZipFile(zipFile.toFile());
                 final OutputStream fileOut = Files.newOutputStream(temp);
                 final ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
                final Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    if (control != null) {
                        control.throwIfCancelled();
                    }
                    final ZipEntry entry = entries.nextElement();
                    final String name = normalizeEntryName(entry.getName());
                    if (name.isEmpty()) {
                        continue;
                    }
                    final RewriteAction action = mapper.map(name, entry, zip);
                    if (action.shouldDrop()) {
                        continue;
                    }
                    String outName = action.newName() == null ? name : normalizeEntryName(action.newName());
                    if (outName.isEmpty()) {
                        continue;
                    }
                    final boolean dir = entry.isDirectory() || name.endsWith("/") || outName.endsWith("/");
                    if (dir && !outName.endsWith("/")) {
                        outName = outName + "/";
                    }
                    final ZipEntry outEntry = new ZipEntry(outName);
                    if (entry.getLastModifiedTime() != null) {
                        outEntry.setLastModifiedTime(entry.getLastModifiedTime());
                    }
                    zipOut.putNextEntry(outEntry);
                    if (!dir) {
                        try (final InputStream in = zip.getInputStream(entry)) {
                            in.transferTo(zipOut);
                        }
                    }
                    zipOut.closeEntry();
                }
                if (appender != null) {
                    appender.append(zipOut);
                }
            }
            try {
                Files.move(temp, zipFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException atomicFailed) {
                Files.move(temp, zipFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Exception ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (final IOException ignored) {
                // best-effort cleanup
            }
            if (ex instanceof IOException io) {
                throw io;
            }
            throw new IOException(ex);
        }
    }

    private static void addDirectory(
            final ZipOutputStream zipOut,
            final Path dir,
            final String entryPrefix,
            final TransferControl control
    ) throws IOException {
        final String dirEntry = entryPrefix.endsWith("/") ? entryPrefix : entryPrefix + "/";
        final ZipEntry dirZip = new ZipEntry(dirEntry);
        dirZip.setLastModifiedTime(FileTime.from(Files.getLastModifiedTime(dir).toInstant()));
        zipOut.putNextEntry(dirZip);
        zipOut.closeEntry();

        try (final var stream = Files.list(dir)) {
            for (final Path child : stream.sorted().toList()) {
                if (control != null) {
                    control.throwIfCancelled();
                }
                final String childName = dirEntry + child.getFileName().toString();
                if (Files.isDirectory(child)) {
                    addDirectory(zipOut, child, childName, control);
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

    /** Read archive-level comment (may be empty). */
    public static String getComment(final Path zipFile) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        try (final ZipFile zip = new ZipFile(zipFile.toFile())) {
            final String c = zip.getComment();
            return c == null ? "" : c;
        }
    }

    /**
     * Set archive-level comment by rewriting the zip (preserves entries).
     */
    public static void setComment(final Path zipFile, final String comment) throws IOException {
        Objects.requireNonNull(zipFile, "zipFile");
        final String text = comment == null ? "" : comment;
        final Path parent = zipFile.getParent();
        final Path temp = parent == null
                ? Files.createTempFile("zipcmt-", ".zip")
                : Files.createTempFile(parent, "zipcmt-", ".zip");
        try {
            try (final ZipFile zip = new ZipFile(zipFile.toFile());
                 final OutputStream fileOut = Files.newOutputStream(temp);
                 final ZipOutputStream zipOut = new ZipOutputStream(fileOut)) {
                zipOut.setComment(text);
                final Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    final String name = normalizeEntryName(entry.getName());
                    if (name.isEmpty()) {
                        continue;
                    }
                    final boolean dir = entry.isDirectory() || name.endsWith("/");
                    String outName = name;
                    if (dir && !outName.endsWith("/")) {
                        outName = outName + "/";
                    }
                    final ZipEntry outEntry = new ZipEntry(outName);
                    if (entry.getLastModifiedTime() != null) {
                        outEntry.setLastModifiedTime(entry.getLastModifiedTime());
                    }
                    zipOut.putNextEntry(outEntry);
                    if (!dir) {
                        try (final InputStream in = zip.getInputStream(entry)) {
                            in.transferTo(zipOut);
                        }
                    }
                    zipOut.closeEntry();
                }
            }
            try {
                Files.move(temp, zipFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException atomicFailed) {
                Files.move(temp, zipFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Exception ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (final IOException ignored) {
                // best-effort
            }
            if (ex instanceof IOException io) {
                throw io;
            }
            throw new IOException(ex);
        }
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
