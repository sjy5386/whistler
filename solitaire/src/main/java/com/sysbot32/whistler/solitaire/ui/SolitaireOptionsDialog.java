package com.sysbot32.whistler.solitaire.ui;

import com.sysbot32.whistler.solitaire.model.ScoringMode;
import com.sysbot32.whistler.solitaire.model.SolitaireOptions;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Classic XP Solitaire Options: Draw, Scoring, Timed / Status / Outline / Keep score.
 */
public final class SolitaireOptionsDialog {
    private SolitaireOptionsDialog() {
    }

    /**
     * @return {@code true} if Draw or Scoring changed (caller should deal again)
     */
    public static boolean show(final Component parent, final SolitaireOptions options) {
        final JRadioButton drawOne = new JRadioButton("Draw One", !options.isDrawThree());
        final JRadioButton drawThree = new JRadioButton("Draw Three", options.isDrawThree());
        final ButtonGroup drawGroup = new ButtonGroup();
        drawGroup.add(drawOne);
        drawGroup.add(drawThree);

        final JRadioButton standard = new JRadioButton("Standard",
                options.getScoring() == ScoringMode.STANDARD);
        final JRadioButton vegas = new JRadioButton("Vegas",
                options.getScoring() == ScoringMode.VEGAS);
        final JRadioButton none = new JRadioButton("None",
                options.getScoring() == ScoringMode.NONE);
        final ButtonGroup scoreGroup = new ButtonGroup();
        scoreGroup.add(standard);
        scoreGroup.add(vegas);
        scoreGroup.add(none);

        final JCheckBox timed = new JCheckBox("Timed game", options.isTimed());
        final JCheckBox status = new JCheckBox("Status bar", options.isStatusBar());
        final JCheckBox outline = new JCheckBox("Outline dragging", options.isOutlineDragging());
        final JCheckBox keep = new JCheckBox("Keep score", options.isKeepScore());

        final JPanel drawPanel = titled("Draw", drawOne, drawThree);
        final JPanel scorePanel = titled("Scoring", standard, vegas, none);
        final JPanel displayPanel = titled("Display", timed, status, outline, keep);

        final JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        panel.add(drawPanel);
        panel.add(scorePanel);
        panel.add(displayPanel);

        final int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        final boolean drawChanged = options.isDrawThree() != drawThree.isSelected();
        final ScoringMode nextScoring = standard.isSelected() ? ScoringMode.STANDARD
                : vegas.isSelected() ? ScoringMode.VEGAS : ScoringMode.NONE;
        final boolean scoringChanged = options.getScoring() != nextScoring;

        if (options.getScoring() == ScoringMode.VEGAS && nextScoring != ScoringMode.VEGAS) {
            options.resetVegasBank();
        }

        options.setDrawThree(drawThree.isSelected());
        options.setScoring(nextScoring);
        options.setTimed(timed.isSelected());
        options.setStatusBar(status.isSelected());
        options.setOutlineDragging(outline.isSelected());
        options.setKeepScore(keep.isSelected());
        return drawChanged || scoringChanged;
    }

    private static JPanel titled(final String title, final JComponent... children) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder(title));
        for (final JComponent child : children) {
            child.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(child);
        }
        return panel;
    }
}
