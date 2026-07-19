package com.sysbot32.whistler.freecell.ui;

import com.sysbot32.whistler.freecell.model.FreeCellOptions;

import javax.swing.*;
import java.awt.*;

/**
 * Classic FreeCell Options: illegal-move messages, quick play, double-click to free cell.
 */
public final class FreeCellOptionsDialog {
    private FreeCellOptionsDialog() {
    }

    public static void show(final Component parent, final FreeCellOptions options) {
        final JCheckBox messages = new JCheckBox(
                "Display messages on illegal moves",
                options.isMessagesOnIllegalMoves()
        );
        final JCheckBox quick = new JCheckBox(
                "Quick play (no animation)",
                options.isQuickPlay()
        );
        final JCheckBox doubleClick = new JCheckBox(
                "Double-click moves card to free cell",
                options.isDoubleClickToFreeCell()
        );

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        panel.add(messages);
        panel.add(Box.createVerticalStrut(6));
        panel.add(quick);
        panel.add(Box.createVerticalStrut(6));
        panel.add(doubleClick);

        final int result = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "Options",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        options.setMessagesOnIllegalMoves(messages.isSelected());
        options.setQuickPlay(quick.isSelected());
        options.setDoubleClickToFreeCell(doubleClick.isSelected());
    }
}
