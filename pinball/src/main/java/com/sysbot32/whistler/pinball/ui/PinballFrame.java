package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.config.Config;
import com.sysbot32.whistler.pinball.model.PinballWorld;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

/**
 * Desktop shell: perspective table view, Game menu, and live key bindings for
 * left/right flippers and the plunger.
 */
public final class PinballFrame extends JFrame {
    public static final String TITLE = "3D Pinball";
    private static final int TICK_MS = 16;
    private static final double DT = TICK_MS / 1000.0;

    private final Config config;
    private final PinballWorld world;
    private final PinballView view;
    private final Timer timer;
    private boolean plungerHeld;

    public PinballFrame(final Config config) {
        super(TITLE);
        this.config = Objects.requireNonNull(config, "config");
        this.world = new PinballWorld();
        this.view = new PinballView(this.world);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLayout(new BorderLayout());
        this.setJMenuBar(this.createMenuBar());
        this.add(this.view, BorderLayout.CENTER);

        this.view.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                PinballFrame.this.onKeyPressed(e);
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                PinballFrame.this.onKeyReleased(e);
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                PinballFrame.this.exit();
            }

            @Override
            public void windowOpened(final WindowEvent e) {
                PinballFrame.this.view.requestFocusInWindow();
            }
        });

        this.timer = new Timer(TICK_MS, e -> this.onTick());
        this.timer.start();

        this.pack();
        this.setMinimumSize(this.getPreferredSize());
        this.setLocationRelativeTo(null);
        this.view.requestFocusInWindow();
    }

    public PinballWorld getWorld() {
        return this.world;
    }

    public PinballView getView() {
        return this.view;
    }

    private void onTick() {
        if (this.plungerHeld) {
            this.world.pullPlunger(DT);
        }
        this.world.tick(DT);
        this.view.repaint();
    }

    private void onKeyPressed(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            this.exit();
            return;
        }
        if (PinballControls.isPlunger(e.getKeyCode())) {
            this.plungerHeld = true;
        }
        PinballControls.keyPressed(this.world, e.getKeyCode(), e.getKeyLocation());
        if (PinballControls.isNewGame(e.getKeyCode())) {
            this.plungerHeld = false;
        }
    }

    private void onKeyReleased(final KeyEvent e) {
        if (PinballControls.isPlunger(e.getKeyCode()) && this.plungerHeld) {
            this.plungerHeld = false;
            this.world.releasePlunger();
        }
        PinballControls.keyReleased(this.world, e.getKeyCode(), e.getKeyLocation());
    }

    private void newGame() {
        this.plungerHeld = false;
        this.world.reset();
        this.view.requestFocusInWindow();
        this.view.repaint();
    }

    private void exit() {
        this.timer.stop();
        this.config.save();
        this.dispose();
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);

        final JMenuItem newItem = new JMenuItem("New Game", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        newItem.addActionListener(e -> this.newGame());

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());

        gameMenu.add(newItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutItem = new JMenuItem("About 3D Pinball...", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                TITLE + "\nWhistler — classic 3D pinball table\n\n"
                        + "Left flipper: Z, Left Arrow, Left Shift\n"
                        + "Right flipper: /, Right Arrow, Right Shift\n"
                        + "Plunger: hold Space, Down, or Enter, then release\n"
                        + "New game: F2",
                "About " + TITLE,
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }
}
