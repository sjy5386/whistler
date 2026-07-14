package com.sysbot32.whistler.minesweeper;

import javax.swing.*;
import java.awt.*;

/**
 * XP-style Fastest Mine Sweepers dialog.
 */
public final class BestTimesDialog {
    private BestTimesDialog() {
    }

    public static void show(final Component parent, final BestTimes bestTimes) {
        final JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));

        final JPanel rows = new JPanel(new GridLayout(0, 3, 16, 6));
        for (final Difficulty difficulty : Difficulty.values()) {
            rows.add(new JLabel(difficulty.getDisplayName() + ":"));
            rows.add(new JLabel(bestTimes.getTime(difficulty) + " seconds"));
            rows.add(new JLabel(bestTimes.getName(difficulty)));
        }
        panel.add(rows, BorderLayout.CENTER);

        final JButton resetButton = new JButton("Reset Scores");
        final JButton okButton = new JButton("OK");
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.add(resetButton);
        buttons.add(okButton);
        panel.add(buttons, BorderLayout.SOUTH);

        final JDialog dialog = new JDialog(
                parent instanceof Frame frame ? frame : null,
                "Fastest Mine Sweepers",
                true
        );
        dialog.setContentPane(panel);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        okButton.addActionListener(e -> dialog.dispose());
        resetButton.addActionListener(e -> {
            final int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Reset all best times?",
                    "Reset Scores",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                bestTimes.reset();
                dialog.dispose();
                show(parent, bestTimes);
            }
        });

        dialog.getRootPane().setDefaultButton(okButton);
        dialog.setVisible(true);
    }
}
