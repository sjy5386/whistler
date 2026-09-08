package com.sysbot32.whistler.sound_recorder.ui;

import com.sysbot32.whistler.sound_recorder.model.SoundClip;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;

/**
 * Classic green oscilloscope of the current clip's samples.
 */
public class WaveformPanel extends JComponent {
    public static final Color BACKGROUND = new Color(0x00, 0x20, 0x00);
    public static final Color TRACE = new Color(0x00, 0xE0, 0x00);
    public static final Color PLAYHEAD = new Color(0xE8, 0xE8, 0x40);

    private SoundClip clip;

    public WaveformPanel(final SoundClip clip) {
        this.clip = Objects.requireNonNull(clip, "clip");
        this.setName("waveform");
        this.setOpaque(true);
        this.setPreferredSize(new Dimension(128, 40));
        this.setMinimumSize(new Dimension(76, 32));
        this.setBackground(BACKGROUND);
    }

    public void setClip(final SoundClip clip) {
        this.clip = Objects.requireNonNull(clip, "clip");
        this.repaint();
    }

    public SoundClip getClip() {
        return this.clip;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        this.paintWaveform(g, this.getWidth(), this.getHeight());
    }

    public void paintWaveform(final Graphics g, final int width, final int height) {
        paintWaveform(g, width, height, this.clip);
    }

    public static void paintWaveform(
            final Graphics g,
            final int width,
            final int height,
            final SoundClip clip
    ) {
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(BACKGROUND);
            g2.fillRect(0, 0, width, height);
            if (width <= 0 || height <= 0 || Objects.isNull(clip)) {
                return;
            }
            final short[] samples = clip.getSamples();
            final int channels = Math.max(1, clip.getChannels());
            final int frames = samples.length / channels;
            if (frames <= 0) {
                return;
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setColor(TRACE);
            final int mid = height / 2;
            final int amp = Math.max(1, mid - 2);
            for (int x = 0; x < width; x++) {
                final int start = (int) ((long) x * frames / width);
                int end = (int) ((long) (x + 1) * frames / width);
                if (end <= start) {
                    end = Math.min(frames, start + 1);
                }
                short min = 0;
                short max = 0;
                for (int frame = start; frame < end; frame++) {
                    final short sample = samples[frame * channels];
                    if (sample < min) {
                        min = sample;
                    }
                    if (sample > max) {
                        max = sample;
                    }
                }
                final int yMax = mid - (int) Math.round(max * (double) amp / 32767.0);
                final int yMin = mid - (int) Math.round(min * (double) amp / 32767.0);
                g2.drawLine(x, yMax, x, yMin);
            }
            final double length = clip.getLengthSeconds();
            if (length > 0) {
                final int playX = (int) Math.round(clip.getPositionSeconds() / length * (width - 1));
                g2.setColor(PLAYHEAD);
                g2.drawLine(playX, 0, playX, height - 1);
            }
        } finally {
            g2.dispose();
        }
    }
}
