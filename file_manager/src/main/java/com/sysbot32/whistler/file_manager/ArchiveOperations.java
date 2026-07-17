package com.sysbot32.whistler.file_manager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Format-agnostic archive I/O surface used by the FM shell.
 * Zip is the only backend today; future formats implement this interface.
 */
public interface ArchiveOperations {
    ArchiveFormat format();

    /** Whether this backend claims {@code archiveFile} (by name/magic as implemented). */
    boolean supports(Path archiveFile);

    List<FileEntry> list(Path archive, String internalDir) throws IOException;

    /** Create a new archive containing {@code sources}. */
    void create(Path archive, List<Path> sources, TransferControl control) throws IOException;

    default void create(
            Path archive,
            List<Path> sources,
            TransferControl control,
            ArchiveWriteOptions options
    ) throws IOException {
        create(archive, sources, control);
    }

    /**
     * Add disk paths into an existing archive under {@code destInternalDir}
     * (empty = root).
     */
    void add(Path archive, String destInternalDir, List<Path> sources, TransferControl control)
            throws IOException;

    default void add(
            Path archive,
            String destInternalDir,
            List<Path> sources,
            TransferControl control,
            ArchiveWriteOptions options
    ) throws IOException {
        add(archive, destInternalDir, sources, control);
    }

    int extract(
            Path archive,
            List<String> internalPaths,
            Path destDir,
            boolean extractAll,
            TransferControl control
    ) throws IOException;

    int test(Path archive) throws IOException;

    int deleteEntries(Path archive, List<String> internalPaths, TransferControl control)
            throws IOException;

    void renameEntry(Path archive, String fromInternal, String newLeafName) throws IOException;

    void mkdir(Path archive, String parentInternalDir, String folderName) throws IOException;

    int copyOrMoveToDisk(
            Path archive,
            List<String> internalPaths,
            Path destDir,
            boolean move,
            TransferControl control
    ) throws IOException;

    String getComment(Path archive) throws IOException;

    void setComment(Path archive, String comment) throws IOException;
}
