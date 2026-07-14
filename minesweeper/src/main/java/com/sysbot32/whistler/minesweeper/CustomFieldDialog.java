package com.sysbot32.whistler.minesweeper;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * XP-style Custom Field dialog: height, width, mines.
 */
public final class CustomFieldDialog {
    private CustomFieldDialog() {
    }

    public static Optional<BoardSpec> show(final Component parent, final BoardSpec current) {
        final int initialRows = current.isCustom() ? current.getRows() : BoardSpec.MIN_ROWS;
        final int initialCols = current.isCustom() ? current.getCols() : BoardSpec.MIN_COLS;
        final int initialMines = current.isCustom() ? current.getMines() : BoardSpec.MIN_MINES;

        final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(
                initialRows, BoardSpec.MIN_ROWS, BoardSpec.MAX_ROWS, 1));
        final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(
                initialCols, BoardSpec.MIN_COLS, BoardSpec.MAX_COLS, 1));
        final JSpinner minesSpinner = new JSpinner(new SpinnerNumberModel(
                Math.min(initialMines, BoardSpec.maxMines(initialRows, initialCols)),
                BoardSpec.MIN_MINES,
                BoardSpec.maxMines(initialRows, initialCols),
                1));

        final Runnable syncMineMax = () -> {
            final int rows = (Integer) heightSpinner.getValue();
            final int cols = (Integer) widthSpinner.getValue();
            final int max = BoardSpec.maxMines(rows, cols);
            final SpinnerNumberModel model = (SpinnerNumberModel) minesSpinner.getModel();
            model.setMaximum(max);
            if ((Integer) model.getValue() > max) {
                model.setValue(max);
            }
        };
        heightSpinner.addChangeListener(e -> syncMineMax.run());
        widthSpinner.addChangeListener(e -> syncMineMax.run());

        final JPanel form = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Height:"), gbc);
        gbc.gridx = 1;
        form.add(heightSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Width:"), gbc);
        gbc.gridx = 1;
        form.add(widthSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Mines:"), gbc);
        gbc.gridx = 1;
        form.add(minesSpinner, gbc);

        final int result = JOptionPane.showConfirmDialog(
                parent,
                form,
                "Custom Field",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return Optional.empty();
        }
        try {
            return Optional.of(BoardSpec.custom(
                    (Integer) heightSpinner.getValue(),
                    (Integer) widthSpinner.getValue(),
                    (Integer) minesSpinner.getValue()
            ));
        } catch (final IllegalArgumentException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Custom Field", JOptionPane.ERROR_MESSAGE);
            return Optional.empty();
        }
    }
}
