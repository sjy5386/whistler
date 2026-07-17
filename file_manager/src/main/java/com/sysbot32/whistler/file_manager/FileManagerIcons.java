package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Classic 7zFM-style glyphs drawn with Graphics2D (no bitmap sprites).
 */
@UtilityClass
public final class FileManagerIcons {
    public static final int TOOL_SIZE = 20;
    public static final int LIST_SIZE = 16;
    /** 7zFM Small Icons roughly mid-size. */
    public static final int SMALL_ICON_SIZE = 24;
    /** 7zFM Large Icons — visibly larger glyphs, not just wider cells. */
    public static final int LARGE_ICON_SIZE = 32;
    /** Compact ↑ for the path-bar Up control. */
    private static final int PATH_UP_SIZE = 12;

    public enum Tool {
        ADD, EXTRACT, TEST, COPY, MOVE, DELETE, INFO
    }

    public enum ListKind {
        FOLDER, FILE, ZIP, UP
    }

    private static final Map<Tool, ImageIcon> TOOL_CACHE = new EnumMap<>(Tool.class);
    private static final Map<ListKind, ImageIcon> LIST_CACHE = new EnumMap<>(ListKind.class);
    private static final Map<ListKind, ImageIcon> SMALL_CACHE = new EnumMap<>(ListKind.class);
    private static final Map<ListKind, ImageIcon> LARGE_CACHE = new EnumMap<>(ListKind.class);
    public static ImageIcon tool(final Tool tool) {
        return TOOL_CACHE.computeIfAbsent(tool, FileManagerIcons::createTool);
    }

    public static ImageIcon list(final ListKind kind) {
        return LIST_CACHE.computeIfAbsent(kind, k -> createList(k, LIST_SIZE));
    }

    public static ImageIcon smallIcon(final ListKind kind) {
        return SMALL_CACHE.computeIfAbsent(kind, k -> createList(k, SMALL_ICON_SIZE));
    }

    public static ImageIcon largeIcon(final ListKind kind) {
        return LARGE_CACHE.computeIfAbsent(kind, k -> createList(k, LARGE_ICON_SIZE));
    }

    /** Icon set for the current view mode. */
    public static ImageIcon forView(final ViewMode mode, final ListKind kind) {
        return switch (mode) {
            case LARGE_ICONS -> largeIcon(kind);
            case SMALL_ICONS -> smallIcon(kind);
            case LIST, DETAILS -> list(kind);
        };
    }

    /** Small ▼ for the address-bar combo button (icon-only — no Unicode that truncates to “…”) */
    public static ImageIcon chevronDown() {
        return CHEVRON_DOWN;
    }

    /** Compact ↑ for the path-bar Up control (not a full folder glyph). */
    public static ImageIcon pathUp() {
        return PATH_UP;
    }

    private static final ImageIcon CHEVRON_DOWN = createChevronDown();
    private static final ImageIcon PATH_UP = createPathUp();

