package com.sysbot32.whistler.notepad;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NotepadApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "notepad.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final Notepad notepad;
            if (args.length == 0) {
                notepad = new Notepad();
            } else {
                notepad = new Notepad(Paths.get(args[0]));
            }
            final NotepadFrame frame = new NotepadFrame(notepad, config);
            frame.setVisible(true);
        });
    }
}
