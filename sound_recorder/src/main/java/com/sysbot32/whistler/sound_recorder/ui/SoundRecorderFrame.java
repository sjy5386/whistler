package com.sysbot32.whistler.sound_recorder.ui;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.sound_recorder.audio.LineTransport;
import com.sysbot32.whistler.sound_recorder.model.SoundClip;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Classic Sound Recorder (녹음기) shell: waveform, Position/Length, tape-deck transport, File menu.
 */
public class SoundRecorderFrame extends JFrame {
    public static final String UNTITLED = "Sound";
    public static final String TITLE = "Sound Recorder";

    private final SoundClip clip;
    private final Config config;
    private final LineTransport transport = new LineTransport();
    private final WaveformPanel waveformPanel;
    private final JLabel positionLabel = new JLabel();
    private final JLabel lengthLabel = new JLabel();
    private final JButton seekToStartButton = transportButton(
            "seekToStart", TransportIcons.seekToStart(), "Seek to Start");
    private final JButton seekToEndButton = transportButton(
            "seekToEnd", TransportIcons.seekToEnd(), "Seek to End");
    private final JButton playButton = transportButton("play", TransportIcons.play(), "Play");
    private final JButton stopButton = transportButton("stop", TransportIcons.stop(), "Stop");
    private final JButton recordButton = transportButton("record", TransportIcons.record(), "Record");
    private final JSlider positionSlider = new JSlider(0, 1000, 0);
    private final Timer uiTimer;
    private long lastTickNanos;
    private boolean mixerDrivingPlayhead;
    private boolean sliderUpdating;

