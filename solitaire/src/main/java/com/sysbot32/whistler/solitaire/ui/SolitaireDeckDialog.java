package com.sysbot32.whistler.solitaire.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Classic XP Solitaire Deck picker (Game → Deck...).
 */
public final class SolitaireDeckDialog {
    private SolitaireDeckDialog() {
    }

    /**
     * @return selected card-back index, or {@code -1} if cancelled
     */
    public static int show(final Component parent, final int current) {
        final JPanel grid = new JPanel(new GridLayout(3, 4, 8, 8));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        final ButtonGroup group = new ButtonGroup();
        final JToggleButton[] buttons = new JToggleButton[CardBack.values().length];
        for (int i = 0; i < CardBack.values().length; i++) {
            final int index = i;
            final JToggleButton button = new JToggleButton();
            button.setPreferredSize(new Dimension(84, 112));
            button.setSelected(index == current);
            button.setIcon(new BackIcon(CardBack.at(index)));
            group.add(button);
            buttons[i] = button;
            grid.add(button);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getClickCount() >= 2) {
                        final Window window = SwingUtilities.getWindowAncestor(button);
                        if (window instanceof JDialog dialog) {
                            dialog.setVisible(false);
                            dialog.getRootPane().putClientProperty("deck.index", index);
                        }
                    }
                }
            });
        }

        final JOptionPane pane = new JOptionPane(
                grid,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );
        final JDialog dialog = pane.createDialog(parent, "Select Card Back");
        dialog.setVisible(true);
        final Object doubleClick = dialog.getRootPane().getClientProperty("deck.index");
        if (doubleClick instanceof Integer index) {
            return index;
        }
        if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())) {
            return -1;
        }
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].isSelected()) {
                return i;
            }
        }
        return current;
    }

    private record BackIcon(CardBack back) implements Icon {
        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            this.back.paint((Graphics2D) g, x, y, this.getIconWidth(), this.getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return 72;
        }

        @Override
        public int getIconHeight() {
            return 100;
        }
    }
}
