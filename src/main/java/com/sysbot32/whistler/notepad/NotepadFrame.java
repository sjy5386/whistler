package com.sysbot32.whistler.notepad;

import javax.swing.*;
import java.awt.*;

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
        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");

        final JMenuItem newMenuItem = new JMenuItem("New");
        final JMenuItem openMenuItem = new JMenuItem("Open");
        final JMenuItem saveMenuItem = new JMenuItem("Save");
        final JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        final JMenuItem exitMenuItem = new JMenuItem("Exit");

        exitMenuItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(exitMenuItem);

        return fileMenu;
    }
}
