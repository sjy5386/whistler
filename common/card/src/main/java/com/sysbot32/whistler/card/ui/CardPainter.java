package com.sysbot32.whistler.card.ui;

import com.sysbot32.whistler.card.Card;
import com.sysbot32.whistler.card.Rank;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Shared Swing face art for FreeCell and Solitaire (pips + J/Q/K figures, no bitmaps).
 */
public final class CardPainter {
    public static final int WIDTH = 72;
    public static final int HEIGHT = 100;

    static final Color CARD_FACE = new Color(0xFF, 0xFF, 0xF0);
    static final Color CARD_SELECTED = new Color(0xFF, 0xFF, 0x99);
    static final Color CARD_BORDER = new Color(0x20, 0x20, 0x20);
    static final Color RED_INK = new Color(0xC0, 0x00, 0x00);
    static final Color BLACK_INK = new Color(0x10, 0x10, 0x10);

    private CardPainter() {
    }

    public static void paint(final Graphics2D g2, final int x, final int y, final Card card,
                             final boolean selected) {
        g2.setColor(selected ? CARD_SELECTED : CARD_FACE);
        g2.fillRoundRect(x, y, WIDTH, HEIGHT, 10, 10);
        g2.setColor(CARD_BORDER);
        g2.drawRoundRect(x, y, WIDTH, HEIGHT, 10, 10);

        final Color ink = card.isRed() ? RED_INK : BLACK_INK;
        g2.setColor(ink);

        final String rank = card.getRank().getLabel();
        final String suit = card.getSuit().getSymbol();

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g2.drawString(rank, x + 5, y + 15);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.drawString(suit, x + 5, y + 28);

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        final FontMetrics fmRank = g2.getFontMetrics();
        g2.drawString(rank, x + WIDTH - 5 - fmRank.stringWidth(rank), y + HEIGHT - 18);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        final FontMetrics fmSuit = g2.getFontMetrics();
        g2.drawString(suit, x + WIDTH - 5 - fmSuit.stringWidth(suit), y + HEIGHT - 5);

        final Rank r = card.getRank();
        if (r == Rank.ACE) {
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
            final FontMetrics fm = g2.getFontMetrics();
            g2.drawString(suit, x + (WIDTH - fm.stringWidth(suit)) / 2,
                    y + HEIGHT / 2 + fm.getAscent() / 2 - 6);
        } else if (r.ordinal() >= Rank.TWO.ordinal() && r.ordinal() <= Rank.TEN.ordinal()) {
            paintPips(g2, x, y, card);
        } else {
            paintFaceCard(g2, x, y, card);
        }
    }

    /**
     * Classic pip grid for ranks 2–10. Length equals the rank value.
     */
    public static float[][] pipLayout(final int rankValue) {
        return switch (rankValue) {
            case 2 -> new float[][]{{0.5f, 0.28f}, {0.5f, 0.72f}};
            case 3 -> new float[][]{{0.5f, 0.26f}, {0.5f, 0.50f}, {0.5f, 0.74f}};
            case 4 -> new float[][]{
                    {0.32f, 0.28f}, {0.68f, 0.28f},
                    {0.32f, 0.72f}, {0.68f, 0.72f}
            };
            case 5 -> new float[][]{
                    {0.32f, 0.28f}, {0.68f, 0.28f},
                    {0.50f, 0.50f},
                    {0.32f, 0.72f}, {0.68f, 0.72f}
            };
            case 6 -> new float[][]{
                    {0.32f, 0.28f}, {0.68f, 0.28f},
                    {0.32f, 0.50f}, {0.68f, 0.50f},
                    {0.32f, 0.72f}, {0.68f, 0.72f}
            };
            case 7 -> new float[][]{
                    {0.32f, 0.26f}, {0.68f, 0.26f},
                    {0.50f, 0.38f},
                    {0.32f, 0.50f}, {0.68f, 0.50f},
                    {0.32f, 0.74f}, {0.68f, 0.74f}
            };
            case 8 -> new float[][]{
                    {0.32f, 0.24f}, {0.68f, 0.24f},
                    {0.50f, 0.36f},
                    {0.32f, 0.48f}, {0.68f, 0.48f},
                    {0.50f, 0.60f},
                    {0.32f, 0.76f}, {0.68f, 0.76f}
            };
            case 9 -> new float[][]{
                    {0.32f, 0.24f}, {0.68f, 0.24f},
                    {0.32f, 0.40f}, {0.68f, 0.40f},
                    {0.50f, 0.50f},
                    {0.32f, 0.60f}, {0.68f, 0.60f},
                    {0.32f, 0.76f}, {0.68f, 0.76f}
            };
            case 10 -> new float[][]{
                    {0.32f, 0.22f}, {0.68f, 0.22f},
                    {0.50f, 0.32f},
                    {0.32f, 0.40f}, {0.68f, 0.40f},
                    {0.32f, 0.58f}, {0.68f, 0.58f},
                    {0.50f, 0.68f},
                    {0.32f, 0.78f}, {0.68f, 0.78f}
            };
            default -> new float[0][];
        };
    }

