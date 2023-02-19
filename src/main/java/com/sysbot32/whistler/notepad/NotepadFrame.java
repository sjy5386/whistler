package com.sysbot32.whistler.notepad;

import say.swing.JFontChooser;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;

public class NotepadFrame extends JFrame {
    private static final String UNTITLED = "Untitled";
    private static final String TITLE = "Notepad";
    private final Notepad notepad;
    private final JTextArea textArea;

    public NotepadFrame(final Notepad notepad) {
        this.notepad = notepad;

        this.setTitle();
        this.setSize(1280, 720);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        this.textArea = new JTextArea(notepad.getContent());
        final JScrollPane scrollPane = new JScrollPane(this.textArea);

        this.setJMenuBar(this.createMenuBar());
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        menuBar.add(this.createFileMenu());
        menuBar.add(this.createEditMenu());
        menuBar.add(this.createFormatMenu());

        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");

        final JMenuItem newMenuItem = new JMenuItem("New");
        final JMenuItem openMenuItem = new JMenuItem("Open");
        final JMenuItem saveMenuItem = new JMenuItem("Save");
        final JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        final JMenuItem exitMenuItem = new JMenuItem("Exit");

        newMenuItem.addActionListener(e -> {
            this.notepad.createNew();
            this.textArea.setText(this.notepad.getContent());
            this.setTitle();
        });
        openMenuItem.addActionListener(e -> {
            final JFileChooser fileChooser = new TextFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    this.textArea.setText(this.notepad.open(Paths.get(fileChooser.getSelectedFile().toURI())));
                } catch (final IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    this.setTitle();
                }
            }
        });
        saveMenuItem.addActionListener(e -> this.save());
        saveAsMenuItem.addActionListener(e -> this.save());
        exitMenuItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        return fileMenu;
    }

    private JMenu createEditMenu() {
        final JMenu editMenu = new JMenu("Edit");

        final JMenuItem undoMenuItem = new JMenuItem("Undo");
        final JMenuItem cutMenuItem = new JMenuItem("Cut");
        final JMenuItem copyMenuItem = new JMenuItem("Copy");
        final JMenuItem pasteMenuItem = new JMenuItem("Paste");
        final JMenuItem deleteMenuItem = new JMenuItem("Delete");
        final JMenuItem findMenuItem = new JMenuItem("Find");
        final JMenuItem findNextMenuItem = new JMenuItem("Find Next");
        final JMenuItem replaceMenuItem = new JMenuItem("Replace");
        final JMenuItem goToMenuItem = new JMenuItem("Go To");
        final JMenuItem selectAllMenuItem = new JMenuItem("Select All");
        final JMenuItem timeDateMenuItem = new JMenuItem("Time/Date");

        copyMenuItem.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(this.textArea.getSelectedText()), null));
        deleteMenuItem.addActionListener(e -> {
            this.notepad.setContent(this.textArea.getText());
            this.textArea.setText(this.notepad.delete(this.textArea.getSelectionStart(), this.textArea.getSelectionEnd()));
        });
        goToMenuItem.addActionListener(e -> {
            try {
                final int offset = this.textArea.getLineStartOffset(Integer.parseInt(JOptionPane.showInputDialog(this, "Line Number: ", "Go To Line", JOptionPane.PLAIN_MESSAGE)) - 1);
                this.textArea.setSelectionStart(offset);
                this.textArea.setSelectionEnd(offset);
            } catch (final BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        });
        selectAllMenuItem.addActionListener(e -> this.textArea.selectAll());
        timeDateMenuItem.addActionListener(e -> {
            this.notepad.setContent(this.textArea.getText());
            this.textArea.setText(this.notepad.replace(this.textArea.getSelectionStart(), this.textArea.getSelectionEnd(), Notepad.getTimeDate()));
        });

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
        editMenu.add(goToMenuItem);
        editMenu.addSeparator();
        editMenu.add(selectAllMenuItem);
        editMenu.add(timeDateMenuItem);

        return editMenu;
    }

    private JMenu createFormatMenu() {
        final JMenu formatMenu = new JMenu("Format");

        final JMenuItem wordWrapMenuItem = new JCheckBoxMenuItem("Word Wrap");
        final JMenuItem fontMenuItem = new JMenuItem("Font");
        final JMenuItem colorMenuItem = new JMenuItem("Color");
        final JMenuItem backgroundColorMenuItem = new JMenuItem("Background Color");

        wordWrapMenuItem.addActionListener(e -> this.textArea.setLineWrap(!this.textArea.getLineWrap()));
        fontMenuItem.addActionListener(e -> {
            final JFontChooser fontChooser = new JFontChooser();
            fontChooser.setSelectedFont(this.textArea.getFont());
            if (fontChooser.showDialog(this) == 0) {
                this.textArea.setFont(fontChooser.getSelectedFont());
            }
        });
        colorMenuItem.addActionListener(e -> {
            final Color color = JColorChooser.showDialog(this, null, this.textArea.getForeground());
            if (Objects.nonNull(color)) {
                this.textArea.setForeground(color);
            }
        });
        backgroundColorMenuItem.addActionListener(e -> {
            final Color color = JColorChooser.showDialog(this, null, this.textArea.getBackground());
            if (Objects.nonNull(color)) {
                this.textArea.setBackground(color);
            }
        });

        formatMenu.add(wordWrapMenuItem);
        formatMenu.add(fontMenuItem);
        formatMenu.addSeparator();
        formatMenu.add(colorMenuItem);
        formatMenu.add(backgroundColorMenuItem);

        return formatMenu;
    }

    private void setTitle() {
        this.setTitle((Objects.isNull(this.notepad.getPath()) ? UNTITLED : this.notepad.getPath().getFileName()) + " - " + TITLE);
    }

    private void save() {
        this.notepad.setContent(this.textArea.getText());
        final JFileChooser fileChooser = new TextFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                this.notepad.save(Paths.get(fileChooser.getSelectedFile().toURI()));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } finally {
                this.setTitle();
            }
        }
    }
}
