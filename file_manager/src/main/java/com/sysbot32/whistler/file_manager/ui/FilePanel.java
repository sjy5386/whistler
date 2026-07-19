package com.sysbot32.whistler.file_manager.ui;

import com.sysbot32.whistler.file_manager.archive.Archives;
import com.sysbot32.whistler.file_manager.archive.ZipArchiveFs;
import com.sysbot32.whistler.file_manager.listing.DirectoryListing;
import com.sysbot32.whistler.file_manager.listing.FlatListing;
import com.sysbot32.whistler.file_manager.listing.InsertSelection;
import com.sysbot32.whistler.file_manager.listing.SelectionHelpers;
import com.sysbot32.whistler.file_manager.model.BrowseLocation;
import com.sysbot32.whistler.file_manager.model.FileEntry;
import com.sysbot32.whistler.file_manager.model.ListingLoadGate;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * One orthodox-style panel: path bar + details table + selection state.
 */
@Slf4j
public final class FilePanel extends JPanel {
    private static final String CARD_DETAILS = "details";
    private static final String CARD_LIST = "list";

    private final FileTableModel tableModel = new FileTableModel();
    @Getter
    private final JTable table = new JTable(this.tableModel);
    private final DefaultListModel<FileEntry> listModel = new DefaultListModel<>();
    private final JList<FileEntry> iconList = new JList<>(this.listModel);
    private final CardLayout viewCards = new CardLayout();
    private final JPanel viewHost = new JPanel(this.viewCards);
    private final PathAddressBar addressBar;
    /** 7zFM-style "Up One Level" control beside the address bar. */
    private final JButton upButton = new JButton(FileManagerIcons.pathUp());
    private final boolean showHidden;

    /** Named locationModel so Lombok {@code getLocationModel()} does not clash with {@link Component#getLocation()}. */
    @Getter
    private BrowseLocation locationModel;
    private final ListingLoadGate loadGate = new ListingLoadGate();
    private Consumer<FilePanel> activationListener = p -> {
    };
    private Consumer<String> statusListener = s -> {
    };
    private Runnable openHandler = () -> {
    };
    private Consumer<BrowseLocation> navigationListener = loc -> {
    };
    /** Right-click popup; receives the listing component that was clicked. */
    private Consumer<MouseEvent> contextMenuHandler = e -> {
    };
    /** Dropped files (from Finder or another panel); frame performs copy/add. */
    private Consumer<List<Path>> dropFilesHandler = paths -> {
    };
    @Getter
    private boolean flatView;
    @Getter
    private ViewMode viewMode = ViewMode.DETAILS;

    public FilePanel(@NonNull final BrowseLocation initial, final boolean showHidden) {
        super(new BorderLayout(0, 2));
        this.showHidden = showHidden;
        this.locationModel = initial;
        this.addressBar = new PathAddressBar(this.locationModel);

        this.setBorder(null);

        this.upButton.setText(null);
        this.upButton.setToolTipText("Up One Level (Backspace)");
        this.upButton.setFocusable(false);
        this.upButton.setMargin(new Insets(0, 0, 0, 0));
        this.upButton.setPreferredSize(new Dimension(22, 22));
        this.upButton.setMaximumSize(new Dimension(22, 22));
        this.upButton.addActionListener(e -> {
            FilePanel.this.fireActivated();
            FilePanel.this.goUp();
        });

        this.addressBar.setCommitListener(this::navigateFromPathField);
        this.addressBar.setActivationListener(v -> FilePanel.this.fireActivated());
        this.addressBar.setNavigateListener(loc -> {
            FilePanel.this.fireActivated();
            FilePanel.this.navigateTo(loc);
        });

        // No FlowLayout — it adds vertical padding and fattened the path row
        final JPanel pathBar = new JPanel(new BorderLayout(3, 0));
        pathBar.setOpaque(false);
        pathBar.setPreferredSize(new Dimension(10, 24));
        pathBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        pathBar.add(this.upButton, BorderLayout.WEST);
        pathBar.add(this.addressBar, BorderLayout.CENTER);

        this.configureTable();
        this.configureIconList();
        this.installDragAndDrop();
        final JScrollPane tableScroll = new JScrollPane(this.table);
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setViewportBorder(BorderFactory.createEmptyBorder());

        final JScrollPane listScroll = new JScrollPane(this.iconList);
        listScroll.getViewport().setBackground(Color.WHITE);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setViewportBorder(BorderFactory.createEmptyBorder());

        this.viewHost.add(tableScroll, CARD_DETAILS);
        this.viewHost.add(listScroll, CARD_LIST);
        this.viewCards.show(this.viewHost, CARD_DETAILS);

        this.add(pathBar, BorderLayout.NORTH);
        this.add(this.viewHost, BorderLayout.CENTER);

        this.updateUpButton();
        this.refreshAsync();
    }

