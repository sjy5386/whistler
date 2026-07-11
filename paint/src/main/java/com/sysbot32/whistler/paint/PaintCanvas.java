package com.sysbot32.whistler.paint;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Scrollable drawing surface that paints the document bitmap.
 */
public class PaintCanvas extends JComponent {
    private Paint paint;

    public PaintCanvas(final Paint paint) {
        this.paint = Objects.requireNonNull(paint);
        this.setBackground(Color.WHITE);
        this.setOpaque(true);
        this.syncPreferredSize();
    }

    public void setPaint(final Paint paint) {
        this.paint = Objects.requireNonNull(paint);
        this.syncPreferredSize();
        this.revalidate();
        this.repaint();
    }

    public void syncPreferredSize() {
        this.setPreferredSize(new Dimension(this.paint.getWidth(), this.paint.getHeight()));
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.drawImage(this.paint.getImage(), 0, 0, null);
        } finally {
            g2.dispose();
        }
    }
}
