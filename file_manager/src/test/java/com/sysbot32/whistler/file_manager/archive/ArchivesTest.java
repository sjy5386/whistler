package com.sysbot32.whistler.file_manager.archive;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the format-agnostic {@link Archives} / {@link ArchiveOperations} surface
 * on real temp zips (shipped zip backend only).
 */
class ArchivesTest {
    @TempDir
    Path tempDir;

    @Test
    void zipBackendIsRegistered() {
        assertTrue(Archives.find(ArchiveFormat.ZIP).isPresent());
        assertEquals(ArchiveFormat.ZIP, Archives.zip().format());
    }

    @Test
    void createAddExtractRoundTripThroughFacade() throws Exception {
        final Path a = Files.writeString(this.tempDir.resolve("a.txt"), "AAA");
        final Path b = Files.writeString(this.tempDir.resolve("b.txt"), "BBB");
        final Path zip = this.tempDir.resolve("pack.zip");

        final ArchiveOperations ops = Archives.forArchive(zip);
        ops.create(zip, List.of(a), null);

        assertTrue(ops.list(zip, "").stream().anyMatch(e -> "a.txt".equals(e.name())));
        assertTrue(ops.list(zip, "").stream().noneMatch(e -> "b.txt".equals(e.name())));

        // add-into-existing
        Archives.createOrAdd(zip, "", List.of(b), null);
        assertTrue(ops.list(zip, "").stream().anyMatch(e -> "b.txt".equals(e.name())));

        final Path out = Files.createDirectory(this.tempDir.resolve("out"));
        final int n = ops.extract(zip, List.of(), out, true, null);
        assertTrue(n >= 2);
        assertEquals("AAA", Files.readString(out.resolve("a.txt")));
        assertEquals("BBB", Files.readString(out.resolve("b.txt")));
    }

    @Test
    void addIntoExistingUnderInternalFolder() throws Exception {
        final Path seed = Files.writeString(this.tempDir.resolve("seed.txt"), "seed");
        final Path more = Files.writeString(this.tempDir.resolve("more.txt"), "more");
        final Path zip = this.tempDir.resolve("nested.zip");
        Archives.zip().create(zip, List.of(seed), null);
        Archives.zip().mkdir(zip, "", "docs");
        Archives.zip().add(zip, "docs/", List.of(more), null);

        assertTrue(Archives.zip().list(zip, "docs/").stream()
                .anyMatch(e -> "more.txt".equals(e.name())));
        final Path out = Files.createDirectory(this.tempDir.resolve("nested-out"));
        Archives.zip().extract(zip, List.of("docs/more.txt"), out, false, null);
        assertEquals("more", Files.readString(out.resolve("docs/more.txt")));
    }

    @Test
    void addReplacesCollidingEntry() throws Exception {
        final Path v1 = Files.writeString(this.tempDir.resolve("v.txt"), "one");
        final Path zip = this.tempDir.resolve("repl.zip");
        Archives.zip().create(zip, List.of(v1), null);
        final Path v2 = Files.writeString(this.tempDir.resolve("v2.txt"), "two");
        // same entry name "v.txt" via add of renamed content - use path with same name
        final Path sameName = this.tempDir.resolve("same").resolve("v.txt");
        Files.createDirectories(sameName.getParent());
        Files.writeString(sameName, "two");
        Archives.zip().add(zip, "", List.of(sameName), null);

        final Path out = Files.createDirectory(this.tempDir.resolve("repl-out"));
        Archives.zip().extract(zip, List.of("v.txt"), out, false, null);
        assertEquals("two", Files.readString(out.resolve("v.txt")));
    }

    @Test
    void interiorMutationThroughFacade() throws Exception {
        final Path f = Files.writeString(this.tempDir.resolve("x.txt"), "x");
        final Path zip = this.tempDir.resolve("mut.zip");
        Archives.zip().create(zip, List.of(f), null);
        Archives.zip().renameEntry(zip, "x.txt", "y.txt");
        assertTrue(Archives.zip().list(zip, "").stream().anyMatch(e -> "y.txt".equals(e.name())));
        Archives.zip().deleteEntries(zip, List.of("y.txt"), null);
        assertFalse(Archives.zip().list(zip, "").stream().anyMatch(e -> "y.txt".equals(e.name())));
    }

    @Test
    void unsupportedExtensionRejected() {
        final Path p = this.tempDir.resolve("nope.rar");
        assertThrows(IOException.class, () -> Archives.forArchive(p));
    }

    @Test
    void createOrAddCreatesWhenMissing() throws Exception {
        final Path f = Files.writeString(this.tempDir.resolve("c.txt"), "c");
        final Path zip = this.tempDir.resolve("new.zip");
        assertFalse(Files.exists(zip));
        Archives.createOrAdd(zip, "", List.of(f), null);
        assertTrue(Files.isRegularFile(zip));
        assertEquals(1, Archives.zip().test(zip));
    }
}
