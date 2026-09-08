package com.sysbot32.whistler.sound_recorder;

import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.sound_recorder.ui.SoundRecorderFrame;
import com.sysbot32.whistler.sound_recorder.ui.WaveformPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundRecorderLaunchTest {
    @Test
    void applicationMainClassExists() throws Exception {
        final Method main = SoundRecorderApplication.class.getMethod("main", String[].class);
        assertTrue(Modifier.isStatic(main.getModifiers()));
    }

    @Test
    void launchDumpsChromeTwice(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        final Path reports = Path.of("build");
        Files.createDirectories(reports);
        final StringBuilder dump = new StringBuilder();
        for (int i = 1; i <= 2; i++) {
            final int run = i;
            runEdt(() -> {
                try {
                    final SoundRecorderFrame frame = SoundRecorderApplication.createWindow(
                            new String[0],
                            new PropertiesConfig(tempDir.resolve("launch-" + run + ".properties"))
                    );
                    dump.append("run=").append(run).append('\n').append(frame.describeChrome()).append('\n');
                    assertTrue(
                            frame.getTitle().contains("Sound Recorder") || frame.getTitle().contains("녹음기"),
                            frame.getTitle()
                    );
                    assertTrue(frame.getWaveformPanel().getPreferredSize().width > 0);
                    assertTrue(frame.getSeekToStartButton().getToolTipText().contains("Seek to Start"));
                    assertTrue(frame.getSeekToEndButton().getToolTipText().contains("Seek to End"));
                    assertTrue(frame.getPlayButton().getToolTipText().contains("Play"));
                    assertTrue(frame.getStopButton().getToolTipText().contains("Stop"));
                    assertTrue(frame.getRecordButton().getToolTipText().contains("Record"));
                    assertTrue(frame.getPositionLabel().getText().contains("Position:"));
                    assertTrue(frame.getLengthLabel().getText().contains("Length:"));
                    if (run == 1) {
                        final BufferedImage image = writeScreenshot(
                                frame,
                                reports.resolve("sound-recorder-frame.png")
                        );
                        final int scope = WaveformPanel.BACKGROUND.getRGB() & 0x00FFFFFF;
                        int scopePixels = 0;
                        for (int x = 0; x < image.getWidth(); x += 2) {
                            for (int y = 0; y < image.getHeight(); y += 2) {
                                if ((image.getRGB(x, y) & 0x00FFFFFF) == scope) {
                                    scopePixels++;
                                }
                            }
                        }
                        assertTrue(scopePixels > 20, "screenshot missing waveform surface, scopePixels=" + scopePixels);
                    }
                    frame.dispose();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.writeString(reports.resolve("sound-recorder-launch.txt"), dump.toString());
        assertTrue(dump.toString().contains("title="));
        assertNotNull(dump);
    }

    private static BufferedImage writeScreenshot(final SoundRecorderFrame frame, final Path path) throws IOException {
        final int w = Math.max(380, Math.max(frame.getWidth(), frame.getPreferredSize().width));
        final int h = Math.max(210, Math.max(frame.getHeight(), frame.getPreferredSize().height));
        frame.setSize(w, h);
        frame.validate();
        frame.doLayout();
        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(frame.getContentPane().getBackground());
            g.fillRect(0, 0, w, h);
            final JMenuBar bar = frame.getJMenuBar();
            int y = 0;
            if (bar != null) {
                final int barHeight = Math.max(24, bar.getPreferredSize().height);
                bar.setSize(w, barHeight);
                bar.doLayout();
                final Graphics gBar = g.create(0, 0, w, barHeight);
                bar.printAll(gBar);
                gBar.dispose();
                y = barHeight;
            }
            final Container content = frame.getContentPane();
            content.setSize(w, Math.max(1, h - y));
            content.doLayout();
            final Graphics gContent = g.create(0, y, w, Math.max(1, h - y));
            content.printAll(gContent);
            gContent.dispose();
        } finally {
            g.dispose();
        }
        ImageIO.write(image, "png", path.toFile());
        return image;
    }

    private static void assumeDisplayOrConstructs() {
        if (GraphicsEnvironment.isHeadless()) {
            try {
                final JFrame probe = new JFrame("probe");
                probe.dispose();
            } catch (final Throwable t) {
                org.junit.jupiter.api.Assumptions.assumeFalse(true, "headless frame construction failed: " + t);
            }
        }
    }

    private static void runEdt(final Runnable action) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeAndWait(action);
    }
}
