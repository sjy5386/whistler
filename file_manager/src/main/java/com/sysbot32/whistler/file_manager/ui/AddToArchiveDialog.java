package com.sysbot32.whistler.file_manager.ui;

import com.sysbot32.whistler.file_manager.archive.ArchiveWriteOptions;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * “Add to Archive” dialog for the zip backend only.
 * 7z-only controls (dictionary, solid, SFX, encryption UI stubs, …) are omitted.
 */
public final class AddToArchiveDialog extends JDialog {
    public record Result(Path archive, ArchiveWriteOptions options) {
    }

    private final JTextField archiveField = new JTextField(40);
    private final JComboBox<String> formatBox = new JComboBox<>(new String[]{"Zip"});
    private final JComboBox<LevelItem> levelBox = new JComboBox<>(LevelItem.values());
    private final JComboBox<String> methodBox = new JComboBox<>(new String[]{"Deflate"});
    private final JComboBox<UpdateItem> updateBox = new JComboBox<>(UpdateItem.values());
    private final JComboBox<String> pathModeBox = new JComboBox<>(new String[]{"Relative pathnames"});
    private final JCheckBox deleteAfter = new JCheckBox("Delete files after compression");

    private Result result;

    public AddToArchiveDialog(final Frame owner, final Path defaultArchive) {
        super(owner, "Add to Archive", true);
        Objects.requireNonNull(defaultArchive, "defaultArchive");
        this.archiveField.setText(defaultArchive.toAbsolutePath().normalize().toString());
        this.levelBox.setSelectedItem(LevelItem.NORMAL);
        this.updateBox.setSelectedItem(UpdateItem.ADD_AND_REPLACE);
        this.pathModeBox.setSelectedIndex(0);
        this.pathModeBox.setEnabled(false); // backend always uses relative names
        this.formatBox.setEnabled(false);   // only Zip registered

        this.levelBox.addActionListener(e -> syncMethodEnabled());
        syncMethodEnabled();

        final JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        root.add(buildArchiveRow(), BorderLayout.NORTH);
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

    /** Store (level 0) has no Deflate method. */
    private void syncMethodEnabled() {
        final LevelItem level = (LevelItem) this.levelBox.getSelectedItem();
        this.methodBox.setEnabled(level != LevelItem.STORE);
    }

    private JPanel buildArchiveRow() {
        final JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel("Archive:"), BorderLayout.WEST);
        row.add(this.archiveField, BorderLayout.CENTER);
        final JButton browse = new JButton("…");
        browse.setPreferredSize(new Dimension(40, browse.getPreferredSize().height));
        browse.addActionListener(e -> browseArchive());
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
        y = addPair(form, c, y, "Archive format:", this.formatBox, "Update mode:", this.updateBox);
        y = addPair(form, c, y, "Compression level:", this.levelBox, "Path mode:", this.pathModeBox);
        y = addPair(form, c, y, "Compression method:", this.methodBox, null, null);

        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 4;
        c.weightx = 1;
        final JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        opts.setBorder(BorderFactory.createTitledBorder("Options"));
        opts.add(this.deleteAfter);
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
                        Add to Archive (Zip)

                        · Archive: new or existing .zip path
                        · Compression level / method (Store disables method)
                        · Update mode: add/replace, update, freshen
                        · Delete files after compression

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

    private void browseArchive() {
        final String text = this.archiveField.getText().trim();
        final Path current = text.isEmpty()
                ? Path.of(System.getProperty("user.home"))
                : Path.of(text);
        final Path start = parentDirFor(current);
        final JFileChooser chooser = new JFileChooser(start.toFile());
        chooser.setDialogTitle("Archive");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (!text.isEmpty()) {
            chooser.setSelectedFile(current.toFile());
        }
        chooser.setFileFilter(new FileNameExtensionFilter("Zip archives (*.zip)", "zip", "jar", "war"));
        if (chooser.showDialog(this, "Select") == JFileChooser.APPROVE_OPTION) {
            this.archiveField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static Path parentDirFor(final Path current) {
        if (java.nio.file.Files.isDirectory(current)) {
            return current;
        }
        final Path parent = current.getParent();
        return parent != null ? parent : Path.of(System.getProperty("user.home"));
    }

    private void onOk() {
        final String text = this.archiveField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an archive path.", "Add to Archive",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Path archive = Path.of(text);
        final String name = archive.getFileName() != null
                ? archive.getFileName().toString().toLowerCase() : "";
        if (!name.endsWith(".zip") && !name.endsWith(".jar") && !name.endsWith(".war")) {
            archive = archive.resolveSibling(
                    (archive.getFileName() != null ? archive.getFileName().toString() : "archive") + ".zip");
        }
        final LevelItem level = (LevelItem) this.levelBox.getSelectedItem();
        final UpdateItem update = (UpdateItem) this.updateBox.getSelectedItem();
        final ArchiveWriteOptions options = new ArchiveWriteOptions(
                level == null ? 6 : level.level,
                update == null ? ArchiveWriteOptions.UpdateMode.ADD_AND_REPLACE : update.mode,
                this.deleteAfter.isSelected()
        );
        this.result = new Result(archive.toAbsolutePath().normalize(), options);
        dispose();
    }

    private enum LevelItem {
        STORE("Store", 0),
        FASTEST("Fastest", 1),
        FAST("Fast", 3),
        NORMAL("Normal", 6),
        MAXIMUM("Maximum", 9);

        final String label;
        final int level;

        LevelItem(final String label, final int level) {
            this.label = label;
            this.level = level;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private enum UpdateItem {
        ADD_AND_REPLACE("Add and replace files", ArchiveWriteOptions.UpdateMode.ADD_AND_REPLACE),
        UPDATE_AND_ADD("Update and add files", ArchiveWriteOptions.UpdateMode.UPDATE_AND_ADD),
        FRESHEN("Freshen existing files", ArchiveWriteOptions.UpdateMode.FRESHEN);

        final String label;
        final ArchiveWriteOptions.UpdateMode mode;

        UpdateItem(final String label, final ArchiveWriteOptions.UpdateMode mode) {
            this.label = label;
            this.mode = mode;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
}
