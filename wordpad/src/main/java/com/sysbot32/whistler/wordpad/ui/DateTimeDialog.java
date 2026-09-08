package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.wordpad.model.WordPad;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.util.List;

public class DateTimeDialog extends JDialog {
    private String chosen;
    private final JList<String> list;

    public DateTimeDialog(final JFrame owner, final LocalDateTime when) {
        super(owner, "Date and Time", true);
        final List<String> formats = WordPad.dateTimeFormats(when);
        this.list = new JList<>(formats.toArray(String[]::new));
        this.list.setName("dateTimeFormats");
        this.list.setSelectedIndex(0);

        final JButton ok = new JButton("OK");
        final JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            this.chosen = this.list.getSelectedValue();
            this.setVisible(false);
        });
        cancel.addActionListener(e -> {
            this.chosen = null;
            this.setVisible(false);
        });
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        final JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(new JScrollPane(this.list), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        this.setContentPane(content);
        this.getRootPane().setDefaultButton(ok);
        this.setSize(280, 260);
        this.setLocationRelativeTo(owner);
    }

    public String showDialog() {
        this.chosen = null;
        this.setVisible(true);
        return this.chosen;
    }
}
