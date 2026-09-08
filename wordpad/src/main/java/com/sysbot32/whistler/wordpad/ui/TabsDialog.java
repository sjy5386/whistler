package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.wordpad.model.MeasurementUnit;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabsDialog extends JDialog {
    private boolean accepted;
    private final MeasurementUnit unit;
    private final JTextField positionField = new JTextField(8);
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final List<Float> stopsPoints = new ArrayList<>();

    public TabsDialog(final JFrame owner, final MeasurementUnit unit, final float[] existing) {
        super(owner, "Tabs", true);
        this.unit = unit;
        this.positionField.setName("tabPosition");
        for (final float stop : existing) {
            this.addStop(stop);
        }
        final JList<String> list = new JList<>(this.listModel);
        final JButton set = new JButton("Set");
        final JButton clear = new JButton("Clear");
        final JButton clearAll = new JButton("Clear All");
        set.addActionListener(e -> this.addStop(this.parseField()));
        clear.addActionListener(e -> {
            final int index = list.getSelectedIndex();
            if (index >= 0) {
                this.stopsPoints.remove(index);
                this.listModel.remove(index);
            }
        });
        clearAll.addActionListener(e -> {
            this.stopsPoints.clear();
            this.listModel.clear();
        });

        final JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(set);
        buttons.add(clear);
        buttons.add(clearAll);

        final JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Tab stop position (" + unit.getDisplayName() + "):"));
        north.add(this.positionField);

        final JButton ok = new JButton("OK");
        final JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            this.accepted = true;
            this.setVisible(false);
        });
        cancel.addActionListener(e -> this.setVisible(false));
        final JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(ok);
        south.add(cancel);

        final JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(north, BorderLayout.NORTH);
        content.add(new JScrollPane(list), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.EAST);
        content.add(south, BorderLayout.SOUTH);
        this.setContentPane(content);
        this.getRootPane().setDefaultButton(ok);
        this.setSize(360, 280);
        this.setLocationRelativeTo(owner);
    }

    public boolean showDialog() {
        this.accepted = false;
        this.setVisible(true);
        return this.accepted;
    }

    public float[] getStopsPoints() {
        Collections.sort(this.stopsPoints);
        final float[] result = new float[this.stopsPoints.size()];
        for (int i = 0; i < this.stopsPoints.size(); i++) {
            result[i] = this.stopsPoints.get(i);
        }
        return result;
    }

    private void addStop(final float points) {
        if (points <= 0f) {
            return;
        }
        this.stopsPoints.add(points);
        this.listModel.addElement(String.format(java.util.Locale.ROOT, "%.2f", this.unit.fromPoints(points)));
    }

    private float parseField() {
        try {
            return this.unit.toPoints(Float.parseFloat(this.positionField.getText().trim()));
        } catch (final NumberFormatException ex) {
            return 0f;
        }
    }
}
