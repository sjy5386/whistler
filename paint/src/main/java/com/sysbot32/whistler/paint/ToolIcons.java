package com.sysbot32.whistler.paint;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Classic-style toolbox glyphs for the 16 XP Paint tools (drawn, not text labels).
 */
public final class ToolIcons {
    public static final int SIZE = 16;

    private static final Map<PaintTool, ImageIcon> CACHE = new EnumMap<>(PaintTool.class);

    private ToolIcons() {
    }

    public static ImageIcon icon(final PaintTool tool) {
        return CACHE.computeIfAbsent(tool, ToolIcons::create);
    }

    private static ImageIcon create(final PaintTool tool) {
        final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            // transparent background — button chrome shows through
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, SIZE, SIZE);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.BLACK);
            switch (tool) {
                case FREE_FORM_SELECT -> drawFreeFormSelect(g);
                case SELECT -> drawSelect(g);
                case ERASER -> drawEraser(g);
                case FILL -> drawFill(g);
                case PICK_COLOR -> drawPickColor(g);
                case MAGNIFIER -> drawMagnifier(g);
                case PENCIL -> drawPencil(g);
                case BRUSH -> drawBrush(g);
                case AIRBRUSH -> drawAirbrush(g);
                case TEXT -> drawText(g);
                case LINE -> drawLine(g);
                case CURVE -> drawCurve(g);
                case RECTANGLE -> drawRectangle(g);
                case POLYGON -> drawPolygon(g);
                case ELLIPSE -> drawEllipse(g);
                case ROUNDED_RECTANGLE -> drawRoundedRectangle(g);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private static void drawFreeFormSelect(final Graphics2D g) {
        g.setStroke(dashed());
        final Path2D path = new Path2D.Float();
        path.moveTo(3, 4);
        path.curveTo(6, 1, 11, 2, 13, 5);
        path.curveTo(14, 9, 12, 13, 8, 14);
        path.curveTo(4, 14, 2, 10, 3, 6);
        path.closePath();
        g.draw(path);
    }

    private static void drawSelect(final Graphics2D g) {
        g.setStroke(dashed());
        g.drawRect(2, 2, 11, 11);
    }

    private static void drawEraser(final Graphics2D g) {
        g.setColor(new Color(255, 200, 180));
        final int[] xs = {4, 12, 14, 6};
        final int[] ys = {10, 2, 4, 12};
        g.fillPolygon(xs, ys, 4);
        g.setColor(Color.BLACK);
        g.drawPolygon(xs, ys, 4);
        g.setColor(new Color(220, 220, 220));
        g.fillRect(3, 11, 8, 3);
        g.setColor(Color.BLACK);
        g.drawRect(3, 11, 8, 3);
    }

    private static void drawFill(final Graphics2D g) {
        // paint bucket
        g.setColor(new Color(180, 180, 200));
        g.fillRect(4, 5, 7, 6);
        g.setColor(Color.BLACK);
        g.drawRect(4, 5, 7, 6);
        g.drawLine(4, 5, 2, 8);
        g.drawLine(2, 8, 4, 11);
        // pouring paint
        g.setColor(new Color(40, 80, 200));
        g.fillOval(9, 10, 5, 4);
        g.setColor(Color.BLACK);
        g.drawOval(9, 10, 5, 4);
        // handle
        g.drawArc(7, 2, 5, 5, 200, 140);
    }

    private static void drawPickColor(final Graphics2D g) {
        // eyedropper
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(3, 13, 10, 6);
        g.setColor(new Color(120, 160, 220));
        g.fillOval(9, 2, 5, 5);
        g.setColor(Color.BLACK);
        g.drawOval(9, 2, 5, 5);
        g.drawLine(11, 4, 13, 2);
        g.setColor(new Color(200, 40, 40));
        g.fillRect(2, 12, 3, 2);
    }

    private static void drawMagnifier(final Graphics2D g) {
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Float(2, 2, 9, 9));
        g.drawLine(10, 10, 14, 14);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(11, 11, 14, 14);
    }

    private static void drawPencil(final Graphics2D g) {
        final int[] xs = {3, 11, 13, 5};
        final int[] ys = {13, 5, 7, 15};
        g.setColor(new Color(255, 220, 80));
        g.fillPolygon(xs, ys, 4);
        g.setColor(Color.BLACK);
        g.drawPolygon(xs, ys, 4);
        // tip
        g.setColor(new Color(40, 40, 40));
        g.fillPolygon(new int[]{3, 5, 4}, new int[]{13, 15, 12}, 3);
        // eraser end
        g.setColor(new Color(255, 150, 160));
        g.fillPolygon(new int[]{11, 13, 14, 12}, new int[]{5, 7, 6, 4}, 4);
        g.setColor(Color.BLACK);
        g.drawPolygon(new int[]{11, 13, 14, 12}, new int[]{5, 7, 6, 4}, 4);
    }

    private static void drawBrush(final Graphics2D g) {
        // handle
        g.setColor(new Color(160, 100, 50));
        g.fillRect(6, 2, 4, 8);
        g.setColor(Color.BLACK);
        g.drawRect(6, 2, 4, 8);
        // ferrule
        g.setColor(new Color(180, 180, 180));
        g.fillRect(5, 9, 6, 2);
        g.setColor(Color.BLACK);
        g.drawRect(5, 9, 6, 2);
        // bristles
        g.setColor(new Color(40, 40, 40));
        g.fillPolygon(new int[]{5, 11, 10, 6}, new int[]{11, 11, 15, 15}, 4);
        g.setColor(Color.BLACK);
        g.drawPolygon(new int[]{5, 11, 10, 6}, new int[]{11, 11, 15, 15}, 4);
    }

    private static void drawAirbrush(final Graphics2D g) {
        // spray can body
        g.setColor(new Color(200, 60, 60));
        g.fillRoundRect(3, 6, 7, 9, 2, 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(3, 6, 7, 9, 2, 2);
        g.drawLine(5, 4, 8, 4);
        g.drawLine(6, 4, 6, 6);
        // spray dots
        g.fillOval(11, 3, 2, 2);
        g.fillOval(13, 5, 2, 2);
        g.fillOval(12, 7, 2, 2);
        g.fillOval(14, 8, 1, 1);
    }

    private static void drawText(final Graphics2D g) {
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        g.drawString("A", 3, 13);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(2, 14, 13, 14);
    }

    private static void drawLine(final Graphics2D g) {
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(2, 13, 13, 2);
    }

    private static void drawCurve(final Graphics2D g) {
        g.setStroke(new BasicStroke(1.5f));
        final Path2D path = new Path2D.Float();
        path.moveTo(2, 13);
        path.curveTo(5, 2, 11, 14, 14, 3);
        g.draw(path);
    }

    private static void drawRectangle(final Graphics2D g) {
        g.drawRect(2, 3, 11, 9);
    }

    private static void drawPolygon(final Graphics2D g) {
        final Polygon p = new Polygon(
                new int[]{8, 14, 11, 5, 2},
                new int[]{2, 7, 14, 14, 7},
                5
        );
        g.drawPolygon(p);
    }

    private static void drawEllipse(final Graphics2D g) {
        g.drawOval(2, 3, 12, 10);
    }

    private static void drawRoundedRectangle(final Graphics2D g) {
        g.draw(new RoundRectangle2D.Float(2, 3, 11, 9, 5, 5));
    }

    private static BasicStroke dashed() {
        return new BasicStroke(
                1f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10f,
                new float[]{2f, 2f},
                0f
        );
    }
}
