package com.sysbot32.whistler.sound_recorder;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.sound_recorder.model.SoundClip;
import com.sysbot32.whistler.sound_recorder.ui.SoundRecorderFrame;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SoundRecorderApplication {
    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"),
            ".whistler",
            "sound-recorder.properties"
    );

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> createWindow(args, new PropertiesConfig(CONFIG_PATH)).setVisible(true));
    }

    public static SoundRecorderFrame createWindow(final String[] args, final Config config) {
        final SoundClip clip;
        if (args == null || args.length == 0) {
            clip = new SoundClip();
        } else {
            clip = new SoundClip(Paths.get(args[0]));
        }
        return new SoundRecorderFrame(clip, config);
    }
}
