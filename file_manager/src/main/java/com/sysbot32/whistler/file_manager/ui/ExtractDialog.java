package com.sysbot32.whistler.file_manager.ui;

import com.sysbot32.whistler.file_manager.archive.ArchiveExtractOptions;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * “Extract” dialog for the zip backend (7zFM-style path / overwrite / root-folder options).
 * Password and multi-format controls are omitted until those backends exist.
 */
public final class ExtractDialog extends JDialog {
    public record Result(Path destDir, ArchiveExtractOptions options) {
    }

    private final JTextField destField = new JTextField(40);
    private final JComboBox<PathModeItem> pathModeBox = new JComboBox<>(PathModeItem.values());
    private final JComboBox<OverwriteItem> overwriteBox = new JComboBox<>(OverwriteItem.values());
    private final JCheckBox eliminateRoot = new JCheckBox("Eliminate duplication of root folder");

    private Result result;

    public ExtractDialog(final Frame owner, final Path defaultDest) {
        super(owner, "Extract", true);
        Objects.requireNonNull(defaultDest, "defaultDest");
        this.destField.setText(defaultDest.toAbsolutePath().normalize().toString());
        this.pathModeBox.setSelectedItem(PathModeItem.FULL);
        this.overwriteBox.setSelectedItem(OverwriteItem.OVERWRITE);

        final JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        root.add(buildDestRow(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        this.setContentPane(root);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.pack();
        this.setMinimumSize(new Dimension(Math.max(480, getWidth()), getHeight()));
        this.setLocationRelativeTo(owner);
    }

    public Optional<Result> showDialog() {
        this.result = null;
        this.setVisible(true);
        return Optional.ofNullable(this.result);
    }

    private JPanel buildDestRow() {
        final JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel("Extract to:"), BorderLayout.WEST);
        row.add(this.destField, BorderLayout.CENTER);
        final JButton browse = new JButton("…");
        browse.setPreferredSize(new Dimension(40, browse.getPreferredSize().height));
        browse.addActionListener(e -> browseDest());
        row.add(browse, BorderLayout.EAST);
        return row;
    }

    private JPanel buildForm() {
        final JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = addPair(form, c, y, "Path mode:", this.pathModeBox, "Overwrite mode:", this.overwriteBox);

        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 4;
        c.weightx = 1;
        final JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        opts.setBorder(BorderFactory.createTitledBorder("Options"));
        opts.add(this.eliminateRoot);
        form.add(opts, c);

        return form;
    }

    private static int addPair(
            final JPanel form,
            final GridBagConstraints c,
            final int y,
            final String leftLabel,
            final java.awt.Component leftField,
            final String rightLabel,
            final java.awt.Component rightField
    ) {
        c.gridy = y;
        c.gridwidth = 1;
        c.weightx = 0;
        c.gridx = 0;
        form.add(new JLabel(leftLabel), c);
        c.gridx = 1;
        c.weightx = 0.5;
        form.add(leftField, c);
        if (rightLabel != null && rightField != null) {
            c.gridx = 2;
            c.weightx = 0;
            form.add(new JLabel(rightLabel), c);
            c.gridx = 3;
            c.weightx = 0.5;
            form.add(rightField, c);
        }
        return y + 1;
    }

    private JPanel buildButtons() {
        final JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        final JButton ok = new JButton("OK");
        final JButton cancel = new JButton("Cancel");
        final JButton help = new JButton("Help");
        help.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                """
                        Extract (Zip)

                        · Extract to: destination folder (created if missing)
                        · Path mode: keep archive paths or flatten to file names
                        · Overwrite mode: replace, skip, or auto-rename collisions
                        · Eliminate duplication of root folder: strip a single top-level folder

                        Only the Zip backend is available in this build.""",
                "Help",
                JOptionPane.INFORMATION_MESSAGE
        ));
        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> {
            this.result = null;
            dispose();
        });
        getRootPane().setDefaultButton(ok);
        p.add(ok);
        p.add(cancel);
        p.add(help);
        return p;
    }

    private void browseDest() {
        final String text = this.destField.getText().trim();
        final Path current = text.isEmpty()
                ? Path.of(System.getProperty("user.home"))
                : Path.of(text);
        final Path start = Files.isDirectory(current)
                ? current
                : (current.getParent() != null ? current.getParent() : Path.of(System.getProperty("user.home")));
        final JFileChooser chooser = new JFileChooser(start.toFile());
        chooser.setDialogTitle("Extract to");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (Files.isDirectory(current)) {
            chooser.setSelectedFile(current.toFile());
        }
        if (chooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
            this.destField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onOk() {
        final String text = this.destField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a destination folder.", "Extract",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        final Path dest = Path.of(text).toAbsolutePath().normalize();
        final PathModeItem pathMode = (PathModeItem) this.pathModeBox.getSelectedItem();
        final OverwriteItem overwrite = (OverwriteItem) this.overwriteBox.getSelectedItem();
        final ArchiveExtractOptions options = new ArchiveExtractOptions(
                pathMode == null ? ArchiveExtractOptions.PathMode.FULL_PATHNAMES : pathMode.mode,
                overwrite == null ? ArchiveExtractOptions.OverwriteMode.OVERWRITE : overwrite.mode,
                this.eliminateRoot.isSelected()
        );
        this.result = new Result(dest, options);
        dispose();
    }

    private enum PathModeItem {
        FULL("Full pathnames", ArchiveExtractOptions.PathMode.FULL_PATHNAMES),
        NONE("No pathnames", ArchiveExtractOptions.PathMode.NO_PATHNAMES);

        final String label;
        final ArchiveExtractOptions.PathMode mode;

        PathModeItem(final String label, final ArchiveExtractOptions.PathMode mode) {
            this.label = label;
            this.mode = mode;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private enum OverwriteItem {
        OVERWRITE("Overwrite without prompt", ArchiveExtractOptions.OverwriteMode.OVERWRITE),
        SKIP("Skip existing files", ArchiveExtractOptions.OverwriteMode.SKIP),
        AUTO_RENAME("Auto rename", ArchiveExtractOptions.OverwriteMode.AUTO_RENAME);

        final String label;
        final ArchiveExtractOptions.OverwriteMode mode;

        OverwriteItem(final String label, final ArchiveExtractOptions.OverwriteMode mode) {
            this.label = label;
            this.mode = mode;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
}
