package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.wordpad.model.MeasurementUnit;
import com.sysbot32.whistler.wordpad.model.ParagraphAlignment;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class ParagraphDialog extends JDialog {
    private boolean accepted;
    private final JTextField leftField = new JTextField(8);
    private final JTextField rightField = new JTextField(8);
    private final JTextField firstField = new JTextField(8);
    private final JComboBox<ParagraphAlignment> alignmentCombo =
            new JComboBox<>(ParagraphAlignment.values());
    private final MeasurementUnit unit;

    public ParagraphDialog(
            final JFrame owner,
            final MeasurementUnit unit,
            final float leftPoints,
            final float rightPoints,
            final float firstPoints,
            final ParagraphAlignment alignment
    ) {
        super(owner, "Paragraph", true);
        this.unit = unit;
        this.leftField.setName("leftIndent");
        this.rightField.setName("rightIndent");
        this.firstField.setName("firstLineIndent");
        this.leftField.setText(format(leftPoints));
        this.rightField.setText(format(rightPoints));
        this.firstField.setText(format(firstPoints));
        this.alignmentCombo.setSelectedItem(alignment);

        final JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("Indentation (" + unit.getDisplayName() + ")"));
        form.add(new JLabel("Left:"));
        form.add(this.leftField);
        form.add(new JLabel("Right:"));
        form.add(this.rightField);
        form.add(new JLabel("First line:"));
        form.add(this.firstField);
        form.add(new JLabel("Alignment:"));
        form.add(this.alignmentCombo);

        final JButton ok = new JButton("OK");
        final JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            this.accepted = true;
            this.setVisible(false);
        });
        cancel.addActionListener(e -> this.setVisible(false));
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        final JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        this.setContentPane(content);
        this.getRootPane().setDefaultButton(ok);
        this.pack();
        this.setLocationRelativeTo(owner);
    }

    public boolean showDialog() {
        this.accepted = false;
        this.setVisible(true);
        return this.accepted;
    }

    public float getLeftPoints() {
        return parse(this.leftField.getText());
    }

    public float getRightPoints() {
        return parse(this.rightField.getText());
    }

    public float getFirstLinePoints() {
        return parse(this.firstField.getText());
    }

    public ParagraphAlignment getAlignment() {
        return (ParagraphAlignment) this.alignmentCombo.getSelectedItem();
    }

    private String format(final float points) {
        return String.format(java.util.Locale.ROOT, "%.2f", this.unit.fromPoints(points));
    }

    private float parse(final String text) {
        try {
            return this.unit.toPoints(Float.parseFloat(text.trim()));
        } catch (final NumberFormatException ex) {
            return 0f;
        }
    }
}
