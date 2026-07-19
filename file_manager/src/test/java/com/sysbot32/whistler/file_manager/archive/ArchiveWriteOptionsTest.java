package com.sysbot32.whistler.file_manager.archive;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchiveWriteOptionsTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsAndValidation() {
        final ArchiveWriteOptions d = ArchiveWriteOptions.defaults();
        assertEquals(6, d.compressionLevel());
        assertEquals(ArchiveWriteOptions.UpdateMode.ADD_AND_REPLACE, d.updateMode());
        assertEquals(ArchiveWriteOptions.PathMode.RELATIVE_PATHNAMES, d.pathMode());
        assertThrows(IllegalArgumentException.class,
                () -> ArchiveWriteOptions.of(10, ArchiveWriteOptions.UpdateMode.ADD_AND_REPLACE, false));
    }

    @Test
    void createWithStoreLevelProducesReadableZip() throws Exception {
        final Path f = Files.writeString(this.tempDir.resolve("a.txt"), "hello-store");
        final Path zip = this.tempDir.resolve("store.zip");
        final ArchiveWriteOptions opts = ArchiveWriteOptions.of(
                0, ArchiveWriteOptions.UpdateMode.ADD_AND_REPLACE, false);
        Archives.zip().create(zip, List.of(f), null, opts);
        assertTrue(Files.isRegularFile(zip));
        try (final ZipFile zf = new ZipFile(zip.toFile())) {
            assertEquals("hello-store", new String(zf.getInputStream(zf.getEntry("a.txt")).readAllBytes()));
        }
    }

    @Test
    void freshenSkipsNewNames() throws Exception {
        final Path a = Files.writeString(this.tempDir.resolve("a.txt"), "A1");
        final Path zip = this.tempDir.resolve("f.zip");
        Archives.zip().create(zip, List.of(a), null);
        final Path b = Files.writeString(this.tempDir.resolve("b.txt"), "B");
        final ArchiveWriteOptions freshen = ArchiveWriteOptions.of(
                6, ArchiveWriteOptions.UpdateMode.FRESHEN, false);
        Archives.zip().add(zip, "", List.of(b), null, freshen);
        assertTrue(Archives.zip().list(zip, "").stream().noneMatch(e -> "b.txt".equals(e.name())));
        assertTrue(Archives.zip().list(zip, "").stream().anyMatch(e -> "a.txt".equals(e.name())));
    }

    @Test
    void noPathnamesFlattensDirectoryOnCreate() throws Exception {
        final Path root = Files.createDirectory(this.tempDir.resolve("tree"));
        final Path nested = Files.createDirectory(root.resolve("deep"));
        Files.writeString(nested.resolve("leaf.txt"), "L");
        final Path zip = this.tempDir.resolve("flat.zip");
        final ArchiveWriteOptions opts = new ArchiveWriteOptions(
                6,
                ArchiveWriteOptions.UpdateMode.ADD_AND_REPLACE,
                ArchiveWriteOptions.PathMode.NO_PATHNAMES,
                false
        );
        Archives.zip().create(zip, List.of(root), null, opts);
        try (final ZipFile zf = new ZipFile(zip.toFile())) {
            assertTrue(zf.getEntry("leaf.txt") != null);
            assertTrue(zf.getEntry("tree/deep/leaf.txt") == null);
        }
    }
}
