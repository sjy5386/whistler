package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSplitCombineTest {
    @TempDir
    Path tempDir;

    @Test
    void splitAndCombineRoundTrip() throws Exception {
        final Path src = this.tempDir.resolve("data.bin");
        final byte[] payload = new byte[2500];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 251);
        }
        Files.write(src, payload);

        final List<Path> parts = FileSplitCombine.split(src, this.tempDir, 1000, null);
        assertEquals(3, parts.size());
        assertTrue(parts.getFirst().getFileName().toString().endsWith(".001"));
        assertTrue(FileSplitCombine.isFirstPart(parts.getFirst().getFileName().toString()));

        final Path combined = this.tempDir.resolve("data-out.bin");
        final int n = FileSplitCombine.combine(parts.getFirst(), combined, null);
        assertEquals(3, n);
        assertEquals(Files.readAllBytes(src).length, Files.size(combined));
        assertTrue(java.util.Arrays.equals(Files.readAllBytes(src), Files.readAllBytes(combined)));
    }

    @Test
    void splitRejectsVolumeTooLarge() throws Exception {
        final Path src = this.tempDir.resolve("small.txt");
        Files.writeString(src, "hi");
        assertThrows(Exception.class, () -> FileSplitCombine.split(src, this.tempDir, 100, null));
    }
}
