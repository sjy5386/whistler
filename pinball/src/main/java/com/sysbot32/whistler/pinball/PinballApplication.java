package com.sysbot32.whistler.pinball;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.pinball.ui.PinballFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PinballApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "pinball.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final PinballFrame frame = new PinballFrame(config);
            frame.setVisible(true);
        });
    }
}
