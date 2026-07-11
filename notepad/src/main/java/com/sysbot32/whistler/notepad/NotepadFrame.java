package com.sysbot32.whistler.notepad;

import com.sysbot32.whistler.config.Config;
import say.swing.JFontChooser;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Objects;

public class NotepadFrame extends JFrame {
    private static final String UNTITLED = "Untitled";
    private static final String TITLE = "Notepad";

    private final Notepad notepad;
    private final Config config;
    private final JTextArea textArea;
    private final UndoManager undoManager = new UndoManager();
    private final JCheckBoxMenuItem wordWrapMenuItem = new JCheckBoxMenuItem("Word Wrap");
    private final JCheckBoxMenuItem statusBarMenuItem = new JCheckBoxMenuItem("Status Bar");
    private final JLabel statusLabel = new JLabel(" Ln 1, Col 1 ");
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private JMenuItem goToMenuItem;

    private FindDialog findDialog;
    private ReplaceDialog replaceDialog;
    private String lastFindText = "";
    private boolean lastMatchCase = false;
    private SearchDirection lastDirection = SearchDirection.DOWN;
    private boolean loadingDocument = false;
    private String pageHeader = "&f";
    private String pageFooter = "Page &p";

    public NotepadFrame(final Notepad notepad, final Config config) {
        this.notepad = notepad;
        this.config = config;

        this.setSize(800, 600);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        final JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        this.textArea = new JTextArea(notepad.getContent());
        this.textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        this.installDocumentHandlers();

        final JScrollPane scrollPane = new JScrollPane(this.textArea);
        this.statusBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.statusBar.add(this.statusLabel, BorderLayout.EAST);
        this.statusBar.setVisible(false);

        this.setJMenuBar(this.createMenuBar());
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(this.statusBar, BorderLayout.SOUTH);

        this.applyConfig();
        this.textArea.addCaretListener(this::onCaretMoved);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                NotepadFrame.this.exit();
            }
        });

        this.refreshTitle();
        this.updateStatusBar();
    }

    private void installDocumentHandlers() {
        this.textArea.getDocument().addUndoableEditListener(this.undoManager);
        this.textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                this.markEdited();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                this.markEdited();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                this.markEdited();
            }

            private void markEdited() {
                if (NotepadFrame.this.loadingDocument) {
                    return;
                }
                NotepadFrame.this.notepad.setContent(NotepadFrame.this.textArea.getText());
                NotepadFrame.this.notepad.setEdited(true);
                NotepadFrame.this.refreshTitle();
                NotepadFrame.this.updateStatusBar();
            }
        });
    }

    private void onCaretMoved(final CaretEvent e) {
        this.updateStatusBar();
    }

    private void applyConfig() {
        this.pageHeader = this.config.get("pageHeader", "&f");
        this.pageFooter = this.config.get("pageFooter", "Page &p");

        final boolean wordWrap = Boolean.parseBoolean(this.config.get("wordWrap", "false"));
        final boolean statusBarVisible = Boolean.parseBoolean(this.config.get("statusBar", "false"));
        this.applyWordWrap(wordWrap, false);
        if (!wordWrap) {
            this.applyStatusBar(statusBarVisible, false);
        }

        final String fontName = this.config.get("fontName", Font.MONOSPACED);
        final int fontStyle = parseIntOrDefault(this.config.get("fontStyle"), Font.PLAIN);
        final int fontSize = parseIntOrDefault(this.config.get("fontSize"), 14);
        this.textArea.setFont(new Font(fontName, fontStyle, Math.max(fontSize, 1)));
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

    private void applyWordWrap(final boolean wrap, final boolean persist) {
        this.textArea.setLineWrap(wrap);
        this.textArea.setWrapStyleWord(wrap);
        this.wordWrapMenuItem.setSelected(wrap);
        // Classic: Word Wrap disables Status Bar and Go To.
        if (Objects.nonNull(this.goToMenuItem)) {
            this.goToMenuItem.setEnabled(!wrap);
        }
        this.statusBarMenuItem.setEnabled(!wrap);
        if (wrap) {
            this.statusBarMenuItem.setSelected(false);
            this.statusBar.setVisible(false);
        }
        if (persist) {
            this.saveConfig();
        }
    }

    private void applyStatusBar(final boolean visible, final boolean persist) {
        if (this.textArea.getLineWrap()) {
            this.statusBarMenuItem.setSelected(false);
            this.statusBar.setVisible(false);
            this.statusBarMenuItem.setEnabled(false);
            return;
        }
        this.statusBarMenuItem.setEnabled(true);
        this.statusBarMenuItem.setSelected(visible);
        this.statusBar.setVisible(visible);
        if (visible) {
            this.updateStatusBar();
        }
        if (persist) {
            this.saveConfig();
        }
    }

    private void updateStatusBar() {
        if (!this.statusBar.isVisible()) {
            return;
        }
        final LineColumn pos = LineColumn.fromOffset(this.textArea.getText(), this.textArea.getCaretPosition());
        this.statusLabel.setText(" " + pos.toStatusText() + " ");
    }

    private void saveConfig() {
        this.config.set("wordWrap", String.valueOf(this.textArea.getLineWrap()));
        this.config.set("statusBar", String.valueOf(this.statusBar.isVisible()));
        this.config.set("pageHeader", this.pageHeader);
        this.config.set("pageFooter", this.pageFooter);
        final Font font = this.textArea.getFont();
        this.config.set("fontName", font.getFamily());
        this.config.set("fontStyle", String.valueOf(font.getStyle()));
        this.config.set("fontSize", String.valueOf(font.getSize()));
        this.config.save();
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(this.createFileMenu());
        menuBar.add(this.createEditMenu());
        menuBar.add(this.createFormatMenu());
        menuBar.add(this.createViewMenu());
        menuBar.add(this.createHelpMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem newMenuItem = menuItem("New", KeyEvent.VK_N, KeyEvent.VK_N);
        final JMenuItem openMenuItem = menuItem("Open...", KeyEvent.VK_O, KeyEvent.VK_O);
        final JMenuItem saveMenuItem = menuItem("Save", KeyEvent.VK_S, KeyEvent.VK_S);
        final JMenuItem saveAsMenuItem = menuItem("Save As...", KeyEvent.VK_A, -1);
        final JMenuItem pageSetupMenuItem = menuItem("Page Setup...", KeyEvent.VK_U, -1);
        final JMenuItem printMenuItem = menuItem("Print...", KeyEvent.VK_P, KeyEvent.VK_P);
        final JMenuItem exitMenuItem = menuItem("Exit", KeyEvent.VK_X, -1);

        newMenuItem.addActionListener(e -> this.newDocument());
        openMenuItem.addActionListener(e -> this.openDocument());
        saveMenuItem.addActionListener(e -> this.saveDocument(false));
        saveAsMenuItem.addActionListener(e -> this.saveDocument(true));
        pageSetupMenuItem.addActionListener(e -> this.showPageSetup());
        printMenuItem.addActionListener(e -> this.printDocument());
        exitMenuItem.addActionListener(e -> this.exit());

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(pageSetupMenuItem);
        fileMenu.add(printMenuItem);
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
        final JMenuItem deleteMenuItem = menuItem("Delete", KeyEvent.VK_L, KeyEvent.VK_DELETE);
        final JMenuItem findMenuItem = menuItem("Find...", KeyEvent.VK_F, KeyEvent.VK_F);
        final JMenuItem findNextMenuItem = menuItem("Find Next", KeyEvent.VK_N, KeyEvent.VK_F3);
        final JMenuItem replaceMenuItem = menuItem("Replace...", KeyEvent.VK_R, KeyEvent.VK_H);
        this.goToMenuItem = menuItem("Go To...", KeyEvent.VK_G, KeyEvent.VK_G);
        final JMenuItem selectAllMenuItem = menuItem("Select All", KeyEvent.VK_A, KeyEvent.VK_A);
        final JMenuItem timeDateMenuItem = menuItem("Time/Date", KeyEvent.VK_D, KeyEvent.VK_F5);

        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        findNextMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        timeDateMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

        undoMenuItem.addActionListener(e -> this.undo());
        cutMenuItem.addActionListener(e -> this.cut());
        copyMenuItem.addActionListener(e -> this.copy());
        pasteMenuItem.addActionListener(e -> this.paste());
        deleteMenuItem.addActionListener(e -> this.deleteSelection());
        findMenuItem.addActionListener(e -> this.showFindDialog());
        findNextMenuItem.addActionListener(e -> this.findNext(true));
        replaceMenuItem.addActionListener(e -> this.showReplaceDialog());
        this.goToMenuItem.addActionListener(e -> this.goToLine());
        selectAllMenuItem.addActionListener(e -> this.textArea.selectAll());
        timeDateMenuItem.addActionListener(e -> this.insertTimeDate());

        editMenu.add(undoMenuItem);
        editMenu.addSeparator();
        editMenu.add(cutMenuItem);
        editMenu.add(copyMenuItem);
        editMenu.add(pasteMenuItem);
        editMenu.add(deleteMenuItem);
        editMenu.addSeparator();
        editMenu.add(findMenuItem);
        editMenu.add(findNextMenuItem);
        editMenu.add(replaceMenuItem);
        editMenu.add(this.goToMenuItem);
        editMenu.addSeparator();
        editMenu.add(selectAllMenuItem);
        editMenu.add(timeDateMenuItem);

        this.goToMenuItem.setEnabled(!this.textArea.getLineWrap());
        return editMenu;
    }

    private JMenu createFormatMenu() {
        final JMenu formatMenu = new JMenu("Format");
        formatMenu.setMnemonic(KeyEvent.VK_O);

        final JMenuItem fontMenuItem = new JMenuItem("Font...");

        this.wordWrapMenuItem.addActionListener(e ->
                this.applyWordWrap(this.wordWrapMenuItem.isSelected(), true));
        fontMenuItem.addActionListener(e -> {
            final JFontChooser fontChooser = new JFontChooser();
            fontChooser.setSelectedFont(this.textArea.getFont());
            if (fontChooser.showDialog(this) == 0) {
                this.textArea.setFont(fontChooser.getSelectedFont());
                this.saveConfig();
            }
        });

        formatMenu.add(this.wordWrapMenuItem);
        formatMenu.add(fontMenuItem);
        return formatMenu;
    }

    private JMenu createViewMenu() {
        final JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        this.statusBarMenuItem.addActionListener(e ->
                this.applyStatusBar(this.statusBarMenuItem.isSelected(), true));
        viewMenu.add(this.statusBarMenuItem);
        return viewMenu;
    }

    private JMenu createHelpMenu() {
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutMenuItem = new JMenuItem("About Notepad");
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.addActionListener(e -> this.showAbout());
        helpMenu.add(aboutMenuItem);
        return helpMenu;
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                "Whistler Notepad\nA classic Windows XP–style Notepad recreation.",
                "About Notepad",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showPageSetup() {
        final PageSetupDialog dialog = new PageSetupDialog(this, this.pageHeader, this.pageFooter);
        if (dialog.showDialog()) {
            this.pageHeader = dialog.getHeader();
            this.pageFooter = dialog.getFooter();
            this.saveConfig();
        }
    }

    private static JMenuItem menuItem(final String text, final int mnemonic, final int acceleratorKey) {
        final JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        if (acceleratorKey == KeyEvent.VK_F3 || acceleratorKey == KeyEvent.VK_F5 || acceleratorKey == KeyEvent.VK_DELETE) {
            item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, 0));
        } else if (acceleratorKey >= 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, InputEvent.CTRL_DOWN_MASK));
        }
        return item;
    }

    private void newDocument() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.notepad.createNew();
        this.loadTextIntoEditor(this.notepad.getContent());
        this.refreshTitle();
    }

    private void openDocument() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        final JFileChooser fileChooser = new TextFileChooser();
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            final Path path = Paths.get(fileChooser.getSelectedFile().toURI());
            this.loadTextIntoEditor(this.notepad.open(path));
            this.refreshTitle();
        } catch (final IOException ex) {
            this.showError("Cannot open file.", ex);
        }
    }

    private boolean saveDocument(final boolean saveAs) {
        this.notepad.setContent(this.textArea.getText());
        Path path = this.notepad.getPath();
        if (saveAs || Objects.isNull(path)) {
            final JFileChooser fileChooser = new TextFileChooser();
            if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            path = Paths.get(fileChooser.getSelectedFile().toURI());
            if (!path.getFileName().toString().contains(".")) {
                path = path.resolveSibling(path.getFileName() + ".txt");
            }
        }
        try {
            this.notepad.save(path);
            this.refreshTitle();
            return true;
        } catch (final IOException ex) {
            this.showError("Cannot save file.", ex);
            return false;
        }
    }

    private void printDocument() {
        try {
            final String fileName = Objects.isNull(this.notepad.getPath())
                    ? UNTITLED
                    : this.notepad.getPath().getFileName().toString();
            final LocalDateTime now = LocalDateTime.now();
            final MessageFormat header = new MessageFormat(
                    HeaderFooter.toPrintMessageFormat(this.pageHeader, fileName, now)
            );
            final MessageFormat footer = new MessageFormat(
                    HeaderFooter.toPrintMessageFormat(this.pageFooter, fileName, now)
            );
            this.textArea.print(header, footer);
        } catch (final PrinterException ex) {
            this.showError("Cannot print.", ex);
        }
    }

    private void exit() {
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.saveConfig();
        this.dispose();
    }

    private boolean confirmDiscardIfNeeded() {
        this.notepad.setContent(this.textArea.getText());
        if (!this.notepad.isEdited()) {
            return true;
        }
        final String name = Objects.isNull(this.notepad.getPath())
                ? UNTITLED
                : this.notepad.getPath().getFileName().toString();
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

    private void undo() {
        try {
            if (this.undoManager.canUndo()) {
                this.undoManager.undo();
            }
        } catch (final CannotUndoException ignored) {
            // nothing to undo
        }
    }

    private void cut() {
        final String selected = this.textArea.getSelectedText();
        if (Objects.isNull(selected)) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
        this.textArea.replaceSelection("");
    }

    private void copy() {
        final String selected = this.textArea.getSelectedText();
        if (Objects.isNull(selected)) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
    }

    private void paste() {
        try {
            final String data = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
            this.textArea.replaceSelection(data);
        } catch (final UnsupportedFlavorException | IOException ex) {
            this.showError("Cannot paste from clipboard.", ex);
        }
    }

    private void deleteSelection() {
        if (this.textArea.getSelectionStart() != this.textArea.getSelectionEnd()) {
            this.textArea.replaceSelection("");
        }
    }

    private void insertTimeDate() {
        this.textArea.replaceSelection(Notepad.getTimeDate());
    }

    private void showFindDialog() {
        if (Objects.isNull(this.findDialog)) {
            this.findDialog = new FindDialog(this);
            this.findDialog.onFindNext(() -> {
                this.lastFindText = this.findDialog.getFindText();
                this.lastMatchCase = this.findDialog.isMatchCase();
                this.lastDirection = this.findDialog.getDirection();
                this.findNext(true);
            });
        }
        // Seed text, Match case, and Direction from the last session so reopening
        // the dialog does not reset Match case to the checkbox default (false).
        this.findDialog.applySession(this.lastFindText, this.lastMatchCase, this.lastDirection);
        if (this.lastFindText.isEmpty() && Objects.nonNull(this.textArea.getSelectedText())) {
            this.findDialog.setFindText(this.textArea.getSelectedText());
        }
        this.findDialog.setVisible(true);
        this.findDialog.focusFindField();
    }

    private void showReplaceDialog() {
        if (Objects.isNull(this.replaceDialog)) {
            this.replaceDialog = new ReplaceDialog(this);
            this.replaceDialog.onFindNext(() -> {
                this.lastFindText = this.replaceDialog.getFindText();
                this.lastMatchCase = this.replaceDialog.isMatchCase();
                this.lastDirection = SearchDirection.DOWN;
                this.findNext(true);
            });
            this.replaceDialog.onReplace(this::replaceOnce);
            this.replaceDialog.onReplaceAll(this::replaceAll);
        }
        this.replaceDialog.setMatchCase(this.lastMatchCase);
        if (!this.lastFindText.isEmpty()) {
            this.replaceDialog.setFindText(this.lastFindText);
        } else if (Objects.nonNull(this.textArea.getSelectedText())) {
            this.replaceDialog.setFindText(this.textArea.getSelectedText());
        }
        this.replaceDialog.setVisible(true);
        this.replaceDialog.focusFindField();
    }

    /**
     * Pull text / match-case / direction from an open Find (or Replace) dialog so F3 and
     * Edit → Find Next use the live controls, not a stale cache.
     */
    private void syncFindQueryFromOpenDialogs() {
        if (Objects.nonNull(this.findDialog) && this.findDialog.isVisible()) {
            final FindQuery live = FindQuery.resolve(
                    new FindQuery(this.lastFindText, this.lastMatchCase, this.lastDirection),
                    true,
                    this.findDialog.getFindText(),
                    this.findDialog.isMatchCase(),
                    this.findDialog.getDirection()
            );
            this.lastFindText = live.getText();
            this.lastMatchCase = live.isMatchCase();
            this.lastDirection = live.getDirection();
            return;
        }
        if (Objects.nonNull(this.replaceDialog) && this.replaceDialog.isVisible()) {
            this.lastFindText = this.replaceDialog.getFindText();
            this.lastMatchCase = this.replaceDialog.isMatchCase();
            this.lastDirection = SearchDirection.DOWN;
        }
    }

    private void findNext(final boolean wrapAround) {
        this.syncFindQueryFromOpenDialogs();
        if (this.lastFindText.isEmpty()) {
            this.showFindDialog();
            return;
        }
        this.syncModelFromEditor();
        final int from;
        if (this.lastDirection == SearchDirection.DOWN) {
            from = this.textArea.getSelectionEnd();
        } else {
            from = this.textArea.getSelectionStart();
        }
        int index = this.notepad.find(this.lastFindText, from, this.lastMatchCase, this.lastDirection);
        if (index < 0 && wrapAround) {
            if (this.lastDirection == SearchDirection.DOWN) {
                index = this.notepad.find(this.lastFindText, 0, this.lastMatchCase, SearchDirection.DOWN);
            } else {
                index = this.notepad.find(
                        this.lastFindText,
                        this.notepad.getContent().length(),
                        this.lastMatchCase,
                        SearchDirection.UP
                );
            }
        }
        if (index < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot find \"" + this.lastFindText + "\"",
                    TITLE,
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        this.textArea.requestFocusInWindow();
        this.textArea.select(index, index + this.lastFindText.length());
        this.updateStatusBar();
    }

    private void replaceOnce() {
        this.lastFindText = this.replaceDialog.getFindText();
        this.lastMatchCase = this.replaceDialog.isMatchCase();
        this.lastDirection = SearchDirection.DOWN;
        final String replacement = this.replaceDialog.getReplaceText();
        final String selected = this.textArea.getSelectedText();
        if (Objects.nonNull(selected) && this.matches(selected, this.lastFindText, this.lastMatchCase)) {
            this.textArea.replaceSelection(replacement);
        }
        this.findNext(true);
    }

    private void replaceAll() {
        this.lastFindText = this.replaceDialog.getFindText();
        this.lastMatchCase = this.replaceDialog.isMatchCase();
        final String replacement = this.replaceDialog.getReplaceText();
        this.syncModelFromEditor();
        final int count = this.notepad.replaceAll(this.lastFindText, replacement, this.lastMatchCase);
        if (count == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot find \"" + this.lastFindText + "\"",
                    TITLE,
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        this.loadTextIntoEditor(this.notepad.getContent());
        this.notepad.setEdited(true);
        this.refreshTitle();
        JOptionPane.showMessageDialog(
                this,
                count + " occurrence(s) replaced.",
                TITLE,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static boolean matches(final String a, final String b, final boolean matchCase) {
        return matchCase ? a.equals(b) : a.equalsIgnoreCase(b);
    }

    private void goToLine() {
        if (this.textArea.getLineWrap()) {
            return;
        }
        final String input = JOptionPane.showInputDialog(this, "Line Number:", "Go To Line", JOptionPane.PLAIN_MESSAGE);
        if (Objects.isNull(input) || input.isBlank()) {
            return;
        }
        try {
            final int line = Integer.parseInt(input.trim());
            if (line < 1) {
                throw new NumberFormatException("line < 1");
            }
            final int offset = this.textArea.getLineStartOffset(line - 1);
            this.textArea.setCaretPosition(offset);
            this.textArea.requestFocusInWindow();
            this.updateStatusBar();
        } catch (final NumberFormatException | BadLocationException ex) {
            JOptionPane.showMessageDialog(this, "The line number is beyond the total number of lines", TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTextIntoEditor(final String text) {
        final boolean keepDirty = this.notepad.isEdited();
        this.loadingDocument = true;
        try {
            this.textArea.setText(text);
            this.textArea.setCaretPosition(0);
            this.undoManager.discardAllEdits();
            this.notepad.setContent(text);
            // Preserve dirty flag when open applied a .LOG stamp (or replace-all).
            this.notepad.setEdited(keepDirty);
        } finally {
            this.loadingDocument = false;
        }
        this.updateStatusBar();
    }

    private void syncModelFromEditor() {
        this.notepad.setContent(this.textArea.getText());
    }

    private void refreshTitle() {
        final String name = Objects.isNull(this.notepad.getPath())
                ? UNTITLED
                : this.notepad.getPath().getFileName().toString();
        final String dirty = this.notepad.isEdited() ? "*" : "";
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
}
