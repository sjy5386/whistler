package com.sysbot32.whistler.freecell;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FreeCellApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "freecell.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final FreeCellFrame frame = new FreeCellFrame(config);
            frame.setVisible(true);
        });
    }
}
