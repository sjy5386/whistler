package com.sysbot32.whistler.solitaire;

import com.sysbot32.whistler.solitaire.ui.SolitaireFrame;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolitaireApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "solitaire.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final SolitaireFrame frame = new SolitaireFrame(config);
            frame.setVisible(true);
        });
    }
}
