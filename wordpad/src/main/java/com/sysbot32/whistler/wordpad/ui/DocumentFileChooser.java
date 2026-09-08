package com.sysbot32.whistler.wordpad.ui;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class DocumentFileChooser extends JFileChooser {
    public static final FileNameExtensionFilter RTF_FILTER =
            new FileNameExtensionFilter("Rich Text Format (*.rtf)", "rtf");
    public static final FileNameExtensionFilter TEXT_FILTER =
            new FileNameExtensionFilter("Text Documents (*.txt)", "txt");
    public static final FileNameExtensionFilter UNICODE_FILTER =
            new FileNameExtensionFilter("Unicode Text Documents (*.txt)", "txt");

    public DocumentFileChooser() {
        this.addChoosableFileFilter(RTF_FILTER);
        this.addChoosableFileFilter(TEXT_FILTER);
        this.addChoosableFileFilter(UNICODE_FILTER);
        this.setFileFilter(RTF_FILTER);
    }
}
