package com.sysbot32.whistler.wordpad.ui;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Classic WordPad toolbar glyphs (drawn, not text labels).
 */
public final class ToolbarIcons {
    public static final int SIZE = 16;

    public enum Glyph {
        NEW, OPEN, SAVE, PRINT, FIND, CUT, COPY, PASTE, UNDO, DATE,
        BOLD, ITALIC, UNDERLINE, COLOR, ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT, BULLETS
    }

    private static final Map<Glyph, ImageIcon> CACHE = new EnumMap<>(Glyph.class);

    private ToolbarIcons() {
    }

    public static ImageIcon icon(final Glyph glyph) {
        return CACHE.computeIfAbsent(glyph, ToolbarIcons::create);
    }

    private static ImageIcon create(final Glyph glyph) {
        final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.BLACK);
            switch (glyph) {
                case NEW -> drawNew(g);
                case OPEN -> drawOpen(g);
                case SAVE -> drawSave(g);
                case PRINT -> drawPrint(g);
                case FIND -> drawFind(g);
                case CUT -> drawCut(g);
                case COPY -> drawCopy(g);
                case PASTE -> drawPaste(g);
                case UNDO -> drawUndo(g);
                case DATE -> drawDate(g);
                case BOLD -> drawLetter(g, "B", Font.BOLD, false);
                case ITALIC -> drawLetter(g, "I", Font.ITALIC, false);
                case UNDERLINE -> drawLetter(g, "U", Font.PLAIN, true);
                case COLOR -> drawColor(g);
                case ALIGN_LEFT -> drawAlign(g, 0);
                case ALIGN_CENTER -> drawAlign(g, 1);
                case ALIGN_RIGHT -> drawAlign(g, 2);
                case BULLETS -> drawBullets(g);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private static void drawNew(final Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(3, 1, 9, 13);
        g.setColor(Color.BLACK);
        g.drawRect(3, 1, 9, 13);
        g.setColor(new Color(220, 220, 220));
        g.fillPolygon(new int[]{9, 12, 9}, new int[]{1, 4, 4}, 3);
        g.setColor(Color.BLACK);
        g.drawLine(9, 1, 9, 4);
        g.drawLine(9, 4, 12, 4);
        g.drawLine(5, 7, 10, 7);
        g.drawLine(5, 9, 10, 9);
        g.drawLine(5, 11, 8, 11);
    }

    private static void drawOpen(final Graphics2D g) {
        g.setColor(new Color(255, 200, 80));
        g.fillRect(1, 6, 14, 8);
        g.fillRect(2, 4, 6, 3);
        g.setColor(Color.BLACK);
        g.drawRect(1, 6, 14, 8);
        g.drawRect(2, 4, 6, 3);
        g.setColor(new Color(255, 230, 140));
        final Path2D lid = new Path2D.Float();
        lid.moveTo(3, 10);
        lid.lineTo(14, 7);
        lid.lineTo(14, 14);
        lid.lineTo(1, 14);
        lid.closePath();
        g.fill(lid);
        g.setColor(Color.BLACK);
        g.draw(lid);
    }

    private static void drawSave(final Graphics2D g) {
        g.setColor(new Color(40, 80, 170));
        g.fillRoundRect(2, 1, 12, 14, 2, 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(2, 1, 12, 14, 2, 2);
        g.setColor(new Color(220, 220, 230));
        g.fillRect(5, 1, 6, 6);
        g.setColor(Color.BLACK);
        g.drawRect(5, 1, 6, 6);
        g.setColor(new Color(180, 180, 190));
        g.fillRect(6, 2, 2, 4);
        g.setColor(Color.WHITE);
        g.fillRect(4, 9, 8, 5);
        g.setColor(Color.BLACK);
        g.drawRect(4, 9, 8, 5);
        g.drawLine(5, 11, 11, 11);
        g.drawLine(5, 13, 10, 13);
    }

    private static void drawPrint(final Graphics2D g) {
        g.setColor(new Color(160, 160, 170));
        g.fillRect(2, 6, 12, 6);
        g.setColor(Color.BLACK);
        g.drawRect(2, 6, 12, 6);
        g.setColor(Color.WHITE);
        g.fillRect(5, 1, 6, 6);
        g.fillRect(5, 10, 6, 5);
        g.setColor(Color.BLACK);
        g.drawRect(5, 1, 6, 6);
        g.drawRect(5, 10, 6, 5);
        g.setColor(new Color(40, 40, 40));
        g.fillOval(4, 8, 2, 2);
        g.fillOval(7, 8, 2, 2);
        g.setColor(Color.BLACK);
        g.drawLine(6, 12, 10, 12);
    }

    private static void drawFind(final Graphics2D g) {
        g.setColor(new Color(80, 80, 90));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Float(2, 2, 8, 8));
        g.drawLine(9, 9, 14, 14);
        g.setStroke(new BasicStroke(1.1f));
        g.draw(new Ellipse2D.Float(1, 3, 6, 6));
        g.drawLine(6, 8, 10, 13);
    }

    private static void drawCut(final Graphics2D g) {
        g.draw(new Ellipse2D.Float(1, 8, 5, 5));
        g.draw(new Ellipse2D.Float(9, 8, 5, 5));
        g.drawLine(5, 9, 11, 2);
        g.drawLine(10, 9, 4, 2);
        g.drawLine(7, 6, 7, 10);
    }

    private static void drawCopy(final Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(5, 4, 8, 10);
        g.setColor(Color.BLACK);
        g.drawRect(5, 4, 8, 10);
        g.setColor(Color.WHITE);
        g.fillRect(2, 1, 8, 10);
        g.setColor(Color.BLACK);
        g.drawRect(2, 1, 8, 10);
        g.drawLine(4, 4, 8, 4);
        g.drawLine(4, 6, 8, 6);
        g.drawLine(4, 8, 7, 8);
    }

    private static void drawPaste(final Graphics2D g) {
        g.setColor(new Color(230, 200, 120));
        g.fillRect(3, 4, 10, 11);
        g.setColor(Color.BLACK);
        g.drawRect(3, 4, 10, 11);
        g.setColor(new Color(200, 160, 80));
        g.fillRect(5, 1, 6, 4);
        g.setColor(Color.BLACK);
        g.drawRect(5, 1, 6, 4);
        g.setColor(Color.WHITE);
        g.fillRect(5, 7, 6, 6);
        g.setColor(Color.BLACK);
        g.drawRect(5, 7, 6, 6);
    }

    private static void drawUndo(final Graphics2D g) {
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(3, 3, 10, 10, 40, 220, Arc2D.OPEN));
        final Path2D arrow = new Path2D.Float();
        arrow.moveTo(3, 4);
        arrow.lineTo(3, 9);
        arrow.lineTo(8, 9);
        arrow.closePath();
        g.fill(arrow);
    }