    private static ImageIcon createChevronDown() {
        final int s = 10;
        final BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x404040));
            final GeneralPath p = new GeneralPath();
            p.moveTo(1.5, 3);
            p.lineTo(8.5, 3);
            p.lineTo(5, 7.5);
            p.closePath();
            g.fill(p);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private static ImageIcon createPathUp() {
        final int s = PATH_UP_SIZE;
        final BufferedImage image = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x303030));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            final int mid = s / 2;
            g.drawLine(mid, s - 2, mid, 3);
            g.drawLine(mid, 3, mid - 3, 6);
            g.drawLine(mid, 3, mid + 3, 6);
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private static ImageIcon createTool(final Tool tool) {
        final BufferedImage image = new BufferedImage(TOOL_SIZE, TOOL_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (tool) {
                case ADD -> drawAdd(g);
                case EXTRACT -> drawExtract(g);
                case TEST -> drawTest(g);
                case COPY -> drawCopy(g);
                case MOVE -> drawMove(g);
                case DELETE -> drawDelete(g);
                case INFO -> drawInfo(g);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private static ImageIcon createList(final ListKind kind, final int size) {
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (kind) {
                case FOLDER -> drawFolder(g, size);
                case FILE -> drawFileDoc(g, size);
                case ZIP -> drawZip(g, size);
                case UP -> drawUp(g, size);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    // --- toolbar (inspired by classic 7zFM chrome) ---

    private static void drawAdd(final Graphics2D g) {
        // green +
        g.setColor(new Color(0x2E8B2E));
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(10, 4, 10, 16);
        g.drawLine(4, 10, 16, 10);
    }

    private static void drawExtract(final Graphics2D g) {
        // blue box + down arrow
        g.setColor(new Color(0x2A5DB0));
        g.setStroke(new BasicStroke(1.6f));
        g.drawRect(4, 3, 12, 9);
        g.fillRect(8, 11, 4, 3);
        final GeneralPath arrow = new GeneralPath();
        arrow.moveTo(6, 14);
        arrow.lineTo(14, 14);
        arrow.lineTo(10, 18);
        arrow.closePath();
        g.fill(arrow);
    }

    private static void drawTest(final Graphics2D g) {
        g.setColor(new Color(0x1A7A1A));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(4, 11, 8, 16);
        g.drawLine(8, 16, 16, 5);
    }

    private static void drawCopy(final Graphics2D g) {
        g.setColor(new Color(0xC0C0C0));
        g.fillRect(6, 5, 10, 12);
        g.setColor(new Color(0x404040));
        g.drawRect(6, 5, 10, 12);
        g.setColor(new Color(0xE8E8E8));
        g.fillRect(3, 2, 10, 12);
        g.setColor(new Color(0x303030));
        g.drawRect(3, 2, 10, 12);
        g.setColor(new Color(0x606060));
        g.drawLine(5, 5, 11, 5);
        g.drawLine(5, 8, 11, 8);
    }

    private static void drawMove(final Graphics2D g) {
        g.setColor(new Color(0xE8E8E8));
        g.fillRect(3, 4, 9, 11);
        g.setColor(new Color(0x303030));
        g.drawRect(3, 4, 9, 11);
        g.setColor(new Color(0x2A5DB0));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(9, 10, 17, 10);
        final GeneralPath head = new GeneralPath();
        head.moveTo(14, 6);
        head.lineTo(18, 10);
        head.lineTo(14, 14);
        g.fill(head);
    }

    private static void drawDelete(final Graphics2D g) {
        g.setColor(new Color(0xC02828));
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(5, 5, 15, 15);
        g.drawLine(15, 5, 5, 15);
    }

    private static void drawInfo(final Graphics2D g) {
        g.setColor(new Color(0x2A5DB0));
        g.fillOval(3, 2, 14, 16);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString("i", 8, 14);
    }

    // --- list / path ---

    private static void drawFolder(final Graphics2D g, final int size) {
        final float s = size;
        g.setColor(new Color(0xE8C04A));
        final GeneralPath tab = new GeneralPath();
        tab.moveTo(1, 4);
        tab.lineTo(6, 4);
        tab.lineTo(8, 6);
        tab.lineTo(s - 2, 6);
        tab.lineTo(s - 2, s - 2);
        tab.lineTo(1, s - 2);
        tab.closePath();
        g.fill(tab);
        g.setColor(new Color(0xF5D76E));
        g.fill(new RoundRectangle2D.Float(1, 7, s - 3, s - 9, 1, 1));
        g.setColor(new Color(0xA08030));
        g.draw(tab);
    }

    private static void drawFileDoc(final Graphics2D g, final int size) {
        final int s = size;
        g.setColor(new Color(0xF4F4F4));
        final GeneralPath page = new GeneralPath();
        page.moveTo(3, 1);
        page.lineTo(s - 6, 1);
        page.lineTo(s - 2, 5);
        page.lineTo(s - 2, s - 2);
        page.lineTo(3, s - 2);
        page.closePath();
        g.fill(page);
        g.setColor(new Color(0x808080));
        g.draw(page);
        g.setColor(new Color(0xD0D0D0));
        g.drawLine(s - 6, 1, s - 6, 5);
        g.drawLine(s - 6, 5, s - 2, 5);
        g.setColor(new Color(0xA0A0A0));
        g.drawLine(5, 8, s - 4, 8);
        g.drawLine(5, 11, s - 4, 11);
    }

    private static void drawZip(final Graphics2D g, final int size) {
        drawFileDoc(g, size);
        g.setColor(new Color(0xB08020));
        g.setStroke(new BasicStroke(1.2f));
        final int mid = size / 2;
        g.drawLine(mid, 3, mid, size - 4);
        g.fillRect(mid - 2, 5, 4, 2);
        g.fillRect(mid - 2, 9, 4, 2);
    }

    private static void drawUp(final Graphics2D g, final int size) {
        drawFolder(g, size);
        g.setColor(new Color(0x203060));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        final int cx = size / 2;
        g.drawLine(cx, size - 4, cx, 7);
        g.drawLine(cx, 7, cx - 3, 10);
        g.drawLine(cx, 7, cx + 3, 10);
    }
}
