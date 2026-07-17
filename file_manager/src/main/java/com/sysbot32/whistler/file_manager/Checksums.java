package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * File checksum helpers (CRC / MessageDigest) used by the context-menu CRC submenu.
 */
@UtilityClass
public final class Checksums {
    private static final int BUFFER = 64 * 1024;
    public static long crc32(final Path file) throws IOException {
        final CRC32 crc = new CRC32();
        try (final InputStream in = Files.newInputStream(file);
             final CheckedInputStream cin = new CheckedInputStream(in, crc)) {
            final byte[] buf = new byte[BUFFER];
            while (cin.read(buf) != -1) {
                // drain
            }
        }
        return crc.getValue();
    }

    public static String digestHex(final Path file, final String algorithm) throws IOException {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance(mapAlgorithm(algorithm));
        } catch (final NoSuchAlgorithmException ex) {
            throw new IOException("Unsupported hash: " + algorithm, ex);
        }
        try (final InputStream in = Files.newInputStream(file)) {
            final byte[] buf = new byte[BUFFER];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        return toHex(md.digest());
    }

    /** Map UI labels to JCA algorithm names. */
    public static String mapAlgorithm(final String label) {
        if (label == null) {
            throw new IllegalArgumentException("algorithm");
        }
        return switch (label.trim().toUpperCase(Locale.ROOT)) {
            case "MD5" -> "MD5";
            case "SHA-1", "SHA1" -> "SHA-1";
            case "SHA-256", "SHA256" -> "SHA-256";
            case "SHA-384", "SHA384" -> "SHA-384";
            case "SHA-512", "SHA512" -> "SHA-512";
            default -> label;
        };
    }

    public static String toHex(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (final byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String formatCrc32(final long value) {
        return String.format("%08x", value & 0xffff_ffffL);
    }
}
