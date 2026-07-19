package com.sysbot32.whistler.paint.ui;

import com.sysbot32.whistler.paint.image.ImagePrintSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;

/**
 * Simple print preview: shows the page imageable area with the fitted bitmap.
 */
public class PrintPreviewDialog extends JDialog {
    public PrintPreviewDialog(
            final Frame owner,
            final BufferedImage image,
            final PageFormat pageFormat
    ) {
        super(owner, "Print Preview", true);
        final PreviewPanel panel = new PreviewPanel(image, pageFormat);
        final JScrollPane scroll = new JScrollPane(panel);
        final JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        final JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(scroll, BorderLayout.CENTER);
        this.getContentPane().add(south, BorderLayout.SOUTH);
        this.setSize(640, 720);
        this.setLocationRelativeTo(owner);
    }

    private static final class PreviewPanel extends JComponent {
        private static final double SCREEN_SCALE = 0.6;
        private final BufferedImage image;
        private final PageFormat pageFormat;

        private PreviewPanel(final BufferedImage image, final PageFormat pageFormat) {
            this.image = image;
            this.pageFormat = pageFormat;
            final int w = (int) Math.ceil(pageFormat.getWidth() * SCREEN_SCALE) + 40;
            final int h = (int) Math.ceil(pageFormat.getHeight() * SCREEN_SCALE) + 40;
            this.setPreferredSize(new Dimension(w, h));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(new Color(128, 128, 128));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.translate(20, 20);
                g2.scale(SCREEN_SCALE, SCREEN_SCALE);

                final int pageW = (int) Math.round(this.pageFormat.getWidth());
                final int pageH = (int) Math.round(this.pageFormat.getHeight());
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, pageW, pageH);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(0, 0, pageW - 1, pageH - 1);

                final Rectangle dest = ImagePrintSupport.fitToPage(
                        this.pageFormat,
                        this.image.getWidth(),
                        this.image.getHeight()
                );
                g2.drawImage(
                        this.image,
                        dest.x,
                        dest.y,
                        dest.x + dest.width,
                        dest.y + dest.height,
                        0,
                        0,
                        this.image.getWidth(),
                        this.image.getHeight(),
                        null
                );
            } finally {
                g2.dispose();
            }
        }
    }
}
