package com.sysbot32.whistler.paint.ui;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.paint.image.ImagePrintSupport;
import com.sysbot32.whistler.paint.model.BrushShape;
import com.sysbot32.whistler.paint.model.ColorPalette;
import com.sysbot32.whistler.paint.model.FillStyle;
import com.sysbot32.whistler.paint.model.Paint;
import com.sysbot32.whistler.paint.model.PaintTool;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Classic XP Paint shell: menus, toolbox, color box, status, canvas.
 */
public class PaintFrame extends JFrame {
    private static final String UNTITLED = "untitled";
    private static final String TITLE = "Paint";

    private final Paint paint;
    private final Config config;
    private final PaintCanvas canvas;
    private final JLabel statusLabel = new JLabel(" For Help, click About Paint on the Help Menu.");
    private final JLabel coordsLabel = new JLabel("  0, 0  ");
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private final JCheckBoxMenuItem toolBoxMenuItem = new JCheckBoxMenuItem("Tool Box", true);
    private final JCheckBoxMenuItem colorBoxMenuItem = new JCheckBoxMenuItem("Color Box", true);
    private final JCheckBoxMenuItem statusBarMenuItem = new JCheckBoxMenuItem("Status Bar", true);
    private final JCheckBoxMenuItem textToolbarMenuItem = new JCheckBoxMenuItem("Text Toolbar", false);
    private final JCheckBoxMenuItem drawOpaqueMenuItem = new JCheckBoxMenuItem("Draw Opaque", true);
    private final JPanel toolBox = new JPanel(new BorderLayout());
    private final JPanel toolButtons = new JPanel(new GridLayout(0, 2, 2, 2));
    private final JPanel toolOptions = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
    private final JPanel colorBox = new JPanel(new BorderLayout(4, 2));
    private final JPanel fgBgPanel = new JPanel(null);
    private final JPanel fgSwatch = new JPanel();
    private final JPanel bgSwatch = new JPanel();
    private final Map<PaintTool, JToggleButton> toolButtonMap = new EnumMap<>(PaintTool.class);
    private final ButtonGroup toolGroup = new ButtonGroup();
    private final TextToolbar textToolbar;
    private PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage();

    private JMenuItem undoItem;
    private JMenuItem repeatItem;
    private JMenuItem cutItem;
    private JMenuItem copyItem;
    private JMenuItem pasteItem;
    private JMenuItem clearSelectionItem;

    public PaintFrame(final Paint paint, final Config config) {
        this.paint = paint;
        this.config = config;

        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        final JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        this.canvas = new PaintCanvas(this.paint);
        this.canvas.putClientProperty("documentListener", (Runnable) this::onDocumentChanged);
        this.canvas.putClientProperty("toolListener", (Runnable) this::syncToolButtons);
        this.canvas.setStatusListener((x, y) -> this.coordsLabel.setText("  " + x + ", " + y + "  "));
        final JScrollPane scrollPane = new JScrollPane(this.canvas);

        this.textToolbar = new TextToolbar(this, this.paint.getTextFont(), this.paint.isTextUnderline());
        this.textToolbar.setStyleListener((font, underline) -> {
            this.paint.setTextFont(font);
            this.paint.setTextUnderline(underline);
            this.canvas.applyLiveTextStyle();
            this.statusLabel.setText(" Font: " + font.getFamily() + " " + font.getSize()
                    + (underline ? " U" : ""));
        });

        this.buildToolBox();
        this.buildColorBox();

        this.statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.statusBar.add(this.statusLabel, BorderLayout.WEST);
        this.statusBar.add(this.coordsLabel, BorderLayout.EAST);

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
        this.textToolbarMenuItem.addActionListener(e -> this.updateTextToolbarVisibility());
        this.drawOpaqueMenuItem.addActionListener(e -> {
            this.paint.setDrawOpaque(this.drawOpaqueMenuItem.isSelected());
            this.canvas.applyLiveTextStyle();
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                PaintFrame.this.exit();
            }
        });