    private static void paintPips(final Graphics2D g2, final int x, final int y, final Card card) {
        final String suit = card.getSuit().getSymbol();
        final int n = card.getRank().getValue();
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, n >= 9 ? 13 : 15));
        final FontMetrics fm = g2.getFontMetrics();
        final int sw = fm.stringWidth(suit);
        final int sh = fm.getAscent();

        final int left = x + 14;
        final int top = y + 18;
        final int w = WIDTH - 28;
        final int h = HEIGHT - 36;
        for (final float[] p : pipLayout(n)) {
            final int px = left + Math.round(p[0] * w) - sw / 2;
            final int py = top + Math.round(p[1] * h) + sh / 3;
            g2.drawString(suit, px, py);
        }
    }

    private static void paintFaceCard(final Graphics2D g2, final int x, final int y, final Card card) {
        final Color ink = card.isRed() ? RED_INK : BLACK_INK;
        final int cx = x + WIDTH / 2;
        final int cy = y + HEIGHT / 2 + 4;

        g2.setColor(new Color(0xF5, 0xEB, 0xD0));
        g2.fillRoundRect(x + 16, y + 30, WIDTH - 32, HEIGHT - 48, 8, 8);
        g2.setColor(ink);
        g2.drawRoundRect(x + 16, y + 30, WIDTH - 32, HEIGHT - 48, 8, 8);

        g2.setColor(new Color(0xFF, 0xE0, 0xBD));
        g2.fillOval(cx - 10, cy - 22, 20, 18);
        g2.setColor(ink);
        g2.drawOval(cx - 10, cy - 22, 20, 18);

        g2.fillOval(cx - 5, cy - 15, 3, 3);
        g2.fillOval(cx + 2, cy - 15, 3, 3);

        switch (card.getRank()) {
            case JACK -> {
                g2.setColor(ink);
                g2.fillRect(cx - 11, cy - 24, 22, 5);
                g2.fillRect(cx - 8, cy - 28, 10, 5);
                g2.setColor(new Color(0x3A, 0x6E, 0xA5));
                g2.fillRoundRect(cx - 12, cy - 4, 24, 22, 6, 6);
                g2.setColor(ink);
                g2.drawRoundRect(cx - 12, cy - 4, 24, 22, 6, 6);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                g2.drawString("J", cx - 3, cy + 10);
            }
            case QUEEN -> {
                g2.setColor(new Color(0xE8, 0xC4, 0x00));
                final int[] xs = {cx - 11, cx - 7, cx - 3, cx, cx + 3, cx + 7, cx + 11, cx + 11, cx - 11};
                final int[] ys = {cy - 20, cy - 28, cy - 22, cy - 29, cy - 22, cy - 28, cy - 20, cy - 18, cy - 18};
                g2.fillPolygon(xs, ys, xs.length);
                g2.setColor(ink);
                g2.drawPolygon(xs, ys, xs.length);
                g2.setColor(new Color(0x8B, 0x00, 0x8B));
                g2.fillRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                g2.setColor(ink);
                g2.drawRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                g2.drawString("Q", cx - 4, cy + 11);
            }
            case KING -> {
                g2.setColor(new Color(0xE8, 0xC4, 0x00));
                final int[] xs = {cx - 11, cx - 6, cx - 2, cx + 2, cx + 6, cx + 11, cx + 11, cx - 11};
                final int[] ys = {cy - 20, cy - 28, cy - 22, cy - 29, cy - 22, cy - 28, cy - 18, cy - 18};
                g2.fillPolygon(xs, ys, xs.length);
                g2.setColor(new Color(0xDC, 0x14, 0x3C));
                g2.fillOval(cx - 2, cy - 30, 4, 4);
                g2.setColor(new Color(0x8B, 0x00, 0x00));
                g2.fillRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                g2.setColor(ink);
                g2.drawRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                g2.setColor(new Color(0x5C, 0x40, 0x33));
                g2.fillOval(cx - 7, cy - 8, 14, 8);
                g2.setColor(ink);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                g2.drawString("K", cx - 4, cy + 11);
            }
            default -> {
                // Ace handled separately.
            }
        }

        g2.setColor(ink);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        final String suit = card.getSuit().getSymbol();
        final FontMetrics fm = g2.getFontMetrics();
        g2.drawString(suit, cx - fm.stringWidth(suit) / 2, cy + 28);
    }
}