    private void configureIconList() {
        this.iconList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.iconList.setLayoutOrientation(JList.VERTICAL);
        this.iconList.setVisibleRowCount(-1);
        this.iconList.setFixedCellHeight(22);
        this.iconList.setCellRenderer(new IconListRenderer());
        this.iconList.setFocusTraversalKeysEnabled(false);
        this.iconList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                FilePanel.this.fireActivated();
                FilePanel.this.handleListingPopup(e, FilePanel.this.iconList);
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                    final int index = FilePanel.this.iconList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        FilePanel.this.iconList.setSelectedIndex(index);
                        FilePanel.this.openHandler.run();
                    }
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                FilePanel.this.handleListingPopup(e, FilePanel.this.iconList);
            }
        });
        this.iconList.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                FilePanel.this.fireActivated();
            }
        });
        this.iconList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                    FilePanel.this.toggleSelectionAtLead();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    FilePanel.this.openHandler.run();
                    e.consume();
                }
            }
        });
        this.iconList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FilePanel.this.updateSelectionStatus();
            }
        });
    }

    private void configureTable() {
        this.table.setShowGrid(false);
        this.table.setIntercellSpacing(new Dimension(0, 0));
        this.table.setRowHeight(18);
        this.table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.table.setFillsViewportHeight(true);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.table.getTableHeader().setReorderingAllowed(true);
        this.table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        // Model-side sort (no TableRowSorter): reliable column click + ".." always pinned
        this.table.setAutoCreateRowSorter(false);
        this.table.setRowSorter(null);
        this.installHeaderSort();

        this.table.setDefaultRenderer(FileEntry.class, new NameCellRenderer());
        this.table.setDefaultRenderer(Long.class, new SizeCellRenderer());
        this.table.setDefaultRenderer(Instant.class, new InstantCellRenderer());
        this.table.setDefaultRenderer(String.class, new DefaultTableCellRenderer());

        this.applyColumnWidths();
        this.installTableOpenBindings();

        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                FilePanel.this.fireActivated();
                FilePanel.this.handleListingPopup(e, FilePanel.this.table);
                // mousePressed is more reliable than mouseClicked (less loss if pointer moves slightly).
                // Handle only here so we do not open twice (pressed + clicked).
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                    FilePanel.this.openRowAtPoint(e);
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                FilePanel.this.handleListingPopup(e, FilePanel.this.table);
            }
        });

        this.table.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                FilePanel.this.fireActivated();
            }
        });

        this.table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_INSERT) {
                    FilePanel.this.toggleSelectionAtLead();
                    e.consume();
                }
            }
        });

        this.table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                FilePanel.this.updateSelectionStatus();
            }
        });
    }

    /**
     * Header sort uses press→release (not mouseClicked) so slight pointer movement
     * does not swallow the click the way double-click used to. Ignores resize/drag.
     */
    private void installHeaderSort() {
        final JTableHeader header = this.table.getTableHeader();
        // Press state (arrays so the listener can mutate)
        final int[] pressViewCol = {-1};
        final Point[] pressPoint = {null};

        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                FilePanel.this.fireActivated();
                pressViewCol[0] = -1;
                pressPoint[0] = null;
                if (!SwingUtilities.isLeftMouseButton(e) || e.isPopupTrigger()) {
                    return;
                }
                if (isHeaderResizeZone(header, e.getPoint())) {
                    return;
                }
                final int col = header.columnAtPoint(e.getPoint());
                if (col < 0) {
                    return;
                }
                pressViewCol[0] = col;
                pressPoint[0] = e.getPoint();
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || pressViewCol[0] < 0 || pressPoint[0] == null) {
                    pressViewCol[0] = -1;
                    return;
                }
                try {
                    if (isHeaderResizeZone(header, e.getPoint())) {
                        return;
                    }
                    final int releaseCol = header.columnAtPoint(e.getPoint());
                    if (releaseCol != pressViewCol[0]) {
                        return;
                    }
                    // Treat as drag (reorder/resize) if the pointer moved noticeably
                    final int dx = Math.abs(e.getX() - pressPoint[0].x);
                    final int dy = Math.abs(e.getY() - pressPoint[0].y);
                    if (dx > 5 || dy > 5) {
                        return;
                    }
                    final int modelCol = FilePanel.this.table.convertColumnIndexToModel(pressViewCol[0]);
                    FilePanel.this.tableModel.toggleSort(modelCol);
                    FilePanel.this.refreshHeaderLabels();
                } finally {
                    pressViewCol[0] = -1;
                    pressPoint[0] = null;
                }
            }
        });
    }

    /** True when the pointer is on a column-edge resize handle. */
    private static boolean isHeaderResizeZone(final JTableHeader header, final Point p) {
        final int col = header.columnAtPoint(p);
        if (col < 0) {
            return false;
        }
        final Rectangle r = header.getHeaderRect(col);
        final int edge = 4;
        return (p.x - r.x) < edge || (r.x + r.width - p.x) < edge;
    }

    /** TableColumn caches header strings — push live names (with ▲/▼) after sort. */
    private void refreshHeaderLabels() {
        final TableColumnModel cols = this.table.getColumnModel();
        for (int i = 0; i < cols.getColumnCount(); i++) {
            final int modelCol = this.table.convertColumnIndexToModel(i);
            cols.getColumn(i).setHeaderValue(this.tableModel.getColumnName(modelCol));
        }
        this.table.getTableHeader().resizeAndRepaint();
    }

    /**
     * 7zFM: Enter opens the current item. JTable's default Enter advances selection — replace it.
     */
    private void installTableOpenBindings() {
        final AbstractAction openAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                FilePanel.this.fireActivated();
                FilePanel.this.openHandler.run();
            }
        };
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        for (final int condition : new int[]{
                JComponent.WHEN_FOCUSED,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        }) {
            final InputMap im = this.table.getInputMap(condition);
            im.put(enter, "fileManagerOpen");
        }
        final ActionMap am = this.table.getActionMap();
        am.put("fileManagerOpen", openAction);
        am.put("selectNextRowCell", openAction);
        am.put("selectNextRow", openAction);
    }

    /** Select the row under the mouse then open — matches 7zFM double-click. */
    private void openRowAtPoint(final MouseEvent e) {
        final int row = this.table.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        this.table.getSelectionModel().setSelectionInterval(row, row);
        this.table.getSelectionModel().setLeadSelectionIndex(row);
        this.table.getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
        this.openHandler.run();
    }

    public void setActivationListener(final Consumer<FilePanel> listener) {
        this.activationListener = listener != null ? listener : p -> {
        };
    }

    public void setStatusListener(final Consumer<String> listener) {
        this.statusListener = listener != null ? listener : s -> {
        };
    }

    public void setOpenHandler(final Runnable handler) {
        this.openHandler = handler != null ? handler : () -> {
        };
    }

    public void setNavigationListener(final Consumer<BrowseLocation> listener) {
        this.navigationListener = listener != null ? listener : loc -> {
        };
    }

    public JTextField getPathField() {
        return this.addressBar.getEditor();
    }

    public void setDropFilesHandler(final Consumer<List<Path>> handler) {
        this.dropFilesHandler = handler != null ? handler : paths -> {
        };
    }

    public void setContextMenuHandler(final Consumer<MouseEvent> handler) {
        this.contextMenuHandler = handler == null ? e -> {
        } : handler;
    }

    /**
     * Platform popup-trigger (macOS often fires on release): select row under cursor if needed, then open menu.
     */
    private void handleListingPopup(final MouseEvent e, final JComponent listing) {
        if (!e.isPopupTrigger()) {
            return;
        }
        selectRowAtPointIfNeeded(e, listing);
        fireActivated();
        this.contextMenuHandler.accept(e);
    }

    private void selectRowAtPointIfNeeded(final MouseEvent e, final JComponent listing) {
        if (listing == this.table) {
            final int row = this.table.rowAtPoint(e.getPoint());
            if (row >= 0 && !this.table.isRowSelected(row)) {
                this.table.getSelectionModel().setSelectionInterval(row, row);
            }
        } else if (listing == this.iconList) {
            final int index = this.iconList.locationToIndex(e.getPoint());
            if (index >= 0 && !this.iconList.isSelectedIndex(index)) {
                this.iconList.setSelectedIndex(index);
            }
        }
    }

    public void requestTableFocus() {
        if (this.viewMode == ViewMode.DETAILS) {
            this.table.requestFocusInWindow();
        } else {
            this.iconList.requestFocusInWindow();
        }
    }

    public void focusPathField() {
        this.addressBar.getEditor().requestFocusInWindow();
        this.addressBar.getEditor().selectAll();
    }

    public void setFlatView(final boolean flat) {
        if (this.flatView == flat) {
            return;
        }
        this.flatView = flat;
        this.refreshAsync();
    }

    public void setViewMode(final ViewMode mode) {
        this.viewMode = mode != null ? mode : ViewMode.DETAILS;
        if (this.viewMode == ViewMode.DETAILS) {
            this.viewCards.show(this.viewHost, CARD_DETAILS);
        } else {
            applyListLayout();
            this.viewCards.show(this.viewHost, CARD_LIST);
        }
        syncListFromTableModel();
        requestTableFocus();
    }

    private void applyListLayout() {
        switch (this.viewMode) {
            case LARGE_ICONS -> {
                this.iconList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                this.iconList.setVisibleRowCount(0);
                // Room for 32px glyph + label under icon
                this.iconList.setFixedCellHeight(64);
                this.iconList.setFixedCellWidth(88);
            }
            case SMALL_ICONS -> {
                this.iconList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                this.iconList.setVisibleRowCount(0);
                // Room for 24px glyph + text to the right
                this.iconList.setFixedCellHeight(40);
                this.iconList.setFixedCellWidth(100);
            }
            case LIST -> {
                this.iconList.setLayoutOrientation(JList.VERTICAL);
                this.iconList.setVisibleRowCount(-1);
                this.iconList.setFixedCellHeight(22);
                this.iconList.setFixedCellWidth(-1);
            }
            default -> {
                // details uses table
            }
        }
        this.iconList.revalidate();
        this.iconList.repaint();
    }

    private void syncListFromTableModel() {
        this.listModel.clear();
        for (final FileEntry entry : this.tableModel.getEntries()) {
            this.listModel.addElement(entry);
        }
    }

    public void sortBy(final int column, final boolean ascending) {
        this.tableModel.sortBy(column, ascending);
        this.refreshHeaderLabels();
        syncListFromTableModel();
    }

    public void setUnsorted() {
        this.tableModel.setUnsorted();
        this.refreshHeaderLabels();
        syncListFromTableModel();
    }

    public void selectAllItems() {
        applyRowSelection(SelectionHelpers.selectableRows(this.tableModel.getEntries()));
    }

    public void deselectAllItems() {
        applyRowSelection(Set.of());
    }

    public void invertSelection() {
        final Set<Integer> current = currentSelectedRows();
        final Set<Integer> next = SelectionHelpers.invert(
                current,
                SelectionHelpers.selectableRows(this.tableModel.getEntries())
        );
        applyRowSelection(next);
    }

    public void selectByType(final boolean select) {
        final FileEntry lead = getLeadEntry();
        if (lead == null || lead.parentLink()) {
            this.statusListener.accept("Select a file to match type");
            return;
        }
        final Set<Integer> next = SelectionHelpers.applyTypeFilter(
                currentSelectedRows(),
                this.tableModel.getEntries(),
                lead.name(),
                select
        );
        applyRowSelection(next);
    }

    private Set<Integer> currentSelectedRows() {
        final Set<Integer> rows = new HashSet<>();
        if (this.viewMode == ViewMode.DETAILS) {
            for (final int r : this.table.getSelectedRows()) {
                rows.add(r);
            }
        } else {
            for (final int r : this.iconList.getSelectedIndices()) {
                rows.add(r);
            }
        }
        return rows;
    }

    private void applyRowSelection(final Set<Integer> rows) {
        if (this.viewMode == ViewMode.DETAILS) {
            this.table.clearSelection();
            for (final int r : SelectionHelpers.sortedRows(rows)) {
                if (r >= 0 && r < this.table.getRowCount()) {
                    this.table.addRowSelectionInterval(r, r);
                }
            }
        } else {
            this.iconList.clearSelection();
            for (final int r : SelectionHelpers.sortedRows(rows)) {
                if (r >= 0 && r < this.listModel.size()) {
                    this.iconList.addSelectionInterval(r, r);
                }
            }
        }
        updateSelectionStatus();
    }

    /**
     * Dual-panel focus chrome only. Single-panel mode uses no outline at all
     * (panel border + scroll borders otherwise look like a stray line above the status bar).
     */
    public void setActiveBorder(final boolean dualMode, final boolean active) {
        if (!dualMode) {
            this.setBorder(null);
            return;
        }
        if (active) {
            this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0x3366CC), 1),
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
        } else {
            this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xC0C0C0), 1),
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
        }
    }

    public void navigateTo(final BrowseLocation next) {
        this.locationModel = Objects.requireNonNull(next, "next");
        // Update path immediately so ops (F5/F6/…) resolve against the new location even while listing.
        this.addressBar.setLocationDisplay(this.locationModel);
        this.updateUpButton();
        this.navigationListener.accept(this.locationModel);
        this.refreshAsync();
    }

    public void goUp() {
        if (this.locationModel.canGoUp()) {
            this.navigateTo(this.locationModel.parent());
        }
    }

    private void updateUpButton() {
        this.upButton.setEnabled(this.locationModel.canGoUp());
    }

    /**
     * Always starts a new listing for {@link #locationModel}. Concurrent older loads are ignored
     * via {@link ListingLoadGate} tokens (navigate during load never applies stale rows).
     */
    public void refreshAsync() {
        final long token = this.loadGate.nextToken();
        final BrowseLocation target = this.locationModel;
        this.addressBar.setLocationDisplay(target);
        this.statusListener.accept("Loading…");

        final SwingWorker<List<FileEntry>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<FileEntry> doInBackground() throws Exception {
                if (target.isZip()) {
                    return Archives.zip().list(target.path(), target.zipInternalPath());
                }
                if (FilePanel.this.flatView) {
                    return FlatListing.list(target.path(), FilePanel.this.showHidden);
                }
                return DirectoryListing.list(target.path(), FilePanel.this.showHidden);
            }

            @Override
            protected void done() {
                if (!FilePanel.this.loadGate.isCurrent(token)) {
                    return; // superseded by a newer navigate/refresh
                }
                try {
                    final List<FileEntry> entries = this.get();
                    if (!FilePanel.this.loadGate.isCurrent(token)) {
                        return;
                    }
                    FilePanel.this.tableModel.setEntries(entries);
                    FilePanel.this.syncListFromTableModel();
                    FilePanel.this.applyColumnWidths();
                    FilePanel.this.refreshHeaderLabels();
                    FilePanel.this.updateUpButton();
                    if (FilePanel.this.tableModel.getRowCount() > 0) {
                        FilePanel.this.applyRowSelection(Set.of(0));
                    }
                    FilePanel.this.updateSelectionStatus();
                } catch (final Exception ex) {
                    if (!FilePanel.this.loadGate.isCurrent(token)) {
                        return;
                    }
                    final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    log.warn("Listing failed for {}: {}", target, cause.toString());
                    FilePanel.this.statusListener.accept("Error: " + cause.getMessage());
                    FilePanel.this.tableModel.setEntries(List.of(FileEntry.upLink()));
                    FilePanel.this.syncListFromTableModel();
                    FilePanel.this.refreshHeaderLabels();
                    FilePanel.this.updateUpButton();
                }
            }
        };
        worker.execute();
    }

    private void applyColumnWidths() {
        final TableColumnModel cols = this.table.getColumnModel();
        if (cols.getColumnCount() < 7) {
            return;
        }
        cols.getColumn(0).setPreferredWidth(200);
        cols.getColumn(1).setPreferredWidth(80);
        cols.getColumn(2).setPreferredWidth(120);
        cols.getColumn(3).setPreferredWidth(120);
        cols.getColumn(4).setPreferredWidth(120);
        cols.getColumn(5).setPreferredWidth(70);
        cols.getColumn(6).setPreferredWidth(90);
    }

    private void navigateFromPathField() {
        final String text = this.addressBar.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            final BrowseLocation parsed = parseDisplayPath(text);
            this.navigateTo(parsed);
        } catch (final Exception ex) {
            this.statusListener.accept("Invalid path: " + ex.getMessage());
        }
    }

    public static BrowseLocation parseDisplayPath(final String text) {
        final int bang = text.indexOf('!');
        if (bang > 0) {
            final Path zip = Path.of(text.substring(0, bang));
            String inner = text.substring(bang + 1).replace('\\', '/');
            while (inner.startsWith("/")) {
                inner = inner.substring(1);
            }
            if (!inner.isEmpty() && !inner.endsWith("/")) {
                inner = inner + "/";
            }
            return BrowseLocation.zip(zip, inner);
        }
        return BrowseLocation.disk(Path.of(text));
    }

    public List<FileEntry> getSelectedEntries(final boolean includeParentLink) {
        final List<FileEntry> selected = new ArrayList<>();
        for (final int row : currentSelectedRows()) {
            final FileEntry entry = this.tableModel.getEntry(row);
            if (entry == null) {
                continue;
            }
            if (entry.parentLink() && !includeParentLink) {
                continue;
            }
            selected.add(entry);
        }
        return selected;
    }

    public FileEntry getLeadEntry() {
        int row;
        if (this.viewMode == ViewMode.DETAILS) {
            row = this.table.getSelectionModel().getLeadSelectionIndex();
            if (row < 0) {
                row = this.table.getSelectedRow();
            }
        } else {
            row = this.iconList.getLeadSelectionIndex();
            if (row < 0) {
                row = this.iconList.getSelectedIndex();
            }
        }
        return this.tableModel.getEntry(row);
    }

    public Path resolveDiskEntry(final FileEntry entry) {
        if (!this.locationModel.isDisk() || entry == null || entry.parentLink()) {
            throw new IllegalStateException("Not a disk child entry");
        }
        if (this.flatView) {
            return FlatListing.resolveRelative(this.locationModel.path(), entry.name());
        }
        // For non-flat, only first path segment is used under current dir
        final String name = entry.name();
        if (name.contains("/") || name.contains("\\")) {
            return FlatListing.resolveRelative(this.locationModel.path(), name);
        }
        return this.locationModel.resolveDiskChild(name);
    }

    public List<Path> getSelectedDiskPaths() {
        if (!this.locationModel.isDisk()) {
            return List.of();
        }
        final List<Path> paths = new ArrayList<>();
        for (final FileEntry entry : getSelectedEntries(false)) {
            paths.add(resolveDiskEntry(entry));
        }
        return paths;
    }

    public List<String> getSelectedZipInternalPaths() {
        if (!this.locationModel.isZip()) {
            return List.of();
        }
        final List<String> paths = new ArrayList<>();
        for (final FileEntry entry : getSelectedEntries(false)) {
            paths.add(this.locationModel.resolveZipChild(entry.name(), entry.directory()));
        }
        return paths;
    }

    public int getSelectionCount() {
        return getSelectedEntries(false).size();
    }

    public long getSelectedTotalSize() {
        long total = 0L;
        for (final FileEntry entry : getSelectedEntries(false)) {
            if (!entry.directory()) {
                total += Math.max(0L, entry.size());
            }
        }
        return total;
    }

    private void toggleSelectionAtLead() {
        if (this.viewMode == ViewMode.DETAILS) {
            final int next = InsertSelection.toggleAndAdvanceLead(
                    this.table.getSelectionModel(),
                    this.table.getRowCount()
            );
            if (next >= 0) {
                this.table.scrollRectToVisible(this.table.getCellRect(next, 0, true));
            }
        } else {
            final int next = InsertSelection.toggleAndAdvanceLead(
                    this.iconList.getSelectionModel(),
                    this.listModel.size()
            );
            if (next >= 0) {
                this.iconList.ensureIndexIsVisible(next);
            }
        }
        this.updateSelectionStatus();
    }

    private void updateSelectionStatus() {
        final int selected = getSelectionCount();
        final long size = getSelectedTotalSize();
        final String objects = selected + " object(s) selected";
        this.statusListener.accept(objects + "    " + FileTableModel.formatSize(size));
    }

    private void fireActivated() {
        this.activationListener.accept(this);
    }

    public void openCurrent() throws IOException {
        final FileEntry entry = getLeadEntry();
        if (entry == null) {
            return;
        }
        if (entry.parentLink()) {
            goUp();
            return;
        }
        if (this.locationModel.isDisk()) {
            final Path child = resolveDiskEntry(entry);
            if (entry.directory()) {
                this.navigateTo(BrowseLocation.disk(child));
            } else if (DirectoryListing.looksLikeZip(child)) {
                this.navigateTo(BrowseLocation.zip(child, ""));
            } else {
                openExternally(child);
            }
            return;
        }
        if (entry.directory()) {
            this.navigateTo(this.locationModel.enterDirectory(entry.name()));
        } else {
            final Path tempDir = Files.createTempDirectory("file-manager-open-");
            final String internal = this.locationModel.resolveZipChild(entry.name(), false);
            Archives.zip().extract(this.locationModel.path(), List.of(internal), tempDir, false, null);
            final Path extracted = ZipArchiveFs.resolveSafe(tempDir.toAbsolutePath().normalize(), internal);
            openExternally(extracted);
        }
    }

    public void openExternallyLead() throws IOException {
        final FileEntry entry = getLeadEntry();
        if (entry == null || entry.parentLink()) {
            return;
        }
        if (!this.locationModel.isDisk()) {
            openCurrent();
            return;
        }
        openExternally(resolveDiskEntry(entry));
    }

    public void openWithDesktopEdit() throws IOException {
        final FileEntry entry = getLeadEntry();
        if (entry == null || entry.parentLink() || entry.directory()) {
            throw new IOException("Select a file to edit");
        }
        if (!this.locationModel.isDisk()) {
            throw new IOException("Edit inside archives is not supported");
        }
        final Path file = resolveDiskEntry(entry);
        if (!java.awt.Desktop.isDesktopSupported()) {
            throw new IOException("Desktop not supported");
        }
        final java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if (desktop.isSupported(java.awt.Desktop.Action.EDIT)) {
            desktop.edit(file.toFile());
            return;
        }
        if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
            desktop.open(file.toFile());
            return;
        }
        throw new IOException("No system viewer/editor available");
    }

    private static void openExternally(final Path file) throws IOException {
        if (!java.awt.Desktop.isDesktopSupported()) {
            throw new IOException("Desktop open not supported");
        }
        final java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if (!desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
            throw new IOException("Open action not supported");
        }
        desktop.open(file.toFile());
    }

    private final class IconListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                final JList<?> list,
                final Object value,
                final int index,
                final boolean isSelected,
                final boolean cellHasFocus
        ) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus
            );
            if (value instanceof FileEntry entry) {
                final String text = entry.parentLink() ? ".." : entry.name();
                label.setText(text);
                final FileManagerIcons.ListKind kind;
                if (entry.parentLink()) {
                    kind = FileManagerIcons.ListKind.UP;
                } else if (entry.directory()) {
                    kind = FileManagerIcons.ListKind.FOLDER;
                } else if (entry.isZipName()) {
                    kind = FileManagerIcons.ListKind.ZIP;
                } else {
                    kind = FileManagerIcons.ListKind.FILE;
                }
                label.setIcon(FileManagerIcons.forView(FilePanel.this.viewMode, kind));
                if (FilePanel.this.viewMode == ViewMode.LARGE_ICONS) {
                    // Classic large-icon layout: glyph on top, name under it
                    label.setHorizontalTextPosition(SwingConstants.CENTER);
                    label.setVerticalTextPosition(SwingConstants.BOTTOM);
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    label.setHorizontalTextPosition(SwingConstants.RIGHT);
                    label.setVerticalTextPosition(SwingConstants.CENTER);
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                }
            }
            return label;
        }
    }

    private static final class NameCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                final JTable table,
                final Object value,
                final boolean isSelected,
                final boolean hasFocus,
                final int row,
                final int column
        ) {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column
            );
            label.setHorizontalAlignment(SwingConstants.LEFT);
            if (value instanceof FileEntry entry) {
                if (entry.parentLink()) {
                    label.setText("..");
                    label.setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.UP));
                } else if (entry.directory()) {
                    label.setText(entry.name());
                    label.setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.FOLDER));
                } else if (entry.isZipName()) {
                    label.setText(entry.name());
                    label.setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.ZIP));
                } else {
                    label.setText(entry.name());
                    label.setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.FILE));
                }
            } else {
                label.setIcon(null);
            }
            return label;
        }
    }

    private void installDragAndDrop() {
        final TransferHandler handler = new ListingTransferHandler();
        this.table.setDragEnabled(true);
        this.table.setDropMode(DropMode.ON);
        this.table.setTransferHandler(handler);
        this.iconList.setDragEnabled(true);
        this.iconList.setDropMode(DropMode.ON);
        this.iconList.setTransferHandler(handler);
    }

    /**
     * Drag selected disk files out; drop files onto disk (copy) or zip (add).
     */
    private final class ListingTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(final JComponent c) {
            if (!FilePanel.this.locationModel.isDisk()) {
                return NONE;
            }
            return COPY_OR_MOVE;
        }

        @Override
        protected Transferable createTransferable(final JComponent c) {
            final List<Path> paths = FilePanel.this.getSelectedDiskPaths();
            if (paths.isEmpty()) {
                return null;
            }
            final List<File> files = new ArrayList<>(paths.size());
            for (final Path p : paths) {
                files.add(p.toFile());
            }
            return new FileListTransferable(files);
        }

        @Override
        public boolean canImport(final TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }
            return FilePanel.this.locationModel.isDisk() || FilePanel.this.locationModel.isZip();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(final TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                final List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                if (files == null || files.isEmpty()) {
                    return false;
                }
                final List<Path> paths = new ArrayList<>(files.size());
                for (final File f : files) {
                    if (f != null) {
                        paths.add(f.toPath());
                    }
                }
                if (paths.isEmpty()) {
                    return false;
                }
                FilePanel.this.fireActivated();
                FilePanel.this.dropFilesHandler.accept(List.copyOf(paths));
                return true;
            } catch (final UnsupportedFlavorException | IOException ex) {
                log.debug("Drop import failed: {}", ex.toString());
                return false;
            }
        }
    }

    private static final class FileListTransferable implements Transferable {
        private final List<File> files;

        FileListTransferable(final List<File> files) {
            this.files = List.copyOf(files);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return this.files;
        }
    }

    private static final class SizeCellRenderer extends DefaultTableCellRenderer {
        {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(
                final JTable table,
                final Object value,
                final boolean isSelected,
                final boolean hasFocus,
                final int row,
                final int column
        ) {
            final String text = value instanceof Long n ? FileTableModel.formatSize(n) : "";
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    private static final class InstantCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                final JTable table,
                final Object value,
                final boolean isSelected,
                final boolean hasFocus,
                final int row,
                final int column
        ) {
            final String text = value instanceof Instant instant ? FileTableModel.formatDate(instant) : "";
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }
}
