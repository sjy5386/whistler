package com.sysbot32.whistler.file_manager.archive;


/**
 * Options for create/add archive (zip-only subset of 7zFM “Add to Archive” dialog).
 */
public record ArchiveWriteOptions(
        /** Deflate level 0–9 ({@link java.util.zip.Deflater}). */
        int compressionLevel,
        UpdateMode updateMode,
        PathMode pathMode,
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

    public enum PathMode {
        /**
         * Store each selected item under its leaf name (folder keeps internal structure).
         * Default — matches prior FM behaviour.
         */
        RELATIVE_PATHNAMES,
        /** Store only leaf file names (directories are flattened). */
        NO_PATHNAMES
    }

    public ArchiveWriteOptions {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("compressionLevel 0–9: " + compressionLevel);
        }
        if (updateMode == null) {
            updateMode = UpdateMode.ADD_AND_REPLACE;
        }
        if (pathMode == null) {
            pathMode = PathMode.RELATIVE_PATHNAMES;
        }
    }

    public static ArchiveWriteOptions defaults() {
        return new ArchiveWriteOptions(6, UpdateMode.ADD_AND_REPLACE, PathMode.RELATIVE_PATHNAMES, false);
    }

    /** Convenience when only level/update/delete change. */
    public static ArchiveWriteOptions of(
            final int compressionLevel,
            final UpdateMode updateMode,
            final boolean deleteFilesAfter
    ) {
        return new ArchiveWriteOptions(compressionLevel, updateMode, PathMode.RELATIVE_PATHNAMES, deleteFilesAfter);
    }
}
