package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.wordpad.model.MeasurementUnit;
import com.sysbot32.whistler.wordpad.model.WordWrapMode;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

public class OptionsDialog extends JDialog {
    private boolean accepted;
    private final JRadioButton noWrap = new JRadioButton("No wrap");
    private final JRadioButton wrapWindow = new JRadioButton("Wrap to window");
    private final JRadioButton wrapRuler = new JRadioButton("Wrap to ruler");
    private final JRadioButton inches = new JRadioButton("Inches");
    private final JRadioButton centimeters = new JRadioButton("Centimeters");
    private final JRadioButton points = new JRadioButton("Points");
    private final JRadioButton picas = new JRadioButton("Picas");

    public OptionsDialog(final JFrame owner, final WordWrapMode wrap, final MeasurementUnit unit) {
        super(owner, "Options", true);
        this.noWrap.setName("noWrap");
        this.wrapWindow.setName("wrapWindow");
        this.wrapRuler.setName("wrapRuler");
        final ButtonGroup wrapGroup = new ButtonGroup();
        wrapGroup.add(this.noWrap);
        wrapGroup.add(this.wrapWindow);
        wrapGroup.add(this.wrapRuler);
        switch (wrap) {
            case NONE -> this.noWrap.setSelected(true);
            case RULER -> this.wrapRuler.setSelected(true);
            default -> this.wrapWindow.setSelected(true);
        }

        final ButtonGroup unitGroup = new ButtonGroup();
        unitGroup.add(this.inches);
        unitGroup.add(this.centimeters);
        unitGroup.add(this.points);
        unitGroup.add(this.picas);
        switch (unit) {
            case CENTIMETERS -> this.centimeters.setSelected(true);
            case POINTS -> this.points.setSelected(true);
            case PICAS -> this.picas.setSelected(true);
            default -> this.inches.setSelected(true);
        }

        final JPanel wrapPanel = new JPanel();
        wrapPanel.setLayout(new BoxLayout(wrapPanel, BoxLayout.Y_AXIS));
        wrapPanel.setBorder(BorderFactory.createTitledBorder("Word wrap"));
        wrapPanel.add(this.noWrap);
        wrapPanel.add(this.wrapWindow);
        wrapPanel.add(this.wrapRuler);

        final JPanel unitPanel = new JPanel(new GridLayout(0, 1));
        unitPanel.setBorder(BorderFactory.createTitledBorder("Measurement units"));
        unitPanel.add(this.inches);
        unitPanel.add(this.centimeters);
        unitPanel.add(this.points);
        unitPanel.add(this.picas);

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
        content.add(wrapPanel, BorderLayout.NORTH);
        content.add(unitPanel, BorderLayout.CENTER);
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

    public WordWrapMode getWrapMode() {
        if (this.noWrap.isSelected()) {
            return WordWrapMode.NONE;
        }
        if (this.wrapRuler.isSelected()) {
            return WordWrapMode.RULER;
        }
        return WordWrapMode.WINDOW;
    }

    public MeasurementUnit getMeasurementUnit() {
        if (this.centimeters.isSelected()) {
            return MeasurementUnit.CENTIMETERS;
        }
        if (this.points.isSelected()) {
            return MeasurementUnit.POINTS;
        }
        if (this.picas.isSelected()) {
            return MeasurementUnit.PICAS;
        }
        return MeasurementUnit.INCHES;
    }
}
