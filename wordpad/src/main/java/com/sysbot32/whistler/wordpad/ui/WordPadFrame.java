package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.wordpad.model.DocumentKind;
import com.sysbot32.whistler.wordpad.model.MeasurementUnit;
import com.sysbot32.whistler.wordpad.model.ParagraphAlignment;
import com.sysbot32.whistler.wordpad.model.WordPad;
import com.sysbot32.whistler.wordpad.model.WordWrapMode;
import say.swing.JFontChooser;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
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
import java.time.LocalDateTime;
import java.util.Objects;

public class WordPadFrame extends JFrame {
    static final String UNTITLED = "Document";
    static final String TITLE = "WordPad";
    private static final Integer[] FONT_SIZES = {
            8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72
    };

    private final WordPad wordPad;
    private final Config config;
    private final JTextPane editor;
    private final UndoManager undoManager = new UndoManager();
    private final JComboBox<String> fontFamilyCombo;
    private final JComboBox<Integer> fontSizeCombo;
    private final JToggleButton boldButton;
    private final JToggleButton italicButton;
    private final JToggleButton underlineButton;
    private final JButton textColorButton;
    private final JToggleButton alignLeftButton;
    private final JToggleButton alignCenterButton;
    private final JToggleButton alignRightButton;
    private final JToggleButton bulletButton;
    private final JToolBar standardToolbar;
    private final JToolBar formatBar;
    private final RulerBar ruler;
    private final JPanel statusBar;
    private final JCheckBoxMenuItem toolbarMenuItem = new JCheckBoxMenuItem("Toolbar", true);
    private final JCheckBoxMenuItem formatBarMenuItem = new JCheckBoxMenuItem("Format Bar", true);
    private final JCheckBoxMenuItem rulerMenuItem = new JCheckBoxMenuItem("Ruler", true);
    private final JCheckBoxMenuItem statusBarMenuItem = new JCheckBoxMenuItem("Status Bar", true);
    private boolean syncingToolbar = false;
    private WordWrapMode wrapMode = WordWrapMode.WINDOW;
    private MeasurementUnit unit = MeasurementUnit.INCHES;
    private PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage();
    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private String lastFind = "";
    private boolean lastMatchCase;
    private boolean lastWholeWord;

