package com.sysbot32.whistler.file_manager.ops;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChecksumsTest {
    @TempDir
    Path tempDir;

    @Test
    void crc32AndSha256OfKnownBytes() throws Exception {
        final Path f = this.tempDir.resolve("t.txt");
        Files.writeString(f, "123456789");
        // CRC-32 of "123456789" is the well-known 0xcbf43926
        assertEquals("cbf43926", Checksums.formatCrc32(Checksums.crc32(f)));
        assertEquals(
                "15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225",
                Checksums.digestHex(f, "SHA-256")
        );
        assertEquals("SHA-256", Checksums.mapAlgorithm("sha-256"));
    }
}
