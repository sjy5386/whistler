package com.sysbot32.whistler.paint;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.paint.model.Paint;
import com.sysbot32.whistler.paint.ui.PaintFrame;
import com.sysbot32.whistler.config.PropertiesConfig;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PaintApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "paint.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            final Config config = new PropertiesConfig(CONFIG_PATH);
            final Paint paint;
            if (args.length == 0) {
                paint = new Paint();
            } else {
                paint = new Paint(Paths.get(args[0]));
            }
            final PaintFrame frame = new PaintFrame(paint, config);
            frame.setVisible(true);
        });
    }
}