    public WordPadFrame(final WordPad wordPad, final Config config) {
        this.wordPad = wordPad;
        this.config = config;

        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.editor = new JTextPane(wordPad.getDocument()) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                if (WordPadFrame.this.wrapMode == WordWrapMode.WINDOW) {
                    return true;
                }
                if (WordPadFrame.this.wrapMode == WordWrapMode.NONE) {
                    return false;
                }
                final Container parent = this.getParent();
                if (parent instanceof javax.swing.JViewport viewport) {
                    return viewport.getWidth() <= RulerBar.PAGE_WIDTH_POINTS;
                }
                return true;
            }
        };
        this.editor.setName("editor");
        this.editor.getDocument().addUndoableEditListener(this.undoManager);

        this.fontFamilyCombo = new JComboBox<>(availableFontFamilies());
        this.fontFamilyCombo.setName("fontFamily");
        this.fontFamilyCombo.setSelectedItem(WordPad.DEFAULT_FONT_FAMILY);
        this.fontFamilyCombo.setMaximumRowCount(16);
        this.fontFamilyCombo.setToolTipText("Font");

        this.fontSizeCombo = new JComboBox<>(FONT_SIZES);
        this.fontSizeCombo.setName("fontSize");
        this.fontSizeCombo.setEditable(true);
        this.fontSizeCombo.setSelectedItem(WordPad.DEFAULT_FONT_SIZE);
        this.fontSizeCombo.setToolTipText("Font Size");

        this.boldButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.BOLD), "bold", "Bold");
        this.italicButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.ITALIC), "italic", "Italic");
        this.underlineButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.UNDERLINE), "underline", "Underline");
        this.textColorButton = new JButton(ToolbarIcons.icon(ToolbarIcons.Glyph.COLOR));
        this.textColorButton.setName("textColor");
        this.textColorButton.setToolTipText("Text Color");
        this.textColorButton.setFocusable(false);
        this.textColorButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        this.alignLeftButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.ALIGN_LEFT), "alignLeft", "Align Left");
        this.alignCenterButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.ALIGN_CENTER), "alignCenter", "Align Center");
        this.alignRightButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.ALIGN_RIGHT), "alignRight", "Align Right");
        this.bulletButton = toggle(ToolbarIcons.icon(ToolbarIcons.Glyph.BULLETS), "bullet", "Bullets");
        final ButtonGroup alignGroup = new ButtonGroup();
        alignGroup.add(this.alignLeftButton);
        alignGroup.add(this.alignCenterButton);
        alignGroup.add(this.alignRightButton);
        this.alignLeftButton.setSelected(true);

        this.fontFamilyCombo.addActionListener(e -> this.onFontFamilyChosen());
        this.fontSizeCombo.addActionListener(e -> this.onFontSizeChosen());
        this.boldButton.addActionListener(e -> this.onBoldToggled());
        this.italicButton.addActionListener(e -> this.onItalicToggled());
        this.underlineButton.addActionListener(e -> this.onUnderlineToggled());
        this.textColorButton.addActionListener(e -> this.onTextColorChosen());
        this.alignLeftButton.addActionListener(e -> this.onAlignmentChosen(ParagraphAlignment.LEFT));
        this.alignCenterButton.addActionListener(e -> this.onAlignmentChosen(ParagraphAlignment.CENTER));
        this.alignRightButton.addActionListener(e -> this.onAlignmentChosen(ParagraphAlignment.RIGHT));
        this.bulletButton.addActionListener(e -> this.onBulletToggled());

        this.standardToolbar = this.createStandardToolbar();
        this.formatBar = this.createFormatBar();
        this.ruler = new RulerBar(this.wordPad, this.editor);
        this.ruler.setOnChange(offset -> this.refreshTitle());
        this.editor.addCaretListener(e -> {
            this.syncToolbarFromCaret();
            this.ruler.repaint();
        });
        this.statusBar = new JPanel(new BorderLayout());
        this.statusBar.setName("statusBar");
        this.statusBar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        this.statusBar.add(new JLabel("For Help, press F1"), BorderLayout.WEST);

        final JPanel formatAndRuler = new JPanel(new BorderLayout());
        formatAndRuler.add(this.formatBar, BorderLayout.NORTH);
        formatAndRuler.add(this.ruler, BorderLayout.SOUTH);
        final JPanel north = new JPanel(new BorderLayout());
        north.add(this.standardToolbar, BorderLayout.NORTH);
        north.add(formatAndRuler, BorderLayout.SOUTH);

        final JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(north, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(this.editor), BorderLayout.CENTER);
        contentPane.add(this.statusBar, BorderLayout.SOUTH);

        this.toolbarMenuItem.addActionListener(e -> this.standardToolbar.setVisible(this.toolbarMenuItem.isSelected()));
        this.formatBarMenuItem.addActionListener(e -> this.formatBar.setVisible(this.formatBarMenuItem.isSelected()));
        this.rulerMenuItem.addActionListener(e -> this.ruler.setVisible(this.rulerMenuItem.isSelected()));
        this.statusBarMenuItem.addActionListener(e -> this.statusBar.setVisible(this.statusBarMenuItem.isSelected()));

        this.setJMenuBar(this.createMenuBar());
        this.applyConfig();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                WordPadFrame.this.exit();
            }
        });
        this.refreshTitle();
        this.syncToolbarFromCaret();
    }

    public JTextPane getEditor() {
        return this.editor;
    }

    public WordPad getWordPad() {
        return this.wordPad;
    }

    void newDocument() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        final DocumentKind kind = new NewDocumentDialog(this, this.wordPad.getKind()).showDialog();
        if (Objects.isNull(kind)) {
            return;
        }
        this.applyNew(kind);
    }

    void newDocument(final DocumentKind kind) {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.applyNew(kind);
    }

    private void applyNew(final DocumentKind kind) {
        this.wordPad.createNew(kind);
        this.editor.setDocument(this.wordPad.getDocument());
        this.undoManager.discardAllEdits();
        this.refreshTitle();
        this.syncToolbarFromCaret();
    }

    void openDocument() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        final JFileChooser fileChooser = new DocumentFileChooser();
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            this.openPath(Paths.get(fileChooser.getSelectedFile().toURI()));
        } catch (final IOException ex) {
            this.showError("Cannot open file.", ex);
        }
    }

    void openPath(final Path path) throws IOException {
        this.wordPad.open(path);
        this.editor.setDocument(this.wordPad.getDocument());
        this.undoManager.discardAllEdits();
        this.refreshTitle();
        this.syncToolbarFromCaret();
    }

    boolean saveDocument(final boolean saveAs) {
        Path path = this.wordPad.getPath();
        DocumentKind kind = this.wordPad.getKind();
        if (saveAs || Objects.isNull(path)) {
            final DocumentFileChooser fileChooser = new DocumentFileChooser();
            this.applyDefaultFilter(fileChooser, kind);
            if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            kind = kindFromFilter(fileChooser.getFileFilter());
            path = ensureExtension(Paths.get(fileChooser.getSelectedFile().toURI()), kind);
        }
        if (kind.isPlainText() && this.wordPad.hasCharacterFormatting()) {
            final int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Saving as text will remove all formatting. Continue?",
                    TITLE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        try {
            this.wordPad.save(path, kind);
            this.refreshTitle();
            return true;
        } catch (final IOException ex) {
            this.showError("Cannot save file.", ex);
            return false;
        }
    }

    boolean savePath(final Path path) throws IOException {
        this.wordPad.save(path);
        this.refreshTitle();
        return true;
    }

    private JToolBar createStandardToolbar() {
        final JToolBar toolBar = new JToolBar("Toolbar");
        toolBar.setName("standardToolbar");
        toolBar.setFloatable(false);
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.NEW, "toolNew", "New", this::newDocument));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.OPEN, "toolOpen", "Open", this::openDocument));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.SAVE, "toolSave", "Save", () -> this.saveDocument(false)));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.PRINT, "toolPrint", "Print", this::printDocument));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.FIND, "toolFind", "Find", this::showFindDialog));
        toolBar.addSeparator();
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.CUT, "toolCut", "Cut", this.editor::cut));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.COPY, "toolCopy", "Copy", this.editor::copy));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.PASTE, "toolPaste", "Paste", this.editor::paste));
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.UNDO, "toolUndo", "Undo", this::undo));
        toolBar.addSeparator();
        toolBar.add(this.toolButton(ToolbarIcons.Glyph.DATE, "toolDate", "Date and Time", this::insertDateTime));
        return toolBar;
    }

    private JToolBar createFormatBar() {
        final JToolBar toolBar = new JToolBar("Format Bar");
        toolBar.setName("formatBar");
        toolBar.setFloatable(false);
        this.fontFamilyCombo.setPreferredSize(new Dimension(180, 24));
        this.fontSizeCombo.setPreferredSize(new Dimension(64, 24));
        toolBar.add(this.fontFamilyCombo);
        toolBar.add(this.fontSizeCombo);
        toolBar.addSeparator();
        toolBar.add(this.boldButton);
        toolBar.add(this.italicButton);
        toolBar.add(this.underlineButton);
        toolBar.add(this.textColorButton);
        toolBar.addSeparator();
        toolBar.add(this.alignLeftButton);
        toolBar.add(this.alignCenterButton);
        toolBar.add(this.alignRightButton);
        toolBar.add(this.bulletButton);
        return toolBar;
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(this.createFileMenu());
        menuBar.add(this.createEditMenu());
        menuBar.add(this.createViewMenu());
        menuBar.add(this.createInsertMenu());
        menuBar.add(this.createFormatMenu());
        menuBar.add(this.createHelpMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        final JMenuItem newMenuItem = menuItem("New...", KeyEvent.VK_N, KeyEvent.VK_N);
        final JMenuItem openMenuItem = menuItem("Open...", KeyEvent.VK_O, KeyEvent.VK_O);
        final JMenuItem saveMenuItem = menuItem("Save", KeyEvent.VK_S, KeyEvent.VK_S);
        final JMenuItem saveAsMenuItem = menuItem("Save As...", KeyEvent.VK_A, -1);
        final JMenuItem printMenuItem = menuItem("Print...", KeyEvent.VK_P, KeyEvent.VK_P);
        final JMenuItem previewMenuItem = new JMenuItem("Print Preview");
        final JMenuItem pageSetupMenuItem = new JMenuItem("Page Setup...");
        final JMenuItem exitMenuItem = menuItem("Exit", KeyEvent.VK_X, -1);
        newMenuItem.addActionListener(e -> this.newDocument());
        openMenuItem.addActionListener(e -> this.openDocument());
        saveMenuItem.addActionListener(e -> this.saveDocument(false));
        saveAsMenuItem.addActionListener(e -> this.saveDocument(true));
        printMenuItem.addActionListener(e -> this.printDocument());
        previewMenuItem.addActionListener(e -> this.printPreview());
        pageSetupMenuItem.addActionListener(e -> this.pageSetup());
        exitMenuItem.addActionListener(e -> this.exit());
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(printMenuItem);
        fileMenu.add(previewMenuItem);
        fileMenu.add(pageSetupMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private JMenu createEditMenu() {
        final JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        final JMenuItem undoMenuItem = menuItem("Undo", KeyEvent.VK_U, KeyEvent.VK_Z);
        final JMenuItem cutMenuItem = menuItem("Cut", KeyEvent.VK_T, KeyEvent.VK_X);
        final JMenuItem copyMenuItem = menuItem("Copy", KeyEvent.VK_C, KeyEvent.VK_C);
        final JMenuItem pasteMenuItem = menuItem("Paste", KeyEvent.VK_P, KeyEvent.VK_V);
        final JMenuItem clearMenuItem = new JMenuItem("Clear");
        final JMenuItem selectAllMenuItem = menuItem("Select All", KeyEvent.VK_A, KeyEvent.VK_A);
        final JMenuItem findMenuItem = menuItem("Find...", KeyEvent.VK_F, KeyEvent.VK_F);
        final JMenuItem findNextMenuItem = new JMenuItem("Find Next");
        findNextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        final JMenuItem replaceMenuItem = menuItem("Replace...", KeyEvent.VK_R, KeyEvent.VK_H);
        undoMenuItem.addActionListener(e -> this.undo());
        cutMenuItem.addActionListener(e -> this.editor.cut());
        copyMenuItem.addActionListener(e -> this.editor.copy());
        pasteMenuItem.addActionListener(e -> this.editor.paste());
        clearMenuItem.addActionListener(e -> this.clearSelection());
        selectAllMenuItem.addActionListener(e -> this.editor.selectAll());
        findMenuItem.addActionListener(e -> this.showFindDialog());
        findNextMenuItem.addActionListener(e -> this.findNext());
        replaceMenuItem.addActionListener(e -> this.showReplaceDialog());
        editMenu.add(undoMenuItem);
        editMenu.addSeparator();
        editMenu.add(cutMenuItem);
        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);
        editMenu.add(clearMenuItem);
        editMenu.addSeparator();
        editMenu.add(selectAllMenuItem);
        editMenu.addSeparator();
        editMenu.add(findMenuItem);
        editMenu.add(findNextMenuItem);
        editMenu.add(replaceMenuItem);
        return editMenu;
    }

    private JMenu createViewMenu() {
        final JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        final JMenuItem optionsMenuItem = new JMenuItem("Options...");
        optionsMenuItem.addActionListener(e -> this.showOptions());
        viewMenu.add(this.toolbarMenuItem);
        viewMenu.add(this.formatBarMenuItem);
        viewMenu.add(this.rulerMenuItem);
        viewMenu.add(this.statusBarMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(optionsMenuItem);
        return viewMenu;
    }

    private JMenu createInsertMenu() {
        final JMenu insertMenu = new JMenu("Insert");
        insertMenu.setMnemonic(KeyEvent.VK_I);
        final JMenuItem dateMenuItem = new JMenuItem("Date and Time...");
        dateMenuItem.addActionListener(e -> this.insertDateTime());
        insertMenu.add(dateMenuItem);
        return insertMenu;
    }

    private JMenu createFormatMenu() {
        final JMenu formatMenu = new JMenu("Format");
        formatMenu.setMnemonic(KeyEvent.VK_O);
        final JMenuItem fontMenuItem = new JMenuItem("Font...");
        final JMenuItem bulletMenuItem = new JMenuItem("Bullet Style");
        final JMenuItem paragraphMenuItem = new JMenuItem("Paragraph...");
        final JMenuItem tabsMenuItem = new JMenuItem("Tabs...");
        fontMenuItem.addActionListener(e -> this.showFontDialog());
        bulletMenuItem.addActionListener(e -> {
            this.bulletButton.setSelected(!this.bulletButton.isSelected());
            this.onBulletToggled();
        });
        paragraphMenuItem.addActionListener(e -> this.showParagraphDialog());
        tabsMenuItem.addActionListener(e -> this.showTabsDialog());
        formatMenu.add(fontMenuItem);
        formatMenu.add(bulletMenuItem);
        formatMenu.add(paragraphMenuItem);
        formatMenu.add(tabsMenuItem);
        return formatMenu;
    }

    private JMenu createHelpMenu() {
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutMenuItem = new JMenuItem("About WordPad");
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Whistler WordPad\nA classic Windows XP–style WordPad recreation.",
                "About WordPad",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutMenuItem);
        return helpMenu;
    }

    private void onFontFamilyChosen() {
        if (this.syncingToolbar) {
            return;
        }
        final Object selected = this.fontFamilyCombo.getSelectedItem();
        if (!(selected instanceof String family) || family.isBlank()) {
            return;
        }
        this.applyToSelection((start, end) -> this.wordPad.setFontFamily(start, end, family));
        StyleConstants.setFontFamily(this.editor.getInputAttributes(), family);
    }

    private void onFontSizeChosen() {
        if (this.syncingToolbar) {
            return;
        }
        final Integer size = parseFontSize(this.fontSizeCombo.getSelectedItem());
        if (Objects.isNull(size)) {
            return;
        }
        this.applyToSelection((start, end) -> this.wordPad.setFontSize(start, end, size));
        StyleConstants.setFontSize(this.editor.getInputAttributes(), size);
    }

    private void onBoldToggled() {
        if (this.syncingToolbar) {
            return;
        }
        final boolean bold = this.boldButton.isSelected();
        this.applyToSelection((start, end) -> this.wordPad.setBold(start, end, bold));
        StyleConstants.setBold(this.editor.getInputAttributes(), bold);
    }

    private void onItalicToggled() {
        if (this.syncingToolbar) {
            return;
        }
        final boolean italic = this.italicButton.isSelected();
        this.applyToSelection((start, end) -> this.wordPad.setItalic(start, end, italic));
        StyleConstants.setItalic(this.editor.getInputAttributes(), italic);
    }

    private void onUnderlineToggled() {
        if (this.syncingToolbar) {
            return;
        }
        final boolean underline = this.underlineButton.isSelected();
        this.applyToSelection((start, end) -> this.wordPad.setUnderline(start, end, underline));
        StyleConstants.setUnderline(this.editor.getInputAttributes(), underline);
    }

    private void onTextColorChosen() {
        if (this.syncingToolbar) {
            return;
        }
        final int caret = this.editor.getCaretPosition();
        final Color current = this.wordPad.getLength() == 0
                ? WordPad.DEFAULT_FOREGROUND
                : this.wordPad.getForeground(Math.min(caret, Math.max(this.wordPad.getLength() - 1, 0)));
        final Color chosen = JColorChooser.showDialog(this, "Text Color", current);
        if (Objects.isNull(chosen)) {
            return;
        }
        this.applyToSelection((start, end) -> this.wordPad.setForeground(start, end, chosen));
        StyleConstants.setForeground(this.editor.getInputAttributes(), chosen);
        this.refreshTitle();
    }

    private void onAlignmentChosen(final ParagraphAlignment alignment) {
        if (this.syncingToolbar) {
            return;
        }
        final int start = this.editor.getSelectionStart();
        final int end = this.editor.getSelectionEnd();
        this.wordPad.setAlignment(start, end, alignment);
        StyleConstants.setAlignment(this.editor.getInputAttributes(), alignment.getSwingConstant());
        this.refreshTitle();
        this.ruler.repaint();
    }

    private void onBulletToggled() {
        if (this.syncingToolbar) {
            return;
        }
        final int start = this.editor.getSelectionStart();
        final int end = this.editor.getSelectionEnd();
        this.wordPad.setBullet(start, end, this.bulletButton.isSelected());
        this.refreshTitle();
        this.ruler.repaint();
    }

    private void applyToSelection(final RangeConsumer consumer) {
        final int start = this.editor.getSelectionStart();
        final int end = this.editor.getSelectionEnd();
        if (end > start) {
            consumer.accept(start, end);
        }
        this.refreshTitle();
    }

    private void syncToolbarFromCaret() {
        this.syncingToolbar = true;
        try {
            final int length = this.wordPad.getLength();
            final int offset = length == 0 ? 0 : Math.min(this.editor.getCaretPosition(), length - 1);
            this.fontFamilyCombo.setSelectedItem(length == 0 ? WordPad.DEFAULT_FONT_FAMILY : this.wordPad.getFontFamily(offset));
            this.fontSizeCombo.setSelectedItem(length == 0 ? WordPad.DEFAULT_FONT_SIZE : this.wordPad.getFontSize(offset));
            this.boldButton.setSelected(length > 0 && this.wordPad.isBold(offset));
            this.italicButton.setSelected(length > 0 && this.wordPad.isItalic(offset));
            this.underlineButton.setSelected(length > 0 && this.wordPad.isUnderline(offset));
            this.bulletButton.setSelected(length > 0 && this.wordPad.isBullet(offset));
            final ParagraphAlignment alignment = length == 0
                    ? ParagraphAlignment.LEFT
                    : this.wordPad.getAlignment(offset);
            this.alignLeftButton.setSelected(alignment == ParagraphAlignment.LEFT);
            this.alignCenterButton.setSelected(alignment == ParagraphAlignment.CENTER);
            this.alignRightButton.setSelected(alignment == ParagraphAlignment.RIGHT);
        } finally {
            this.syncingToolbar = false;
        }
    }

    private void undo() {
        try {
            if (this.undoManager.canUndo()) {
                this.undoManager.undo();
            }
        } catch (final CannotUndoException ignored) {
            // nothing to undo
        }
        this.refreshTitle();
    }

    private void clearSelection() {
        final int start = this.editor.getSelectionStart();
        final int end = this.editor.getSelectionEnd();
        if (end > start) {
            this.wordPad.deleteRange(start, end);
        }
        this.refreshTitle();
    }

    private void showFindDialog() {
        if (Objects.isNull(this.findDialog)) {
            this.findDialog = new FindDialog(this);
            this.findDialog.onFindNext(this::findNextFromDialog);
        }
        if (Objects.nonNull(this.editor.getSelectedText())) {
            this.findDialog.setFindText(this.editor.getSelectedText());
        }
        this.findDialog.setVisible(true);
        this.findDialog.focusFindField();
    }

    private void showReplaceDialog() {
        if (Objects.isNull(this.replaceDialog)) {
            this.replaceDialog = new ReplaceDialog(this);
            this.replaceDialog.onFindNext(this::findNextFromReplace);
            this.replaceDialog.onReplace(this::replaceOnce);
            this.replaceDialog.onReplaceAll(this::replaceAll);
        }
        if (Objects.nonNull(this.editor.getSelectedText())) {
            this.replaceDialog.setFindText(this.editor.getSelectedText());
        }
        this.replaceDialog.setVisible(true);
        this.replaceDialog.focusFindField();
    }

    private void findNextFromDialog() {
        this.lastFind = this.findDialog.getFindText();
        this.lastMatchCase = this.findDialog.isMatchCase();
        this.lastWholeWord = this.findDialog.isWholeWord();
        this.findNext();
    }

    private void findNextFromReplace() {
        this.lastFind = this.replaceDialog.getFindText();
        this.lastMatchCase = this.replaceDialog.isMatchCase();
        this.lastWholeWord = this.replaceDialog.isWholeWord();
        this.findNext();
    }

    private void findNext() {
        if (this.lastFind.isEmpty()) {
            this.showFindDialog();
            return;
        }
        int index = this.wordPad.find(
                this.lastFind, this.editor.getSelectionEnd(), this.lastMatchCase, this.lastWholeWord);
        if (index < 0) {
            index = this.wordPad.find(this.lastFind, 0, this.lastMatchCase, this.lastWholeWord);
        }
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Cannot find \"" + this.lastFind + "\"", TITLE, JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        this.editor.requestFocusInWindow();
        this.editor.select(index, index + this.lastFind.length());
    }

    private void replaceOnce() {
        this.lastFind = this.replaceDialog.getFindText();
        this.lastMatchCase = this.replaceDialog.isMatchCase();
        this.lastWholeWord = this.replaceDialog.isWholeWord();
        final String selected = this.editor.getSelectedText();
        if (Objects.nonNull(selected) && (selected.equals(this.lastFind)
                || (!this.lastMatchCase && selected.equalsIgnoreCase(this.lastFind)))) {
            this.editor.replaceSelection(this.replaceDialog.getReplaceText());
        }
        this.findNext();
    }

    private void replaceAll() {
        this.lastFind = this.replaceDialog.getFindText();
        this.lastMatchCase = this.replaceDialog.isMatchCase();
        this.lastWholeWord = this.replaceDialog.isWholeWord();
        final int count = this.wordPad.replaceAll(
                this.lastFind, this.replaceDialog.getReplaceText(), this.lastMatchCase, this.lastWholeWord);
        this.refreshTitle();
        JOptionPane.showMessageDialog(this, count + " occurrence(s) replaced.", TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFontDialog() {
        final JFontChooser chooser = new JFontChooser();
        final int offset = Math.max(this.editor.getCaretPosition(), 0);
        final String family = this.wordPad.getLength() == 0
                ? WordPad.DEFAULT_FONT_FAMILY : this.wordPad.getFontFamily(offset);
        final int size = this.wordPad.getLength() == 0 ? WordPad.DEFAULT_FONT_SIZE : this.wordPad.getFontSize(offset);
        int style = Font.PLAIN;
        if (this.wordPad.getLength() > 0 && this.wordPad.isBold(offset)) {
            style |= Font.BOLD;
        }
        if (this.wordPad.getLength() > 0 && this.wordPad.isItalic(offset)) {
            style |= Font.ITALIC;
        }
        chooser.setSelectedFont(new Font(family, style, size));
        if (chooser.showDialog(this) == 0) {
            final Font font = chooser.getSelectedFont();
            final int start = this.editor.getSelectionStart();
            final int end = this.editor.getSelectionEnd();
            if (end > start) {
                this.wordPad.setFontFamily(start, end, font.getFamily());
                this.wordPad.setFontSize(start, end, font.getSize());
                this.wordPad.setBold(start, end, font.isBold());
                this.wordPad.setItalic(start, end, font.isItalic());
            }
            final MutableAttributeSet input = this.editor.getInputAttributes();
            StyleConstants.setFontFamily(input, font.getFamily());
            StyleConstants.setFontSize(input, font.getSize());
            StyleConstants.setBold(input, font.isBold());
            StyleConstants.setItalic(input, font.isItalic());
            this.refreshTitle();
            this.syncToolbarFromCaret();
        }
    }

    private void showParagraphDialog() {
        final int offset = Math.max(this.editor.getCaretPosition(), 0);
        final ParagraphDialog dialog = new ParagraphDialog(
                this,
                this.unit,
                this.wordPad.getLeftIndent(offset),
                this.wordPad.getRightIndent(offset),
                this.wordPad.getFirstLineIndent(offset),
                this.wordPad.getAlignment(offset)
        );
        if (!dialog.showDialog()) {
            return;
        }
        final int start = this.editor.getSelectionStart();
        final int end = this.editor.getSelectionEnd();
        this.wordPad.setParagraphIndents(start, end, dialog.getLeftPoints(), dialog.getRightPoints(), dialog.getFirstLinePoints());
        this.wordPad.setAlignment(start, end, dialog.getAlignment());
        this.refreshTitle();
        this.ruler.repaint();
        this.syncToolbarFromCaret();
    }

    private void showTabsDialog() {
        final int offset = Math.max(this.editor.getCaretPosition(), 0);
        final TabsDialog dialog = new TabsDialog(this, this.unit, this.wordPad.getTabStops(offset));
        if (!dialog.showDialog()) {
            return;
        }
        final int start = this.editor.getSelectionStart();
        final int end = this.editor.getSelectionEnd();
        this.wordPad.setTabStops(start, end, dialog.getStopsPoints());
        this.refreshTitle();
    }

    private void showOptions() {
        final OptionsDialog dialog = new OptionsDialog(this, this.wrapMode, this.unit);
        if (!dialog.showDialog()) {
            return;
        }
        this.wrapMode = dialog.getWrapMode();
        this.unit = dialog.getMeasurementUnit();
        this.ruler.setUnit(this.unit);
        this.editor.revalidate();
        this.editor.repaint();
        this.saveConfig();
    }

    private void insertDateTime() {
        final String chosen = new DateTimeDialog(this, LocalDateTime.now()).showDialog();
        if (Objects.nonNull(chosen)) {
            this.editor.replaceSelection(chosen);
            this.refreshTitle();
        }
    }

    void insertDateTime(final String stamp) {
        this.editor.replaceSelection(stamp);
        this.refreshTitle();
    }

    private void printDocument() {
        try {
            this.editor.print();
        } catch (final PrinterException ex) {
            this.showError("Cannot print.", ex);
        }
    }

    private void printPreview() {
        new PrintPreviewDialog(this, this.editor, this.pageFormat).setVisible(true);
    }

    private void pageSetup() {
        this.pageFormat = PrinterJob.getPrinterJob().pageDialog(this.pageFormat);
    }

    private void applyConfig() {
        final int width = parseIntOrDefault(this.config.get("width"), 800);
        final int height = parseIntOrDefault(this.config.get("height"), 600);
        this.setSize(Math.max(width, 400), Math.max(height, 300));
        this.wrapMode = parseWrap(this.config.get("wrap"));
        this.unit = parseUnit(this.config.get("unit"));
        this.ruler.setUnit(this.unit);
        this.standardToolbar.setVisible(Boolean.parseBoolean(this.config.get("toolbar", "true")));
        this.formatBar.setVisible(Boolean.parseBoolean(this.config.get("formatBar", "true")));
        this.ruler.setVisible(Boolean.parseBoolean(this.config.get("ruler", "true")));
        this.statusBar.setVisible(Boolean.parseBoolean(this.config.get("statusBar", "true")));
        this.toolbarMenuItem.setSelected(this.standardToolbar.isVisible());
        this.formatBarMenuItem.setSelected(this.formatBar.isVisible());
        this.rulerMenuItem.setSelected(this.ruler.isVisible());
        this.statusBarMenuItem.setSelected(this.statusBar.isVisible());
    }

    private void saveConfig() {
        this.config.set("width", String.valueOf(this.getWidth()));
        this.config.set("height", String.valueOf(this.getHeight()));
        this.config.set("wrap", this.wrapMode.name());
        this.config.set("unit", this.unit.name());
        this.config.set("toolbar", String.valueOf(this.standardToolbar.isVisible()));
        this.config.set("formatBar", String.valueOf(this.formatBar.isVisible()));
        this.config.set("ruler", String.valueOf(this.ruler.isVisible()));
        this.config.set("statusBar", String.valueOf(this.statusBar.isVisible()));
        this.config.save();
    }

    private void exit() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.saveConfig();
        this.dispose();
    }

    private boolean confirmDiscardIfNeeded() {
        if (!this.wordPad.isEdited()) {
            return true;
        }
        final String name = Objects.isNull(this.wordPad.getPath())
                ? UNTITLED
                : this.wordPad.getPath().getFileName().toString();
        final int choice = JOptionPane.showConfirmDialog(
                this,
                "Do you want to save changes to " + name + "?",
                TITLE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            return this.saveDocument(false);
        }
        return true;
    }

    private void refreshTitle() {
        final String name = Objects.isNull(this.wordPad.getPath())
                ? UNTITLED
                : this.wordPad.getPath().getFileName().toString();
        final String dirty = this.wordPad.isEdited() ? "*" : "";
        this.setTitle(dirty + name + " - " + TITLE);
    }

    private void showError(final String message, final Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + ex.getMessage(),
                TITLE,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private JButton toolButton(
            final ToolbarIcons.Glyph glyph,
            final String name,
            final String tooltip,
            final Runnable action
    ) {
        final JButton button = new JButton(ToolbarIcons.icon(glyph));
        button.setName(name);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(2, 2, 2, 2));
        button.addActionListener(e -> action.run());
        return button;
    }

    private static Path ensureExtension(final Path path, final DocumentKind kind) {
        final String fileName = path.getFileName().toString();
        if (fileName.contains(".")) {
            return path;
        }
        return path.resolveSibling(fileName + "." + kind.getExtension());
    }

    private static DocumentKind kindFromFilter(final FileFilter filter) {
        if (filter == DocumentFileChooser.TEXT_FILTER) {
            return DocumentKind.TEXT;
        }
        if (filter == DocumentFileChooser.UNICODE_FILTER) {
            return DocumentKind.UNICODE_TEXT;
        }
        return DocumentKind.RICH_TEXT;
    }

    private void applyDefaultFilter(final DocumentFileChooser chooser, final DocumentKind kind) {
        if (kind == DocumentKind.TEXT) {
            chooser.setFileFilter(DocumentFileChooser.TEXT_FILTER);
        } else if (kind == DocumentKind.UNICODE_TEXT) {
            chooser.setFileFilter(DocumentFileChooser.UNICODE_FILTER);
        } else {
            chooser.setFileFilter(DocumentFileChooser.RTF_FILTER);
        }
    }

    private static JToggleButton toggle(final javax.swing.Icon icon, final String name, final String tooltip) {
        final JToggleButton button = new JToggleButton(icon);
        button.setName(name);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(2, 2, 2, 2));
        return button;
    }

    private static JMenuItem menuItem(final String text, final int mnemonic, final int acceleratorKey) {
        final JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        if (acceleratorKey >= 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, InputEvent.CTRL_DOWN_MASK));
        }
        return item;
    }

    private static String[] availableFontFamilies() {
        final String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        if (families.length > 0) {
            return families;
        }
        return new String[]{WordPad.DEFAULT_FONT_FAMILY, "SansSerif", "Serif", "Monospaced"};
    }

    private static Integer parseFontSize(final Object value) {
        if (value instanceof Integer integer) {
            return integer > 0 ? integer : null;
        }
        if (value instanceof String text) {
            try {
                final int parsed = Integer.parseInt(text.trim());
                return parsed > 0 ? parsed : null;
            } catch (final NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static int parseIntOrDefault(final String value, final int defaultValue) {
        if (Objects.isNull(value) || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static WordWrapMode parseWrap(final String value) {
        try {
            return Objects.isNull(value) ? WordWrapMode.WINDOW : WordWrapMode.valueOf(value);
        } catch (final IllegalArgumentException ex) {
            return WordWrapMode.WINDOW;
        }
    }

    private static MeasurementUnit parseUnit(final String value) {
        try {
            return Objects.isNull(value) ? MeasurementUnit.INCHES : MeasurementUnit.valueOf(value);
        } catch (final IllegalArgumentException ex) {
            return MeasurementUnit.INCHES;
        }
    }

    @FunctionalInterface
    private interface RangeConsumer {
        void accept(int start, int end);
    }

    public AbstractButton formatControl(final String name) {
        return switch (name) {
            case "bold" -> this.boldButton;
            case "italic" -> this.italicButton;
            case "underline" -> this.underlineButton;
            case "textColor" -> this.textColorButton;
            case "alignLeft" -> this.alignLeftButton;
            case "alignCenter" -> this.alignCenterButton;
            case "alignRight" -> this.alignRightButton;
            case "bullet" -> this.bulletButton;
            default -> throw new IllegalArgumentException("Unknown control: " + name);
        };
    }

    public JComboBox<String> getFontFamilyCombo() {
        return this.fontFamilyCombo;
    }

    public JComboBox<Integer> getFontSizeCombo() {
        return this.fontSizeCombo;
    }

    public boolean editorWrapsAtWindow() {
        return this.wrapMode == WordWrapMode.WINDOW;
    }

    public WordWrapMode getWrapMode() {
        return this.wrapMode;
    }

    public JToolBar getStandardToolbar() {
        return this.standardToolbar;
    }

    public RulerBar getRuler() {
        return this.ruler;
    }

    public JPanel getStatusBar() {
        return this.statusBar;
    }
}
