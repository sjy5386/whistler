package com.sysbot32.whistler.sound_recorder.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundClipTest {
    @Test
    void emptyStartHasZeroLengthAndNoPath() {
        final SoundClip clip = new SoundClip();
        assertEquals(0, clip.getLengthSeconds());
        assertEquals(0, clip.getPositionSeconds());
        assertEquals(0, clip.frameCount());
        assertNull(clip.getPath());
        assertFalse(clip.isDirty());
        assertEquals(SoundClip.State.STOPPED, clip.getState());
    }

    @Test
    void injectPcmThenStopLeavesNonZeroLength() {
        final SoundClip clip = new SoundClip();
        final short[] burst = sineBurst(clip.getSampleRate(), 440.0, 0.4);
        clip.record();
        clip.writeRecordedSamples(burst);
        clip.stop();

        assertTrue(clip.getLengthSeconds() > 0, "recorded length was " + clip.getLengthSeconds());
        assertEquals(burst.length, clip.frameCount());
        assertArrayEquals(burst, clip.getSamples());
        assertTrue(clip.isDirty());
        assertEquals(SoundClip.State.STOPPED, clip.getState());
    }

    @Test
    void playThenStopAdvancesPositionThroughTheClip() {
        final SoundClip clip = recordedSine();
        clip.seekToStart();
        final double before = clip.getPositionSeconds();
        clip.play();
        assertTrue(clip.isPlaying());
        clip.tick(0.15);
        clip.stop();

        assertTrue(clip.getPositionSeconds() > before, "position did not advance");
        assertTrue(clip.getPositionSeconds() <= clip.getLengthSeconds());
        assertEquals(SoundClip.State.STOPPED, clip.getState());
    }

    @Test
    void seekToStartEndAndMidClip() {
        final SoundClip clip = recordedSine();
        clip.seekToEnd();
        assertEquals(clip.getLengthSeconds(), clip.getPositionSeconds(), samplePeriod(clip));

        clip.seekToStart();
        assertEquals(0, clip.getPositionSeconds());

        final double mid = clip.getLengthSeconds() / 2.0;
        clip.seekTo(mid);
        assertEquals(mid, clip.getPositionSeconds(), samplePeriod(clip));
        assertTrue(clip.getPositionSeconds() > 0);
        assertTrue(clip.getPositionSeconds() < clip.getLengthSeconds());
    }

    @Test
    void wavRoundTripPreservesSamplesAndDuration(@TempDir final Path tempDir) throws IOException {
        final SoundClip writer = recordedSine();
        final Path file = tempDir.resolve("burst.wav");
        writer.save(file);

        assertTrue(Files.size(file) > 44);
        final byte[] raw = Files.readAllBytes(file);
        assertEquals('R', raw[0]);
        assertEquals('I', raw[1]);
        assertEquals('F', raw[2]);
        assertEquals('F', raw[3]);

        final SoundClip reader = new SoundClip();
        reader.open(file);

        assertArrayEquals(writer.getSamples(), reader.getSamples());
        assertEquals(writer.getLengthSeconds(), reader.getLengthSeconds());
        assertEquals(writer.getSampleRate(), reader.getSampleRate());
        assertEquals(writer.getChannels(), reader.getChannels());
        assertEquals(file, reader.getPath());
        assertFalse(reader.isDirty());
        assertEquals(0, reader.getPositionSeconds());
    }

    @Test
    void constructorOpenLoadsFileWithoutDirtyFlag(@TempDir final Path tempDir) throws IOException {
        final SoundClip writer = recordedSine();
        final Path file = tempDir.resolve("from-args.wav");
        writer.save(file);

        final SoundClip opened = new SoundClip(file);
        assertArrayEquals(writer.getSamples(), opened.getSamples());
        assertEquals(file, opened.getPath());
        assertFalse(opened.isDirty());
    }

    @Test
    void createNewClearsLengthPathAndDirty(@TempDir final Path tempDir) throws IOException {
        final SoundClip clip = recordedSine();
        final Path file = tempDir.resolve("keep.wav");
        clip.save(file);
        clip.record();
        clip.writeRecordedSamples(sineBurst(clip.getSampleRate(), 880.0, 0.1));
        clip.stop();
        assertTrue(clip.isDirty());
        assertTrue(clip.getLengthSeconds() > 0);

        clip.createNew();

        assertEquals(0, clip.getLengthSeconds());
        assertEquals(0, clip.frameCount());
        assertNull(clip.getPath());
        assertFalse(clip.isDirty());
        assertEquals(SoundClip.State.STOPPED, clip.getState());
    }

    @Test
    void saveAfterChangeClearsDirty(@TempDir final Path tempDir) throws IOException {
        final SoundClip clip = new SoundClip();
        clip.record();
        clip.writeRecordedSamples(sineBurst(clip.getSampleRate(), 330.0, 0.2));
        clip.stop();
        assertTrue(clip.isDirty());

        final Path file = tempDir.resolve("saved.wav");
        clip.save(file);

        assertFalse(clip.isDirty());
        assertEquals(file, clip.getPath());
        assertTrue(Files.size(file) > 44);
    }

    @Test
    void writeRecordedPcmUsesTheSameRecordPath() {
        final SoundClip clip = new SoundClip();
        final short[] burst = sineBurst(clip.getSampleRate(), 523.25, 0.12);
        final byte[] pcm = new byte[burst.length * 2];
        for (int i = 0; i < burst.length; i++) {
            pcm[i * 2] = (byte) (burst[i] & 0xFF);
            pcm[i * 2 + 1] = (byte) ((burst[i] >> 8) & 0xFF);
        }
        clip.record();
        clip.writeRecordedPcm(pcm, 0, pcm.length);
        clip.stop();
        assertArrayEquals(burst, clip.getSamples());
        assertTrue(clip.getLengthSeconds() > 0);
    }

    @Test
    void injectWithoutRecordIsRejected() {
        final SoundClip clip = new SoundClip();
        assertThrows(IllegalStateException.class, () -> clip.writeRecordedSamples(new short[]{1, 2, 3}));
    }

    @Test
    void tickDoesNotAdvanceWhenStopped() {
        final SoundClip clip = recordedSine();
        clip.seekToStart();
        clip.tick(0.5);
        assertEquals(0, clip.getPositionSeconds());
    }

    @Test
    void playFromEndRestartsThenAdvances() {
        final SoundClip clip = recordedSine();
        clip.seekToEnd();
        clip.play();
        assertEquals(0, clip.getPositionSeconds());
        clip.tick(0.05);
        assertTrue(clip.getPositionSeconds() > 0);
        clip.stop();
    }

    @Test
    void formatTimeUsesClassicHundredths() {
        assertEquals("0:00.00", SoundClip.formatTime(0));
        assertEquals("0:01.50", SoundClip.formatTime(1.5));
        assertEquals("1:02.03", SoundClip.formatTime(62.03));
    }

    private static SoundClip recordedSine() {
        final SoundClip clip = new SoundClip();
        clip.record();
        clip.writeRecordedSamples(sineBurst(clip.getSampleRate(), 440.0, 0.5));
        clip.stop();
        return clip;
    }

    private static short[] sineBurst(final float sampleRate, final double frequency, final double seconds) {
        final int n = (int) Math.round(seconds * sampleRate);
        final short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            samples[i] = (short) Math.round(Math.sin(2 * Math.PI * frequency * i / sampleRate) * 16_000);
        }
        boolean audible = false;
        for (final short sample : samples) {
            if (sample != 0) {
                audible = true;
                break;
            }
        }
        assertTrue(audible);
        return samples;
    }

    private static double samplePeriod(final SoundClip clip) {
        return 1.0 / clip.getSampleRate() + 1e-9;
    }
}
