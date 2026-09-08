package com.sysbot32.whistler.sound_recorder.ui;

import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.sound_recorder.SoundRecorderApplication;
import com.sysbot32.whistler.sound_recorder.model.SoundClip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundRecorderFrameTest {
    @Test
    void windowIdentifiesAsSoundRecorderWithTapeDeckChrome(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        final AtomicReference<SoundRecorderFrame> ref = new AtomicReference<>();
        runEdt(() -> {
            final SoundRecorderFrame frame = SoundRecorderApplication.createWindow(
                    new String[0],
                    new PropertiesConfig(tempDir.resolve("sound-recorder.properties"))
            );
            ref.set(frame);
            assertTrue(
                    frame.getTitle().contains("Sound Recorder") || frame.getTitle().contains("녹음기"),
                    frame.getTitle()
            );
            assertEquals("Sound - Sound Recorder", frame.getTitle());
            assertEquals("seekToStart", frame.getSeekToStartButton().getName());
            assertEquals("Seek to Start", frame.getSeekToStartButton().getToolTipText());
            assertEquals("seekToEnd", frame.getSeekToEndButton().getName());
            assertEquals("Seek to End", frame.getSeekToEndButton().getToolTipText());
            assertEquals("play", frame.getPlayButton().getName());
            assertEquals("Play", frame.getPlayButton().getToolTipText());
            assertEquals("stop", frame.getStopButton().getName());
            assertEquals("Stop", frame.getStopButton().getToolTipText());
            assertEquals("record", frame.getRecordButton().getName());
            assertEquals("Record", frame.getRecordButton().getToolTipText());
            assertEquals("waveform", frame.getWaveformPanel().getName());
            assertTrue(
                    frame.getWaveformPanel().getWidth() > 0
                            || frame.getWaveformPanel().getPreferredSize().width > 0
            );
            assertTrue(
                    frame.getWaveformPanel().getHeight() > 0
                            || frame.getWaveformPanel().getPreferredSize().height > 0
            );
            assertEquals("position", frame.getPositionLabel().getName());
            assertEquals("length", frame.getLengthLabel().getName());
            assertTrue(frame.getPositionLabel().getText().contains("Position:"));
            assertTrue(frame.getLengthLabel().getText().contains("Length:"));
            assertNotNull(frame.getPositionSlider());
            assertEquals("positionSlider", frame.getPositionSlider().getName());
            assertTrue(frame.getPositionLabel().getText().contains(
                    SoundClip.formatTime(frame.getClip().getPositionSeconds())));
            assertTrue(frame.getLengthLabel().getText().contains(
                    SoundClip.formatTime(frame.getClip().getLengthSeconds())));
            frame.dispose();
        });
        assertNotNull(ref.get());
        assertFalse(ref.get().isDisplayable());
    }

    @Test
    void secondLaunchShowsTheSameChrome(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        runEdt(() -> {
            final SoundRecorderFrame frame = SoundRecorderApplication.createWindow(
                    new String[0],
                    new PropertiesConfig(tempDir.resolve("sound-recorder-2.properties"))
            );
            assertTrue(frame.getTitle().contains("Sound Recorder"));
            assertNotNull(frame.getSeekToStartButton());
            assertNotNull(frame.getSeekToEndButton());
            assertNotNull(frame.getPlayButton());
            assertNotNull(frame.getStopButton());
            assertNotNull(frame.getRecordButton());
            assertTrue(frame.getWaveformPanel().getPreferredSize().width > 0);
            assertTrue(frame.getPositionLabel().getText().contains("Position:"));
            assertTrue(frame.getLengthLabel().getText().contains("Length:"));
            frame.dispose();
        });
    }

    @Test
    void fileNewOpenSaveRoundTripThroughFrame(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        runEdt(() -> {
            final SoundClip clip = new SoundClip();
            final SoundRecorderFrame frame = new SoundRecorderFrame(
                    clip,
                    new PropertiesConfig(tempDir.resolve("cfg.properties"))
            );
            clip.record();
            clip.writeRecordedSamples(sineBurst(clip.getSampleRate(), 440.0, 0.25));
            clip.stop();
            frame.getSeekToStartButton().doClick();
            assertEquals(0, clip.getPositionSeconds());

            final Path file = tempDir.resolve("frame.wav");
            assertTrue(frame.savePath(file));
            assertFalse(clip.isDirty());
            assertEquals(file, clip.getPath());
            assertTrue(frame.getTitle().contains("frame.wav"));
            assertTrue(frame.getLengthLabel().getText().contains(
                    SoundClip.formatTime(clip.getLengthSeconds())));

            final short[] saved = clip.getSamples();
            frame.newDocument();
            assertEquals(0, clip.getLengthSeconds());
            assertNull(clip.getPath());
            assertFalse(clip.isDirty());
            assertTrue(frame.getTitle().contains("Sound Recorder"));

            frame.openPath(file);
            assertTrue(java.util.Arrays.equals(saved, clip.getSamples()));
            assertEquals(file, clip.getPath());
            assertFalse(clip.isDirty());
            assertTrue(frame.getTitle().contains("frame.wav"));
            frame.dispose();
        });
    }

    @Test
    void transportButtonsDriveTheClipEngine(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        runEdt(() -> {
            final SoundClip clip = new SoundClip();
            clip.record();
            clip.writeRecordedSamples(sineBurst(clip.getSampleRate(), 660.0, 0.4));
            clip.stop();
            final SoundRecorderFrame frame = new SoundRecorderFrame(
                    clip,
                    new PropertiesConfig(tempDir.resolve("cfg.properties"))
            );
            frame.getSeekToStartButton().doClick();
            assertEquals(0, clip.getPositionSeconds());
            frame.getSeekToEndButton().doClick();
            assertEquals(clip.getLengthSeconds(), clip.getPositionSeconds(), 1.0 / clip.getSampleRate() + 1e-9);
            frame.getSeekToStartButton().doClick();
            frame.getPlayButton().doClick();
            if (clip.isPlaying()) {
                clip.tick(0.1);
            }
            frame.getStopButton().doClick();
            assertEquals(SoundClip.State.STOPPED, clip.getState());
            assertTrue(clip.getPositionSeconds() > 0);
            assertTrue(frame.getPositionLabel().getText().contains(
                    SoundClip.formatTime(clip.getPositionSeconds())));
            frame.dispose();
        });
    }

    @Test
    void fileMenuWiresNewOpenSaveSaveAs() throws Exception {
        assumeDisplayOrConstructs();
        final Path source = Path.of("src/main/java/com/sysbot32/whistler/sound_recorder/ui/SoundRecorderFrame.java");
        final String text = java.nio.file.Files.readString(source);
        assertTrue(text.contains("createFileMenu"));
        assertTrue(text.contains("this.clip.createNew()"));
        assertTrue(text.contains("this.clip.open(path)"));
        assertTrue(text.contains("this.clip.save(path)"));
        assertTrue(text.contains("Save As..."));
        runEdt(() -> {
            final SoundRecorderFrame frame = new SoundRecorderFrame(
                    new SoundClip(),
                    new PropertiesConfig(Path.of("build").resolve("menu-cfg.properties"))
            );
            final List<String> items = new ArrayList<>();
            final JMenu file = frame.getJMenuBar().getMenu(0);
            assertEquals("File", file.getText());
            for (int i = 0; i < file.getItemCount(); i++) {
                final JMenuItem item = file.getItem(i);
                if (item != null) {
                    items.add(item.getText());
                }
            }
            assertTrue(items.contains("New"));
            assertTrue(items.contains("Open..."));
            assertTrue(items.contains("Save"));
            assertTrue(items.contains("Save As..."));
            frame.dispose();
        });
    }

    private static short[] sineBurst(final float sampleRate, final double frequency, final double seconds) {
        final int n = (int) Math.round(seconds * sampleRate);
        final short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            samples[i] = (short) Math.round(Math.sin(2 * Math.PI * frequency * i / sampleRate) * 16_000);
        }
        return samples;
    }

    private static void assumeDisplayOrConstructs() {
        if (GraphicsEnvironment.isHeadless()) {
            try {
                final JFrame probe = new JFrame("probe");
                probe.dispose();
            } catch (final Throwable t) {
                org.junit.jupiter.api.Assumptions.assumeFalse(true, "headless frame construction failed: " + t);
            }
        }
    }

    private static void runEdt(final Runnable action) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeAndWait(action);
    }
}
