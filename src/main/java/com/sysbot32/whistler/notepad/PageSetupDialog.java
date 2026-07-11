package com.sysbot32.whistler.notepad;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class PageSetupDialog extends JDialog {
    private final JTextField headerField = new JTextField(28);
    private final JTextField footerField = new JTextField(28);
    private boolean accepted;

    public PageSetupDialog(final Frame owner, final String header, final String footer) {
        super(owner, "Page Setup", true);
        this.headerField.setText(Objects.requireNonNullElse(header, "&f"));
        this.footerField.setText(Objects.requireNonNullElse(footer, "Page &p"));

        final JPanel form = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Header:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(this.headerField, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(new JLabel("Footer:"), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(this.footerField, c);

        final JLabel hint = new JLabel(
                "<html>Codes: &amp;f file, &amp;p page, &amp;d date, &amp;t time, &amp;l/&amp;c/&amp;r align</html>"
        );

        final JButton ok = new JButton("OK");
        final JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            this.accepted = true;
            this.setVisible(false);
        });
        cancel.addActionListener(e -> {
            this.accepted = false;
            this.setVisible(false);
        });
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        final JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(form, BorderLayout.NORTH);
        content.add(hint, BorderLayout.CENTER);
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

    public String getHeader() {
        return this.headerField.getText();
    }

    public String getFooter() {
        return this.footerField.getText();
    }
}
