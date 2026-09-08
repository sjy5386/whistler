package com.sysbot32.whistler.sound_recorder.model;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Pure PCM clip engine: samples, play-head, dirty/path, WAVE load/save, record-append, seek.
 * No {@code TargetDataLine}/{@code SourceDataLine} and no Swing.
 */
public class SoundClip {
    public static final float DEFAULT_SAMPLE_RATE = 22_050f;
    public static final int DEFAULT_CHANNELS = 1;
    public static final int BITS_PER_SAMPLE = 16;

    public enum State {
        STOPPED,
        PLAYING,
        RECORDING
    }

    private short[] samples = new short[0];
    private float sampleRate = DEFAULT_SAMPLE_RATE;
    private int channels = DEFAULT_CHANNELS;
    private int playHead;
    private boolean dirty;
    private Path path;
    private State state = State.STOPPED;

    public SoundClip() {
    }

    public SoundClip(final Path path) {
        try {
            this.open(path);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to open sound file: " + path, e);
        }
    }

    public synchronized void createNew() {
        this.samples = new short[0];
        this.sampleRate = DEFAULT_SAMPLE_RATE;
        this.channels = DEFAULT_CHANNELS;
        this.playHead = 0;
        this.dirty = false;
        this.path = null;
        this.state = State.STOPPED;
    }

    public synchronized void record() {
        this.state = State.RECORDING;
    }

    /**
     * Inject PCM frames into the active recording (the same path a capture line uses).
     */
    public synchronized void writeRecordedSamples(final short[] incoming) {
        if (this.state != State.RECORDING) {
            throw new IllegalStateException("not recording");
        }
        Objects.requireNonNull(incoming, "incoming");
        if (incoming.length == 0) {
            return;
        }
        if (incoming.length % this.channels != 0) {
            throw new IllegalArgumentException("sample count is not aligned to channels");
        }
        final int incomingFrames = incoming.length / this.channels;
        final int currentFrames = this.frameCount();
        final int endFrame = this.playHead + incomingFrames;
        final int newFrames = Math.max(currentFrames, endFrame);
        final short[] next = new short[newFrames * this.channels];
        System.arraycopy(this.samples, 0, next, 0, this.samples.length);
        System.arraycopy(incoming, 0, next, this.playHead * this.channels, incoming.length);
        this.samples = next;
        this.playHead = endFrame;
        this.dirty = true;
    }

    /**
     * Inject little-endian 16-bit PCM bytes into the active recording.
     */
    public synchronized void writeRecordedPcm(final byte[] buffer, final int offset, final int byteCount) {
        if (this.state != State.RECORDING) {
            throw new IllegalStateException("not recording");
        }
        if (byteCount <= 0) {
            return;
        }
        if (byteCount % 2 != 0) {
            throw new IllegalArgumentException("PCM byte count must be even");
        }
        final short[] incoming = new short[byteCount / 2];
        ByteBuffer.wrap(buffer, offset, byteCount).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(incoming);
        this.writeRecordedSamples(incoming);
    }

    public synchronized void play() {
        if (this.frameCount() == 0) {
            return;
        }
        if (this.playHead >= this.frameCount()) {
            this.playHead = 0;
        }
        this.state = State.PLAYING;
    }

    public synchronized void stop() {
        this.state = State.STOPPED;
    }

    /**
     * Advance the play-head as if {@code seconds} of audio elapsed. No-op unless playing.
     */
    public synchronized void tick(final double seconds) {
        if (this.state != State.PLAYING || seconds <= 0) {
            return;
        }
        final int frames = (int) Math.round(seconds * this.sampleRate);
        this.playHead = Math.min(this.frameCount(), this.playHead + Math.max(0, frames));
        if (this.playHead >= this.frameCount()) {
            this.playHead = this.frameCount();
            this.state = State.STOPPED;
        }
    }

    /**
     * Copy playing PCM into {@code buffer} and advance the play-head. Returns bytes written.
     */
    public synchronized int consumePlayPcm(final byte[] buffer, final int offset, final int byteCount) {
        if (this.state != State.PLAYING) {
            return 0;
        }
        final int frameSize = this.channels * 2;
        if (frameSize <= 0 || byteCount < frameSize) {
            return 0;
        }
        final int framesRequested = byteCount / frameSize;
        final int frames = Math.min(framesRequested, this.frameCount() - this.playHead);
        if (frames <= 0) {
            this.playHead = this.frameCount();
            this.state = State.STOPPED;
            return 0;
        }
        final ByteBuffer out = ByteBuffer.wrap(buffer, offset, frames * frameSize).order(ByteOrder.LITTLE_ENDIAN);
        final int start = this.playHead * this.channels;
        for (int i = 0; i < frames * this.channels; i++) {
            out.putShort(this.samples[start + i]);
        }
        this.playHead += frames;
        if (this.playHead >= this.frameCount()) {
            this.playHead = this.frameCount();
            this.state = State.STOPPED;
        }
        return frames * frameSize;
    }

    public synchronized void seekToStart() {
        this.playHead = 0;
    }

    public synchronized void seekToEnd() {
        this.playHead = this.frameCount();
    }

    public synchronized void seekTo(final double seconds) {
        final int frame = (int) Math.round(seconds * this.sampleRate);
        this.playHead = Math.max(0, Math.min(this.frameCount(), frame));
    }

    public synchronized double getPositionSeconds() {
        return this.playHead / (double) this.sampleRate;
    }

    public synchronized double getLengthSeconds() {
        return this.frameCount() / (double) this.sampleRate;
    }

    public synchronized int frameCount() {
        if (this.channels <= 0) {
            return 0;
        }
        return this.samples.length / this.channels;
    }

    public synchronized short[] getSamples() {
        return this.samples.clone();
    }

    public synchronized float getSampleRate() {
        return this.sampleRate;
    }

    public synchronized int getChannels() {
        return this.channels;
    }

    public synchronized boolean isDirty() {
        return this.dirty;
    }

    public synchronized Path getPath() {
        return this.path;
    }

    public synchronized State getState() {
        return this.state;
    }

    public synchronized boolean isPlaying() {
        return this.state == State.PLAYING;
    }

    public synchronized boolean isRecording() {
        return this.state == State.RECORDING;
    }

    public synchronized AudioFormat getAudioFormat() {
        return new AudioFormat(this.sampleRate, BITS_PER_SAMPLE, this.channels, true, false);
    }

    public synchronized void save(final Path path) throws IOException {
        WaveFile.writePcm16(path, this.samples, this.sampleRate, this.channels);
        this.path = path;
        this.dirty = false;
    }

    public synchronized void open(final Path path) throws IOException {
        final WaveFile.PcmClip loaded = WaveFile.read(path);
        this.samples = loaded.samples();
        this.sampleRate = loaded.sampleRate();
        this.channels = loaded.channels();
        this.playHead = 0;
        this.path = path;
        this.dirty = false;
        this.state = State.STOPPED;
    }

    /**
     * Classic Sound Recorder time: {@code m:ss.hh}.
     */
    public static String formatTime(final double seconds) {
        double value = seconds;
        if (Double.isNaN(value) || value < 0 || Double.isInfinite(value)) {
            value = 0;
        }
        final long hundredths = Math.round(value * 100.0);
        final long minutes = hundredths / 6_000;
        final long secs = (hundredths % 6_000) / 100;
        final long hs = hundredths % 100;
        return String.format("%d:%02d.%02d", minutes, secs, hs);
    }
}
