package com.sysbot32.whistler.sound_recorder.audio;

import com.sysbot32.whistler.sound_recorder.model.SoundClip;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.Objects;

/**
 * Optional mixer adapter. Failures (no device) leave the clip engine usable without a line.
 */
public final class LineTransport {
    private final Object lock = new Object();
    private Thread worker;
    private volatile boolean running;
    private TargetDataLine target;
    private SourceDataLine source;

    public boolean startPlayback(final SoundClip clip) {
        Objects.requireNonNull(clip, "clip");
        this.stop();
        try {
            final AudioFormat format = clip.getAudioFormat();
            final SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            synchronized (this.lock) {
                this.source = line;
                this.running = true;
                this.worker = new Thread(() -> this.playbackLoop(clip, format), "sound-recorder-play");
                this.worker.setDaemon(true);
                this.worker.start();
            }
            return true;
        } catch (final Throwable ignored) {
            this.stop();
            return false;
        }
    }

    public boolean startCapture(final SoundClip clip) {
        Objects.requireNonNull(clip, "clip");
        this.stop();
        try {
            final AudioFormat format = clip.getAudioFormat();
            final TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
            synchronized (this.lock) {
                this.target = line;
                this.running = true;
                this.worker = new Thread(() -> this.captureLoop(clip, format), "sound-recorder-record");
                this.worker.setDaemon(true);
                this.worker.start();
            }
            return true;
        } catch (final Throwable ignored) {
            this.stop();
            return false;
        }
    }

    public void stop() {
        this.running = false;
        final Thread thread;
        final TargetDataLine capture;
        final SourceDataLine playback;
        synchronized (this.lock) {
            thread = this.worker;
            this.worker = null;
            capture = this.target;
            playback = this.source;
            this.target = null;
            this.source = null;
        }
        if (Objects.nonNull(capture)) {
            try {
                capture.stop();
            } catch (final Exception ignored) {
                // closing a mixer line is best-effort
            }
            capture.close();
        }
        if (Objects.nonNull(playback)) {
            try {
                playback.stop();
            } catch (final Exception ignored) {
                // closing a mixer line is best-effort
            }
            playback.close();
        }
        if (Objects.nonNull(thread) && thread != Thread.currentThread()) {
            try {
                thread.join(500);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    private void playbackLoop(final SoundClip clip, final AudioFormat format) {
        final byte[] buf = new byte[Math.max(format.getFrameSize() * 1024, format.getFrameSize())];
        try {
            while (this.running && clip.isPlaying()) {
                final SourceDataLine line;
                synchronized (this.lock) {
                    line = this.source;
                }
                if (Objects.isNull(line)) {
                    break;
                }
                final int n = clip.consumePlayPcm(buf, 0, buf.length);
                if (n <= 0) {
                    break;
                }
                line.write(buf, 0, n);
            }
        } finally {
            clip.stop();
            this.running = false;
        }
    }

    private void captureLoop(final SoundClip clip, final AudioFormat format) {
        final byte[] buf = new byte[Math.max(format.getFrameSize() * 1024, format.getFrameSize())];
        try {
            while (this.running && clip.isRecording()) {
                final TargetDataLine line;
                synchronized (this.lock) {
                    line = this.target;
                }
                if (Objects.isNull(line)) {
                    break;
                }
                final int n = line.read(buf, 0, buf.length);
                if (n > 0) {
                    clip.writeRecordedPcm(buf, 0, n & ~1);
                }
            }
        } finally {
            this.running = false;
        }
    }
}
