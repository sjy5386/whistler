package com.sysbot32.whistler.file_manager;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Current browse target: a disk directory, or a path inside a zip archive.
 * Display uses {@code archive.zip!/inner/path} for zip locations.
 */
@Getter
@Accessors(fluent = true)
public final class BrowseLocation {
    public enum Kind {
        DISK,
        ZIP
    }

    private final Kind kind;
    private final Path path;
    /** Normalized internal zip path: empty = root; folders end with {@code /}. */
    private final String zipInternalPath;

    private BrowseLocation(final Kind kind, final Path path, final String zipInternalPath) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.zipInternalPath = normalizeZipInternal(zipInternalPath);
    }

    public static BrowseLocation disk(final Path directory) {
        return new BrowseLocation(Kind.DISK, directory, "");
    }

    public static BrowseLocation zip(final Path zipFile, final String internalPath) {
        return new BrowseLocation(Kind.ZIP, zipFile, internalPath);
    }

    public boolean isDisk() {
        return this.kind == Kind.DISK;
    }

    public boolean isZip() {
        return this.kind == Kind.ZIP;
    }

    public String displayPath() {
        if (this.kind == Kind.DISK) {
            return this.path.toString();
        }
        final String inner = this.zipInternalPath.isEmpty() ? "/" : "/" + this.zipInternalPath;
        return this.path + "!" + inner;
    }

    public boolean canGoUp() {
        if (this.kind == Kind.ZIP) {
            return true;
        }
        final Path parent = this.path.getParent();
        return parent != null;
    }

    public BrowseLocation parent() {
        if (this.kind == Kind.ZIP) {
            if (this.zipInternalPath.isEmpty()) {
                final Path parentDir = this.path.getParent();
                if (parentDir == null) {
                    return disk(this.path.getRoot() != null ? this.path.getRoot() : this.path);
                }
                return disk(parentDir);
            }
            return zip(this.path, parentZipPath(this.zipInternalPath));
        }
        final Path parent = this.path.getParent();
        if (parent == null) {
            return this;
        }
        return disk(parent);
    }

    public BrowseLocation enterDirectory(final String name) {
        Objects.requireNonNull(name, "name");
        if ("..".equals(name)) {
            return parent();
        }
        if (this.kind == Kind.DISK) {
            return disk(this.path.resolve(name));
        }
        final String next = this.zipInternalPath + name + "/";
        return zip(this.path, next);
    }

    /** Resolve a child name to a disk path (disk locations only). */
    public Path resolveDiskChild(final String name) {
        if (this.kind != Kind.DISK) {
            throw new IllegalStateException("Not a disk location");
        }
        return this.path.resolve(name);
    }

    /** Full internal path for a child entry inside the current zip folder. */
    public String resolveZipChild(final String name, final boolean directory) {
        if (this.kind != Kind.ZIP) {
            throw new IllegalStateException("Not a zip location");
        }
        final String base = this.zipInternalPath + name;
        return directory ? base + "/" : base;
    }

    private static String normalizeZipInternal(final String raw) {
        if (raw == null || raw.isBlank() || "/".equals(raw)) {
            return "";
        }
        String s = raw.replace('\\', '/');
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        // Callers pass directory paths with a trailing '/'; leave file-like paths unchanged.
        return s;
    }

    private static String parentZipPath(final String internal) {
        String s = internal;
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        final int slash = s.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return s.substring(0, slash + 1);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BrowseLocation that)) {
            return false;
        }
        return this.kind == that.kind
                && Objects.equals(this.path, that.path)
                && Objects.equals(this.zipInternalPath, that.zipInternalPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.kind, this.path, this.zipInternalPath);
    }

    @Override
    public String toString() {
        return displayPath();
    }
}
