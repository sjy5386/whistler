package com.sysbot32.whistler.file_manager;

import com.sysbot32.whistler.config.Config;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CancellationException;

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

    @NonNull
    private final Config config;
    private final FolderBookmarks bookmarks;
    private final FolderHistory history;
    private final FilePanel leftPanel;
    private final FilePanel rightPanel;
    private final JSplitPane splitPane;
    private final JLabel statusLabel = new JLabel(" Ready ");
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private final JCheckBoxMenuItem flatViewItem = new JCheckBoxMenuItem("Flat View");

    private FilePanel activePanel;
    private boolean dualPanel;

    public FileManagerFrame(@NonNull final Config config) {
        super(APP_TITLE);
        this.config = config;
        this.bookmarks = new FolderBookmarks(this.config);
        this.history = new FolderHistory(this.config);

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
        this.leftPanel.setNavigationListener(this::onPanelNavigated);
        this.rightPanel.setNavigationListener(this::onPanelNavigated);
        this.leftPanel.setContextMenuHandler(this::showListingContextMenu);
        this.rightPanel.setContextMenuHandler(this::showListingContextMenu);
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
        updateWindowTitle();
    }

    private FilePanel inactivePanel() {
        return this.activePanel == this.leftPanel ? this.rightPanel : this.leftPanel;
    }

    /** 7zFM-style: window title is the active panel's current path. */
    private void updateWindowTitle() {
        this.setTitle(this.activePanel.getLocationModel().displayPath());
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

        // Order matches 7-Zip FM resource.rc File menu (archive ops live on toolbar + context)
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(menuItem("Open", KeyEvent.VK_ENTER, 0, this::openActive));
        fileMenu.add(menuItem("Open Inside", KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK, this::openInside));
        fileMenu.add(menuItem("Open Outside", KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK, this::openOutside));
        fileMenu.add(menuItem("View", KeyEvent.VK_F3, 0, this::actionView));
        fileMenu.add(menuItem("Edit", KeyEvent.VK_F4, 0, this::actionEdit));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Rename", KeyEvent.VK_F2, 0, this::actionRename));
        fileMenu.add(menuItem("Copy To...", KeyEvent.VK_F5, 0, this::actionCopy));
        fileMenu.add(menuItem("Move To...", KeyEvent.VK_F6, 0, this::actionMove));
        fileMenu.add(menuItem("Delete", KeyEvent.VK_DELETE, 0, this::actionDelete));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Split file...", 0, 0, this::actionSplit));
        fileMenu.add(menuItem("Combine files...", 0, 0, this::actionCombine));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Properties", KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK, this::actionInfo));
        fileMenu.add(menuItem("Comment...", KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, this::actionComment));
        final JMenu crcMenu = new JMenu("CRC");
        for (final String algo : new String[]{"CRC-32", "MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512"}) {
            final String chosen = algo;
            final JMenuItem crcItem = new JMenuItem(chosen);
            crcItem.addActionListener(ev -> actionChecksum(chosen));
            crcMenu.add(crcItem);
        }
        fileMenu.add(crcMenu);
        fileMenu.add(menuItem("Diff", 0, 0, this::actionDiff));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Create Folder", KeyEvent.VK_F7, 0, this::actionMkdir));
        fileMenu.add(menuItem("Create File", KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK, this::actionCreateFile));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Link...", 0, 0, this::actionLink));
        fileMenu.add(menuItem("Alternate streams", 0, 0, this::actionAlternateStreams));
        fileMenu.addSeparator();
        fileMenu.add(menuItem("Exit", KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK, this::exit));

        final JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(menuItem("Select All", KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK, this::selectAll));
        editMenu.add(menuItem("Deselect All", 0, 0, this::deselectAll));
        editMenu.add(menuItem("Invert Selection", 0, 0, this::invertSelection));
        editMenu.addSeparator();
        editMenu.add(menuItem("Select by Type", 0, 0, () -> this.activePanel.selectByType(true)));
        editMenu.add(menuItem("Deselect by Type", 0, 0, () -> this.activePanel.selectByType(false)));
        editMenu.addSeparator();
        editMenu.add(menuItem("Copy Name to Clipboard", KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, this::copyNameToClipboard));

        final JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        final ButtonGroup viewGroup = new ButtonGroup();
        viewMenu.add(viewModeItem("Large Icons", ViewMode.LARGE_ICONS, KeyEvent.VK_1, viewGroup));
        viewMenu.add(viewModeItem("Small Icons", ViewMode.SMALL_ICONS, KeyEvent.VK_2, viewGroup));
        viewMenu.add(viewModeItem("List", ViewMode.LIST, KeyEvent.VK_3, viewGroup));
        final JRadioButtonMenuItem details = viewModeItem("Details", ViewMode.DETAILS, KeyEvent.VK_4, viewGroup);
        details.setSelected(true);
        viewMenu.add(details);
        viewMenu.addSeparator();
        viewMenu.add(menuItem("Name", KeyEvent.VK_F3, InputEvent.CTRL_DOWN_MASK,
                () -> this.activePanel.sortBy(EntryComparators.COL_NAME, true)));
        viewMenu.add(menuItem("Date", KeyEvent.VK_F5, InputEvent.CTRL_DOWN_MASK,
                () -> this.activePanel.sortBy(EntryComparators.COL_MODIFIED, true)));
        viewMenu.add(menuItem("Size", KeyEvent.VK_F6, InputEvent.CTRL_DOWN_MASK,
                () -> this.activePanel.sortBy(EntryComparators.COL_SIZE, true)));
        viewMenu.add(menuItem("Unsorted", KeyEvent.VK_F7, InputEvent.CTRL_DOWN_MASK,
                () -> this.activePanel.setUnsorted()));
        viewMenu.addSeparator();
        this.flatViewItem.addActionListener(e -> {
            final boolean on = this.flatViewItem.isSelected();
            this.leftPanel.setFlatView(on);
            this.rightPanel.setFlatView(on);
            setStatus(on ? "Flat view on" : "Flat view off");
        });
        viewMenu.add(this.flatViewItem);
        viewMenu.add(menuItem("2 Panels", KeyEvent.VK_F9, 0, this::toggleDualPanel));
        viewMenu.addSeparator();
        viewMenu.add(menuItem("Open Root Folder", KeyEvent.VK_BACK_SLASH, 0, this::openRoot));
        viewMenu.add(menuItem("Up One Level", KeyEvent.VK_BACK_SPACE, 0, this::goUp));
        viewMenu.add(menuItem("Folders History…", KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK, this::showHistory));
        viewMenu.add(menuItem("Refresh", KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, this::refreshActive));

        final JMenu favoritesMenu = new JMenu("Favorites");
        favoritesMenu.setMnemonic(KeyEvent.VK_A);
        // Rebuild on open so each slot shows the registered path (7zFM-style, no separate dialog)
        favoritesMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(final MenuEvent e) {
                FileManagerFrame.this.rebuildFavoritesMenu(favoritesMenu);
            }

            @Override
            public void menuDeselected(final MenuEvent e) {
            }

            @Override
            public void menuCanceled(final MenuEvent e) {
            }
        });
        rebuildFavoritesMenu(favoritesMenu);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(menuItem("About File Manager…", 0, 0, this::showAbout));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(favoritesMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    /**
     * 7zFM Favorites: submenu to assign current folder to slot 0–9, then list of slots with paths.
     */
    private void rebuildFavoritesMenu(final JMenu favoritesMenu) {
        favoritesMenu.removeAll();

        final JMenu addAs = new JMenu("Add folder to Favorites as");
        for (int i = 0; i < FolderBookmarks.SLOT_COUNT; i++) {
            final int slot = i;
            final String existing = this.bookmarks.get(slot)
                    .map(Path::toString)
                    .orElse("(empty)");
            final JMenuItem setItem = new JMenuItem(slot + "  " + existing);
            setItem.setAccelerator(KeyStroke.getKeyStroke(
                    KeyEvent.VK_0 + slot,
                    InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK
            ));
            setItem.setToolTipText("Save current folder into bookmark " + slot);
            setItem.addActionListener(e -> setBookmark(slot));
            addAs.add(setItem);
        }
        favoritesMenu.add(addAs);
        favoritesMenu.addSeparator();

        for (int i = 0; i < FolderBookmarks.SLOT_COUNT; i++) {
            final int slot = i;
            final var pathOpt = this.bookmarks.get(slot);
            final String label = pathOpt
                    .map(p -> slot + "  " + p)
                    .orElse(slot + "  (empty)");
            final JMenuItem openItem = new JMenuItem(label);
            openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + slot, InputEvent.ALT_DOWN_MASK));
            openItem.setEnabled(pathOpt.isPresent());
            openItem.addActionListener(e -> openBookmark(slot));
            favoritesMenu.add(openItem);
        }
    }

    private JRadioButtonMenuItem viewModeItem(
            final String text,
            final ViewMode mode,
            final int digitKey,
            final ButtonGroup group
    ) {
        final JRadioButtonMenuItem item = new JRadioButtonMenuItem(text);
        item.setAccelerator(KeyStroke.getKeyStroke(digitKey, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> {
            this.leftPanel.setViewMode(mode);
            this.rightPanel.setViewMode(mode);
            setStatus("View: " + text);
        });
        group.add(item);
        return item;
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
        bind(im, am, "openOutside", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), this::openOutside);
        bind(im, am, "info", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), this::actionInfo);
        bind(im, am, "view", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), this::actionView);
        bind(im, am, "edit", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), this::actionEdit);
        bind(im, am, "createFile", KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK), this::actionCreateFile);
        bind(im, am, "comment", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), this::actionComment);
        bind(im, am, "insertToggle", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), this::toggleInsertSelection);
        bind(im, am, "openRoot", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0), this::openRoot);
        bind(im, am, "history", KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK), this::showHistory);
        bind(im, am, "sameOther", KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), this::openSameInOther);
        bind(im, am, "currentOther", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_DOWN_MASK), this::openCurrentInOther);
        bind(im, am, "currentOtherR", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_DOWN_MASK), this::openCurrentInOther);
        bind(im, am, "leftPath", KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.ALT_DOWN_MASK),
                () -> this.leftPanel.focusPathField());
        bind(im, am, "rightPath", KeyStroke.getKeyStroke(KeyEvent.VK_F2, InputEvent.ALT_DOWN_MASK),
                () -> this.rightPanel.focusPathField());
        for (int d = 0; d <= 9; d++) {
            final int slot = d;
            bind(im, am, "bmOpen" + d, KeyStroke.getKeyStroke(KeyEvent.VK_0 + d, InputEvent.ALT_DOWN_MASK),
                    () -> openBookmark(slot));
            bind(im, am, "bmSet" + d,
                    KeyStroke.getKeyStroke(KeyEvent.VK_0 + d, InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                    () -> setBookmark(slot));
        }
    }

    private void onPanelNavigated(final BrowseLocation loc) {
        if (loc != null && loc.isDisk()) {
            this.history.record(loc.path());
        }
        updateWindowTitle();
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
        try {
            final Path child = this.activePanel.resolveDiskEntry(entry);
            if (DirectoryListing.looksLikeZip(child) || entry.directory()) {
                if (entry.directory()) {
                    this.activePanel.navigateTo(BrowseLocation.disk(child));
                } else {
                    this.activePanel.navigateTo(BrowseLocation.zip(child, ""));
                }
            } else {
                setStatus("Not an archive: " + entry.name());
            }
        } catch (final RuntimeException ex) {
            setStatus("Open Inside failed: " + ex.getMessage());
        }
    }

    private void openOutside() {
        try {
            this.activePanel.openExternallyLead();
            setStatus("Opened outside");
        } catch (final IOException ex) {
            showError("Open Outside failed", ex);
        }
    }

    private void actionView() {
        try {
            this.activePanel.openExternallyLead();
            setStatus("View");
        } catch (final IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No system viewer available or selection is invalid.\n" + ex.getMessage(),
                    "View",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void actionEdit() {
        try {
            this.activePanel.openWithDesktopEdit();
            setStatus("Edit");
        } catch (final IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No system editor available or selection is invalid.\n" + ex.getMessage(),
                    "Edit",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void goUp() {
        this.activePanel.goUp();
    }

    private void openRoot() {
        Path root = null;
        for (final Path r : FileSystems.getDefault().getRootDirectories()) {
            root = r;
            break;
        }
        if (root == null) {
            root = Path.of(System.getProperty("user.home"));
        }
        this.activePanel.navigateTo(BrowseLocation.disk(root));
        setStatus("Root: " + root);
    }

    private void showHistory() {
        final List<Path> entries = this.history.entries();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No folder history yet.", "Folders History",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final String[] labels = entries.stream().map(Path::toString).toArray(String[]::new);
        final String choice = (String) JOptionPane.showInputDialog(
                this,
                "Recent folders:",
                "Folders History",
                JOptionPane.PLAIN_MESSAGE,
                null,
                labels,
                labels[0]
        );
        if (choice != null) {
            this.activePanel.navigateTo(BrowseLocation.disk(Path.of(choice)));
        }
    }

    private void openBookmark(final int slot) {
        this.bookmarks.get(slot).ifPresentOrElse(
                path -> {
                    this.activePanel.navigateTo(BrowseLocation.disk(path));
                    setStatus("Bookmark " + slot + ": " + path);
                },
                () -> setStatus("Bookmark " + slot + " is empty")
        );
    }

    private void setBookmark(final int slot) {
        final BrowseLocation loc = this.activePanel.getLocationModel();
        if (!loc.isDisk()) {
            setStatus("Bookmarks store disk folders only");
            return;
        }
        this.bookmarks.set(slot, loc.path());
        setStatus("Saved bookmark " + slot + " → " + loc.path());
    }

    private void openSameInOther() {
        if (!this.dualPanel) {
            setStatus("Enable 2 panels (F9) first");
            return;
        }
        inactivePanel().navigateTo(this.activePanel.getLocationModel());
        setStatus("Opened same folder in other panel");
    }

    private void openCurrentInOther() {
        if (!this.dualPanel) {
            setStatus("Enable 2 panels (F9) first");
            return;
        }
        final FileEntry lead = this.activePanel.getLeadEntry();
        final BrowseLocation loc = this.activePanel.getLocationModel();
        if (lead != null && !lead.parentLink() && loc.isDisk() && lead.directory()) {
            try {
                inactivePanel().navigateTo(BrowseLocation.disk(this.activePanel.resolveDiskEntry(lead)));
                setStatus("Opened selection in other panel");
                return;
            } catch (final RuntimeException ignored) {
                // fall through
            }
        }
        inactivePanel().navigateTo(loc);
        setStatus("Opened current folder in other panel");
    }

    private void copyNameToClipboard() {
        final FileEntry lead = this.activePanel.getLeadEntry();
        if (lead == null || lead.parentLink()) {
            setStatus("Nothing to copy");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(lead.name()), null);
        setStatus("Copied name: " + lead.name());
    }

    private void actionCreateFile() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Create file only on disk");
            return;
        }
        final String name = JOptionPane.showInputDialog(this, "New file name:", "New File.txt");
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            FileOperations.createFile(panel.getLocationModel().path(), name.trim());
            setStatus("Created " + name.trim());
            panel.refreshAsync();
        } catch (final IOException ex) {
            showError("Create file failed", ex);
        }
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
        this.activePanel.selectAllItems();
    }

    private void deselectAll() {
        this.activePanel.deselectAllItems();
    }

    private void invertSelection() {
        this.activePanel.invertSelection();
    }

    private void actionCopy() {
        transfer(false);
    }

    private void actionMove() {
        transfer(true);
    }

    private void transfer(final boolean move) {
        final FilePanel source = this.activePanel;
        final BrowseLocation loc = source.getLocationModel();

        Path destDir = TransferTargets.otherPanelDiskPath(this.dualPanel, inactivePanel().getLocationModel())
                .orElse(null);
        if (destDir == null) {
            final Path start = loc.isDisk()
                    ? loc.path()
                    : (loc.path().getParent() != null ? loc.path().getParent() : Path.of(System.getProperty("user.home")));
            final JFileChooser chooser = new JFileChooser(start.toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle(move ? "Move to folder" : "Copy to folder");
            if (chooser.showDialog(this, move ? "Move" : "Copy") != JFileChooser.APPROVE_OPTION) {
                return;
            }
            destDir = chooser.getSelectedFile().toPath();
        }

        if (loc.isZip()) {
            final List<String> internal = source.getSelectedZipInternalPaths();
            if (internal.isEmpty()) {
                setStatus("Nothing selected");
                return;
            }
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    (move ? "Move" : "Copy") + " " + internal.size() + " item(s) from archive to:\n" + destDir + "?",
                    move ? "Move" : "Copy",
                    JOptionPane.OK_CANCEL_OPTION
            );
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
            final Path dest = destDir;
            final Path zip = loc.path();
            final TransferControl control = new TransferControl();
            setStatus(move ? "Moving…" : "Copying…");
            ProgressTasks.run(
                    this,
                    move ? "Move" : "Copy",
                    control,
                    () -> ZipArchiveFs.copyOrMoveToDisk(zip, internal, dest, move, control),
                    count -> {
                        setStatus((move ? "Moved" : "Copied") + " " + count + " file(s) from archive");
                        refreshActive();
                    },
                    ex -> handleTransferError(move ? "Move failed" : "Copy failed", ex)
            );
            return;
        }

        final List<Path> paths = source.getSelectedDiskPaths();
        if (paths.isEmpty()) {
            setStatus("Nothing selected");
            return;
        }

        final int confirm = JOptionPane.showConfirmDialog(
                this,
                (move ? "Move" : "Copy") + " " + paths.size() + " item(s) to:\n" + destDir + "?",
                move ? "Move" : "Copy",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        final Path dest = destDir;
        final TransferControl control = new TransferControl();
        final FileOperations.CollisionAsk ask = (src, target) -> {
            final CollisionPolicy.UserChoice[] box = new CollisionPolicy.UserChoice[1];
            try {
                SwingUtilities.invokeAndWait(() -> box[0] = promptCollision(src, target, move));
            } catch (final Exception ex) {
                box[0] = CollisionPolicy.UserChoice.CANCEL;
            }
            return box[0] == null ? CollisionPolicy.UserChoice.CANCEL : box[0];
        };
        setStatus(move ? "Moving…" : "Copying…");
        ProgressTasks.run(
                this,
                move ? "Move" : "Copy",
                control,
                () -> {
                    if (move) {
                        FileOperations.move(paths, dest, ask, control);
                    } else {
                        FileOperations.copy(paths, dest, ask, control);
                    }
                    return paths.size();
                },
                n -> {
                    setStatus((move ? "Moved" : "Copied") + " " + n + " item(s)");
                    refreshActive();
                },
                ex -> handleTransferError(move ? "Move failed" : "Copy failed", ex)
        );
    }

    private CollisionPolicy.UserChoice promptCollision(final Path source, final Path target, final boolean move) {
        final String verb = move ? "Move" : "Copy";
        final Object[] options = {
                "Overwrite", "Overwrite All", "Skip", "Skip All", "Cancel"
        };
        final int result = JOptionPane.showOptionDialog(
                this,
                verb + ":\n" + source.getFileName() + "\n\nTarget already exists:\n" + target
                        + "\n\nWhat should we do?",
                "Name collision",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return switch (result) {
            case 0 -> CollisionPolicy.UserChoice.OVERWRITE;
            case 1 -> CollisionPolicy.UserChoice.OVERWRITE_ALL;
            case 2 -> CollisionPolicy.UserChoice.SKIP;
            case 3 -> CollisionPolicy.UserChoice.SKIP_ALL;
            default -> CollisionPolicy.UserChoice.CANCEL;
        };
    }

    private void handleTransferError(final String title, final Exception ex) {
        final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof CancellationException || (ex.getMessage() != null && ex.getMessage().contains("Cancelled"))) {
            setStatus("Cancelled");
            return;
        }
        showError(title, ex);
    }

    private void actionDelete() {
        final FilePanel panel = this.activePanel;
        final BrowseLocation loc = panel.getLocationModel();
        if (loc.isZip()) {
            final List<String> internal = panel.getSelectedZipInternalPaths();
            if (internal.isEmpty()) {
                setStatus("Nothing selected");
                return;
            }
            final int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Delete " + internal.size() + " item(s) from archive?\nThis cannot be undone.",
                    "Delete",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
            final Path zip = loc.path();
            final TransferControl control = new TransferControl();
            setStatus("Deleting…");
            ProgressTasks.run(
                    this,
                    "Delete",
                    control,
                    () -> ZipArchiveFs.deleteEntries(zip, internal, control),
                    n -> {
                        setStatus("Deleted " + n + " entr(y/ies) from archive");
                        panel.refreshAsync();
                    },
                    ex -> handleTransferError("Delete failed", ex)
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
        final TransferControl control = new TransferControl();
        setStatus("Deleting…");
        ProgressTasks.run(
                this,
                "Delete",
                control,
                () -> {
                    FileOperations.delete(paths, control);
                    return paths.size();
                },
                n -> {
                    setStatus("Deleted " + n + " item(s)");
                    panel.refreshAsync();
                },
                ex -> handleTransferError("Delete failed", ex)
        );
    }

    private void actionRename() {
        final FilePanel panel = this.activePanel;
        final FileEntry entry = panel.getLeadEntry();
        if (entry == null || entry.parentLink()) {
            return;
        }
        final String newName = JOptionPane.showInputDialog(this, "New name:", entry.name());
        if (newName == null || newName.isBlank() || newName.equals(entry.name())) {
            return;
        }
        final BrowseLocation loc = panel.getLocationModel();
        try {
            if (loc.isZip()) {
                final String from = loc.resolveZipChild(entry.name(), entry.directory());
                ZipArchiveFs.renameEntry(loc.path(), from, newName.trim());
            } else {
                FileOperations.rename(panel.resolveDiskEntry(entry), newName.trim());
            }
            setStatus("Renamed to " + newName.trim());
            panel.refreshAsync();
        } catch (final Exception ex) {
            showError("Rename failed", ex);
        }
    }

    private void actionMkdir() {
        final FilePanel panel = this.activePanel;
        final String name = JOptionPane.showInputDialog(this, "Folder name:", "New Folder");
        if (name == null || name.isBlank()) {
            return;
        }
        final BrowseLocation loc = panel.getLocationModel();
        try {
            if (loc.isZip()) {
                ZipArchiveFs.mkdir(loc.path(), loc.zipInternalPath(), name.trim());
            } else {
                FileOperations.mkdir(loc.path(), name.trim());
            }
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
        final TransferControl control = new TransferControl();
        setStatus("Creating archive…");
        ProgressTasks.run(
                this,
                "Add to Archive",
                control,
                () -> {
                    ZipArchiveFs.createZip(target, sources, control);
                    return target.getFileName().toString();
                },
                name -> {
                    setStatus("Created " + name);
                    panel.refreshAsync();
                    if (this.dualPanel) {
                        inactivePanel().refreshAsync();
                    }
                },
                ex -> handleTransferError("Add failed", ex)
        );
    }

    /**
     * Listing context menu mirrors 7-Zip FM (resource.rc File menu + archive plugin items).
     * Order: archive ops (disk only) → Open/View/Edit → rename/copy/move/delete →
     * split/combine → properties/CRC → create → link.
     */
    private void showListingContextMenu(final MouseEvent e) {
        final Component invoker = (Component) e.getSource();
        final FilePanel panel = this.activePanel;
        final BrowseLocation loc = panel.getLocationModel();
        final FileEntry lead = panel.getLeadEntry();
        final int selCount = panel.getSelectionCount();
        final boolean hasRealSelection = selCount > 0 || (lead != null && !lead.parentLink());
        final boolean onParent = lead != null && lead.parentLink() && selCount == 0;
        final boolean disk = loc.isDisk();
        final boolean zip = loc.isZip();
        final boolean canMutateSelection = hasRealSelection && !onParent;
        final boolean single = (selCount == 1) || (selCount == 0 && lead != null && !lead.parentLink());
        final boolean leadFile = lead != null && !lead.parentLink() && !lead.directory();
        final boolean canOpen = lead != null;
        final boolean canView = canMutateSelection && (disk || zip);
        final boolean canEdit = canMutateSelection && disk && leadFile;
        final boolean canRename = canMutateSelection && lead != null && !lead.parentLink()
                && (selCount <= 1);
        final boolean canAdd = disk && canMutateSelection;
        final Path diskLead = safeResolveDisk(panel, lead);
        final List<Path> diskSelected = disk ? panel.getSelectedDiskPaths() : List.of();
        final boolean anyZipSelected = zip || diskSelected.stream().anyMatch(DirectoryListing::looksLikeZip)
                || (diskLead != null && DirectoryListing.looksLikeZip(diskLead));
        final boolean canExtract = zip || anyZipSelected;
        final boolean canTest = canExtract;
        final boolean oneDiskFile = disk && single && leadFile
                && diskSelected.size() <= 1
                && (diskSelected.isEmpty() ? diskLead != null && Files.isRegularFile(diskLead)
                : diskSelected.size() == 1 && Files.isRegularFile(diskSelected.getFirst()));
        final boolean multiDiskFiles = disk && diskSelected.size() >= 2
                && diskSelected.stream().allMatch(Files::isRegularFile);
        final boolean canChecksum = disk && canMutateSelection
                && (oneDiskFile || diskSelected.stream().anyMatch(Files::isRegularFile));

        final JPopupMenu popup = new JPopupMenu();

        // Cascaded archive submenu (7zFM-style cascade; app is File Manager, not 7-Zip)
        final JMenu archiveMenu = new JMenu("Archive");
        if (disk) {
            archiveMenu.add(popupItem("Add to archive...", canAdd, this::actionAdd));
            archiveMenu.add(popupItem("Extract files...", canExtract, this::actionExtract));
            archiveMenu.add(popupItem("Test archive", canTest, this::actionTest));
        } else if (zip) {
            archiveMenu.add(popupItem("Extract...", true, this::actionExtract));
            archiveMenu.add(popupItem("Test archive", true, this::actionTest));
        }
        if (archiveMenu.getItemCount() > 0) {
            popup.add(archiveMenu);
            popup.addSeparator();
        }

        // File menu body (7zFM resource.rc order; Exit omitted in context menu)
        popup.add(popupItem("Open\tEnter", canOpen, this::openActive));
        popup.add(popupItem("Open Inside\tCtrl+PgDn", canOpen, this::openInside));
        popup.add(popupItem("Open Outside\tShift+Enter", canOpen && !onParent, this::openOutside));
        popup.add(popupItem("View\tF3", canView, this::actionView));
        popup.add(popupItem("Edit\tF4", canEdit, this::actionEdit));
        popup.addSeparator();
        popup.add(popupItem("Rename\tF2", canRename, this::actionRename));
        popup.add(popupItem("Copy To...\tF5", canMutateSelection, this::actionCopy));
        popup.add(popupItem("Move To...\tF6", canMutateSelection, this::actionMove));
        popup.add(popupItem("Delete\tDel", canMutateSelection, this::actionDelete));
        popup.addSeparator();
        final boolean canCombine = disk && (
                (oneDiskFile && diskLead != null && FileSplitCombine.isFirstPart(diskLead.getFileName().toString()))
                        || multiDiskFiles
                        || diskSelected.stream().anyMatch(p -> FileSplitCombine.isFirstPart(p.getFileName().toString()))
        );
        final boolean canComment = (disk && canMutateSelection) || zip;
        final boolean canDiff = disk && diskSelected.size() == 2
                && diskSelected.stream().allMatch(Files::isRegularFile);
        final boolean canLink = disk && single && canMutateSelection;
        final boolean canAltStreams = disk && single && canMutateSelection;

        popup.add(popupItem("Split file...", oneDiskFile, this::actionSplit));
        popup.add(popupItem("Combine files...", canCombine, this::actionCombine));
        popup.addSeparator();
        popup.add(popupItem("Properties\tAlt+Enter", lead != null, this::actionInfo));
        popup.add(popupItem("Comment...\tCtrl+Z", canComment, this::actionComment));

        final JMenu crcMenu = new JMenu("CRC");
        crcMenu.setEnabled(canChecksum);
        for (final String algo : new String[]{"CRC-32", "MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512"}) {
            final String chosen = algo;
            final JMenuItem crcItem = new JMenuItem(chosen);
            crcItem.setEnabled(canChecksum);
            crcItem.addActionListener(ev -> actionChecksum(chosen));
            crcMenu.add(crcItem);
        }
        popup.add(crcMenu);
        popup.add(popupItem("Diff", canDiff, this::actionDiff));
        popup.addSeparator();
        popup.add(popupItem("Create Folder\tF7", true, this::actionMkdir));
        popup.add(popupItem("Create File\tShift+F4", disk, this::actionCreateFile));
        popup.addSeparator();
        popup.add(popupItem("Link...", canLink, this::actionLink));
        popup.add(popupItem("Alternate streams", canAltStreams, this::actionAlternateStreams));

        popup.show(invoker, e.getX(), e.getY());
    }

    private void actionSplit() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Split only on disk files");
            return;
        }
        Path source = null;
        final List<Path> selected = panel.getSelectedDiskPaths();
        if (selected.size() == 1 && Files.isRegularFile(selected.getFirst())) {
            source = selected.getFirst();
        } else {
            final FileEntry lead = panel.getLeadEntry();
            if (lead != null && !lead.parentLink() && !lead.directory()) {
                final Path p = panel.resolveDiskEntry(lead);
                if (Files.isRegularFile(p)) {
                    source = p;
                }
            }
        }
        if (source == null) {
            setStatus("Select one file to split");
            return;
        }
        final long size;
        try {
            size = Files.size(source);
        } catch (final IOException ex) {
            showError("Split failed", ex);
            return;
        }
        final Object sizeInput = JOptionPane.showInputDialog(
                this,
                "Split to volumes, bytes:\n(File size: " + size + ")",
                "Split File",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                String.valueOf(Math.max(1, size / 2))
        );
        if (sizeInput == null || sizeInput.toString().isBlank()) {
            return;
        }
        final long volumeSize;
        try {
            volumeSize = parseVolumeSize(sizeInput.toString().trim());
        } catch (final Exception ex) {
            showError("Split failed", new IOException("Incorrect volume size"));
            return;
        }
        if (volumeSize <= 0 || volumeSize >= size) {
            showError("Split failed", new IOException("Volume size must be smaller than size of original file"));
            return;
        }
        final long vols = (size + volumeSize - 1) / volumeSize;
        final int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to split file into " + vols + " volumes?",
                "Confirm Splitting",
                JOptionPane.OK_CANCEL_OPTION
        );
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        final Path src = source;
        final Path destDir = source.getParent() != null ? source.getParent() : Path.of(".");
        final TransferControl control = new TransferControl();
        setStatus("Splitting…");
        ProgressTasks.run(
                this,
                "Split File",
                control,
                () -> FileSplitCombine.split(src, destDir, volumeSize, control),
                parts -> {
                    setStatus("Split into " + parts.size() + " volume(s)");
                    panel.refreshAsync();
                },
                ex -> handleTransferError("Split failed", ex)
        );
    }

    /** Accept plain bytes or trailing K/M/G (1024-based). */
    static long parseVolumeSize(final String raw) {
        String s = raw.trim().replace("_", "").replace(",", "");
        long mult = 1L;
        if (s.length() >= 2) {
            final char u = Character.toUpperCase(s.charAt(s.length() - 1));
            if (u == 'K' || u == 'M' || u == 'G') {
                mult = switch (u) {
                    case 'K' -> 1024L;
                    case 'M' -> 1024L * 1024L;
                    case 'G' -> 1024L * 1024L * 1024L;
                    default -> 1L;
                };
                s = s.substring(0, s.length() - 1).trim();
            }
        }
        return Long.parseLong(s) * mult;
    }

    private void actionCombine() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Combine only on disk");
            return;
        }
        Path first = null;
        for (final Path p : panel.getSelectedDiskPaths()) {
            if (Files.isRegularFile(p) && FileSplitCombine.isFirstPart(p.getFileName().toString())) {
                first = p;
                break;
            }
        }
        if (first == null) {
            final FileEntry lead = panel.getLeadEntry();
            if (lead != null && !lead.parentLink()) {
                final Path p = panel.resolveDiskEntry(lead);
                if (Files.isRegularFile(p) && FileSplitCombine.isFirstPart(p.getFileName().toString())) {
                    first = p;
                }
            }
        }
        if (first == null) {
            setStatus("Select only first part of split file (.001)");
            return;
        }
        final String base = first.getFileName().toString().replaceFirst("\\.\\d+$", "");
        final Path defaultDest = first.resolveSibling(base);
        final JFileChooser chooser = new JFileChooser(defaultDest.getParent() != null
                ? defaultDest.getParent().toFile() : new java.io.File("."));
        chooser.setDialogTitle("Combine to");
        chooser.setSelectedFile(defaultDest.toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final Path dest = chooser.getSelectedFile().toPath();
        final Path part = first;
        final TransferControl control = new TransferControl();
        setStatus("Combining…");
        ProgressTasks.run(
                this,
                "Combine Files",
                control,
                () -> FileSplitCombine.combine(part, dest, control),
                n -> {
                    setStatus("Combined " + n + " part(s) → " + dest.getFileName());
                    panel.refreshAsync();
                },
                ex -> handleTransferError("Combine failed", ex)
        );
    }

    private void actionComment() {
        final FilePanel panel = this.activePanel;
        final BrowseLocation loc = panel.getLocationModel();
        try {
            if (loc.isZip()) {
                final String current = ZipArchiveFs.getComment(loc.path());
                final String next = (String) JOptionPane.showInputDialog(
                        this, "Archive comment:", "Comment",
                        JOptionPane.PLAIN_MESSAGE, null, null, current);
                if (next == null) {
                    return;
                }
                ZipArchiveFs.setComment(loc.path(), next);
                setStatus("Archive comment updated");
                return;
            }
            Path target = null;
            final List<Path> selected = panel.getSelectedDiskPaths();
            if (selected.size() == 1) {
                target = selected.getFirst();
            } else {
                final FileEntry lead = panel.getLeadEntry();
                if (lead != null && !lead.parentLink()) {
                    target = panel.resolveDiskEntry(lead);
                }
            }
            if (target == null) {
                setStatus("Select one item for comment");
                return;
            }
            if (DirectoryListing.looksLikeZip(target) && Files.isRegularFile(target)) {
                final String current = ZipArchiveFs.getComment(target);
                final String next = (String) JOptionPane.showInputDialog(
                        this, "Archive comment:", "Comment",
                        JOptionPane.PLAIN_MESSAGE, null, null, current);
                if (next == null) {
                    return;
                }
                ZipArchiveFs.setComment(target, next);
                setStatus("Archive comment updated");
                return;
            }
            final String current = FileComments.get(target).orElse("");
            final String next = (String) JOptionPane.showInputDialog(
                    this, "Comment:", "Comment",
                    JOptionPane.PLAIN_MESSAGE, null, null, current);
            if (next == null) {
                return;
            }
            if (next.isBlank()) {
                FileComments.clear(target);
                setStatus("Comment cleared");
            } else {
                FileComments.set(target, next);
                setStatus("Comment saved");
            }
        } catch (final Exception ex) {
            showError("Comment failed", ex instanceof Exception e ? e : new Exception(ex));
        }
    }

    private void actionDiff() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Diff only on disk files");
            return;
        }
        final List<Path> files = panel.getSelectedDiskPaths().stream()
                .filter(Files::isRegularFile)
                .toList();
        if (files.size() != 2) {
            setStatus("Select exactly two files to diff");
            return;
        }
        final Path left = files.get(0);
        final Path right = files.get(1);
        final TransferControl control = new TransferControl();
        setStatus("Diff…");
        ProgressTasks.run(
                this,
                "Diff",
                control,
                () -> {
                    control.begin(1);
                    final FileDiff.Result r = FileDiff.compare(left, right);
                    control.advance("done");
                    return r;
                },
                result -> {
                    setStatus(result.identical() ? "Files identical" : "Files differ");
                    final JTextArea area = new JTextArea(result.report());
                    area.setEditable(false);
                    area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
                    final JScrollPane scroll = new JScrollPane(area);
                    scroll.setPreferredSize(new Dimension(560, 320));
                    JOptionPane.showMessageDialog(
                            this, scroll, "Diff",
                            result.identical() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                    );
                },
                ex -> handleTransferError("Diff failed", ex)
        );
    }

    private void actionLink() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Link only on disk");
            return;
        }
        Path target = null;
        final List<Path> selected = panel.getSelectedDiskPaths();
        if (selected.size() == 1) {
            target = selected.getFirst();
        } else {
            final FileEntry lead = panel.getLeadEntry();
            if (lead != null && !lead.parentLink()) {
                target = panel.resolveDiskEntry(lead);
            }
        }
        if (target == null || !Files.exists(target)) {
            setStatus("Select one existing item as link target");
            return;
        }
        final Object[] kinds = {"Symbolic Link", "Hard Link"};
        final Object kindChoice = JOptionPane.showInputDialog(
                this,
                "Link type:\nTarget: " + target,
                "Link",
                JOptionPane.QUESTION_MESSAGE,
                null,
                kinds,
                kinds[0]
        );
        if (kindChoice == null) {
            return;
        }
        final FileLinks.Kind kind = "Hard Link".equals(kindChoice)
                ? FileLinks.Kind.HARD
                : FileLinks.Kind.SYMBOLIC;
        final String defaultName = target.getFileName() + (kind == FileLinks.Kind.HARD ? ".hard" : ".link");
        final String name = JOptionPane.showInputDialog(this, "Link name (in current folder):", defaultName);
        if (name == null || name.isBlank()) {
            return;
        }
        final Path linkPath = panel.getLocationModel().path().resolve(name.trim());
        try {
            FileLinks.create(kind, linkPath, target);
            setStatus("Created " + kindChoice + ": " + name.trim());
            panel.refreshAsync();
        } catch (final Exception ex) {
            showError("Link failed", ex instanceof Exception e ? e : new Exception(ex));
        }
    }

    private void actionAlternateStreams() {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Alternate streams only on disk");
            return;
        }
        Path target = null;
        final List<Path> selected = panel.getSelectedDiskPaths();
        if (selected.size() == 1) {
            target = selected.getFirst();
        } else {
            final FileEntry lead = panel.getLeadEntry();
            if (lead != null && !lead.parentLink()) {
                target = panel.resolveDiskEntry(lead);
            }
        }
        if (target == null || !Files.exists(target)) {
            setStatus("Select one item");
            return;
        }
        try {
            if (!AlternateStreams.isSupported(target)) {
                JOptionPane.showMessageDialog(
                        this,
                        "User-defined attributes / alternate streams are not supported on this path.",
                        "Alternate streams",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            final Map<String, String> preview = AlternateStreams.listAsTextPreview(target, 200);
            final StringBuilder sb = new StringBuilder();
            sb.append(target).append("\n\n");
            if (preview.isEmpty()) {
                sb.append("(no alternate streams / user attributes)\n");
            } else {
                for (final var e : preview.entrySet()) {
                    sb.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
                }
            }
            final Object[] options = {"Add…", "Delete…", "Close"};
            final int choice = JOptionPane.showOptionDialog(
                    this,
                    sb.toString(),
                    "Alternate streams",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[2]
            );
            if (choice == 0) {
                final String streamName = JOptionPane.showInputDialog(this, "Stream / attribute name:", "comment");
                if (streamName == null || streamName.isBlank()) {
                    return;
                }
                final String value = JOptionPane.showInputDialog(this, "Value:", "");
                if (value == null) {
                    return;
                }
                AlternateStreams.write(target, streamName.trim(), value);
                setStatus("Wrote stream " + streamName.trim());
            } else if (choice == 1) {
                final List<AlternateStreams.StreamInfo> list = AlternateStreams.list(target);
                if (list.isEmpty()) {
                    setStatus("Nothing to delete");
                    return;
                }
                final String[] names = list.stream().map(AlternateStreams.StreamInfo::name).toArray(String[]::new);
                final String pick = (String) JOptionPane.showInputDialog(
                        this, "Delete stream:", "Alternate streams",
                        JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
                if (pick == null) {
                    return;
                }
                AlternateStreams.delete(target, pick);
                setStatus("Deleted stream " + pick);
            }
        } catch (final Exception ex) {
            showError("Alternate streams failed", ex instanceof Exception e ? e : new Exception(ex));
        }
    }

    private void actionChecksum(final String algorithm) {
        final FilePanel panel = this.activePanel;
        if (!panel.getLocationModel().isDisk()) {
            setStatus("Checksum only on disk files");
            return;
        }
        List<Path> paths = panel.getSelectedDiskPaths();
        if (paths.isEmpty()) {
            final FileEntry lead = panel.getLeadEntry();
            if (lead != null && !lead.parentLink() && !lead.directory()) {
                paths = List.of(panel.resolveDiskEntry(lead));
            }
        }
        paths = paths.stream().filter(Files::isRegularFile).toList();
        if (paths.isEmpty()) {
            setStatus("Select file(s) for checksum");
            return;
        }
        final List<Path> files = paths;
        final TransferControl control = new TransferControl();
        setStatus("Calculating " + algorithm + "…");
        ProgressTasks.run(
                this,
                "CRC",
                control,
                () -> {
                    control.begin(files.size());
                    final StringBuilder sb = new StringBuilder();
                    for (final Path file : files) {
                        control.throwIfCancelled();
                        final String value;
                        if ("CRC-32".equals(algorithm)) {
                            value = Checksums.formatCrc32(Checksums.crc32(file));
                        } else {
                            value = Checksums.digestHex(file, algorithm);
                        }
                        sb.append(algorithm).append("  ").append(value)
                                .append("  ").append(file.getFileName()).append('\n');
                        control.advance(file.getFileName().toString());
                    }
                    return sb.toString();
                },
                text -> {
                    setStatus(algorithm + " done");
                    final JTextArea area = new JTextArea(text);
                    area.setEditable(false);
                    area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
                    final JScrollPane scroll = new JScrollPane(area);
                    scroll.setPreferredSize(new Dimension(520, 200));
                    JOptionPane.showMessageDialog(this, scroll, algorithm, JOptionPane.INFORMATION_MESSAGE);
                },
                ex -> handleTransferError("Checksum failed", ex)
        );
    }

    private static Path safeResolveDisk(final FilePanel panel, final FileEntry lead) {
        try {
            if (panel.getLocationModel().isDisk() && lead != null && !lead.parentLink()) {
                return panel.resolveDiskEntry(lead);
            }
        } catch (final RuntimeException ignored) {
            // not a disk child
        }
        return null;
    }

    private JMenuItem popupItem(final String text, final boolean enabled, final Runnable action) {
        final JMenuItem item = new JMenuItem(text);
        item.setEnabled(enabled);
        if (enabled && action != null) {
            item.addActionListener(ev -> action.run());
        }
        return item;
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
                    final Path p = panel.resolveDiskEntry(lead);
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
        final TransferControl control = new TransferControl();
        setStatus("Extracting…");
        ProgressTasks.run(
                this,
                "Extract",
                control,
                () -> ZipArchiveFs.extract(zip, paths, dest, all, control),
                count -> {
                    setStatus("Extracted " + count + " file(s)");
                    if (this.dualPanel) {
                        inactivePanel().refreshAsync();
                    }
                    panel.refreshAsync();
                },
                ex -> handleTransferError("Extract failed", ex)
        );
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
                    final Path p = panel.resolveDiskEntry(lead);
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
                        Dual-panel orthodox file manager
                        inspired by 7-Zip File Manager (FM shell).

                        FM: F2 Rename · F5 Copy · F6 Move · F7 Mkdir
                        F3 View · F4 Edit · Shift+F4 New file · Alt+Enter Props
                        F9 2 Panels · Tab panel · Insert multi-select
                        Ctrl+1…4 views · Flat View · Alt+0…9 bookmarks
                        Alt+F12 history · \\ root · Alt+↑ same folder other panel

                        Archive Add/Extract/Test remain zip-only (unchanged).""",
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
