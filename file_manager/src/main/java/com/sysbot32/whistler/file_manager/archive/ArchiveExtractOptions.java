package com.sysbot32.whistler.file_manager.archive;

/**
 * Options for extract (zip subset of 7zFM “Extract” dialog).
 */
public record ArchiveExtractOptions(
        PathMode pathMode,
        OverwriteMode overwriteMode,
        /* When the archive has a single top-level folder, strip that folder from output paths. */
        boolean eliminateDuplicationOfRootFolder
) {
    public enum PathMode {
        /** Keep internal directory structure (default). */
        FULL_PATHNAMES,
        /** Write only leaf file names into {@code destDir}. */
        NO_PATHNAMES
    }

    public enum OverwriteMode {
        /** Replace existing files without prompting (default; matches prior FM behaviour). */
        OVERWRITE,
        /** Leave existing targets; skip those entries. */
        SKIP,
        /** Write beside existing files as {@code name_1.ext}, {@code name_2.ext}, … */
        AUTO_RENAME
    }

    public ArchiveExtractOptions {
        if (pathMode == null) {
            pathMode = PathMode.FULL_PATHNAMES;
        }
        if (overwriteMode == null) {
            overwriteMode = OverwriteMode.OVERWRITE;
        }
    }

    public static ArchiveExtractOptions defaults() {
        return new ArchiveExtractOptions(PathMode.FULL_PATHNAMES, OverwriteMode.OVERWRITE, false);
    }
}
