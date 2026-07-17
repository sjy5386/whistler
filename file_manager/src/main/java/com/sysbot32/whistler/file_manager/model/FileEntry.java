package com.sysbot32.whistler.file_manager.model;


import java.time.Instant;
import java.util.Objects;

/**
 * One row in the file list (disk or archive).
 */
public record FileEntry(
        String name,
        boolean directory,
        boolean parentLink,
        long size,
        Long packedSize,
        Instant modified,
        Instant created,
        Instant accessed,
        String attributes
) {
    public FileEntry {
        Objects.requireNonNull(name, "name");
        if (attributes == null) {
            attributes = "";
        }
    }

    public static FileEntry upLink() {
        return new FileEntry("..", true, true, 0L, null, null, null, null, "");
    }

    public boolean isZipName() {
        return !directory && !parentLink && name.toLowerCase().endsWith(".zip");
    }
}
