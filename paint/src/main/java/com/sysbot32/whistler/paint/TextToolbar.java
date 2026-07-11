package com.sysbot32.whistler.paint;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Floating classic-style text toolbar (font / size / bold / italic).
 */
public class TextToolbar extends JDialog {
    private final JComboBox<String> fontCombo;
    private final JSpinner sizeSpinner;
    private final JToggleButton boldButton;
    private final JToggleButton italicButton;
    private Consumer<Font> fontListener = f -> {
    };
    private boolean updating;

    public TextToolbar(final Frame owner, final Font initial) {
        super(owner, "Fonts", false);
        this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        final String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        this.fontCombo = new JComboBox<>(families);
        this.fontCombo.setSelectedItem(initial.getFamily());
        this.fontCombo.setMaximumRowCount(12);

        this.sizeSpinner = new JSpinner(new SpinnerNumberModel(
                Math.max(8, initial.getSize()), 8, 72, 1
        ));
        this.boldButton = new JToggleButton("B");
        this.boldButton.setFont(this.boldButton.getFont().deriveFont(Font.BOLD));
        this.boldButton.setSelected(initial.isBold());
        this.italicButton = new JToggleButton("I");
        this.italicButton.setFont(this.italicButton.getFont().deriveFont(Font.ITALIC));
        this.italicButton.setSelected(initial.isItalic());

        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        panel.add(new JLabel("Font:"));
        panel.add(this.fontCombo);
        panel.add(new JLabel("Size:"));
        panel.add(this.sizeSpinner);
        panel.add(this.boldButton);
        panel.add(this.italicButton);
        this.setContentPane(panel);
        this.pack();
        this.setResizable(false);

        this.fontCombo.addActionListener(e -> this.emitFont());
        this.sizeSpinner.addChangeListener(e -> this.emitFont());
        this.boldButton.addActionListener(e -> this.emitFont());
        this.italicButton.addActionListener(e -> this.emitFont());
    }

    public void setFontListener(final Consumer<Font> fontListener) {
        this.fontListener = Objects.requireNonNull(fontListener);
    }

    public void syncFrom(final Font font) {
        this.updating = true;
        try {
            this.fontCombo.setSelectedItem(font.getFamily());
            this.sizeSpinner.setValue(Math.max(8, font.getSize()));
            this.boldButton.setSelected(font.isBold());
            this.italicButton.setSelected(font.isItalic());
        } finally {
            this.updating = false;
        }
    }

    private void emitFont() {
        if (this.updating) {
            return;
        }
        final String family = (String) this.fontCombo.getSelectedItem();
        final int size = ((Number) this.sizeSpinner.getValue()).intValue();
        int style = Font.PLAIN;
        if (this.boldButton.isSelected()) {
            style |= Font.BOLD;
        }
        if (this.italicButton.isSelected()) {
            style |= Font.ITALIC;
        }
        this.fontListener.accept(new Font(family, style, size));
    }
}
