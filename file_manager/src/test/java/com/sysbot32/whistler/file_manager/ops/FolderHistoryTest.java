package com.sysbot32.whistler.file_manager.ops;


import com.sysbot32.whistler.config.PropertiesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderHistoryTest {
    @TempDir
    Path tempDir;

    @Test
    void recordsMostRecentFirstAndPersists() throws Exception {
        final Path a = Files.createDirectory(this.tempDir.resolve("a"));
        final Path b = Files.createDirectory(this.tempDir.resolve("b"));
        final Path props = this.tempDir.resolve("h.properties");

        final FolderHistory history = new FolderHistory(new PropertiesConfig(props));
        history.record(a);
        history.record(b);
        history.record(a);

        final List<Path> entries = history.entries();
        assertEquals(a.toAbsolutePath().normalize(), entries.getFirst());
        assertEquals(2, entries.size());

        final FolderHistory reloaded = new FolderHistory(new PropertiesConfig(props));
        assertEquals(a.toAbsolutePath().normalize(), reloaded.entries().getFirst());
        assertTrue(reloaded.displayEntries().getFirst().contains("a"));
    }
}
