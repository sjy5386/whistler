package com.sysbot32.whistler.file_manager;

import com.sysbot32.whistler.config.Config;
import lombok.extern.slf4j.Slf4j;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Shell: menu, toolbar (Add/Extract/Test/Copy/Move/Delete/Info), dual panels, status bar.
 * Keyboard map follows 7-Zip File Manager (Windows) conventions.
 */
@Slf4j
public final class FileManagerFrame extends JFrame {
    private static final String APP_TITLE = "File Manager";
    private static final String KEY_LEFT = "leftPath";
    private static final String KEY_RIGHT = "rightPath";
    private static final String KEY_DUAL = "dualPanel";
    private static final String KEY_SHOW_HIDDEN = "showHidden";

    private final Config config;
    private final FilePanel leftPanel;
    private final FilePanel rightPanel;
    private final JSplitPane splitPane;
    private final JLabel statusLabel = new JLabel(" Ready ");
    private final JPanel statusBar = new JPanel(new BorderLayout());

    private FilePanel activePanel;
    private boolean dualPanel;

    public FileManagerFrame(final Config config) {
        super(APP_TITLE);
        this.config = Objects.requireNonNull(config, "config");

        final boolean showHidden = Boolean.parseBoolean(this.config.get(KEY_SHOW_HIDDEN, "true"));
        final Path defaultPath = Path.of(System.getProperty("user.home"));
        final BrowseLocation leftLoc = loadLocation(this.config.get(KEY_LEFT), defaultPath);
        final BrowseLocation rightLoc = loadLocation(this.config.get(KEY_RIGHT), defaultPath);

        this.leftPanel = new FilePanel(leftLoc, showHidden);
        this.rightPanel = new FilePanel(rightLoc, showHidden);
        this.activePanel = this.leftPanel;
        // Default single panel (F9 toggles dual) — matches how many people use 7zFM
        this.dualPanel = Boolean.parseBoolean(this.config.get(KEY_DUAL, "false"));

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setSize(900, 600);
        this.setMinimumSize(new Dimension(640, 400));

        this.leftPanel.setActivationListener(this::setActivePanel);
        this.rightPanel.setActivationListener(this::setActivePanel);
        this.leftPanel.setStatusListener(this::setStatus);
        this.rightPanel.setStatusListener(this::setStatus);
        this.leftPanel.setOpenHandler(this::openActive);
        this.rightPanel.setOpenHandler(this::openActive);
        // Tab switches panels (7zFM); disable default focus traversal on panel controls
        this.leftPanel.getTable().setFocusTraversalKeysEnabled(false);
        this.rightPanel.getTable().setFocusTraversalKeysEnabled(false);
        this.leftPanel.getPathField().setFocusTraversalKeysEnabled(false);
        this.rightPanel.getPathField().setFocusTraversalKeysEnabled(false);

        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftPanel, this.rightPanel);
        this.splitPane.setResizeWeight(0.5);
        this.splitPane.setContinuousLayout(true);
        this.splitPane.setBorder(null);
        this.splitPane.setDividerSize(0);
        // Avoid one-sided split leaving a focus/divider artifact in single-panel mode
        this.splitPane.setFocusable(false);

        final JPanel content = new JPanel(new BorderLayout());
        content.setBorder(null);
        content.add(createToolBar(), BorderLayout.NORTH);
        content.add(this.splitPane, BorderLayout.CENTER);

