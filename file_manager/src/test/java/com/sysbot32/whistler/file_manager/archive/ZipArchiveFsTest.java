package com.sysbot32.whistler.file_manager.archive;

import com.sysbot32.whistler.file_manager.model.BrowseLocation;
import com.sysbot32.whistler.file_manager.model.FileEntry;
import com.sysbot32.whistler.file_manager.ops.CollisionPolicy;
import com.sysbot32.whistler.file_manager.ops.TransferControl;
import com.sysbot32.whistler.file_manager.ops.TransferTargets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipArchiveFsTest {
    @TempDir
    Path tempDir;

    @Test
    void createListExtractRoundTrip() throws Exception {
        final Path srcDir = Files.createDirectory(this.tempDir.resolve("src"));
        Files.writeString(srcDir.resolve("hello.txt"), "hello world");
        final Path nested = Files.createDirectory(srcDir.resolve("nested"));
        Files.writeString(nested.resolve("inner.txt"), "inner");

        final Path zip = this.tempDir.resolve("out.zip");
        ZipArchiveFs.createZip(zip, List.of(srcDir));

        final List<FileEntry> root = ZipArchiveFs.list(zip, "");
        assertTrue(root.stream().anyMatch(FileEntry::parentLink));
        assertTrue(root.stream().anyMatch(e -> "src".equals(e.name()) && e.directory()));

        final List<FileEntry> inside = ZipArchiveFs.list(zip, "src/");
        assertTrue(inside.stream().anyMatch(e -> "hello.txt".equals(e.name()) && !e.directory()));
        assertTrue(inside.stream().anyMatch(e -> "nested".equals(e.name()) && e.directory()));

        final Path extractTo = Files.createDirectory(this.tempDir.resolve("extracted"));
        final int count = ZipArchiveFs.extract(zip, List.of(), extractTo, true);
        assertTrue(count >= 2);
        assertEquals("hello world", Files.readString(extractTo.resolve("src/hello.txt")));
        assertEquals("inner", Files.readString(extractTo.resolve("src/nested/inner.txt")));

        assertEquals(2, ZipArchiveFs.test(zip));
    }

    @Test
    void partialExtractSelectedFolder() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        final Path bDir = Files.createDirectory(this.tempDir.resolve("b"));
        Files.writeString(a, "A");
        Files.writeString(bDir.resolve("only.txt"), "B");

        final Path zip = this.tempDir.resolve("part.zip");
        ZipArchiveFs.createZip(zip, List.of(a, bDir));

        final Path out = Files.createDirectory(this.tempDir.resolve("part-out"));
        final int count = ZipArchiveFs.extract(zip, List.of("b/"), out, false);
        assertEquals(1, count);
        assertTrue(Files.exists(out.resolve("b/only.txt")));
        assertTrue(Files.notExists(out.resolve("a.txt")));
    }

    @Test
    void partialExtractSingleFile() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        final Path b = this.tempDir.resolve("b.txt");
        Files.writeString(a, "AAA");
        Files.writeString(b, "BBB");
        final Path zip = this.tempDir.resolve("files.zip");
        ZipArchiveFs.createZip(zip, List.of(a, b));

        final Path out = Files.createDirectory(this.tempDir.resolve("one-out"));
        final int count = ZipArchiveFs.extract(zip, List.of("a.txt"), out, false);
        assertEquals(1, count);
        assertEquals("AAA", Files.readString(out.resolve("a.txt")));
        assertTrue(Files.notExists(out.resolve("b.txt")));
    }

    @Test
    void extractNestedFolderViaPlanPrefix() throws Exception {
        final Path root = Files.createDirectory(this.tempDir.resolve("pack"));
        final Path docs = Files.createDirectory(root.resolve("docs"));
        Files.writeString(docs.resolve("readme.txt"), "hi");
        Files.writeString(root.resolve("skip.txt"), "no");
        final Path zip = this.tempDir.resolve("nested.zip");
        ZipArchiveFs.createZip(zip, List.of(root));

        final BrowseLocation loc = BrowseLocation.zip(zip, "pack/docs/");
        final TransferTargets.ExtractPlan plan = TransferTargets.planZipExtract(loc, List.of());
        final Path out = Files.createDirectory(this.tempDir.resolve("nested-out"));
        final int count = ZipArchiveFs.extract(zip, plan.internalPaths(), out, plan.extractAll());
        assertEquals(1, count);
        assertEquals("hi", Files.readString(out.resolve("pack/docs/readme.txt")));
        assertTrue(Files.notExists(out.resolve("pack/skip.txt")));
    }

    @Test
    void resolveSafeBlocksZipSlip() throws Exception {
        final Path dest = Files.createDirectories(this.tempDir.resolve("safe")).toAbsolutePath().normalize();
        assertThrows(Exception.class, () -> ZipArchiveFs.resolveSafe(dest, "../evil.txt"));
        assertThrows(Exception.class, () -> ZipArchiveFs.resolveSafe(dest, "foo/../../evil.txt"));
        final Path absoluteStyle = ZipArchiveFs.resolveSafe(dest, "/tmp/absolute");
        assertTrue(absoluteStyle.startsWith(dest));
        assertEquals(dest.resolve("tmp/absolute"), absoluteStyle);
    }

    @Test
    void resolveSafeAllowsNormalPaths() throws Exception {
        final Path dest = Files.createDirectories(this.tempDir.resolve("safe")).toAbsolutePath().normalize();
        final Path target = ZipArchiveFs.resolveSafe(dest, "dir/file.txt");
        assertTrue(target.startsWith(dest));
        assertEquals(dest.resolve("dir/file.txt"), target);
    }

    @Test
    void extractRejectsZipSlipEntries() throws Exception {
        final Path evilZip = this.tempDir.resolve("evil.zip");
        try (final var out = Files.newOutputStream(evilZip);
             final var zip = new java.util.zip.ZipOutputStream(out)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("../evil.txt"));
            zip.write("pwned".getBytes());
            zip.closeEntry();
        }

        final Path out = Files.createDirectory(this.tempDir.resolve("extract-evil"));
        assertThrows(Exception.class, () -> ZipArchiveFs.extract(evilZip, List.of(), out, true));
        assertTrue(Files.notExists(this.tempDir.resolve("evil.txt")));
    }

    @Test
    void deleteRenameMkdirAndCopyMoveRoundTrip() throws Exception {
        final Path a = this.tempDir.resolve("keep.txt");
        final Path b = this.tempDir.resolve("gone.txt");
        final Path folder = Files.createDirectory(this.tempDir.resolve("box"));
        Files.writeString(a, "KEEP");
        Files.writeString(b, "GONE");
        Files.writeString(folder.resolve("inner.txt"), "INNER");

        final Path zip = this.tempDir.resolve("mut.zip");
        ZipArchiveFs.createZip(zip, List.of(a, b, folder));

        // delete gone.txt
        final int removed = ZipArchiveFs.deleteEntries(zip, List.of("gone.txt"));
        assertTrue(removed >= 1);
        assertTrue(ZipArchiveFs.list(zip, "").stream().noneMatch(e -> "gone.txt".equals(e.name())));
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "keep.txt".equals(e.name())));

        // rename keep.txt → kept.txt
        ZipArchiveFs.renameEntry(zip, "keep.txt", "kept.txt");
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "kept.txt".equals(e.name())));
        assertTrue(ZipArchiveFs.list(zip, "").stream().noneMatch(e -> "keep.txt".equals(e.name())));

        // mkdir inside zip
        ZipArchiveFs.mkdir(zip, "", "fresh");
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "fresh".equals(e.name()) && e.directory()));

        // copy folder out
        final Path copyOut = Files.createDirectory(this.tempDir.resolve("copy-out"));
        final int copied = ZipArchiveFs.copyOrMoveToDisk(zip, List.of("box/"), copyOut, false, null);
        assertTrue(copied >= 1);
        assertEquals("INNER", Files.readString(copyOut.resolve("box/inner.txt")));
        // still in archive after copy
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "box".equals(e.name()) && e.directory()));

        // move kept.txt out
        final Path moveOut = Files.createDirectory(this.tempDir.resolve("move-out"));
        final int moved = ZipArchiveFs.copyOrMoveToDisk(zip, List.of("kept.txt"), moveOut, true, null);
        assertEquals(1, moved);
        assertEquals("KEEP", Files.readString(moveOut.resolve("kept.txt")));
        assertTrue(ZipArchiveFs.list(zip, "").stream().noneMatch(e -> "kept.txt".equals(e.name())));

        // rename folder leaf
        ZipArchiveFs.renameEntry(zip, "box/", "crate");
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "crate".equals(e.name()) && e.directory()));
        assertTrue(ZipArchiveFs.list(zip, "crate/").stream().anyMatch(e -> "inner.txt".equals(e.name())));
    }

    @Test
    void extractCancelStopsWork() throws Exception {
        final Path a = this.tempDir.resolve("big-a.txt");
        final Path b = this.tempDir.resolve("big-b.txt");
        Files.writeString(a, "A");
        Files.writeString(b, "B");
        final Path zip = this.tempDir.resolve("cancel.zip");
        ZipArchiveFs.createZip(zip, List.of(a, b));

        final TransferControl control = new TransferControl();
        control.cancel();
        final Path out = Files.createDirectory(this.tempDir.resolve("cancel-out"));
        assertThrows(Exception.class, () -> ZipArchiveFs.extract(zip, List.of(), out, true, control));
    }

    @Test
    void extractSkipExistingLeavesOldContent() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        Files.writeString(a, "NEW");
        final Path zip = this.tempDir.resolve("skip.zip");
        ZipArchiveFs.createZip(zip, List.of(a));

        final Path out = Files.createDirectory(this.tempDir.resolve("skip-out"));
        Files.writeString(out.resolve("a.txt"), "OLD");
        final ArchiveExtractOptions opts = new ArchiveExtractOptions(
                ArchiveExtractOptions.PathMode.FULL_PATHNAMES,
                ArchiveExtractOptions.OverwriteMode.SKIP,
                false
        );
        final int count = ZipArchiveFs.extract(zip, List.of(), out, true, null, opts);
        assertEquals(0, count);
        assertEquals("OLD", Files.readString(out.resolve("a.txt")));
    }

    @Test
    void extractAutoRenameAvoidsCollision() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        Files.writeString(a, "NEW");
        final Path zip = this.tempDir.resolve("rename.zip");
        ZipArchiveFs.createZip(zip, List.of(a));

        final Path out = Files.createDirectory(this.tempDir.resolve("rename-out"));
        Files.writeString(out.resolve("a.txt"), "OLD");
        final ArchiveExtractOptions opts = new ArchiveExtractOptions(
                ArchiveExtractOptions.PathMode.FULL_PATHNAMES,
                ArchiveExtractOptions.OverwriteMode.AUTO_RENAME,
                false
        );
        final int count = ZipArchiveFs.extract(zip, List.of(), out, true, null, opts);
        assertEquals(1, count);
        assertEquals("OLD", Files.readString(out.resolve("a.txt")));
        assertEquals("NEW", Files.readString(out.resolve("a_1.txt")));
    }

    @Test
    void extractNoPathnamesFlattens() throws Exception {
        final Path root = Files.createDirectory(this.tempDir.resolve("pack"));
        final Path nested = Files.createDirectory(root.resolve("deep"));
        Files.writeString(nested.resolve("leaf.txt"), "flat");
        final Path zip = this.tempDir.resolve("flat.zip");
        ZipArchiveFs.createZip(zip, List.of(root));

        final Path out = Files.createDirectory(this.tempDir.resolve("flat-out"));
        final ArchiveExtractOptions opts = new ArchiveExtractOptions(
                ArchiveExtractOptions.PathMode.NO_PATHNAMES,
                ArchiveExtractOptions.OverwriteMode.OVERWRITE,
                false
        );
        final int count = ZipArchiveFs.extract(zip, List.of(), out, true, null, opts);
        assertEquals(1, count);
        assertEquals("flat", Files.readString(out.resolve("leaf.txt")));
        assertTrue(Files.notExists(out.resolve("pack")));
    }

    @Test
    void extractEliminateSingleRootFolder() throws Exception {
        final Path root = Files.createDirectory(this.tempDir.resolve("onlyroot"));
        Files.writeString(root.resolve("readme.txt"), "hi");
        final Path zip = this.tempDir.resolve("root.zip");
        ZipArchiveFs.createZip(zip, List.of(root));

        final Path out = Files.createDirectory(this.tempDir.resolve("strip-out"));
        final ArchiveExtractOptions opts = new ArchiveExtractOptions(
                ArchiveExtractOptions.PathMode.FULL_PATHNAMES,
                ArchiveExtractOptions.OverwriteMode.OVERWRITE,
                true
        );
        final int count = ZipArchiveFs.extract(zip, List.of(), out, true, null, opts);
        assertEquals(1, count);
        assertEquals("hi", Files.readString(out.resolve("readme.txt")));
        assertTrue(Files.notExists(out.resolve("onlyroot/readme.txt")));
    }

    @Test
    void extractAskSkipLeavesOld() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        Files.writeString(a, "NEW");
        final Path zip = this.tempDir.resolve("ask.zip");
        ZipArchiveFs.createZip(zip, List.of(a));

        final Path out = Files.createDirectory(this.tempDir.resolve("ask-out"));
        Files.writeString(out.resolve("a.txt"), "OLD");
        final ArchiveExtractOptions opts = new ArchiveExtractOptions(
                ArchiveExtractOptions.PathMode.FULL_PATHNAMES,
                ArchiveExtractOptions.OverwriteMode.ASK,
                false
        );
        final int count = ZipArchiveFs.extract(
                zip, List.of(), out, true, null, opts,
                (target, entry) -> CollisionPolicy.UserChoice.SKIP
        );
        assertEquals(0, count);
        assertEquals("OLD", Files.readString(out.resolve("a.txt")));
    }

    @Test
    void extractAskOverwriteAllReplaces() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        Files.writeString(a, "NEW");
        final Path zip = this.tempDir.resolve("ask2.zip");
        ZipArchiveFs.createZip(zip, List.of(a));

        final Path out = Files.createDirectory(this.tempDir.resolve("ask2-out"));
        Files.writeString(out.resolve("a.txt"), "OLD");
        final ArchiveExtractOptions opts = new ArchiveExtractOptions(
                ArchiveExtractOptions.PathMode.FULL_PATHNAMES,
                ArchiveExtractOptions.OverwriteMode.ASK,
                false
        );
        final int count = ZipArchiveFs.extract(
                zip, List.of(), out, true, null, opts,
                (target, entry) -> CollisionPolicy.UserChoice.OVERWRITE_ALL
        );
        assertEquals(1, count);
        assertEquals("NEW", Files.readString(out.resolve("a.txt")));
    }

    @Test
    void addToExistingZipWithCancelCheck() throws Exception {
        final Path a = Files.writeString(this.tempDir.resolve("old.txt"), "old");
        final Path zip = this.tempDir.resolve("add.zip");
        ZipArchiveFs.createZip(zip, List.of(a));
        final Path b = Files.writeString(this.tempDir.resolve("new.txt"), "new");
        final TransferControl control = new TransferControl();
        ZipArchiveFs.addToZip(zip, "", List.of(b), control);
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "new.txt".equals(e.name())));
        assertTrue(ZipArchiveFs.list(zip, "").stream().anyMatch(e -> "old.txt".equals(e.name())));
        assertEquals(2, ZipArchiveFs.test(zip));

        control.cancel();
        final Path c = Files.writeString(this.tempDir.resolve("c.txt"), "c");
        assertThrows(Exception.class, () -> ZipArchiveFs.addToZip(zip, "", List.of(c), control));
    }
}
