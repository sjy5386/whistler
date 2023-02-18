package com.sysbot32.whistler.notepad;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NotepadFrame extends JFrame {
    private final JTextArea textArea;

    public NotepadFrame() {
        super("Notepad");
        this.setSize(1280, 720);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JPanel contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(new BorderLayout());

        this.textArea = new JTextArea();
        final JScrollPane scrollPane = new JScrollPane(this.textArea);

        this.setJMenuBar(this.createMenuBar());
        contentPane.add(scrollPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        menuBar.add(this.createFileMenu());
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

        newMenuItem.addActionListener(e -> this.textArea.setText(""));
        openMenuItem.addActionListener(e -> {
            final JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    this.textArea.setText(Files.readString(Paths.get(fileChooser.getSelectedFile().toURI())));
                } catch (final IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        exitMenuItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(exitMenuItem);

        return fileMenu;
    }

    private JMenu createFormatMenu() {
        final JMenu formatMenu = new JMenu("Format");

        final JMenuItem wordWrapMenuItem = new JCheckBoxMenuItem("Word Wrap");
        final JMenuItem fontMenuItem = new JMenuItem("Font");

        wordWrapMenuItem.addActionListener(e -> this.textArea.setLineWrap(!this.textArea.getLineWrap()));

        formatMenu.add(wordWrapMenuItem);
        formatMenu.add(fontMenuItem);

        return formatMenu;
    }
}
