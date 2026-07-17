package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Simple file comparison for the Diff menu item (no external tool required).
 */
@UtilityClass
public final class FileDiff {
    public record Result(boolean identical, String report) {
    }

    public static Result compare(final Path left, final Path right) throws IOException {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        if (!Files.isRegularFile(left) || !Files.isRegularFile(right)) {
            throw new IOException("Diff requires two regular files");
        }
        final long sizeL = Files.size(left);
        final long sizeR = Files.size(right);
        final byte[] a = Files.readAllBytes(left);
        final byte[] b = Files.readAllBytes(right);
        if (java.util.Arrays.equals(a, b)) {
            return new Result(true, "Files are identical (" + sizeL + " bytes).\n"
                    + left + "\n" + right);
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Files differ.\n");
        sb.append("Left:  ").append(left).append(" (").append(sizeL).append(" bytes)\n");
        sb.append("Right: ").append(right).append(" (").append(sizeR).append(" bytes)\n\n");

        if (looksLikeText(a) && looksLikeText(b) && sizeL <= 512 * 1024 && sizeR <= 512 * 1024) {
            sb.append(lineDiff(
                    new String(a, StandardCharsets.UTF_8),
                    new String(b, StandardCharsets.UTF_8),
                    left.getFileName().toString(),
                    right.getFileName().toString()
            ));
        } else {
            int first = -1;
            final int n = Math.min(a.length, b.length);
            for (int i = 0; i < n; i++) {
                if (a[i] != b[i]) {
                    first = i;
                    break;
                }
            }
            if (first < 0) {
                first = n;
            }
            sb.append("First difference at byte offset ").append(first).append(".\n");
        }
        return new Result(false, sb.toString());
    }

    static boolean looksLikeText(final byte[] data) {
        if (data.length == 0) {
            return true;
        }
        int sample = Math.min(data.length, 8000);
        int ctrl = 0;
        for (int i = 0; i < sample; i++) {
            final int b = data[i] & 0xff;
            if (b == 0) {
                return false;
            }
            if (b < 0x09 || (b > 0x0d && b < 0x20)) {
                ctrl++;
            }
        }
        return ctrl * 20 < sample;
    }

    /** Minimal line-oriented diff (not full LCS — enough for FM dialog). */
    static String lineDiff(final String left, final String right, final String nameL, final String nameR) {
        final String[] a = left.split("\\R", -1);
        final String[] b = right.split("\\R", -1);
        final StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(nameL).append('\n');
        sb.append("+++ ").append(nameR).append('\n');
        final int max = Math.max(a.length, b.length);
        int shown = 0;
        final int limit = 200;
        for (int i = 0; i < max && shown < limit; i++) {
            final String la = i < a.length ? a[i] : null;
            final String lb = i < b.length ? b[i] : null;
            if (Objects.equals(la, lb)) {
                continue;
            }
            if (la != null) {
                sb.append('-').append(la).append('\n');
                shown++;
            }
            if (lb != null) {
                sb.append('+').append(lb).append('\n');
                shown++;
            }
        }
        if (shown >= limit) {
            sb.append("… (diff truncated)\n");
        }
        return sb.toString();
    }
}
