package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.wordpad.model.MeasurementUnit;
import com.sysbot32.whistler.wordpad.model.WordPad;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

/**
 * Simple inch/cm ruler with left and first-line indent markers.
 */
public class RulerBar extends JComponent {
    static final float PAGE_WIDTH_POINTS = 6.5f * 72f;
    private static final int HEIGHT = 24;
    private static final int LEFT_PAD = 8;

    private final WordPad wordPad;
    private final JTextPane editor;
    private MeasurementUnit unit = MeasurementUnit.INCHES;
    private IntConsumer onChange = offset -> {
    };
    private boolean draggingLeft;
    private boolean draggingFirst;

    public RulerBar(final WordPad wordPad, final JTextPane editor) {
        this.wordPad = wordPad;
        this.editor = editor;
        this.setName("ruler");
        this.setPreferredSize(new Dimension(400, HEIGHT));
        this.setBackground(new Color(236, 233, 216));
        this.setOpaque(true);
        final MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                final int offset = Math.max(editor.getCaretPosition(), 0);
                final float left = RulerBar.this.wordPad.getLeftIndent(offset);
                final float first = left + RulerBar.this.wordPad.getFirstLineIndent(offset);
                final int leftX = RulerBar.this.pointsToX(left);
                final int firstX = RulerBar.this.pointsToX(first);
                RulerBar.this.draggingLeft = Math.abs(e.getX() - leftX) <= 6 && e.getY() > HEIGHT / 2;
                RulerBar.this.draggingFirst = Math.abs(e.getX() - firstX) <= 6 && e.getY() <= HEIGHT / 2;
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                final int offset = Math.max(editor.getCaretPosition(), 0);
                final float points = Math.max(0f, RulerBar.this.xToPoints(e.getX()));
                if (RulerBar.this.draggingLeft) {
                    final float first = RulerBar.this.wordPad.getFirstLineIndent(offset);
                    RulerBar.this.wordPad.setParagraphIndents(
                            offset, offset, points, RulerBar.this.wordPad.getRightIndent(offset), first);
                    RulerBar.this.onChange.accept(offset);
                    RulerBar.this.repaint();
                } else if (RulerBar.this.draggingFirst) {
                    final float left = RulerBar.this.wordPad.getLeftIndent(offset);
                    RulerBar.this.wordPad.setParagraphIndents(
                            offset, offset, left, RulerBar.this.wordPad.getRightIndent(offset), points - left);
                    RulerBar.this.onChange.accept(offset);
                    RulerBar.this.repaint();
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                RulerBar.this.draggingLeft = false;
                RulerBar.this.draggingFirst = false;
            }
        };
        this.addMouseListener(mouse);
        this.addMouseMotionListener(mouse);
    }

    public void setUnit(final MeasurementUnit unit) {
        this.unit = unit;
        this.repaint();
    }

    public void setOnChange(final IntConsumer onChange) {
        this.onChange = onChange;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(this.getBackground());
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            final float maxPoints = Math.max(PAGE_WIDTH_POINTS, this.getWidth() - LEFT_PAD);
            final float step = this.unit.toPoints(1f);
            int tick = 0;
            for (float p = 0; p <= maxPoints + 0.1f; p += step / 2f) {
                final int x = this.pointsToX(p);
                final boolean major = Math.abs((p / step) - Math.round(p / step)) < 0.01f;
                g2.drawLine(x, major ? 4 : 10, x, HEIGHT - 6);
                if (major) {
                    g2.drawString(String.valueOf(tick), x + 2, 11);
                    tick++;
                }
            }
            final int offset = Math.max(this.editor.getCaretPosition(), 0);
            final float left = this.wordPad.getLength() == 0 ? 0f : this.wordPad.getLeftIndent(offset);
            final float first = left + (this.wordPad.getLength() == 0 ? 0f : this.wordPad.getFirstLineIndent(offset));
            this.drawMarker(g2, this.pointsToX(first), 3, true);
            this.drawMarker(g2, this.pointsToX(left), HEIGHT - 8, false);
        } finally {
            g2.dispose();
        }
    }

    private void drawMarker(final Graphics2D g2, final int x, final int y, final boolean up) {
        g2.setColor(Color.BLACK);
        final int[] xs = {x - 4, x + 4, x};
        final int[] ys = up ? new int[]{y + 8, y + 8, y} : new int[]{y, y, y + 8};
        g2.fillPolygon(xs, ys, 3);
    }

    private int pointsToX(final float points) {
        return LEFT_PAD + Math.round(points);
    }

    private float xToPoints(final int x) {
        return Math.max(0, x - LEFT_PAD);
    }
}
