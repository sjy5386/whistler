package com.sysbot32.whistler.file_manager.archive;

import com.sysbot32.whistler.file_manager.model.FileEntry;
import com.sysbot32.whistler.file_manager.ops.ExtractCollisionAsk;
import com.sysbot32.whistler.file_manager.ops.TransferControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Zip backend for {@link ArchiveOperations} — delegates to {@link ZipArchiveFs}.
 */
public final class ZipArchiveOperations implements ArchiveOperations {
    @Override
    public ArchiveFormat format() {
        return ArchiveFormat.ZIP;
    }

    @Override
    public boolean supports(final Path archiveFile) {
        if (archiveFile == null) {
            return false;
        }
        final Path name = archiveFile.getFileName();
        if (name == null) {
            return false;
        }
        final String n = name.toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".zip") || n.endsWith(".jar") || n.endsWith(".war");
    }

    @Override
    public List<FileEntry> list(final Path archive, final String internalDir) throws IOException {
        return ZipArchiveFs.list(archive, internalDir);
    }

    @Override
    public void create(final Path archive, final List<Path> sources, final TransferControl control)
            throws IOException {
        create(archive, sources, control, ArchiveWriteOptions.defaults());
    }

    @Override
    public void create(
            final Path archive,
            final List<Path> sources,
            final TransferControl control,
            final ArchiveWriteOptions options
    ) throws IOException {
        ZipArchiveFs.createZip(archive, sources, control, options);
    }

    @Override
    public void add(
            final Path archive,
            final String destInternalDir,
            final List<Path> sources,
            final TransferControl control
    ) throws IOException {
        add(archive, destInternalDir, sources, control, ArchiveWriteOptions.defaults());
    }

    @Override
    public void add(
            final Path archive,
            final String destInternalDir,
            final List<Path> sources,
            final TransferControl control,
            final ArchiveWriteOptions options
    ) throws IOException {
        ZipArchiveFs.addToZip(archive, destInternalDir, sources, control, options);
    }

    @Override
    public int extract(
            final Path archive,
            final List<String> internalPaths,
            final Path destDir,
            final boolean extractAll,
            final TransferControl control
    ) throws IOException {
        return extract(archive, internalPaths, destDir, extractAll, control, ArchiveExtractOptions.defaults());
    }

    @Override
    public int extract(
            final Path archive,
            final List<String> internalPaths,
            final Path destDir,
            final boolean extractAll,
            final TransferControl control,
            final ArchiveExtractOptions options
    ) throws IOException {
        return extract(archive, internalPaths, destDir, extractAll, control, options, null);
    }

    @Override
    public int extract(
            final Path archive,
            final List<String> internalPaths,
            final Path destDir,
            final boolean extractAll,
            final TransferControl control,
            final ArchiveExtractOptions options,
            final ExtractCollisionAsk ask
    ) throws IOException {
        return ZipArchiveFs.extract(archive, internalPaths, destDir, extractAll, control, options, ask);
    }

    @Override
    public int test(final Path archive) throws IOException {
        return ZipArchiveFs.test(archive);
    }

    @Override
    public int deleteEntries(
            final Path archive,
            final List<String> internalPaths,
            final TransferControl control
    ) throws IOException {
        return ZipArchiveFs.deleteEntries(archive, internalPaths, control);
    }

    @Override
    public void renameEntry(final Path archive, final String fromInternal, final String newLeafName)
            throws IOException {
        ZipArchiveFs.renameEntry(archive, fromInternal, newLeafName);
    }

    @Override
    public void mkdir(final Path archive, final String parentInternalDir, final String folderName)
            throws IOException {
        ZipArchiveFs.mkdir(archive, parentInternalDir, folderName);
    }

    @Override
    public int copyOrMoveToDisk(
            final Path archive,
            final List<String> internalPaths,
            final Path destDir,
            final boolean move,
            final TransferControl control
    ) throws IOException {
        return ZipArchiveFs.copyOrMoveToDisk(archive, internalPaths, destDir, move, control);
    }

    @Override
    public String getComment(final Path archive) throws IOException {
        return ZipArchiveFs.getComment(archive);
    }

    @Override
    public void setComment(final Path archive, final String comment) throws IOException {
        ZipArchiveFs.setComment(archive, comment);
    }
}
