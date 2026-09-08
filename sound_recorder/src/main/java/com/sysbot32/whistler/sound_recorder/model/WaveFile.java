package com.sysbot32.whistler.sound_recorder.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Uncompressed PCM WAVE ({@code .wav}) reader/writer. 16-bit signed little-endian internally.
 */
public final class WaveFile {
    private static final int PCM_FORMAT_TAG = 1;
    private static final int WAVEFORMATEXTENSIBLE = 0xFFFE;

    private WaveFile() {
    }

    public static void writePcm16(
            final Path path,
            final short[] samples,
            final float sampleRate,
            final int channels
    ) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(samples, "samples");
        if (channels < 1) {
            throw new IllegalArgumentException("channels must be >= 1");
        }
        if (samples.length % channels != 0) {
            throw new IllegalArgumentException("sample count is not aligned to channels");
        }
        final int dataSize = samples.length * 2;
        final int riffSize = 36 + dataSize;
        final int byteRate = Math.round(sampleRate) * channels * 2;
        final short blockAlign = (short) (channels * 2);
        final ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 'R');
        buffer.put((byte) 'I');
        buffer.put((byte) 'F');
        buffer.put((byte) 'F');
        buffer.putInt(riffSize);
        buffer.put((byte) 'W');
        buffer.put((byte) 'A');
        buffer.put((byte) 'V');
        buffer.put((byte) 'E');
        buffer.put((byte) 'f');
        buffer.put((byte) 'm');
        buffer.put((byte) 't');
        buffer.put((byte) ' ');
        buffer.putInt(16);
        buffer.putShort((short) PCM_FORMAT_TAG);
        buffer.putShort((short) channels);
        buffer.putInt(Math.round(sampleRate));
        buffer.putInt(byteRate);
        buffer.putShort(blockAlign);
        buffer.putShort((short) 16);
        buffer.put((byte) 'd');
        buffer.put((byte) 'a');
        buffer.put((byte) 't');
        buffer.put((byte) 'a');
        buffer.putInt(dataSize);
        for (final short sample : samples) {
            buffer.putShort(sample);
        }
        Files.write(path, buffer.array());
    }

    public static PcmClip read(final Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        final byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < 44) {
            throw new IOException("WAVE file too small: " + path);
        }
        final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.getInt() != fourCc("RIFF") || buffer.getInt() < 4 || buffer.getInt() != fourCc("WAVE")) {
            throw new IOException("Not a RIFF/WAVE file: " + path);
        }
        int channels = 0;
        float sampleRate = 0;
        int bitsPerSample = 0;
        int formatTag = 0;
        short[] samples = null;
        while (buffer.remaining() >= 8) {
            final int chunkId = buffer.getInt();
            final int chunkSize = buffer.getInt();
            if (chunkSize < 0 || buffer.remaining() < chunkSize) {
                throw new IOException("Truncated WAVE chunk in " + path);
            }
            final int chunkStart = buffer.position();
            if (chunkId == fourCc("fmt ")) {
                if (chunkSize < 16) {
                    throw new IOException("fmt chunk too small in " + path);
                }
                formatTag = Short.toUnsignedInt(buffer.getShort());
                channels = Short.toUnsignedInt(buffer.getShort());
                sampleRate = buffer.getInt();
                buffer.getInt(); // byte rate
                buffer.getShort(); // block align
                bitsPerSample = Short.toUnsignedInt(buffer.getShort());
                if (formatTag == WAVEFORMATEXTENSIBLE && chunkSize >= 40) {
                    buffer.position(chunkStart + 24);
                    formatTag = Short.toUnsignedInt(buffer.getShort());
                }
            } else if (chunkId == fourCc("data")) {
                if (channels < 1 || bitsPerSample <= 0) {
                    throw new IOException("data chunk before fmt in " + path);
                }
                samples = decodePcm(buffer, chunkSize, bitsPerSample);
            }
            int next = chunkStart + chunkSize;
            if ((chunkSize & 1) != 0) {
                next++;
            }
            if (next > bytes.length) {
                next = bytes.length;
            }
            buffer.position(next);
        }
        if (formatTag != PCM_FORMAT_TAG) {
            throw new IOException("Unsupported WAVE format tag " + formatTag + " in " + path);
        }
        if (Objects.isNull(samples)) {
            throw new IOException("WAVE file has no data chunk: " + path);
        }
        if (channels < 1) {
            throw new IOException("WAVE file has invalid channel count: " + path);
        }
        return new PcmClip(samples, sampleRate, channels);
    }

    private static short[] decodePcm(final ByteBuffer buffer, final int chunkSize, final int bitsPerSample) {
        if (bitsPerSample == 16) {
            final int count = chunkSize / 2;
            final short[] samples = new short[count];
            for (int i = 0; i < count; i++) {
                samples[i] = buffer.getShort();
            }
            return samples;
        }
        if (bitsPerSample == 8) {
            final short[] samples = new short[chunkSize];
            for (int i = 0; i < chunkSize; i++) {
                final int unsigned = Byte.toUnsignedInt(buffer.get());
                samples[i] = (short) ((unsigned - 128) << 8);
            }
            return samples;
        }
        throw new IllegalArgumentException("Unsupported PCM bits per sample: " + bitsPerSample);
    }

    private static int fourCc(final String id) {
        final char[] c = id.toCharArray();
        return (c[3] << 24) | (c[2] << 16) | (c[1] << 8) | c[0];
    }

    public record PcmClip(short[] samples, float sampleRate, int channels) {
    }
}
