package com.sysbot32.whistler.file_manager.ops;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationsCollisionTest {
    @TempDir
    Path tempDir;

    @Test
    void copySkipLeavesExistingContent() throws Exception {
        final Path src = this.tempDir.resolve("a.txt");
        final Path destDir = Files.createDirectory(this.tempDir.resolve("dest"));
        final Path existing = destDir.resolve("a.txt");
        Files.writeString(src, "new");
        Files.writeString(existing, "old");

        FileOperations.copy(
                List.of(src),
                destDir,
                (s, t) -> CollisionPolicy.UserChoice.SKIP,
                null
        );
        assertEquals("old", Files.readString(existing));
    }

    @Test
    void copyOverwriteReplaces() throws Exception {
        final Path src = this.tempDir.resolve("a.txt");
        final Path destDir = Files.createDirectory(this.tempDir.resolve("dest2"));
        final Path existing = destDir.resolve("a.txt");
        Files.writeString(src, "new");
        Files.writeString(existing, "old");

        FileOperations.copy(
                List.of(src),
                destDir,
                (s, t) -> CollisionPolicy.UserChoice.OVERWRITE,
                null
        );
        assertEquals("new", Files.readString(existing));
    }

    @Test
    void copyCancelAbortsFurtherItems() throws Exception {
        final Path a = this.tempDir.resolve("a.txt");
        final Path b = this.tempDir.resolve("b.txt");
        Files.writeString(a, "A");
        Files.writeString(b, "B");
        final Path destDir = Files.createDirectory(this.tempDir.resolve("dest3"));
        Files.writeString(destDir.resolve("a.txt"), "oldA");
        Files.writeString(destDir.resolve("b.txt"), "oldB");

        final AtomicInteger asks = new AtomicInteger();
        assertThrows(CancellationException.class, () -> FileOperations.copy(
                List.of(a, b),
                destDir,
                (s, t) -> {
                    asks.incrementAndGet();
                    return CollisionPolicy.UserChoice.CANCEL;
                },
                null
        ));
        assertEquals(1, asks.get());
        assertEquals("oldA", Files.readString(destDir.resolve("a.txt")));
        assertEquals("oldB", Files.readString(destDir.resolve("b.txt")));
    }

    @Test
    void deleteRespectsCancel() throws Exception {
        final Path a = Files.writeString(this.tempDir.resolve("del-a.txt"), "x");
        final Path b = Files.writeString(this.tempDir.resolve("del-b.txt"), "y");
        final TransferControl control = new TransferControl();
        control.cancel();
        assertThrows(CancellationException.class, () -> FileOperations.delete(List.of(a, b), control));
        assertTrue(Files.exists(a));
        assertTrue(Files.exists(b));
    }

    @Test
    void copyWithControlAdvances() throws Exception {
        final Path src = Files.writeString(this.tempDir.resolve("c.txt"), "c");
        final Path destDir = Files.createDirectory(this.tempDir.resolve("dest4"));
        final TransferControl control = new TransferControl();
        FileOperations.copy(List.of(src), destDir, null, control);
        assertEquals(1, control.completed());
        assertTrue(Files.exists(destDir.resolve("c.txt")));
        assertFalse(control.isCancelled());
    }
}
