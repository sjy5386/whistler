package com.sysbot32.whistler.notepad;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class TextFileChooser extends JFileChooser {
    public TextFileChooser() {
        this.addChoosableFileFilter(new FileNameExtensionFilter("Text Documents", "txt"));
    }
}
