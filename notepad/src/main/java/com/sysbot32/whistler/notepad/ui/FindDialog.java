package com.sysbot32.whistler.notepad.ui;

import com.sysbot32.whistler.notepad.model.SearchDirection;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class FindDialog extends JDialog {
    private final JTextField findField = new JTextField(24);
    private final JCheckBox matchCaseCheckBox = new JCheckBox("Match case");
    private final JRadioButton upRadio = new JRadioButton("Up");
    private final JRadioButton downRadio = new JRadioButton("Down", true);
    private final JButton findNextButton = new JButton("Find Next");
    private final JButton cancelButton = new JButton("Cancel");

    public FindDialog(final Frame owner) {
        super(owner, "Find", false);

        final ButtonGroup directionGroup = new ButtonGroup();
        directionGroup.add(this.upRadio);
        directionGroup.add(this.downRadio);

        final JPanel directionPanel = new JPanel(new GridLayout(2, 1));
        directionPanel.setBorder(BorderFactory.createTitledBorder("Direction"));
        directionPanel.add(this.upRadio);
        directionPanel.add(this.downRadio);

        final JPanel form = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel("Find what:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(this.findField, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(this.matchCaseCheckBox, c);

        final JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(form, BorderLayout.CENTER);
        center.add(directionPanel, BorderLayout.EAST);

        final JPanel buttons = new JPanel(new GridLayout(2, 1, 4, 4));
        buttons.add(this.findNextButton);
        buttons.add(this.cancelButton);

        final JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(center, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.EAST);
        this.setContentPane(content);

        this.cancelButton.addActionListener(e -> this.setVisible(false));
        this.getRootPane().setDefaultButton(this.findNextButton);
        this.setResizable(false);
        this.pack();
        this.setLocationRelativeTo(owner);
    }

    public String getFindText() {
        return this.findField.getText();
    }

    public void setFindText(final String text) {
        this.findField.setText(text);
        this.findField.selectAll();
    }

    public boolean isMatchCase() {
        return this.matchCaseCheckBox.isSelected();
    }

    public SearchDirection getDirection() {
        return this.upRadio.isSelected() ? SearchDirection.UP : SearchDirection.DOWN;
    }

    private void setMatchCase(final boolean matchCase) {
        this.matchCaseCheckBox.setSelected(matchCase);
    }

    private void setDirection(final SearchDirection direction) {
        if (direction == SearchDirection.UP) {
            this.upRadio.setSelected(true);
        } else {
            this.downRadio.setSelected(true);
        }
    }

    /**
     * Restore the last find session into the dialog controls (text, match case, direction).
     * Empty {@code text} leaves the field unchanged so a current selection can be applied separately.
     */
    public void applySession(final String text, final boolean matchCase, final SearchDirection direction) {
        if (Objects.nonNull(text) && !text.isEmpty()) {
            this.setFindText(text);
        }
        this.setMatchCase(matchCase);
        this.setDirection(direction);
    }

    public void onFindNext(final Runnable action) {
        this.findNextButton.addActionListener(e -> action.run());
    }

    public void focusFindField() {
        this.findField.requestFocusInWindow();
        this.findField.selectAll();
    }
}
