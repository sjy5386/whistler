package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.wordpad.model.DocumentKind;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.EnumMap;
import java.util.Map;

public class NewDocumentDialog extends JDialog {
    private DocumentKind chosen;
    private final Map<DocumentKind, JRadioButton> radios = new EnumMap<>(DocumentKind.class);

    public NewDocumentDialog(final JFrame owner, final DocumentKind initial) {
        super(owner, "New", true);
        final ButtonGroup group = new ButtonGroup();
        final JPanel choices = new JPanel();
        choices.setLayout(new BoxLayout(choices, BoxLayout.Y_AXIS));
        choices.setBorder(BorderFactory.createTitledBorder("New Document Type"));
        for (final DocumentKind kind : DocumentKind.values()) {
            final JRadioButton radio = new JRadioButton(kind.getDisplayName(), kind == initial);
            radio.setName(kind.name());
            group.add(radio);
            this.radios.put(kind, radio);
            choices.add(radio);
            choices.add(Box.createVerticalStrut(4));
        }

        final JButton ok = new JButton("OK");
        final JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            this.chosen = this.selected();
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
        content.add(choices, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        this.setContentPane(content);
        this.getRootPane().setDefaultButton(ok);
        this.pack();
        this.setLocationRelativeTo(owner);
    }

    public DocumentKind showDialog() {
        this.chosen = null;
        this.setVisible(true);
        return this.chosen;
    }

    private DocumentKind selected() {
        for (final Map.Entry<DocumentKind, JRadioButton> entry : this.radios.entrySet()) {
            if (entry.getValue().isSelected()) {
                return entry.getKey();
            }
        }
        return DocumentKind.RICH_TEXT;
    }
}
