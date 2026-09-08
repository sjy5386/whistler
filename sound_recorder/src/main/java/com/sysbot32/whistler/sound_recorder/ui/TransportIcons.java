package com.sysbot32.whistler.sound_recorder.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Classic sndrec32-style transport glyphs (Seek Start/End, Play, Stop, Record).
 */
public final class TransportIcons {
    public static final int SIZE = 16;

    private TransportIcons() {
    }

    public static Icon seekToStart() {
        return glyph((g, s) -> {
            g.setColor(Color.BLACK);
            g.fillRect(3, 3, 2, s - 6);
            g.fillPolygon(new int[]{12, 6, 12}, new int[]{3, s / 2, s - 3}, 3);
        });
    }

    public static Icon seekToEnd() {
        return glyph((g, s) -> {
            g.setColor(Color.BLACK);
            g.fillPolygon(new int[]{4, 10, 4}, new int[]{3, s / 2, s - 3}, 3);
            g.fillRect(s - 5, 3, 2, s - 6);
        });
    }

    public static Icon play() {
        return glyph((g, s) -> {
            g.setColor(Color.BLACK);
            g.fillPolygon(new int[]{4, s - 3, 4}, new int[]{3, s / 2, s - 3}, 3);
        });
    }

    public static Icon stop() {
        return glyph((g, s) -> {
            g.setColor(Color.BLACK);
            g.fillRect(4, 4, s - 8, s - 8);
        });
    }

    public static Icon record() {
        return glyph((g, s) -> {
            g.setColor(new Color(0xC0, 0x00, 0x00));
            g.fillOval(3, 3, s - 6, s - 6);
            g.setColor(new Color(0x80, 0x00, 0x00));
            g.drawOval(3, 3, s - 7, s - 7);
        });
    }

    private static Icon glyph(final Painter painter) {
        final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            painter.paint(g, SIZE);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    @FunctionalInterface
    private interface Painter {
        void paint(Graphics2D g, int size);
    }
}