    public SoundRecorderFrame(final SoundClip clip, final Config config) {
        this.clip = Objects.requireNonNull(clip, "clip");
        this.config = Objects.requireNonNull(config, "config");

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setResizable(false);

        this.waveformPanel = new WaveformPanel(this.clip);
        this.waveformPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        this.positionLabel.setName("position");
        this.lengthLabel.setName("length");
        this.positionLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        this.lengthLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        this.positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.lengthLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.positionSlider.setName("positionSlider");
        this.positionSlider.setFocusable(false);
        this.positionSlider.addChangeListener(e -> this.onSliderMoved());

        this.seekToStartButton.addActionListener(e -> this.seekToStart());
        this.seekToEndButton.addActionListener(e -> this.seekToEnd());
        this.playButton.addActionListener(e -> this.play());
        this.stopButton.addActionListener(e -> this.stopTransport());
        this.recordButton.addActionListener(e -> this.record());

        final JPanel positionBox = sunkenBox(this.positionLabel);
        final JPanel lengthBox = sunkenBox(this.lengthLabel);
        positionBox.setPreferredSize(new Dimension(78, 44));
        lengthBox.setPreferredSize(new Dimension(78, 44));

        final JPanel deck = new JPanel(new BorderLayout(6, 0));
        deck.add(positionBox, BorderLayout.WEST);
        deck.add(this.waveformPanel, BorderLayout.CENTER);
        deck.add(lengthBox, BorderLayout.EAST);

        final JPanel buttons = new JPanel(new GridLayout(1, 5, 4, 0));
        buttons.add(this.seekToStartButton);
        buttons.add(this.seekToEndButton);
        buttons.add(this.playButton);
        buttons.add(this.stopButton);
        buttons.add(this.recordButton);

        final JPanel south = new JPanel(new BorderLayout(0, 2));
        south.add(this.positionSlider, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);

        final JPanel content = (JPanel) this.getContentPane();
        content.setLayout(new BorderLayout(0, 4));
        content.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        content.add(deck, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);

        this.setJMenuBar(this.createMenuBar());

        this.uiTimer = new Timer(50, e -> this.onUiTick());
        this.uiTimer.setRepeats(true);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                SoundRecorderFrame.this.exit();
            }
        });

        this.refreshView();
        this.pack();
        this.setSize(new Dimension(Math.max(308, this.getWidth()), Math.max(164, this.getHeight())));
        this.validate();
    }

    public WaveformPanel getWaveformPanel() {
        return this.waveformPanel;
    }

    public JLabel getPositionLabel() {
        return this.positionLabel;
    }

    public JLabel getLengthLabel() {
        return this.lengthLabel;
    }

    public JButton getSeekToStartButton() {
        return this.seekToStartButton;
    }

    public JButton getSeekToEndButton() {
        return this.seekToEndButton;
    }

    public JButton getPlayButton() {
        return this.playButton;
    }

    public JButton getStopButton() {
        return this.stopButton;
    }

    public JButton getRecordButton() {
        return this.recordButton;
    }

    public JSlider getPositionSlider() {
        return this.positionSlider;
    }

    public SoundClip getClip() {
        return this.clip;
    }

    public String describeChrome() {
        return "title=" + this.getTitle()
                + "\nseekToStart=" + this.seekToStartButton.getName()
                + " tooltip=" + this.seekToStartButton.getToolTipText()
                + "\nseekToEnd=" + this.seekToEndButton.getName()
                + " tooltip=" + this.seekToEndButton.getToolTipText()
                + "\nplay=" + this.playButton.getName()
                + " tooltip=" + this.playButton.getToolTipText()
                + "\nstop=" + this.stopButton.getName()
                + " tooltip=" + this.stopButton.getToolTipText()
                + "\nrecord=" + this.recordButton.getName()
                + " tooltip=" + this.recordButton.getToolTipText()
                + "\nwaveform=" + this.waveformPanel.getName()
                + " size=" + this.waveformPanel.getWidth() + "x" + this.waveformPanel.getHeight()
                + " preferred=" + this.waveformPanel.getPreferredSize().width
                + "x" + this.waveformPanel.getPreferredSize().height
                + "\nposition=" + this.positionLabel.getText()
                + "\nlength=" + this.lengthLabel.getText()
                + "\nslider=" + this.positionSlider.getName()
                + "\n";
    }

    public void newDocument() {
        this.stopTransport();
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.clip.createNew();
        this.refreshView();
    }

    public boolean openPath(final Path path) {
        this.stopTransport();
        if (!this.confirmDiscardIfNeeded()) {
            return false;
        }
        try {
            this.clip.open(path);
            this.refreshView();
            return true;
        } catch (final IOException ex) {
            this.showError("Cannot open file.", ex);
            return false;
        }
    }

    public boolean savePath(final Path path) {
        this.stopTransport();
        try {
            this.clip.save(path);
            this.refreshView();
            return true;
        } catch (final IOException ex) {
            this.showError("Cannot save file.", ex);
            return false;
        }
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        menuBar.add(this.createFileMenu());
        menuBar.add(this.createHelpMenu());
        return menuBar;
    }

    private JMenu createFileMenu() {
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem newMenuItem = menuItem("New", KeyEvent.VK_N, KeyEvent.VK_N);
        final JMenuItem openMenuItem = menuItem("Open...", KeyEvent.VK_O, KeyEvent.VK_O);
        final JMenuItem saveMenuItem = menuItem("Save", KeyEvent.VK_S, KeyEvent.VK_S);
        final JMenuItem saveAsMenuItem = menuItem("Save As...", KeyEvent.VK_A, -1);
        final JMenuItem revertMenuItem = menuItem("Revert...", KeyEvent.VK_R, -1);
        final JMenuItem propertiesMenuItem = menuItem("Properties", KeyEvent.VK_P, -1);
        final JMenuItem exitMenuItem = menuItem("Exit", KeyEvent.VK_X, -1);

        newMenuItem.addActionListener(e -> this.newDocument());
        openMenuItem.addActionListener(e -> this.openDocument());
        saveMenuItem.addActionListener(e -> this.saveDocument(false));
        saveAsMenuItem.addActionListener(e -> this.saveDocument(true));
        revertMenuItem.addActionListener(e -> this.revert());
        propertiesMenuItem.addActionListener(e -> this.showProperties());
        exitMenuItem.addActionListener(e -> this.exit());

        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(revertMenuItem);
        fileMenu.add(propertiesMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        return fileMenu;
    }

    private JMenu createHelpMenu() {
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutMenuItem = new JMenuItem("About Sound Recorder");
        aboutMenuItem.setMnemonic(KeyEvent.VK_A);
        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Whistler Sound Recorder\nA classic Windows Sound Recorder recreation.",
                "About Sound Recorder",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutMenuItem);
        return helpMenu;
    }

    private void openDocument() {
        this.stopTransport();
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        final JFileChooser chooser = waveChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        this.openPath(Paths.get(chooser.getSelectedFile().toURI()));
    }

    private boolean saveDocument(final boolean saveAs) {
        this.stopTransport();
        Path path = this.clip.getPath();
        if (saveAs || Objects.isNull(path)) {
            final JFileChooser chooser = waveChooser();
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            path = Paths.get(chooser.getSelectedFile().toURI());
            if (!path.getFileName().toString().contains(".")) {
                path = path.resolveSibling(path.getFileName() + ".wav");
            }
        }
        return this.savePath(path);
    }

    private void exit() {
        this.stopTransport();
        if (!this.confirmDiscardIfNeeded()) {
            return;
        }
        this.uiTimer.stop();
        this.dispose();
    }

    private boolean confirmDiscardIfNeeded() {
        if (!this.clip.isDirty()) {
            return true;
        }
        final String name = Objects.isNull(this.clip.getPath())
                ? UNTITLED
                : this.clip.getPath().getFileName().toString();
        final int choice = JOptionPane.showConfirmDialog(
                this,
                "Do you want to save changes to " + name + "?",
                TITLE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            return this.saveDocument(false);
        }
        return true;
    }

    private void revert() {
        this.stopTransport();
        final Path path = this.clip.getPath();
        if (Objects.isNull(path) || !this.clip.isDirty()) {
            return;
        }
        try {
            this.clip.open(path);
            this.refreshView();
        } catch (final IOException ex) {
            this.showError("Cannot revert file.", ex);
        }
    }

    private void showProperties() {
        final String path = Objects.isNull(this.clip.getPath())
                ? UNTITLED
                : this.clip.getPath().toString();
        JOptionPane.showMessageDialog(
                this,
                "File: " + path
                        + "\nLength: " + SoundClip.formatTime(this.clip.getLengthSeconds())
                        + "\nAudio Format: PCM "
                        + Math.round(this.clip.getSampleRate()) + " Hz, "
                        + this.clip.getChannels() + " channel(s), 16-bit",
                "Properties for " + (Objects.isNull(this.clip.getPath())
                        ? UNTITLED
                        : this.clip.getPath().getFileName()),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void onSliderMoved() {
        if (this.sliderUpdating) {
            return;
        }
        this.clip.seekTo(this.positionSlider.getValue() / 1000.0);
        this.positionLabel.setText(timeBox("Position:", this.clip.getPositionSeconds()));
        this.waveformPanel.repaint();
    }

    private void seekToStart() {
        this.clip.seekToStart();
        this.refreshView();
    }

    private void seekToEnd() {
        this.clip.seekToEnd();
        this.refreshView();
    }

    private void play() {
        if (this.clip.isRecording()) {
            this.stopTransport();
        }
        this.clip.play();
        if (!this.clip.isPlaying()) {
            this.refreshView();
            return;
        }
        this.mixerDrivingPlayhead = this.transport.startPlayback(this.clip);
        this.lastTickNanos = System.nanoTime();
        this.uiTimer.start();
        this.refreshView();
    }

    private void record() {
        if (this.clip.isPlaying()) {
            this.stopTransport();
        }
        this.clip.record();
        this.transport.startCapture(this.clip);
        this.lastTickNanos = System.nanoTime();
        this.uiTimer.start();
        this.refreshView();
    }

    private void stopTransport() {
        this.transport.stop();
        this.clip.stop();
        this.mixerDrivingPlayhead = false;
        this.uiTimer.stop();
        this.refreshView();
    }

    private void onUiTick() {
        final long now = System.nanoTime();
        final double elapsed = (now - this.lastTickNanos) / 1_000_000_000.0;
        this.lastTickNanos = now;
        if (!this.mixerDrivingPlayhead && this.clip.isPlaying()) {
            this.clip.tick(elapsed);
        }
        this.refreshView();
        if (!this.clip.isPlaying() && !this.clip.isRecording()) {
            this.transport.stop();
            this.uiTimer.stop();
            this.mixerDrivingPlayhead = false;
        }
    }

    private void refreshView() {
        this.positionLabel.setText(timeBox("Position:", this.clip.getPositionSeconds()));
        this.lengthLabel.setText(timeBox("Length:", this.clip.getLengthSeconds()));
        this.sliderUpdating = true;
        try {
            final int max = Math.max(1, (int) Math.round(this.clip.getLengthSeconds() * 1000.0));
            this.positionSlider.setMaximum(max);
            this.positionSlider.setValue((int) Math.round(this.clip.getPositionSeconds() * 1000.0));
            this.positionSlider.setEnabled(this.clip.frameCount() > 0 && !this.clip.isRecording());
        } finally {
            this.sliderUpdating = false;
        }
        this.waveformPanel.repaint();
        this.refreshTitle();
        this.updateTransportEnabled();
    }

    private void updateTransportEnabled() {
        final boolean hasClip = this.clip.frameCount() > 0;
        this.playButton.setEnabled(!this.clip.isRecording() && hasClip);
        this.recordButton.setEnabled(!this.clip.isPlaying());
        this.seekToStartButton.setEnabled(!this.clip.isRecording());
        this.seekToEndButton.setEnabled(!this.clip.isRecording() && hasClip);
    }

    private void refreshTitle() {
        final String name = Objects.isNull(this.clip.getPath())
                ? UNTITLED
                : this.clip.getPath().getFileName().toString();
        final String dirty = this.clip.isDirty() ? "*" : "";
        this.setTitle(dirty + name + " - " + TITLE);
    }

    private void showError(final String message, final Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + ex.getMessage(),
                TITLE,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private static JButton transportButton(final String name, final Icon icon, final String tooltip) {
        final JButton button = new JButton(icon);
        button.setName(name);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setPreferredSize(new Dimension(48, 24));
        return button;
    }

    private static String timeBox(final String caption, final double seconds) {
        return "<html><center>" + caption + "<br>" + SoundClip.formatTime(seconds) + "</center></html>";
    }

    private static JPanel sunkenBox(final JLabel label) {
        final JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        box.add(label, BorderLayout.CENTER);
        return box;
    }

    private static JMenuItem menuItem(final String text, final int mnemonic, final int acceleratorKey) {
        final JMenuItem item = new JMenuItem(text);
        item.setMnemonic(mnemonic);
        if (acceleratorKey >= 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, InputEvent.CTRL_DOWN_MASK));
        }
        return item;
    }

    private static JFileChooser waveChooser() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Wave Sound (*.wav)", "wav"));
        return chooser;
    }
}