        this.applyConfig();
        this.installToolSizeShortcuts();
        this.syncToolButtons();
        this.syncColorSwatches();
        this.refreshToolOptions();
        this.refreshTitle();
        this.updateEditMenuState();
    }

    private void installToolSizeShortcuts() {
        // Classic Paint: Ctrl+NumPad+ / Ctrl+NumPad- grow or shrink the active tool size.
        final JRootPane root = this.getRootPane();
        final InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actionMap = root.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "toolSizeUp");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "toolSizeDown");
        actionMap.put("toolSizeUp", new AbstractAction() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                PaintFrame.this.nudgeToolSize(+1);
            }
        });
        actionMap.put("toolSizeDown", new AbstractAction() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                PaintFrame.this.nudgeToolSize(-1);
            }
        });
    }

    private void nudgeToolSize(final int delta) {
        final int size = this.paint.nudgeToolSize(delta);
        if (size < 0) {
            return;
        }
        this.refreshToolOptions();
        this.statusLabel.setText(" Size: " + size);
    }

    private void buildToolBox() {
        this.toolBox.setPreferredSize(new Dimension(56, 0));
        this.toolBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.toolButtons.setOpaque(false);
        for (final PaintTool tool : PaintTool.values()) {
            final JToggleButton button = new JToggleButton(ToolIcons.icon(tool));
            button.setToolTipText(tool.displayName());
            button.setMargin(new Insets(2, 2, 2, 2));
            button.setFocusable(false);
            button.setPreferredSize(new Dimension(24, 24));
            button.setMinimumSize(new Dimension(24, 24));
            button.setHorizontalAlignment(SwingConstants.CENTER);
            button.addActionListener(e -> {
                this.canvas.commitTextIfEditing();
                this.paint.setTool(tool);
                this.refreshToolOptions();
                this.updateEditMenuState();
                this.updateTextToolbarVisibility();
                this.statusLabel.setText(" " + tool.displayName());
            });
            this.toolGroup.add(button);
            this.toolButtonMap.put(tool, button);
            this.toolButtons.add(button);
        }
        this.toolOptions.setPreferredSize(new Dimension(52, 72));
        this.toolOptions.setBorder(BorderFactory.createEtchedBorder());
        this.toolBox.add(this.toolButtons, BorderLayout.NORTH);
        this.toolBox.add(this.toolOptions, BorderLayout.CENTER);
    }

    private void buildColorBox() {
        this.colorBox.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.fgBgPanel.setPreferredSize(new Dimension(40, 40));
        this.bgSwatch.setBounds(12, 12, 22, 22);
        this.bgSwatch.setBorder(new LineBorder(Color.GRAY));
        this.fgSwatch.setBounds(4, 4, 22, 22);
        this.fgSwatch.setBorder(new LineBorder(Color.DARK_GRAY));
        this.fgBgPanel.add(this.bgSwatch);
        this.fgBgPanel.add(this.fgSwatch);

        // Fixed square cells (GridLayout stretches in CENTER; keep a fixed preferred size).
        final int cell = 16;
        final int gap = 1;
        final int cols = 14;
        final int rows = 2;
        final JPanel swatches = new JPanel(new GridLayout(rows, cols, gap, gap));
        swatches.setPreferredSize(new Dimension(
                cols * cell + (cols - 1) * gap,
                rows * cell + (rows - 1) * gap
        ));
        swatches.setMaximumSize(swatches.getPreferredSize());
        for (final Color color : ColorPalette.DEFAULT_COLORS) {
            final JPanel swatch = new JPanel();
            swatch.setBackground(color);
            swatch.setOpaque(true);
            swatch.setBorder(new LineBorder(Color.GRAY));
            swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            swatch.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(final java.awt.event.MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        PaintFrame.this.paint.setBackground(color);
                    } else {
                        PaintFrame.this.paint.setForeground(color);
                    }
                    PaintFrame.this.syncColorSwatches();
                    PaintFrame.this.canvas.applyLiveTextStyle();
                }
            });
            swatches.add(swatch);
        }
        final JPanel swatchesHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        swatchesHolder.setOpaque(false);
        swatchesHolder.add(swatches);
        this.colorBox.add(this.fgBgPanel, BorderLayout.WEST);
        this.colorBox.add(swatchesHolder, BorderLayout.CENTER);
    }

    private void refreshToolOptions() {
        this.toolOptions.removeAll();
        final PaintTool tool = this.paint.getTool();
        switch (tool) {
            case ERASER -> addSizeOptions(Paint.ERASER_SIZES, this.paint.getEraserSize(), this.paint::setEraserSize);
            case BRUSH -> {
                addSizeOptions(Paint.BRUSH_SIZES, this.paint.getBrushSize(), this.paint::setBrushSize);
                for (final BrushShape shape : BrushShape.values()) {
                    final JButton b = new JButton(shape.name().substring(0, 1));
                    b.setMargin(new Insets(0, 2, 0, 2));
                    b.setToolTipText(shape.name());
                    b.addActionListener(e -> this.paint.setBrushShape(shape));
                    this.toolOptions.add(b);
                }
            }
            case AIRBRUSH ->
                    addSizeOptions(Paint.AIRBRUSH_RADII, this.paint.getAirbrushRadius(), this.paint::setAirbrushRadius);
            case LINE, CURVE ->
                    addSizeOptions(Paint.LINE_WIDTHS, this.paint.getLineWidth(), this.paint::setLineWidth);
            case RECTANGLE, POLYGON, ELLIPSE, ROUNDED_RECTANGLE -> {
                addSizeOptions(Paint.LINE_WIDTHS, this.paint.getLineWidth(), this.paint::setLineWidth);
                for (final FillStyle style : FillStyle.values()) {
                    final JButton b = new JButton(switch (style) {
                        case OUTLINE -> "□";
                        case OUTLINE_FILL -> "▣";
                        case FILL -> "■";
                    });
                    b.setMargin(new Insets(0, 2, 0, 2));
                    b.setToolTipText(style.name());
                    b.addActionListener(e -> this.paint.setFillStyle(style));
                    this.toolOptions.add(b);
                }
            }
            case MAGNIFIER -> {
                for (final int z : Paint.ZOOM_LEVELS) {
                    final JButton b = new JButton(z + "x");
                    b.setMargin(new Insets(0, 2, 0, 2));
                    b.addActionListener(e -> {
                        this.paint.setZoom(z);
                        this.canvas.syncPreferredSize();
                        this.canvas.revalidate();
                        this.canvas.repaint();
                    });
                    this.toolOptions.add(b);
                }
            }
            default -> this.toolOptions.add(new JLabel(" "));
        }
        this.toolOptions.revalidate();
        this.toolOptions.repaint();
    }

    private void addSizeOptions(final int[] sizes, final int current, final java.util.function.IntConsumer setter) {
        for (final int size : sizes) {
            final JButton b = new JButton(String.valueOf(size));
            b.setMargin(new Insets(0, 2, 0, 2));
            if (size == current) {
                b.setEnabled(false);
            }
            b.addActionListener(e -> {
                setter.accept(size);
                this.refreshToolOptions();
            });
            this.toolOptions.add(b);
        }
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

        final JMenuItem printPreviewItem = new JMenuItem("Print Preview", KeyEvent.VK_V);
        printPreviewItem.addActionListener(e -> this.printPreview());

        final JMenuItem pageSetupItem = new JMenuItem("Page Setup...", KeyEvent.VK_U);
        pageSetupItem.addActionListener(e -> this.pageSetup());

        final JMenuItem printItem = new JMenuItem("Print...", KeyEvent.VK_P);
        printItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        printItem.addActionListener(e -> this.printDocument());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(printPreviewItem);
        fileMenu.add(pageSetupItem);
        fileMenu.add(printItem);
        fileMenu.addSeparator();

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());
        fileMenu.add(exitItem);
        return fileMenu;
    }

    private JMenu createEditMenu() {
        final JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        this.undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        this.undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        this.undoItem.addActionListener(e -> {
            this.paint.undo();
            this.refreshCanvas();
        });

        this.repeatItem = new JMenuItem("Repeat", KeyEvent.VK_R);
        this.repeatItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        this.repeatItem.addActionListener(e -> {
            this.paint.repeat();
            this.refreshCanvas();
        });

        this.cutItem = new JMenuItem("Cut", KeyEvent.VK_T);
        this.cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        this.cutItem.addActionListener(e -> {
            this.paint.cutSelection();
            this.refreshCanvas();
        });

        this.copyItem = new JMenuItem("Copy", KeyEvent.VK_C);
        this.copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        this.copyItem.addActionListener(e -> {
            this.paint.copySelection();
            this.updateEditMenuState();
        });

        this.pasteItem = new JMenuItem("Paste", KeyEvent.VK_P);
        this.pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        this.pasteItem.addActionListener(e -> {
            this.paint.pasteClipboard();
            this.paint.setTool(PaintTool.SELECT);
            this.syncToolButtons();
            this.refreshCanvas();
        });

        this.clearSelectionItem = new JMenuItem("Clear Selection", KeyEvent.VK_L);
        this.clearSelectionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        this.clearSelectionItem.addActionListener(e -> {
            this.paint.clearSelection();
            this.refreshCanvas();
        });

        final JMenuItem selectAllItem = new JMenuItem("Select All", KeyEvent.VK_A);
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAllItem.addActionListener(e -> {
            this.paint.selectAll();
            this.paint.setTool(PaintTool.SELECT);
            this.syncToolButtons();
            this.refreshCanvas();
        });

        final JMenuItem copyToItem = new JMenuItem("Copy To...");
        copyToItem.addActionListener(e -> this.copyTo());

        final JMenuItem pasteFromItem = new JMenuItem("Paste From...");
        pasteFromItem.addActionListener(e -> this.pasteFrom());

        editMenu.add(this.undoItem);
        editMenu.add(this.repeatItem);
        editMenu.addSeparator();
        editMenu.add(this.cutItem);
        editMenu.add(this.copyItem);
        editMenu.add(this.pasteItem);
        editMenu.add(this.clearSelectionItem);
        editMenu.add(selectAllItem);
        editMenu.addSeparator();
        editMenu.add(copyToItem);
        editMenu.add(pasteFromItem);
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

        final JMenu zoomMenu = new JMenu("Zoom");
        zoomMenu.setMnemonic(KeyEvent.VK_Z);
        for (final int z : Paint.ZOOM_LEVELS) {
            final JMenuItem item = new JMenuItem(z == 1 ? "Normal Size" : (z + "x"));
            item.addActionListener(e -> {
                this.paint.setZoom(z);
                this.canvas.syncPreferredSize();
                this.canvas.revalidate();
                this.canvas.repaint();
            });
            zoomMenu.add(item);
        }
        viewMenu.add(zoomMenu);

        final JMenuItem viewBitmap = new JMenuItem("View Bitmap", KeyEvent.VK_V);
        viewBitmap.addActionListener(e -> this.viewBitmap());
        viewMenu.add(viewBitmap);
        viewMenu.addSeparator();
        this.textToolbarMenuItem.setMnemonic(KeyEvent.VK_E);
        viewMenu.add(this.textToolbarMenuItem);
        return viewMenu;
    }

    private JMenu createImageMenu() {
        final JMenu imageMenu = new JMenu("Image");
        imageMenu.setMnemonic(KeyEvent.VK_I);

        final JMenuItem flipRotate = new JMenuItem("Flip/Rotate...", KeyEvent.VK_F);
        flipRotate.addActionListener(e -> this.flipRotate());

        final JMenuItem stretchSkew = new JMenuItem("Stretch/Skew...", KeyEvent.VK_S);
        stretchSkew.addActionListener(e -> this.stretchSkew());

        final JMenuItem invert = new JMenuItem("Invert Colors", KeyEvent.VK_I);
        invert.addActionListener(e -> {
            this.paint.invertColors();
            this.refreshCanvas();
        });

        final JMenuItem attributes = new JMenuItem("Attributes...", KeyEvent.VK_A);
        attributes.addActionListener(e -> this.attributes());

        final JMenuItem clearItem = new JMenuItem("Clear Image", KeyEvent.VK_C);
        clearItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        clearItem.addActionListener(e -> this.clearImage());

        this.drawOpaqueMenuItem.setMnemonic(KeyEvent.VK_D);

        imageMenu.add(flipRotate);
        imageMenu.add(stretchSkew);
        imageMenu.add(invert);
        imageMenu.add(attributes);
        imageMenu.add(clearItem);
        imageMenu.add(this.drawOpaqueMenuItem);
        return imageMenu;
    }

    private JMenu createColorsMenu() {
        final JMenu colorsMenu = new JMenu("Colors");
        colorsMenu.setMnemonic(KeyEvent.VK_C);
        final JMenuItem editColors = new JMenuItem("Edit Colors...", KeyEvent.VK_E);
        editColors.addActionListener(e -> {
            final Color chosen = JColorChooser.showDialog(this, "Edit Colors", this.paint.getForeground());
            if (Objects.nonNull(chosen)) {
                this.paint.setForeground(chosen);
                this.syncColorSwatches();
                this.canvas.applyLiveTextStyle();
            }
        });
        colorsMenu.add(editColors);
        return colorsMenu;
    }

    private JMenu createHelpMenu() {
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

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

    private void printPreview() {
        this.canvas.commitTextIfEditing();
        this.paint.commitSelectionIfAny();
        final PrintPreviewDialog dialog = new PrintPreviewDialog(
                this,
                this.paint.getImage(),
                this.pageFormat
        );
        dialog.setVisible(true);
    }

    private void pageSetup() {
        final PrinterJob job = PrinterJob.getPrinterJob();
        this.pageFormat = job.pageDialog(this.pageFormat);
    }

    private void printDocument() {
        this.canvas.commitTextIfEditing();
        this.paint.commitSelectionIfAny();
        final PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(Objects.isNull(this.paint.getPath())
                ? UNTITLED
                : this.paint.getPath().getFileName().toString());
        job.setPrintable(new ImagePrintSupport(this.paint.getImage()), this.pageFormat);
        if (!job.printDialog()) {
            return;
        }
        try {
            job.print();
        } catch (final PrinterException ex) {
            showError("Could not print.\n" + ex.getMessage());
        }
    }

    private void updateTextToolbarVisibility() {
        final boolean show = this.textToolbarMenuItem.isSelected()
                || this.paint.getTool() == PaintTool.TEXT;
        if (show) {
            this.textToolbar.syncFrom(this.paint.getTextFont(), this.paint.isTextUnderline());
            if (!this.textToolbar.isVisible()) {
                this.textToolbar.setLocationRelativeTo(this);
            }
            this.textToolbar.setVisible(true);
            // Keep checkbox in sync when auto-shown for the Text tool.
            if (this.paint.getTool() == PaintTool.TEXT && !this.textToolbarMenuItem.isSelected()) {
                this.textToolbarMenuItem.setSelected(true);
            }
        } else {
            this.textToolbar.setVisible(false);
        }
    }

    private void newDocument() {
        this.canvas.commitTextIfEditing();
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.paint.createNew();
        this.refreshCanvas();
    }

    private void open() {
        this.canvas.commitTextIfEditing();
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
            this.refreshCanvas();
        } catch (final IOException e) {
            showError("Could not open image.\n" + e.getMessage());
        }
    }

    private void save() {
        this.canvas.commitTextIfEditing();
        if (Objects.isNull(this.paint.getPath())) {
            this.saveAs();
            return;
        }
        try {
            this.paint.save(this.paint.getPath());
            this.refreshTitle();
        } catch (final IOException e) {
            showError("Could not save image.\n" + e.getMessage());
        }
    }

    private void saveAs() {
        this.canvas.commitTextIfEditing();
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
            showError("Could not save image.\n" + e.getMessage());
        }
    }

    private void copyTo() {
        if (!this.paint.getSelection().isActive()) {
            return;
        }
        final JFileChooser chooser = imageFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = Paths.get(chooser.getSelectedFile().toURI());
        if (!path.getFileName().toString().contains(".")) {
            path = path.resolveSibling(path.getFileName() + ".png");
        }
        try {
            this.paint.copySelectionTo(path);
        } catch (final IOException e) {
            showError("Could not copy selection.\n" + e.getMessage());
        }
    }

    private void pasteFrom() {
        final JFileChooser chooser = imageFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            this.paint.pasteFrom(Paths.get(chooser.getSelectedFile().toURI()));
            this.paint.setTool(PaintTool.SELECT);
            this.syncToolButtons();
            this.refreshCanvas();
        } catch (final IOException e) {
            showError("Could not paste from file.\n" + e.getMessage());
        }
    }

    private void clearImage() {
        this.paint.clearImage();
        this.refreshCanvas();
    }

    private void flipRotate() {
        final String[] options = {
                "Flip horizontal", "Flip vertical",
                "Rotate 90°", "Rotate 180°", "Rotate 270°"
        };
        final String choice = (String) JOptionPane.showInputDialog(
                this, "Flip or rotate:", "Flip/Rotate",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]
        );
        if (Objects.isNull(choice)) {
            return;
        }
        switch (choice) {
            case "Flip horizontal" -> this.paint.flipHorizontal();
            case "Flip vertical" -> this.paint.flipVertical();
            case "Rotate 90°" -> this.paint.rotate90();
            case "Rotate 180°" -> this.paint.rotate180();
            case "Rotate 270°" -> this.paint.rotate270();
            default -> {
            }
        }
        this.refreshCanvas();
    }

    private void stretchSkew() {
        final JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        final JTextField hStretch = new JTextField("100");
        final JTextField vStretch = new JTextField("100");
        final JTextField hSkew = new JTextField("0");
        final JTextField vSkew = new JTextField("0");
        panel.add(new JLabel("Stretch horizontal %"));
        panel.add(hStretch);
        panel.add(new JLabel("Stretch vertical %"));
        panel.add(vStretch);
        panel.add(new JLabel("Skew horizontal °"));
        panel.add(hSkew);
        panel.add(new JLabel("Skew vertical °"));
        panel.add(vSkew);
        final int result = JOptionPane.showConfirmDialog(
                this, panel, "Stretch/Skew", JOptionPane.OK_CANCEL_OPTION
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            final int hs = Integer.parseInt(hStretch.getText().trim());
            final int vs = Integer.parseInt(vStretch.getText().trim());
            final int hk = Integer.parseInt(hSkew.getText().trim());
            final int vk = Integer.parseInt(vSkew.getText().trim());
            if (hs != 100 || vs != 100) {
                this.paint.stretch(hs, vs);
            }
            if (hk != 0 || vk != 0) {
                this.paint.skew(hk, vk);
            }
            this.refreshCanvas();
        } catch (final NumberFormatException ex) {
            showError("Enter whole numbers for stretch/skew.");
        }
    }

    private void attributes() {
        final JPanel panel = new JPanel(new GridLayout(0, 2, 4, 4));
        final JTextField widthField = new JTextField(String.valueOf(this.paint.getWidth()));
        final JTextField heightField = new JTextField(String.valueOf(this.paint.getHeight()));
        panel.add(new JLabel("Width (pixels)"));
        panel.add(widthField);
        panel.add(new JLabel("Height (pixels)"));
        panel.add(heightField);
        final int result = JOptionPane.showConfirmDialog(
                this, panel, "Attributes", JOptionPane.OK_CANCEL_OPTION
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            final int w = Integer.parseInt(widthField.getText().trim());
            final int h = Integer.parseInt(heightField.getText().trim());
            this.paint.attributes(w, h);
            this.refreshCanvas();
        } catch (final IllegalArgumentException ex) {
            showError("Width and height must be positive integers.");
        }
    }

    private void viewBitmap() {
        final JDialog dialog = new JDialog(this, "View Bitmap", true);
        final JLabel label = new JLabel(new ImageIcon(this.paint.getImage()));
        dialog.add(new JScrollPane(label));
        dialog.setSize(Math.min(800, this.paint.getWidth() + 40), Math.min(600, this.paint.getHeight() + 60));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exit() {
        this.canvas.commitTextIfEditing();
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.textToolbar.dispose();
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

    private void onDocumentChanged() {
        this.refreshTitle();
        this.updateEditMenuState();
        this.syncColorSwatches();
    }

    private void refreshCanvas() {
        this.canvas.notifyDocumentChanged();
        this.refreshTitle();
        this.updateEditMenuState();
    }

    private void syncToolButtons() {
        final JToggleButton button = this.toolButtonMap.get(this.paint.getTool());
        if (Objects.nonNull(button)) {
            button.setSelected(true);
        }
        this.refreshToolOptions();
        this.updateTextToolbarVisibility();
    }

    private void syncColorSwatches() {
        this.fgSwatch.setBackground(this.paint.getForeground());
        this.bgSwatch.setBackground(this.paint.getBackground());
        this.fgSwatch.repaint();
        this.bgSwatch.repaint();
    }

    private void updateEditMenuState() {
        if (Objects.isNull(this.undoItem)) {
            return;
        }
        this.undoItem.setEnabled(this.paint.canUndo());
        if (Objects.nonNull(this.repeatItem)) {
            this.repeatItem.setEnabled(this.paint.canRepeat());
        }
        final boolean hasSel = this.paint.getSelection().isActive();
        this.cutItem.setEnabled(hasSel);
        this.copyItem.setEnabled(hasSel);
        this.clearSelectionItem.setEnabled(hasSel);
        this.pasteItem.setEnabled(this.paint.canPaste());
    }

    private void refreshTitle() {
        final String name = Objects.isNull(this.paint.getPath())
                ? UNTITLED
                : this.paint.getPath().getFileName().toString();
        final String dirty = this.paint.isEdited() ? "*" : "";
        this.setTitle(dirty + name + " - " + TITLE);
    }

    private void applyConfig() {
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

    private void showError(final String message) {
        JOptionPane.showMessageDialog(this, message, TITLE, JOptionPane.ERROR_MESSAGE);
    }

    private static JFileChooser imageFileChooser() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "Image Files (*.png, *.jpg, *.bmp, *.gif, *.tif)",
                "png", "jpg", "jpeg", "bmp", "gif", "tif", "tiff"
        ));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JPEG (*.jpg;*.jpeg)", "jpg", "jpeg"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Bitmap (*.bmp)", "bmp"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("GIF (*.gif)", "gif"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("TIFF (*.tif;*.tiff)", "tif", "tiff"));
        return chooser;
    }
}
