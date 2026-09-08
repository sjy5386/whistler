package com.sysbot32.whistler.sound_recorder.ui;

import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.sound_recorder.model.SoundClip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaveformPanelTest {
    @Test
    void nonSilentClipPaintsFilledOscilloscopeNotBlankRect(@TempDir final Path tempDir) {
        final SoundClip clip = new SoundClip();
        clip.record();
        clip.writeRecordedSamples(sineBurst(clip.getSampleRate(), 440.0, 0.5));
        clip.stop();
        clip.seekTo(clip.getLengthSeconds() / 3.0);

        final int width = 320;
        final int height = 80;
        final WaveformPanel panel = new WaveformPanel(clip);
        panel.setSize(width, height);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            panel.paint(g);
        } finally {
            g.dispose();
        }

        final int background = WaveformPanel.BACKGROUND.getRGB();
        int painted = 0;
        int columnsWithTrace = 0;
        for (int x = 0; x < width; x++) {
            boolean columnHit = false;
            for (int y = 0; y < height; y++) {
                if (image.getRGB(x, y) != background) {
                    painted++;
                    columnHit = true;
                }
            }
            if (columnHit) {
                columnsWithTrace++;
            }
        }
        assertTrue(painted > width, "waveform was nearly blank: painted=" + painted);
        assertTrue(columnsWithTrace > width / 2, "oscilloscope did not span the clip: columns=" + columnsWithTrace);

        assertEquals(clip.getPositionSeconds(), panel.getClip().getPositionSeconds());
        assertEquals(clip.getLengthSeconds(), panel.getClip().getLengthSeconds());
        assertTrue(panel.getClip().getLengthSeconds() > 0);

        final SoundRecorderFrame frame = new SoundRecorderFrame(
                clip,
                new PropertiesConfig(tempDir.resolve("waveform.properties"))
        );
        assertTrue(frame.getPositionLabel().getText().contains("Position:"));
        assertTrue(frame.getPositionLabel().getText().contains(
                SoundClip.formatTime(clip.getPositionSeconds())));
        assertTrue(frame.getLengthLabel().getText().contains("Length:"));
        assertTrue(frame.getLengthLabel().getText().contains(
                SoundClip.formatTime(clip.getLengthSeconds())));
        frame.dispose();
    }

    @Test
    void emptyClipPaintsBackgroundOnly() {
        final SoundClip clip = new SoundClip();
        final int width = 200;
        final int height = 60;
        final WaveformPanel panel = new WaveformPanel(clip);
        panel.setSize(width, height);
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            panel.paint(g);
        } finally {
            g.dispose();
        }
        final int background = WaveformPanel.BACKGROUND.getRGB();
        int other = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (image.getRGB(x, y) != background) {
                    other++;
                }
            }
        }
        assertEquals(0, other, "empty clip should be a blank oscilloscope");
        assertEquals(0, clip.getLengthSeconds());
        assertEquals(0, clip.getPositionSeconds());
        assertEquals(0, panel.getClip().getLengthSeconds());
    }

    private static short[] sineBurst(final float sampleRate, final double frequency, final double seconds) {
        final int n = (int) Math.round(seconds * sampleRate);
        final short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            samples[i] = (short) Math.round(Math.sin(2 * Math.PI * frequency * i / sampleRate) * 20_000);
        }
        return samples;
    }
}
