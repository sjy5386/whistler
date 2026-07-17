package com.sysbot32.whistler.file_manager;

import com.sysbot32.whistler.config.PropertiesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderBookmarksTest {
    @TempDir
    Path tempDir;

    @Test
    void setGetRoundTrip() throws Exception {
        final Path dir = Files.createDirectory(this.tempDir.resolve("bm"));
        final Path props = this.tempDir.resolve("b.properties");
        final FolderBookmarks written = new FolderBookmarks(new PropertiesConfig(props));
        written.set(3, dir);

        final FolderBookmarks loaded = new FolderBookmarks(new PropertiesConfig(props));
        assertTrue(loaded.get(3).isPresent());
        assertEquals(dir.toAbsolutePath().normalize(), loaded.get(3).orElseThrow());
        assertTrue(loaded.get(0).isEmpty());
    }
}
