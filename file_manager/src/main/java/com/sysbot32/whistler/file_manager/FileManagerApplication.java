package com.sysbot32.whistler.file_manager;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;
import lombok.experimental.UtilityClass;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class FileManagerApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "file_manager.properties"
    );

    public void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final FileManagerFrame frame = new FileManagerFrame(config);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
