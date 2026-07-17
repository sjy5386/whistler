package com.sysbot32.whistler.file_manager.ops;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationsTest {
    @TempDir
    Path tempDir;

    @Test
    void copyMoveDeleteRenameMkdir() throws Exception {
        final Path src = this.tempDir.resolve("src");
        Files.createDirectory(src);
        Files.writeString(src.resolve("f.txt"), "data");

        final Path copyDest = this.tempDir.resolve("copy");
        Files.createDirectory(copyDest);
        FileOperations.copy(List.of(src), copyDest);
        assertTrue(Files.isDirectory(copyDest.resolve("src")));
        assertEquals("data", Files.readString(copyDest.resolve("src/f.txt")));

        final Path movedFrom = copyDest.resolve("src");
        final Path moveDest = this.tempDir.resolve("moved");
        Files.createDirectory(moveDest);
        FileOperations.move(List.of(movedFrom), moveDest);
        assertTrue(Files.exists(moveDest.resolve("src/f.txt")));
        assertFalse(Files.exists(movedFrom));

        FileOperations.rename(moveDest.resolve("src/f.txt"), "g.txt");
        assertTrue(Files.exists(moveDest.resolve("src/g.txt")));

        FileOperations.mkdir(moveDest, "newfolder");
        assertTrue(Files.isDirectory(moveDest.resolve("newfolder")));

        FileOperations.delete(List.of(moveDest.resolve("src")));
        assertFalse(Files.exists(moveDest.resolve("src")));
    }

    @Test
    void createEmptyFile() throws Exception {
        final Path parent = Files.createDirectory(this.tempDir.resolve("p"));
        final Path created = FileOperations.createFile(parent, "new.txt");
        assertTrue(Files.isRegularFile(created));
        assertEquals(0L, Files.size(created));
    }
}
