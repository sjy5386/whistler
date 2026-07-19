package com.sysbot32.whistler.minesweeper;

import com.sysbot32.whistler.minesweeper.ui.MinesweeperFrame;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinesweeperApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "minesweeper.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final MinesweeperFrame frame = new MinesweeperFrame(config);
            frame.setVisible(true);
        });
    }
}
