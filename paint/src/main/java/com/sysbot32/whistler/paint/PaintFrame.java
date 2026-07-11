package com.sysbot32.whistler.paint;

import com.sysbot32.whistler.config.Config;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Classic XP Paint shell: menus, canvas, toolbox/color/status placeholders.
 */
public class PaintFrame extends JFrame {
    private static final String UNTITLED = "untitled";
    private static final String TITLE = "Paint";

    private final Paint paint;
    private final Config config;
    private final PaintCanvas canvas;
    private final JLabel statusLabel = new JLabel(" For Help, click Help Topics on the Help Menu.");
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private final JCheckBoxMenuItem toolBoxMenuItem = new JCheckBoxMenuItem("Tool Box", true);
    private final JCheckBoxMenuItem colorBoxMenuItem = new JCheckBoxMenuItem("Color Box", true);
    private final JCheckBoxMenuItem statusBarMenuItem = new JCheckBoxMenuItem("Status Bar", true);
    private final JPanel toolBox = new JPanel();
    private final JPanel colorBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));

    public PaintFrame(final Paint paint, final Config config) {
        this.paint = paint;
        this.config = config;

        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        final JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        this.canvas = new PaintCanvas(this.paint);
        final JScrollPane scrollPane = new JScrollPane(this.canvas);

        this.toolBox.setPreferredSize(new Dimension(56, 0));
        this.toolBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.toolBox.setLayout(new GridLayout(0, 2, 2, 2));
        // Placeholder tools — real tools land in later XP Paint feature work.
        for (int i = 0; i < 16; i++) {
            final JButton stub = new JButton();
            stub.setEnabled(false);
            stub.setPreferredSize(new Dimension(24, 24));
            this.toolBox.add(stub);
        }

        this.colorBox.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.colorBox.add(new JLabel("Colors"));

        this.statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.statusBar.add(this.statusLabel, BorderLayout.WEST);

        // Classic shell: toolbox | canvas, color box + status under canvas.
        final JPanel south = new JPanel(new BorderLayout());
        south.add(this.colorBox, BorderLayout.CENTER);
        south.add(this.statusBar, BorderLayout.SOUTH);

        this.setJMenuBar(this.createMenuBar());
        contentPane.add(this.toolBox, BorderLayout.WEST);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(south, BorderLayout.SOUTH);

        this.toolBoxMenuItem.addActionListener(e -> this.toolBox.setVisible(this.toolBoxMenuItem.isSelected()));
        this.colorBoxMenuItem.addActionListener(e -> this.colorBox.setVisible(this.colorBoxMenuItem.isSelected()));
        this.statusBarMenuItem.addActionListener(e -> this.statusBar.setVisible(this.statusBarMenuItem.isSelected()));

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                PaintFrame.this.exit();
            }
        });

        this.applyConfig();
        this.refreshTitle();
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(this.createFileMenu());
        menuBar.add(this.createEditMenu());
        menuBar.add(this.createViewMenu());
        menuBar.add(this.createImageMenu());
        menuBar.add(this.createColorsMenu());
        menuBar.add(this.createHelpMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem newItem = new JMenuItem("New", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> this.newDocument());

        final JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> this.open());

        final JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> this.save());

        final JMenuItem saveAsItem = new JMenuItem("Save As...", KeyEvent.VK_A);
        saveAsItem.addActionListener(e -> this.saveAs());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(disabledItem("Print Preview", KeyEvent.VK_V));
        fileMenu.add(disabledItem("Page Setup...", KeyEvent.VK_U));
        fileMenu.add(disabledItem("Print...", KeyEvent.VK_P));
        fileMenu.addSeparator();
        fileMenu.add(disabledItem("Send...", KeyEvent.VK_E));
        fileMenu.addSeparator();
        fileMenu.add(disabledItem("Set As Wallpaper (Tiled)", 0));
        fileMenu.add(disabledItem("Set As Wallpaper (Centered)", 0));
        fileMenu.addSeparator();

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());
        fileMenu.add(exitItem);
        return fileMenu;
    }

    private JMenu createEditMenu() {
        final JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(disabledItem("Undo", KeyEvent.VK_U));
        editMenu.add(disabledItem("Repeat", KeyEvent.VK_R));
        editMenu.addSeparator();
        editMenu.add(disabledItem("Cut", KeyEvent.VK_T));
        editMenu.add(disabledItem("Copy", KeyEvent.VK_C));
        editMenu.add(disabledItem("Paste", KeyEvent.VK_P));
        editMenu.add(disabledItem("Clear Selection", KeyEvent.VK_L));
        editMenu.add(disabledItem("Select All", KeyEvent.VK_A));
        editMenu.addSeparator();
        editMenu.add(disabledItem("Copy To...", 0));
        editMenu.add(disabledItem("Paste From...", 0));
        return editMenu;
    }

    private JMenu createViewMenu() {
        final JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        this.toolBoxMenuItem.setMnemonic(KeyEvent.VK_T);
        this.colorBoxMenuItem.setMnemonic(KeyEvent.VK_C);
        this.statusBarMenuItem.setMnemonic(KeyEvent.VK_S);
        viewMenu.add(this.toolBoxMenuItem);
        viewMenu.add(this.colorBoxMenuItem);
        viewMenu.add(this.statusBarMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(disabledItem("Zoom", KeyEvent.VK_Z));
        viewMenu.add(disabledItem("View Bitmap", KeyEvent.VK_V));
        viewMenu.addSeparator();
        viewMenu.add(disabledItem("Text Toolbar", 0));
        return viewMenu;
    }

    private JMenu createImageMenu() {
        final JMenu imageMenu = new JMenu("Image");
        imageMenu.setMnemonic(KeyEvent.VK_I);
        imageMenu.add(disabledItem("Flip/Rotate...", KeyEvent.VK_F));
        imageMenu.add(disabledItem("Stretch/Skew...", KeyEvent.VK_S));
        imageMenu.add(disabledItem("Invert Colors", KeyEvent.VK_I));
        imageMenu.add(disabledItem("Attributes...", KeyEvent.VK_A));

        final JMenuItem clearItem = new JMenuItem("Clear Image", KeyEvent.VK_C);
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        clearItem.addActionListener(e -> this.clearImage());
        imageMenu.add(clearItem);

        imageMenu.add(disabledItem("Draw Opaque", KeyEvent.VK_D));
        return imageMenu;
    }

    private JMenu createColorsMenu() {
        final JMenu colorsMenu = new JMenu("Colors");
        colorsMenu.setMnemonic(KeyEvent.VK_C);
        colorsMenu.add(disabledItem("Edit Colors...", KeyEvent.VK_E));
        return colorsMenu;
    }

    private JMenu createHelpMenu() {
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(disabledItem("Help Topics", KeyEvent.VK_H));
        helpMenu.addSeparator();

        final JMenuItem aboutItem = new JMenuItem("About Paint", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Paint\nWhistler — XP basics reimplementation",
                "About Paint",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);
        return helpMenu;
    }

    private static JMenuItem disabledItem(final String text, final int mnemonic) {
        final JMenuItem item = new JMenuItem(text);
        if (mnemonic != 0) {
            item.setMnemonic(mnemonic);
        }
        item.setEnabled(false);
        return item;
    }

    private void newDocument() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.paint.createNew();
        this.canvas.syncPreferredSize();
        this.canvas.revalidate();
        this.canvas.repaint();
        this.refreshTitle();
    }

    private void open() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        final JFileChooser chooser = imageFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            final Path path = Paths.get(chooser.getSelectedFile().toURI());
            this.paint.open(path);
            this.canvas.syncPreferredSize();
            this.canvas.revalidate();
            this.canvas.repaint();
            this.refreshTitle();
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not open image.\n" + e.getMessage(),
                    TITLE,
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void save() {
        if (Objects.isNull(this.paint.getPath())) {
            this.saveAs();
            return;
        }
        try {
            this.paint.save(this.paint.getPath());
            this.refreshTitle();
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save image.\n" + e.getMessage(),
                    TITLE,
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveAs() {
        final JFileChooser chooser = imageFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = Paths.get(chooser.getSelectedFile().toURI());
        if (!path.getFileName().toString().contains(".")) {
            path = path.resolveSibling(path.getFileName() + ".png");
        }
        try {
            this.paint.save(path);
            this.refreshTitle();
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save image.\n" + e.getMessage(),
                    TITLE,
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearImage() {
        this.paint.clearImage();
        this.canvas.repaint();
        this.refreshTitle();
    }

    private void exit() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.dispose();
    }

    private boolean confirmDiscardIfNeeded() {
        if (!this.paint.isEdited()) {
            return true;
        }
        final String name = Objects.isNull(this.paint.getPath())
                ? UNTITLED
                : this.paint.getPath().getFileName().toString();
        final int choice = JOptionPane.showConfirmDialog(
                this,
                "Save changes to " + name + "?",
                TITLE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            this.save();
            return !this.paint.isEdited();
        }
        return true;
    }

    private void refreshTitle() {
        final String name = Objects.isNull(this.paint.getPath())
                ? UNTITLED
                : this.paint.getPath().getFileName().toString();
        final String dirty = this.paint.isEdited() ? "*" : "";
        this.setTitle(dirty + name + " - " + TITLE);
    }

    private void applyConfig() {
        // Reserved for window size / last path / UI toggles (like Notepad).
        final String w = this.config.get("window.width");
        final String h = this.config.get("window.height");
        if (Objects.nonNull(w) && Objects.nonNull(h)) {
            try {
                this.setSize(Integer.parseInt(w), Integer.parseInt(h));
            } catch (final NumberFormatException ignored) {
                // keep default size
            }
        }
    }

    private static JFileChooser imageFileChooser() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg, *.bmp, *.gif)",
                "png", "jpg", "jpeg", "bmp", "gif"
        ));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JPEG (*.jpg;*.jpeg)", "jpg", "jpeg"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Bitmap (*.bmp)", "bmp"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("GIF (*.gif)", "gif"));
        return chooser;
    }
}
