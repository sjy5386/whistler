package com.sysbot32.whistler.wordpad;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.wordpad.model.WordPad;
import com.sysbot32.whistler.wordpad.ui.WordPadFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WordPadApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "wordpad.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final WordPad wordPad;
            if (args.length == 0) {
                wordPad = new WordPad();
            } else {
                wordPad = new WordPad(Paths.get(args[0]));
            }
            final WordPadFrame frame = new WordPadFrame(wordPad, config);
            frame.setVisible(true);
        });
    }
}
