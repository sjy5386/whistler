package com.sysbot32.whistler.wordpad.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

public class PrintPreviewDialog extends JDialog {
    public PrintPreviewDialog(final JFrame owner, final JTextPane editor, final PageFormat pageFormat) {
        super(owner, "Print Preview", true);
        final PreviewPanel panel = new PreviewPanel(editor, pageFormat);
        final JButton close = new JButton("Close");
        close.addActionListener(e -> this.dispose());
        final JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
        this.getContentPane().add(south, BorderLayout.SOUTH);
        this.setSize(640, 720);
        this.setLocationRelativeTo(owner);
    }

    private static final class PreviewPanel extends JComponent {
        private static final double SCALE = 0.7;
        private final JTextPane editor;
        private final PageFormat pageFormat;

        private PreviewPanel(final JTextPane editor, final PageFormat pageFormat) {
            this.editor = editor;
            this.pageFormat = pageFormat;
            final int w = (int) Math.ceil(pageFormat.getWidth() * SCALE) + 40;
            final int h = (int) Math.ceil(pageFormat.getHeight() * SCALE) + 40;
            this.setPreferredSize(new Dimension(w, h));
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(new Color(128, 128, 128));
                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
                g2.translate(20, 20);
                g2.scale(SCALE, SCALE);
                final int pageW = (int) Math.round(this.pageFormat.getWidth());
                final int pageH = (int) Math.round(this.pageFormat.getHeight());
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, pageW, pageH);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(0, 0, pageW, pageH);
                g2.translate(this.pageFormat.getImageableX(), this.pageFormat.getImageableY());
                try {
                    this.editor.getPrintable(null, null).print(g2, this.pageFormat, 0);
                } catch (final PrinterException ignored) {
                    // preview still shows the page frame
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