        // Subtle top separator only — not a focus-colored outline
        this.statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0xD0D0D0)),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        this.statusBar.add(this.statusLabel, BorderLayout.WEST);
        content.add(this.statusBar, BorderLayout.SOUTH);

        this.setContentPane(content);
        this.setJMenuBar(createMenuBar());
        this.installKeyBindings(content);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                FileManagerFrame.this.exit();
            }
        });

        this.applyDualPanelVisibility();
        this.setActivePanel(this.leftPanel);
        SwingUtilities.invokeLater(() -> {
            if (FileManagerFrame.this.dualPanel) {
                this.splitPane.setDividerLocation(0.5);
            }
            this.activePanel.requestTableFocus();
        });
    }

    private static BrowseLocation loadLocation(final String stored, final Path fallback) {
        if (stored == null || stored.isBlank()) {
            return BrowseLocation.disk(fallback);
        }
        try {
            final BrowseLocation loc = FilePanel.parseDisplayPath(stored);
            if (loc.isDisk() && !Files.isDirectory(loc.path())) {
                return BrowseLocation.disk(fallback);
            }
            if (loc.isZip() && !Files.isRegularFile(loc.path())) {
                return BrowseLocation.disk(fallback);
            }
            return loc;
        } catch (final Exception ex) {
            return BrowseLocation.disk(fallback);
        }
    }

    private void setActivePanel(final FilePanel panel) {
        this.activePanel = panel;
        // Active-panel chrome is only useful with two panels; single-panel blue outline looks like a stray line.
        this.leftPanel.setActiveBorder(this.dualPanel, panel == this.leftPanel);
        this.rightPanel.setActiveBorder(this.dualPanel, panel == this.rightPanel);
    }

    private FilePanel inactivePanel() {
        return this.activePanel == this.leftPanel ? this.rightPanel : this.leftPanel;
    }

    private void setStatus(final String message) {
        this.statusLabel.setText(" " + message + " ");
    }

    private JToolBar createToolBar() {
        final JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setRollover(true);
        bar.add(toolButton(FileManagerIcons.Tool.ADD, "Add", "Add to archive (zip)", this::actionAdd));
        bar.add(toolButton(FileManagerIcons.Tool.EXTRACT, "Extract", "Extract", this::actionExtract));
        bar.add(toolButton(FileManagerIcons.Tool.TEST, "Test", "Test archive", this::actionTest));
        bar.addSeparator();
        bar.add(toolButton(FileManagerIcons.Tool.COPY, "Copy", "Copy (F5)", this::actionCopy));
        bar.add(toolButton(FileManagerIcons.Tool.MOVE, "Move", "Move (F6)", this::actionMove));
        bar.add(toolButton(FileManagerIcons.Tool.DELETE, "Delete", "Delete", this::actionDelete));
        bar.addSeparator();
        bar.add(toolButton(FileManagerIcons.Tool.INFO, "Info", "Properties", this::actionInfo));
        return bar;
    }

    private static JButton toolButton(
            final FileManagerIcons.Tool icon,
            final String text,
            final String tip,
            final Runnable action
    ) {
        final JButton button = new JButton(text, FileManagerIcons.tool(icon));
        button.setToolTipText(tip);
        button.setFocusable(false);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setMargin(new Insets(2, 6, 2, 6));
        button.addActionListener(e -> action.run());
        return button;
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(menuItem("Open", KeyEvent.VK_ENTER, 0, this::openActive));
        fileMenu.add(menuItem("Open Inside", KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK, this::openInside));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Rename", KeyEvent.VK_F2, 0, this::actionRename));
        fileMenu.add(menuItem("Copy To…", KeyEvent.VK_F5, 0, this::actionCopy));
        fileMenu.add(menuItem("Move To…", KeyEvent.VK_F6, 0, this::actionMove));
        fileMenu.add(menuItem("Delete", KeyEvent.VK_DELETE, 0, this::actionDelete));
        fileMenu.add(menuItem("Properties", KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK, this::actionInfo));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Create Folder", KeyEvent.VK_F7, 0, this::actionMkdir));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Add to Archive…", 0, 0, this::actionAdd));
        fileMenu.add(menuItem("Extract…", 0, 0, this::actionExtract));
        fileMenu.add(menuItem("Test Archive", 0, 0, this::actionTest));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK, this::exit));

        final JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(menuItem("Select All", KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, this::selectAll));

        final JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(menuItem("2 Panels", KeyEvent.VK_F9, 0, this::toggleDualPanel));
        viewMenu.add(menuItem("Up One Level", KeyEvent.VK_BACK_SPACE, 0, this::goUp));
        viewMenu.add(menuItem("Refresh", KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, this::refreshActive));

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(menuItem("About File Manager…", 0, 0, this::showAbout));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private JMenuItem menuItem(final String text, final int keyCode, final int modifiers, final Runnable action) {
        final JMenuItem item = new JMenuItem(text);
        if (keyCode != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        }
        item.addActionListener(e -> action.run());
        return item;
    }

    private void installKeyBindings(final JComponent root) {
        // Window-level map so F-keys work while the table has focus (7zFM core map)
        final InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap am = root.getActionMap();

        bind(im, am, "open", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), () -> {
            if (pathFieldFocused()) {
                return;
            }
            openActive();
        });
        bind(im, am, "up", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), () -> {
            if (pathFieldFocused()) {
                return;
            }
            goUp();
        });
        bind(im, am, "rename", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), this::actionRename);
        bind(im, am, "copy", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this::actionCopy);
        bind(im, am, "move", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), this::actionMove);
        bind(im, am, "mkdir", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), this::actionMkdir);
        bind(im, am, "delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), () -> {
            if (pathFieldFocused()) {
                return;
            }
            actionDelete();
        });
        bind(im, am, "dual", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), this::toggleDualPanel);
        bind(im, am, "tabPanels", KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), this::switchPanel);
        bind(im, am, "refresh", KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), this::refreshActive);
        bind(im, am, "openInside", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK), this::openInside);
        bind(im, am, "info", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), this::actionInfo);
        bind(im, am, "insertToggle", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), this::toggleInsertSelection);
    }

    private boolean pathFieldFocused() {
        return this.leftPanel.getPathField().isFocusOwner()
                || this.rightPanel.getPathField().isFocusOwner();
    }

    private void toggleInsertSelection() {
        if (pathFieldFocused()) {
            return;
        }
        final java.awt.event.KeyEvent ev = new java.awt.event.KeyEvent(
                this.activePanel.getTable(),
                java.awt.event.KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_INSERT,
                KeyEvent.CHAR_UNDEFINED
        );
        this.activePanel.getTable().dispatchEvent(ev);
    }

    private static void bind(
            final InputMap im,
            final ActionMap am,
            final String name,
            final KeyStroke stroke,
            final Runnable action
    ) {
        im.put(stroke, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                action.run();
            }
        });
    }

    private void openActive() {
        try {
            this.activePanel.openCurrent();
        } catch (final IOException ex) {
            showError("Open failed", ex);
        }
    }

    private void openInside() {
        final FileEntry entry = this.activePanel.getLeadEntry();
        if (entry == null || entry.parentLink()) {
            return;
        }
        final BrowseLocation loc = this.activePanel.getLocationModel();
        if (!loc.isDisk()) {
            openActive();
            return;
        }
        final Path child = loc.resolveDiskChild(entry.name());
        if (DirectoryListing.looksLikeZip(child) || entry.directory()) {
            if (entry.directory()) {
                this.activePanel.navigateTo(BrowseLocation.disk(child));
            } else {
                this.activePanel.navigateTo(BrowseLocation.zip(child, ""));
            }
        } else {
            setStatus("Not an archive: " + entry.name());
        }
    }

    private void goUp() {
        this.activePanel.goUp();
    }

    private void refreshActive() {
        this.activePanel.refreshAsync();
        if (this.dualPanel) {
            this.inactivePanel().refreshAsync();
        }
    }

    private void switchPanel() {
        if (!this.dualPanel) {
            return;
        }
        setActivePanel(inactivePanel());
        this.activePanel.requestTableFocus();
    }

    private void toggleDualPanel() {
        this.dualPanel = !this.dualPanel;
        applyDualPanelVisibility();
        setActivePanel(this.activePanel); // refresh borders for dual on/off
        setStatus(this.dualPanel ? "Two panels" : "One panel");
    }

    private void applyDualPanelVisibility() {
        if (this.dualPanel) {
            this.splitPane.setLeftComponent(this.leftPanel);
            this.splitPane.setRightComponent(this.rightPanel);
            this.splitPane.setDividerSize(6);
            SwingUtilities.invokeLater(() -> this.splitPane.setDividerLocation(0.5));
        } else {
            if (this.activePanel == this.rightPanel) {
                this.activePanel = this.leftPanel;
            }
            // Single panel: host left panel alone (no empty right half / divider chrome)
            this.splitPane.setRightComponent(null);
            this.splitPane.setLeftComponent(this.leftPanel);
            this.splitPane.setDividerSize(0);
        }
    }

    private void selectAll() {
        final int rows = this.activePanel.getTable().getRowCount();
        if (rows > 0) {
            // skip ".." row if present
            int start = 0;
            final FileEntry first = this.activePanel.getTable().getModel() instanceof FileTableModel model
                    ? model.getEntry(0) : null;
            if (first != null && first.parentLink() && rows > 1) {
                start = 1;
            }
            this.activePanel.getTable().setRowSelectionInterval(start, rows - 1);
        }
    }

    private void actionCopy() {
        transfer(false);
    }

    private void actionMove() {
        transfer(true);
    }

    private void transfer(final boolean move) {
        final FilePanel source = this.activePanel;
        if (!source.getLocationModel().isDisk()) {
            setStatus((move ? "Move" : "Copy") + " inside archives is not supported in MVP");
            JOptionPane.showMessageDialog(
                    this,
                    (move ? "Move" : "Copy") + " of items inside a zip is not supported yet.\nExtract first.",
                    APP_TITLE,
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        final List<Path> paths = source.getSelectedDiskPaths();
        if (paths.isEmpty()) {
            setStatus("Nothing selected");
            return;
        }

        Path destDir = TransferTargets.otherPanelDiskPath(this.dualPanel, inactivePanel().getLocationModel())
                .orElse(null);
        if (destDir == null) {
            final JFileChooser chooser = new JFileChooser(source.getLocationModel().path().toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle(move ? "Move to folder" : "Copy to folder");
            if (chooser.showDialog(this, move ? "Move" : "Copy") != JFileChooser.APPROVE_OPTION) {
                return;
            }
            destDir = chooser.getSelectedFile().toPath();
        } else {
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    (move ? "Move" : "Copy") + " " + paths.size() + " item(s) to:\n" + destDir + "?",
                    move ? "Move" : "Copy",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
        }

        final Path dest = destDir;
        setStatus((move ? "Moving…" : "Copying…"));
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (move) {
                    FileOperations.move(paths, dest);
                } else {
                    FileOperations.copy(paths, dest);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    this.get();
                    FileManagerFrame.this.setStatus((move ? "Moved" : "Copied") + " " + paths.size() + " item(s)");
                    FileManagerFrame.this.refreshActive();
                } catch (final Exception ex) {
                    FileManagerFrame.this.showError(move ? "Move failed" : "Copy failed", ex);
                }
            }
        }.execute();
    }

    private void actionDelete() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Deleting inside a zip is not supported in MVP.",
                    APP_TITLE,
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        final List<Path> paths = panel.getSelectedDiskPaths();
        if (paths.isEmpty()) {
            setStatus("Nothing selected");
            return;
        }
        final int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete " + paths.size() + " item(s)?\nThis cannot be undone.",
                "Delete",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        setStatus("Deleting…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                FileOperations.delete(paths);
                return null;
            }

            @Override
            protected void done() {
                try {
                    this.get();
                    FileManagerFrame.this.setStatus("Deleted " + paths.size() + " item(s)");
                    panel.refreshAsync();
                } catch (final Exception ex) {
                    FileManagerFrame.this.showError("Delete failed", ex);
                }
            }
        }.execute();
    }

    private void actionRename() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Rename inside zip is not supported in MVP");
            return;
        }
        final FileEntry entry = panel.getLeadEntry();
        if (entry == null || entry.parentLink()) {
            return;
        }
        final String newName = JOptionPane.showInputDialog(this, "New name:", entry.name());
        if (newName == null || newName.isBlank() || newName.equals(entry.name())) {
            return;
        }
        try {
            FileOperations.rename(panel.getLocationModel().resolveDiskChild(entry.name()), newName.trim());
            setStatus("Renamed to " + newName.trim());
            panel.refreshAsync();
        } catch (final IOException ex) {
            showError("Rename failed", ex);
        }
    }

    private void actionMkdir() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Create folder inside zip is not supported in MVP");
            return;
        }
        final String name = JOptionPane.showInputDialog(this, "Folder name:", "New Folder");
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            FileOperations.mkdir(panel.getLocationModel().path(), name.trim());
            setStatus("Created folder " + name.trim());
            panel.refreshAsync();
        } catch (final IOException ex) {
            showError("Create folder failed", ex);
        }
    }

    private void actionAdd() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            JOptionPane.showMessageDialog(this, "Add only works from a disk folder.", APP_TITLE,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final List<Path> sources = panel.getSelectedDiskPaths();
        if (sources.isEmpty()) {
            setStatus("Select files or folders to compress");
            return;
        }
        final Path saveDir = TransferTargets.defaultArchiveSaveDir(
                this.dualPanel,
                panel.getLocationModel(),
                inactivePanel().getLocationModel()
        );
        final JFileChooser chooser = new JFileChooser(saveDir.toFile());
        chooser.setDialogTitle("Create zip archive");
        chooser.setSelectedFile(saveDir.resolve(suggestZipName(sources)).toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path zipPath = chooser.getSelectedFile().toPath();
        if (!zipPath.getFileName().toString().toLowerCase().endsWith(".zip")) {
            zipPath = zipPath.resolveSibling(zipPath.getFileName().toString() + ".zip");
        }
        final Path target = zipPath;
        setStatus("Creating archive…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ZipArchiveFs.createZip(target, sources);
                return null;
            }

            @Override
            protected void done() {
                try {
                    this.get();
                    FileManagerFrame.this.setStatus("Created " + target.getFileName());
                    panel.refreshAsync();
                    if (FileManagerFrame.this.dualPanel) {
                        FileManagerFrame.this.inactivePanel().refreshAsync();
                    }
                } catch (final Exception ex) {
                    FileManagerFrame.this.showError("Add failed", ex);
                }
            }
        }.execute();
    }

    private static String suggestZipName(final List<Path> sources) {
        if (sources.size() == 1) {
            final String name = sources.getFirst().getFileName().toString();
            final int dot = name.lastIndexOf('.');
            if (dot > 0 && Files.isRegularFile(sources.getFirst())) {
                return name.substring(0, dot) + ".zip";
            }
            return name + ".zip";
        }
        return "archive.zip";
    }

    private void actionExtract() {
        final FilePanel panel = this.activePanel;
        final BrowseLocation loc = panel.getLocationModel();

        Path zipFile;
        List<String> internal;
        boolean extractAll;

        if (loc.isZip()) {
            zipFile = loc.path();
            final TransferTargets.ExtractPlan plan =
                    TransferTargets.planZipExtract(loc, panel.getSelectedZipInternalPaths());
            extractAll = plan.extractAll();
            internal = plan.internalPaths();
        } else {
            final List<Path> selected = panel.getSelectedDiskPaths();
            final Path zipCandidate;
            if (selected.size() == 1 && DirectoryListing.looksLikeZip(selected.getFirst())) {
                zipCandidate = selected.getFirst();
            } else if (selected.isEmpty()) {
                final FileEntry lead = panel.getLeadEntry();
                if (lead != null && !lead.parentLink()) {
                    final Path p = loc.resolveDiskChild(lead.name());
                    if (DirectoryListing.looksLikeZip(p)) {
                        zipCandidate = p;
                    } else {
                        setStatus("Select a zip archive to extract");
                        return;
                    }
                } else {
                    setStatus("Select a zip archive to extract");
                    return;
                }
            } else {
                setStatus("Select a single zip archive to extract");
                return;
            }
            zipFile = zipCandidate;
            extractAll = true;
            internal = List.of();
        }

        Path destDir = TransferTargets.defaultExtractDir(this.dualPanel, inactivePanel().getLocationModel())
                .orElse(null);
        if (destDir == null) {
            final Path start = loc.isDisk()
                    ? loc.path()
                    : (zipFile.getParent() != null ? zipFile.getParent() : Path.of("."));
            final JFileChooser chooser = new JFileChooser(start.toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Extract to");
            if (chooser.showDialog(this, "Extract") != JFileChooser.APPROVE_OPTION) {
                return;
            }
            destDir = chooser.getSelectedFile().toPath();
        } else {
            final String what = extractAll ? "entire archive" : (internal.size() + " selected path(s)");
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Extract " + what + " to:\n" + destDir + "?",
                    "Extract",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
        }

        final Path dest = destDir;
        final Path zip = zipFile;
        final List<String> paths = internal;
        final boolean all = extractAll;
        setStatus("Extracting…");
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return ZipArchiveFs.extract(zip, paths, dest, all);
            }

            @Override
            protected void done() {
                try {
                    final int count = this.get();
                    FileManagerFrame.this.setStatus("Extracted " + count + " file(s)");
                    if (FileManagerFrame.this.dualPanel) {
                        FileManagerFrame.this.inactivePanel().refreshAsync();
                    }
                    panel.refreshAsync();
                } catch (final Exception ex) {
                    FileManagerFrame.this.showError("Extract failed", ex);
                }
            }
        }.execute();
    }

    private void actionTest() {
        final FilePanel panel = this.activePanel;
        final BrowseLocation loc = panel.getLocationModel();
        Path zipFile = null;
        if (loc.isZip()) {
            zipFile = loc.path();
        } else {
            final List<Path> selected = panel.getSelectedDiskPaths();
            if (selected.size() == 1 && DirectoryListing.looksLikeZip(selected.getFirst())) {
                zipFile = selected.getFirst();
            } else {
                final FileEntry lead = panel.getLeadEntry();
                if (lead != null && !lead.parentLink()) {
                    final Path p = loc.resolveDiskChild(lead.name());
                    if (DirectoryListing.looksLikeZip(p)) {
                        zipFile = p;
                    }
                }
            }
        }
        if (zipFile == null) {
            setStatus("Select a zip archive to test");
            return;
        }
        final Path zip = zipFile;
        setStatus("Testing…");
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return ZipArchiveFs.test(zip);
            }

            @Override
            protected void done() {
                try {
                    final int count = this.get();
                    FileManagerFrame.this.setStatus("OK — " + count + " file(s) in " + zip.getFileName());
                    JOptionPane.showMessageDialog(
                            FileManagerFrame.this,
                            "Archive is OK.\n" + count + " file(s) readable.",
                            "Test",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (final Exception ex) {
                    FileManagerFrame.this.showError("Test failed", ex);
                }
            }
        }.execute();
    }

    private void actionInfo() {
        final FilePanel panel = this.activePanel;
        final FileEntry entry = panel.getLeadEntry();
        if (entry == null) {
            return;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(entry.name()).append('\n');
        sb.append("Type: ").append(entry.parentLink() ? "Parent" : (entry.directory() ? "Folder" : "File")).append('\n');
        if (!entry.parentLink()) {
            sb.append("Size: ").append(entry.size()).append('\n');
            if (entry.packedSize() != null) {
                sb.append("Packed: ").append(entry.packedSize()).append('\n');
            }
            if (entry.modified() != null) {
                sb.append("Modified: ").append(entry.modified()).append('\n');
            }
            if (entry.created() != null) {
                sb.append("Created: ").append(entry.created()).append('\n');
            }
            if (entry.accessed() != null) {
                sb.append("Accessed: ").append(entry.accessed()).append('\n');
            }
            sb.append("Attributes: ").append(entry.attributes()).append('\n');
        }
        sb.append("Location: ").append(panel.getLocationModel().displayPath());
        JOptionPane.showMessageDialog(this, sb.toString(), "Properties", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                """
                        File Manager
                        A lightweight dual-panel file manager
                        inspired by 7-Zip File Manager.

                        Shortcuts (Windows 7zFM map):
                        F2 Rename · F5 Copy · F6 Move · F7 Mkdir
                        F9 2 Panels · Tab switch panel · Del Delete
                        Enter Open · Backspace Up · Ctrl+R Refresh

                        ZIP only (JDK). Whistler project.""",
                "About File Manager",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(final String title, final Exception ex) {
        final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        log.warn("{}: {}", title, cause.getMessage(), cause);
        setStatus("Error: " + cause.getMessage());
        JOptionPane.showMessageDialog(this, cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void exit() {
        saveConfig();
        this.dispose();
        System.exit(0);
    }

    private void saveConfig() {
        this.config.set(KEY_LEFT, this.leftPanel.getLocationModel().displayPath());
        this.config.set(KEY_RIGHT, this.rightPanel.getLocationModel().displayPath());
        this.config.set(KEY_DUAL, Boolean.toString(this.dualPanel));
        this.config.set(KEY_SHOW_HIDDEN, this.config.get(KEY_SHOW_HIDDEN, "true"));
        this.config.save();
    }
}
