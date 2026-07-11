package com.sysbot32.whistler.notepad;

import javax.swing.*;
import java.awt.*;

public class ReplaceDialog extends JDialog {
    private final JTextField findField = new JTextField(24);
    private final JTextField replaceField = new JTextField(24);
    private final JCheckBox matchCaseCheckBox = new JCheckBox("Match case");
    private final JButton findNextButton = new JButton("Find Next");
    private final JButton replaceButton = new JButton("Replace");
    private final JButton replaceAllButton = new JButton("Replace All");
    private final JButton cancelButton = new JButton("Cancel");

    public ReplaceDialog(final Frame owner) {
        super(owner, "Replace", false);
        this.setLayout(new BorderLayout(8, 8));

        final JPanel form = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Find what:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(this.findField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Replace with:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(this.replaceField, c);

        c.gridx = 1;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(this.matchCaseCheckBox, c);

        final JPanel buttons = new JPanel(new GridLayout(4, 1, 4, 4));
        buttons.add(this.findNextButton);
        buttons.add(this.replaceButton);
        buttons.add(this.replaceAllButton);
        buttons.add(this.cancelButton);

        final JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(form, BorderLayout.CENTER);
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

    public String getReplaceText() {
        return this.replaceField.getText();
    }

    public boolean isMatchCase() {
        return this.matchCaseCheckBox.isSelected();
    }

    public void setMatchCase(final boolean matchCase) {
        this.matchCaseCheckBox.setSelected(matchCase);
    }

    public void onFindNext(final Runnable action) {
        this.findNextButton.addActionListener(e -> action.run());
    }

    public void onReplace(final Runnable action) {
        this.replaceButton.addActionListener(e -> action.run());
    }

    public void onReplaceAll(final Runnable action) {
        this.replaceAllButton.addActionListener(e -> action.run());
    }

    public void focusFindField() {
        this.findField.requestFocusInWindow();
        this.findField.selectAll();
    }
}
