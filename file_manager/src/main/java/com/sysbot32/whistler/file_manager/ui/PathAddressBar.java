package com.sysbot32.whistler.file_manager.ui;

import com.sysbot32.whistler.file_manager.model.BrowseLocation;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 7zFM-style address bar: single-line field with folder icon inset on the left
 * and a combo dropdown showing a folder tree.
 */
public final class PathAddressBar extends JPanel {
    /** Room for list-size (16px) folder icon + thin border — still one text-field row. */
    private static final int FIELD_HEIGHT = 24;
    private static final Color BORDER = new Color(0xA8A8A8);
    private static final Color BG = Color.WHITE;

    @Getter
    private final JTextField editor = new JTextField();
    /** Same glyph size as the file list (not the compact path-only set). */
    private final JLabel iconLabel = new JLabel(FileManagerIcons.list(FileManagerIcons.ListKind.FOLDER));
    private final JButton dropButton = new JButton(FileManagerIcons.chevronDown());
    private final JPanel fieldChrome = new JPanel(new BorderLayout(0, 0));

    private BrowseLocation location;
    private Consumer<BrowseLocation> navigateListener = loc -> {
    };
    private Consumer<Void> activationListener = v -> {
    };
    private Runnable commitListener = () -> {
    };

    public PathAddressBar(@NonNull final BrowseLocation initial) {
        super(new BorderLayout());
        this.location = initial;
        this.setOpaque(false);

        this.fieldChrome.setBackground(BG);
        this.fieldChrome.setOpaque(true);
        // Thin line border (macOS TextField.border is tall / padded)
        this.fieldChrome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        this.iconLabel.setOpaque(false);
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 2));
        this.iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.iconLabel.setPreferredSize(new Dimension(FileManagerIcons.LIST_SIZE + 8, FIELD_HEIGHT - 2));

        this.editor.setText(this.location.displayPath());
        this.editor.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        this.editor.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        this.editor.setMargin(new Insets(0, 0, 0, 0));
        this.editor.setOpaque(false);
        this.editor.setBackground(BG);
        this.editor.addActionListener(e -> this.commitListener.run());
        this.editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                PathAddressBar.this.activationListener.accept(null);
            }
        });

        this.dropButton.setIcon(FileManagerIcons.chevronDown());
        this.dropButton.setText(null);
        this.dropButton.setMargin(new Insets(0, 0, 0, 0));
        this.dropButton.setFocusable(false);
        this.dropButton.setBorderPainted(false);
        this.dropButton.setContentAreaFilled(false);
        this.dropButton.setOpaque(false);
        this.dropButton.setPreferredSize(new Dimension(16, FIELD_HEIGHT - 2));
        this.dropButton.setToolTipText("Show folder tree");
        this.dropButton.addActionListener(e -> {
            PathAddressBar.this.activationListener.accept(null);
            PathAddressBar.this.showFolderTree();
        });

        this.fieldChrome.add(this.iconLabel, BorderLayout.WEST);
        this.fieldChrome.add(this.editor, BorderLayout.CENTER);
        this.fieldChrome.add(this.dropButton, BorderLayout.EAST);

        final Dimension fixed = new Dimension(10, FIELD_HEIGHT);
        this.fieldChrome.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
        this.fieldChrome.setMinimumSize(fixed);
        this.fieldChrome.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        this.setPreferredSize(new Dimension(200, FIELD_HEIGHT));
        this.setMinimumSize(fixed);
        this.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));

        this.add(this.fieldChrome, BorderLayout.CENTER);
    }

    public void setNavigateListener(final Consumer<BrowseLocation> listener) {
        this.navigateListener = listener != null ? listener : loc -> {
        };
    }

    public void setActivationListener(final Consumer<Void> listener) {
        this.activationListener = listener != null ? listener : v -> {
        };
    }

    public void setCommitListener(final Runnable listener) {
        this.commitListener = listener != null ? listener : () -> {
        };
    }

    public void setLocationDisplay(final BrowseLocation loc) {
        this.location = Objects.requireNonNull(loc, "loc");
        final String text = loc.displayPath();
        if (!text.equals(this.editor.getText())) {
            this.editor.setText(text);
        }
        if (loc.isZip()) {
            this.iconLabel.setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.ZIP));
        } else {
            this.iconLabel.setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.FOLDER));
        }
    }

    public String getText() {
        return this.editor.getText();
    }

    private void showFolderTree() {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new TreeEntry("Computer", null, true));
        for (final Path r : FileSystems.getDefault().getRootDirectories()) {
            root.add(new FolderNode(r));
        }
        final Path home = Path.of(System.getProperty("user.home"));
        if (Files.isDirectory(home)) {
            root.add(new FolderNode(home));
        }

        final DefaultTreeModel model = new DefaultTreeModel(root);
        final JTree tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new FolderTreeRenderer());
        tree.setRowHeight(18);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(final TreeExpansionEvent event) {
                final Object last = event.getPath().getLastPathComponent();
                if (last instanceof FolderNode node) {
                    node.ensureChildrenLoaded(model);
                }
            }

            @Override
            public void treeWillCollapse(final TreeExpansionEvent event) {
            }
        });

        expandTowardCurrent(tree, root);

        final JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER));
        final JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(Math.max(320, this.getWidth()), 280));
        popup.add(scroll);

        final Runnable navigateFromSelection = () -> {
            final TreePath sel = tree.getSelectionPath();
            if (sel == null) {
                return;
            }
            final Object last = sel.getLastPathComponent();
            if (last instanceof FolderNode node && node.path != null) {
                popup.setVisible(false);
                PathAddressBar.this.navigateListener.accept(BrowseLocation.disk(node.path));
            }
        };

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                final TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                final Rectangle bounds = tree.getPathBounds(path);
                if (bounds != null && e.getX() < bounds.x) {
                    return; // disclosure triangle — expand only
                }
                tree.setSelectionPath(path);
                if (path.getLastPathComponent() instanceof FolderNode) {
                    navigateFromSelection.run();
                }
            }
        });
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateFromSelection.run();
                    e.consume();
                }
            }
        });

        popup.show(this.fieldChrome, 0, this.fieldChrome.getHeight());
        SwingUtilities.invokeLater(tree::requestFocusInWindow);
    }

    private void expandTowardCurrent(final JTree tree, final DefaultMutableTreeNode root) {
        if (this.location == null) {
            return;
        }
        Path target = this.location.isDisk() ? this.location.path() : this.location.path().getParent();
        if (target == null) {
            target = this.location.path();
        }
        target = target.toAbsolutePath().normalize();

        final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        FolderNode anchor = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            if (root.getChildAt(i) instanceof FolderNode fn && fn.path != null
                    && target.startsWith(fn.path)) {
                if (anchor == null || fn.path.getNameCount() >= anchor.path.getNameCount()) {
                    anchor = fn;
                }
            }
        }
        if (anchor == null) {
            return;
        }

        DefaultMutableTreeNode cursor = anchor;
        tree.expandPath(new TreePath(cursor.getPath()));
        final List<Path> chain = pathChain(target);
        for (final Path step : chain) {
            if (!(cursor instanceof FolderNode fn)) {
                break;
            }
            if (step.equals(fn.path)) {
                continue;
            }
            if (!step.startsWith(fn.path)) {
                continue;
            }
            fn.ensureChildrenLoaded(model);
            FolderNode next = null;
            for (int i = 0; i < cursor.getChildCount(); i++) {
                if (cursor.getChildAt(i) instanceof FolderNode child && step.equals(child.path)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            cursor = next;
            tree.expandPath(new TreePath(cursor.getPath()));
        }
        tree.setSelectionPath(new TreePath(cursor.getPath()));
        tree.scrollPathToVisible(new TreePath(cursor.getPath()));
    }

    private static List<Path> pathChain(final Path target) {
        final List<Path> parts = new ArrayList<>();
        Path p = target;
        while (p != null) {
            parts.addFirst(p);
            p = p.getParent();
        }
        return parts;
    }

    private record TreeEntry(String label, Path path, boolean synthetic) {
        @Override
        public String toString() {
            return this.label;
        }
    }

    private static final class FolderNode extends DefaultMutableTreeNode {
        private final Path path;
        private boolean loaded;

        FolderNode(final Path path) {
            super(new TreeEntry(
                    path.getFileName() != null ? path.getFileName().toString() : path.toString(),
                    path,
                    false
            ));
            this.path = path;
            this.add(new DefaultMutableTreeNode("Loading…"));
        }

        void ensureChildrenLoaded(final DefaultTreeModel model) {
            if (this.loaded) {
                return;
            }
            this.loaded = true;
            this.removeAllChildren();
            try {
                if (!Files.isDirectory(this.path)) {
                    model.nodeStructureChanged(this);
                    return;
                }
                final List<Path> dirs = new ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.path)) {
                    for (final Path child : stream) {
                        try {
                            if (Files.isDirectory(child) && !Files.isHidden(child)) {
                                dirs.add(child);
                            }
                        } catch (final IOException ignored) {
                            // skip
                        }
                    }
                }
                dirs.sort(Comparator.comparing(
                        p -> p.getFileName() != null ? p.getFileName().toString() : p.toString(),
                        String.CASE_INSENSITIVE_ORDER
                ));
                for (final Path d : dirs) {
                    this.add(new FolderNode(d));
                }
            } catch (final IOException ex) {
                this.add(new DefaultMutableTreeNode("(inaccessible)"));
            }
            model.nodeStructureChanged(this);
        }
    }

    private static final class FolderTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(
                final JTree tree,
                final Object value,
                final boolean sel,
                final boolean expanded,
                final boolean leaf,
                final int row,
                final boolean hasFocus
        ) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof FolderNode) {
                setIcon(FileManagerIcons.list(FileManagerIcons.ListKind.FOLDER));
            } else if (value instanceof DefaultMutableTreeNode node
                    && node.getUserObject() instanceof TreeEntry te
                    && te.synthetic()) {
                setIcon(null);
            }
            return this;
        }
    }
}
