package com.sysbot32.whistler.solitaire.ui;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * XP-style card-back designs (Game → Deck). Index 0 is the classic blue weave.
 */
public enum CardBack {
    BLUE_WEAVE(new Color(0x1A, 0x3A, 0x8A), new Color(0xC8, 0xD4, 0xF0)),
    RED_WEAVE(new Color(0x8A, 0x14, 0x14), new Color(0xF0, 0xC8, 0xC8)),
    GREEN_WEAVE(new Color(0x0E, 0x5A, 0x28), new Color(0xC0, 0xE8, 0xC8)),
    PURPLE_WEAVE(new Color(0x4A, 0x1A, 0x6A), new Color(0xE0, 0xC8, 0xF0)),
    FISH(new Color(0x0A, 0x4A, 0x6A), new Color(0x80, 0xC8, 0xE0)),
    ROBOT(new Color(0x40, 0x40, 0x48), new Color(0xC0, 0xC4, 0xC8)),
    CASTLE(new Color(0x3A, 0x2A, 0x18), new Color(0xD8, 0xC0, 0x80)),
    BEACH(new Color(0xC8, 0xA0, 0x40), new Color(0x40, 0x80, 0xC8)),
    FLOWER(new Color(0x6A, 0x20, 0x40), new Color(0xF0, 0x80, 0xA8)),
    GEOMETRIC(new Color(0x20, 0x20, 0x70), new Color(0xF0, 0xD0, 0x40)),
    BLACK(new Color(0x18, 0x18, 0x18), new Color(0x80, 0x80, 0x80)),
    TIES(new Color(0x10, 0x38, 0x10), new Color(0xE8, 0xE0, 0x70));

    private final Color field;
    private final Color pattern;

    CardBack(final Color field, final Color pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    public static CardBack at(final int index) {
        final CardBack[] all = values();
        if (index < 0) {
            return all[0];
        }
        return all[index % all.length];
    }

    public void paint(final Graphics2D g2, final int x, final int y, final int w, final int h) {
        g2.setColor(this.field);
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(new Color(0x20, 0x20, 0x20));
        g2.drawRoundRect(x, y, w, h, 10, 10);
        g2.setColor(this.pattern);
        g2.drawRoundRect(x + 6, y + 6, w - 12, h - 12, 8, 8);
        switch (this) {
            case FISH -> {
                g2.fillOval(x + w / 2 - 14, y + h / 2 - 8, 22, 14);
                final int[] xs = {x + w / 2 + 8, x + w / 2 + 18, x + w / 2 + 18};
                final int[] ys = {y + h / 2, y + h / 2 - 8, y + h / 2 + 8};
                g2.fillPolygon(xs, ys, 3);
            }
            case ROBOT -> {
                g2.fillRect(x + w / 2 - 10, y + h / 2 - 14, 20, 18);
                g2.fillRect(x + w / 2 - 6, y + h / 2 + 4, 12, 10);
                g2.setColor(this.field);
                g2.fillOval(x + w / 2 - 6, y + h / 2 - 10, 4, 4);
                g2.fillOval(x + w / 2 + 2, y + h / 2 - 10, 4, 4);
            }
            case CASTLE -> {
                g2.fillRect(x + 18, y + 38, w - 36, h - 56);
                g2.fillRect(x + 16, y + 28, 10, 14);
                g2.fillRect(x + w / 2 - 5, y + 24, 10, 18);
                g2.fillRect(x + w - 26, y + 28, 10, 14);
            }
            case FLOWER -> {
                g2.fillOval(x + w / 2 - 6, y + h / 2 - 16, 12, 12);
                g2.fillOval(x + w / 2 - 6, y + h / 2 + 4, 12, 12);
                g2.fillOval(x + w / 2 - 16, y + h / 2 - 6, 12, 12);
                g2.fillOval(x + w / 2 + 4, y + h / 2 - 6, 12, 12);
                g2.setColor(new Color(0xF0, 0xE0, 0x40));
                g2.fillOval(x + w / 2 - 5, y + h / 2 - 5, 10, 10);
            }
            case GEOMETRIC -> {
                for (int i = 0; i < 3; i++) {
                    g2.drawRect(x + 12 + i * 4, y + 16 + i * 4, w - 24 - i * 8, h - 32 - i * 8);
                }
            }
            case TIES -> {
                g2.fillPolygon(
                        new int[]{x + w / 2, x + w / 2 - 10, x + w / 2, x + w / 2 + 10},
                        new int[]{y + 22, y + h / 2, y + h - 22, y + h / 2},
                        4
                );
            }
            case BEACH -> {
                g2.fillOval(x + 14, y + 18, 18, 18);
                g2.fillRect(x + 8, y + h - 28, w - 16, 10);
            }
            default -> {
                for (int row = 0; row < 4; row++) {
                    for (int col = 0; col < 3; col++) {
                        final int px = x + 16 + col * 16;
                        final int py = y + 20 + row * 18;
                        g2.drawLine(px, py + 6, px + 6, py);
                        g2.drawLine(px + 6, py, px + 12, py + 6);
                        g2.drawLine(px + 12, py + 6, px + 6, py + 12);
                        g2.drawLine(px + 6, py + 12, px, py + 6);
                    }
                }
            }
        }
    }
}