    private static void drawDate(final Graphics2D g) {
        g.setColor(new Color(220, 60, 60));
        g.fillRect(2, 2, 12, 4);
        g.setColor(Color.WHITE);
        g.fillRect(2, 6, 12, 9);
        g.setColor(Color.BLACK);
        g.drawRect(2, 2, 12, 13);
        g.drawLine(2, 6, 14, 6);
        g.drawLine(5, 1, 5, 4);
        g.drawLine(11, 1, 11, 4);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
        g.drawString("31", 4, 13);
    }

    private static void drawLetter(final Graphics2D g, final String letter, final int style, final boolean underline) {
        g.setFont(new Font(Font.SERIF, style, 13));
        g.drawString(letter, 4, 13);
        if (underline) {
            g.drawLine(3, 14, 12, 14);
        }
    }

    private static void drawColor(final Graphics2D g) {
        g.setFont(new Font(Font.SERIF, Font.BOLD, 11));
        g.drawString("A", 4, 11);
        g.setColor(new Color(200, 30, 30));
        g.fillRect(2, 13, 4, 2);
        g.setColor(new Color(40, 160, 40));
        g.fillRect(6, 13, 4, 2);
        g.setColor(new Color(40, 80, 200));
        g.fillRect(10, 13, 4, 2);
    }

    private static void drawAlign(final Graphics2D g, final int mode) {
        g.setStroke(new BasicStroke(1.2f));
        final int[] widths = {12, 8, 10, 7};
        for (int i = 0; i < widths.length; i++) {
            final int w = widths[i];
            final int y = 2 + i * 3;
            final int x = switch (mode) {
                case 1 -> (SIZE - w) / 2;
                case 2 -> SIZE - 2 - w;
                default -> 2;
            };
            g.drawLine(x, y, x + w, y);
        }
    }

    private static void drawBullets(final Graphics2D g) {
        g.fillOval(2, 3, 3, 3);
        g.fillOval(2, 8, 3, 3);
        g.fillOval(2, 13, 3, 3);
        g.drawLine(7, 4, 14, 4);
        g.drawLine(7, 9, 14, 9);
        g.drawLine(7, 14, 14, 14);
    }
}
