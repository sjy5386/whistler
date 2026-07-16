package com.sysbot32.whistler.file_manager;

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
}
