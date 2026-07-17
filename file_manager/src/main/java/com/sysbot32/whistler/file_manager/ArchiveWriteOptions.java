package com.sysbot32.whistler.file_manager;

/**
 * Options for create/add archive (zip-only subset of 7zFM “Add to Archive” dialog).
 */
public record ArchiveWriteOptions(
        /** Deflate level 0–9 ({@link java.util.zip.Deflater}). */
        int compressionLevel,
        UpdateMode updateMode,
        boolean deleteFilesAfter
) {
    public enum UpdateMode {
        /** Always write sources; replace same paths (default). */
        ADD_AND_REPLACE,
        /** Add new; replace only when source is newer than archive entry. */
        UPDATE_AND_ADD,
        /** Only refresh paths that already exist in the archive. */
        FRESHEN
    }

    public ArchiveWriteOptions {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel 0–9: " + compressionLevel);
        }
        if (updateMode == null) {
            updateMode = UpdateMode.ADD_AND_REPLACE;
        }
    }

    public static ArchiveWriteOptions defaults() {
        return new ArchiveWriteOptions(6, UpdateMode.ADD_AND_REPLACE, false);
    }
}
