package com.sysbot32.whistler.notepad;

import java.nio.file.Paths;

public class NotepadApplication {
    public static void main(final String[] args) {
        if (args.length == 0) {
            new NotepadFrame(new Notepad()).setVisible(true);
        } else {
            new NotepadFrame(new Notepad(Paths.get(args[0]))).setVisible(true);
        }
    }
}
