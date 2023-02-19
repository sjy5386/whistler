package com.sysbot32.whistler.notepad;

import say.swing.JFontChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Paths;

public class NotepadFrame extends JFrame {
    private final Notepad notepad;
    private final JTextArea textArea;

    public NotepadFrame(final Notepad notepad) {
        this.notepad = notepad;

        this.setTitle("Notepad");
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
        });
        openMenuItem.addActionListener(e -> {
            final JFileChooser fileChooser = new TextFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    this.textArea.setText(this.notepad.open(Paths.get(fileChooser.getSelectedFile().toURI())));
                } catch (final IOException ex) {
                    throw new RuntimeException(ex);
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

    private JMenu createFormatMenu() {
        final JMenu formatMenu = new JMenu("Format");

        final JMenuItem wordWrapMenuItem = new JCheckBoxMenuItem("Word Wrap");
        final JMenuItem fontMenuItem = new JMenuItem("Font");

        wordWrapMenuItem.addActionListener(e -> this.textArea.setLineWrap(!this.textArea.getLineWrap()));
        fontMenuItem.addActionListener(e -> {
            final JFontChooser fontChooser = new JFontChooser();
            fontChooser.setSelectedFont(this.textArea.getFont());
            if (fontChooser.showDialog(this) == 0) {
                this.textArea.setFont(fontChooser.getSelectedFont());
            }
        });

        formatMenu.add(wordWrapMenuItem);
        formatMenu.add(fontMenuItem);

        return formatMenu;
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

    private void save() {
        this.notepad.setContent(this.textArea.getText());
        final JFileChooser fileChooser = new TextFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                this.notepad.save(Paths.get(fileChooser.getSelectedFile().toURI()));
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
