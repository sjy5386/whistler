package com.sysbot32.whistler.freecell;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Classic FreeCell Statistics dialog (session / total / streaks).
 */
public final class FreeCellStatisticsDialog {
    private FreeCellStatisticsDialog() {
    }

    public static void show(final Component parent, final FreeCellStatistics stats) {
        final JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(10, 14, 8, 14));

        final JPanel sections = new JPanel();
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.add(sessionPanel(stats));
        sections.add(Box.createVerticalStrut(8));
        sections.add(totalPanel(stats));
        sections.add(Box.createVerticalStrut(8));
        sections.add(streaksPanel(stats));
        root.add(sections, BorderLayout.CENTER);

        final JButton ok = new JButton("OK");
        final JButton clear = new JButton("Clear");
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.add(ok);
        buttons.add(clear);
        root.add(buttons, BorderLayout.SOUTH);

        final JDialog dialog = new JDialog(
                parent instanceof Frame frame ? frame : null,
                "FreeCell Statistics",
                true
        );
        dialog.setContentPane(root);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        ok.addActionListener(e -> dialog.dispose());
        clear.addActionListener(e -> {
            final int confirm = JOptionPane.showConfirmDialog(
                    dialog,
                    "Clear all FreeCell statistics?",
                    "FreeCell Statistics",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                stats.reset();
                dialog.dispose();
                show(parent, stats);
            }
        });

        dialog.getRootPane().setDefaultButton(ok);
        dialog.setVisible(true);
    }

    private static JPanel sessionPanel(final FreeCellStatistics stats) {
        final JPanel panel = titled("This session");
        panel.add(row("won:", stats.getSessionWon(), stats.sessionWinPercentage()));
        panel.add(row("lost:", stats.getSessionLost(), null));
        return panel;
    }

    private static JPanel totalPanel(final FreeCellStatistics stats) {
        final JPanel panel = titled("Total");
        panel.add(row("won:", stats.getTotalWon(), stats.totalWinPercentage()));
        panel.add(row("lost:", stats.getTotalLost(), null));
        return panel;
    }

    private static JPanel streaksPanel(final FreeCellStatistics stats) {
        final JPanel panel = titled("Streaks");
        panel.add(simpleRow("wins:", stats.getBestWinStreak()));
        panel.add(simpleRow("losses:", stats.getBestLossStreak()));
        panel.add(simpleRow("current:", stats.getCurrentStreak()));
        return panel;
    }

    private static JPanel titled(final String title) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP
                ),
                new EmptyBorder(4, 8, 6, 8)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel row(final String label, final int value, final Double percent) {
        final JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.add(new JLabel(label), BorderLayout.WEST);
        final String text = percent == null
                ? Integer.toString(value)
                : value + "          " + String.format("%.0f%%", percent);
        final JLabel v = new JLabel(text);
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private static JPanel simpleRow(final String label, final int value) {
        final JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.add(new JLabel(label), BorderLayout.WEST);
        final JLabel v = new JLabel(Integer.toString(value));
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(v, BorderLayout.EAST);
        return row;
    }
}
