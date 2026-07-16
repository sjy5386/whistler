package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryListingTest {
    @TempDir
    Path tempDir;

    @Test
    void listsFilesAndDirectoriesWithParentLink() throws Exception {
        Files.createDirectory(this.tempDir.resolve("subdir"));
        Files.writeString(this.tempDir.resolve("a.txt"), "hello");
        Files.writeString(this.tempDir.resolve(".hidden"), "x");

        final List<FileEntry> visible = DirectoryListing.list(this.tempDir, false);
        assertTrue(visible.stream().anyMatch(FileEntry::parentLink));
        assertTrue(visible.stream().anyMatch(e -> "a.txt".equals(e.name())));
        assertTrue(visible.stream().anyMatch(e -> "subdir".equals(e.name()) && e.directory()));
        assertFalse(visible.stream().anyMatch(e -> ".hidden".equals(e.name())));

        final List<FileEntry> all = DirectoryListing.list(this.tempDir, true);
        assertTrue(all.stream().anyMatch(e -> ".hidden".equals(e.name())));
    }

    @Test
    void looksLikeZipByExtension() throws Exception {
        final Path zip = this.tempDir.resolve("x.zip");
        Files.writeString(zip, "not really");
        assertTrue(DirectoryListing.looksLikeZip(zip));
        assertFalse(DirectoryListing.looksLikeZip(this.tempDir.resolve("a.txt")));
    }

    @Test
    void parentLinkIsFirstWhenHasParent() throws Exception {
        final Path child = Files.createDirectory(this.tempDir.resolve("child"));
        final List<FileEntry> entries = DirectoryListing.list(child, true);
        assertFalse(entries.isEmpty());
        assertTrue(entries.getFirst().parentLink());
        assertEquals("..", entries.getFirst().name());
    }
}
