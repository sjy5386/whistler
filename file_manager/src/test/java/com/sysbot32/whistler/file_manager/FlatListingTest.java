package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlatListingTest {
    @TempDir
    Path tempDir;

    @Test
    void listsNestedFilesWithRelativeNames() throws Exception {
        final Path sub = Files.createDirectory(this.tempDir.resolve("sub"));
        Files.writeString(this.tempDir.resolve("root.txt"), "r");
        Files.writeString(sub.resolve("nested.txt"), "n");
        Files.createDirectory(sub.resolve("deep"));
        Files.writeString(sub.resolve("deep").resolve("x.txt"), "x");

        final List<FileEntry> entries = FlatListing.list(this.tempDir, true);
        assertTrue(entries.getFirst().parentLink());
        assertTrue(entries.stream().anyMatch(e -> "root.txt".equals(e.name()) && !e.directory()));
        assertTrue(entries.stream().anyMatch(e -> "sub".equals(e.name()) && e.directory()));
        assertTrue(entries.stream().anyMatch(e -> "sub/nested.txt".equals(e.name())));
        assertTrue(entries.stream().anyMatch(e -> "sub/deep/x.txt".equals(e.name())));
    }

    @Test
    void hidesDotFilesWhenRequested() throws Exception {
        Files.writeString(this.tempDir.resolve(".secret"), "s");
        Files.writeString(this.tempDir.resolve("visible.txt"), "v");
        final List<FileEntry> hidden = FlatListing.list(this.tempDir, false);
        assertTrue(hidden.stream().noneMatch(e -> e.name().contains(".secret")));
        assertTrue(hidden.stream().anyMatch(e -> "visible.txt".equals(e.name())));
    }

    @Test
    void resolveRelativeRejectsDotDot() {
        assertThrows(IllegalArgumentException.class,
                () -> FlatListing.resolveRelative(this.tempDir, "../escape"));
        assertEquals(
                this.tempDir.resolve("a/b").normalize(),
                FlatListing.resolveRelative(this.tempDir, "a/b")
        );
    }
}
