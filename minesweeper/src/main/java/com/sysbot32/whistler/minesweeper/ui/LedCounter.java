package com.sysbot32.whistler.minesweeper.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Three-digit LED-style counter used for remaining mines and the timer.
 */
public class LedCounter extends JPanel {
    private static final Color LED_BG = new Color(0x20, 0x00, 0x00);
    private static final Color LED_FG = new Color(0xFF, 0x20, 0x20);

    private final JLabel label = new JLabel("000", SwingConstants.CENTER);

    public LedCounter() {
        this.setLayout(new BorderLayout());
        this.setBackground(LED_BG);
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        this.label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        this.label.setForeground(LED_FG);
        this.label.setOpaque(true);
        this.label.setBackground(LED_BG);
        this.add(this.label, BorderLayout.CENTER);
    }

    public void setValue(final int value) {
        final int clamped = Math.max(-99, Math.min(999, value));
        if (clamped < 0) {
            this.label.setText(String.format("-%02d", Math.abs(clamped)));
        } else {
            this.label.setText(String.format("%03d", clamped));
        }
    }
}
