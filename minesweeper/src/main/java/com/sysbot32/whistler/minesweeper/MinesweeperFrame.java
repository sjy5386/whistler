package com.sysbot32.whistler.minesweeper;

import com.sysbot32.whistler.config.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Optional;

/**
 * Classic XP-style Minesweeper shell: counters, face button, cell grid, menus.
 */
public class MinesweeperFrame extends JFrame {
    private static final String TITLE = "Minesweeper";
    private static final String CONFIG_MODE = "difficulty";
    private static final String CONFIG_CUSTOM_ROWS = "custom.rows";
    private static final String CONFIG_CUSTOM_COLS = "custom.cols";
    private static final String CONFIG_CUSTOM_MINES = "custom.mines";
    private static final String CONFIG_MARKS = "marks";
    private static final Color PANEL_GRAY = new Color(0xC0, 0xC0, 0xC0);

    private static final Color[] NUMBER_COLORS = {
            PANEL_GRAY,
            new Color(0x00, 0x00, 0xFF),
            new Color(0x00, 0x80, 0x00),
            new Color(0xFF, 0x00, 0x00),
            new Color(0x00, 0x00, 0x80),
            new Color(0x80, 0x00, 0x00),
            new Color(0x00, 0x80, 0x80),
            new Color(0x00, 0x00, 0x00),
            new Color(0x80, 0x80, 0x80)
    };

    private final Config config;
    private final BestTimes bestTimes;
    private BoardSpec boardSpec;
    private Minesweeper game;

    private final LedCounter mineCounter = new LedCounter();
    private final LedCounter timerCounter = new LedCounter();
    private final JButton faceButton = new JButton("🙂");
    private final JPanel boardPanel = new JPanel();
    private final JPanel outerPanel = new JPanel(new BorderLayout(0, 6));
    private CellButton[][] cellButtons;

    private JRadioButtonMenuItem beginnerItem;
    private JRadioButtonMenuItem intermediateItem;
    private JRadioButtonMenuItem expertItem;
    private JRadioButtonMenuItem customItem;
    private JCheckBoxMenuItem marksMenuItem;

    private final Timer swingTimer;
    private int elapsedSeconds;
    private boolean timerRunning;
    private boolean mouseDownOnCell;
    private boolean marksEnabled;

    /** Chord / multi-button tracking (classic both-button click). */
    private boolean leftButtonDown;
    private boolean rightButtonDown;
    private boolean chordArmed;
    private boolean chordConsumed;
    private int pressRow = -1;
    private int pressCol = -1;
    private int previewRow = -1;
    private int previewCol = -1;

    public MinesweeperFrame(final Config config) {
        super(TITLE);
        this.config = Objects.requireNonNull(config, "config");
        this.bestTimes = new BestTimes(config);
        this.marksEnabled = this.loadMarksEnabled();
        this.boardSpec = this.loadBoardSpec();
        this.game = new Minesweeper(this.boardSpec.getRows(), this.boardSpec.getCols(), this.boardSpec.getMines());

        this.swingTimer = new Timer(1000, e -> {
            if (this.elapsedSeconds < 999) {
                this.elapsedSeconds++;
                this.timerCounter.setValue(this.elapsedSeconds);
            }
        });
        this.swingTimer.setRepeats(true);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.getContentPane().setBackground(PANEL_GRAY);
        this.setLayout(new BorderLayout());

        this.setJMenuBar(this.createMenuBar());
        this.buildChrome();
        this.rebuildBoard();
        this.refreshUi();

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                MinesweeperFrame.this.exit();
            }
        });

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(null);
    }

    private void buildChrome() {
        final JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(PANEL_GRAY);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                new EmptyBorder(4, 6, 4, 6)
        ));

        this.faceButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        this.faceButton.setFocusable(false);
        this.faceButton.setMargin(new Insets(2, 6, 2, 6));
        this.faceButton.setBackground(PANEL_GRAY);
        this.faceButton.addActionListener(e -> this.newGame(this.boardSpec));

        final JPanel faceWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        faceWrap.setOpaque(false);
        faceWrap.add(this.faceButton);

        header.add(this.mineCounter, BorderLayout.WEST);
        header.add(faceWrap, BorderLayout.CENTER);
        header.add(this.timerCounter, BorderLayout.EAST);

        this.boardPanel.setBackground(PANEL_GRAY);
        this.boardPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        this.outerPanel.setBackground(PANEL_GRAY);
        this.outerPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        this.outerPanel.add(header, BorderLayout.NORTH);
        this.outerPanel.add(this.boardPanel, BorderLayout.CENTER);

        this.add(this.outerPanel, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        final JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);

        final JMenuItem newItem = new JMenuItem("New", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        newItem.addActionListener(e -> this.newGame(this.boardSpec));

        this.beginnerItem = new JRadioButtonMenuItem(Difficulty.BEGINNER.getDisplayName());
        this.intermediateItem = new JRadioButtonMenuItem(Difficulty.INTERMEDIATE.getDisplayName());
        this.expertItem = new JRadioButtonMenuItem(Difficulty.EXPERT.getDisplayName());
        this.customItem = new JRadioButtonMenuItem("Custom...");
        final ButtonGroup group = new ButtonGroup();
        group.add(this.beginnerItem);
        group.add(this.intermediateItem);
        group.add(this.expertItem);
        group.add(this.customItem);

        this.beginnerItem.addActionListener(e -> this.newGame(BoardSpec.of(Difficulty.BEGINNER)));
        this.intermediateItem.addActionListener(e -> this.newGame(BoardSpec.of(Difficulty.INTERMEDIATE)));
        this.expertItem.addActionListener(e -> this.newGame(BoardSpec.of(Difficulty.EXPERT)));
        this.customItem.addActionListener(e -> this.promptCustomField());

        this.marksMenuItem = new JCheckBoxMenuItem("Marks (?)", this.marksEnabled);
        this.marksMenuItem.setMnemonic(KeyEvent.VK_M);
        this.marksMenuItem.addActionListener(e -> this.setMarksEnabled(this.marksMenuItem.isSelected()));

        final JMenuItem bestTimesItem = new JMenuItem("Best Times...", KeyEvent.VK_T);
        bestTimesItem.addActionListener(e -> BestTimesDialog.show(this, this.bestTimes));

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());

        gameMenu.add(newItem);
        gameMenu.addSeparator();
        gameMenu.add(this.beginnerItem);
        gameMenu.add(this.intermediateItem);
        gameMenu.add(this.expertItem);
        gameMenu.add(this.customItem);
        gameMenu.addSeparator();
        gameMenu.add(this.marksMenuItem);
        gameMenu.add(bestTimesItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutItem = new JMenuItem("About Minesweeper...", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Minesweeper\nWhistler — Windows XP classic reimplementation\n\n"
                        + "Left-click: open\nRight-click: flag"
                        + (this.marksEnabled ? " / ? / clear" : "")
                        + "\nBoth buttons or middle-click: chord\nF2: new game",
                "About Minesweeper",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        this.syncDifficultyMenu();
        return menuBar;
    }

    private void promptCustomField() {
        final Optional<BoardSpec> result = CustomFieldDialog.show(this, this.boardSpec);
        if (result.isEmpty()) {
            this.syncDifficultyMenu();
            return;
        }
        this.newGame(result.get());
    }

    private void rebuildBoard() {
        this.boardPanel.removeAll();
        this.boardPanel.setLayout(new GridLayout(this.game.getRows(), this.game.getCols(), 0, 0));
        this.cellButtons = new CellButton[this.game.getRows()][this.game.getCols()];
        for (int r = 0; r < this.game.getRows(); r++) {
            for (int c = 0; c < this.game.getCols(); c++) {
                final CellButton button = new CellButton(r, c);
                this.cellButtons[r][c] = button;
                this.boardPanel.add(button);
            }
        }
        this.boardPanel.revalidate();
        this.boardPanel.repaint();
    }

    private void newGame(final BoardSpec boardSpec) {
        this.stopTimer(true);
        this.boardSpec = boardSpec;
        this.saveBoardSpec();
        this.game = new Minesweeper(boardSpec.getRows(), boardSpec.getCols(), boardSpec.getMines());
        this.resetPointerState();
        this.rebuildBoard();
        this.refreshUi();
        this.pack();
        this.syncDifficultyMenu();
    }

    private void syncDifficultyMenu() {
        if (this.beginnerItem == null) {
            return;
        }
        if (this.boardSpec.isCustom()) {
            this.customItem.setSelected(true);
            return;
        }
        switch (this.boardSpec.getDifficulty()) {
            case INTERMEDIATE -> this.intermediateItem.setSelected(true);
            case EXPERT -> this.expertItem.setSelected(true);
            default -> this.beginnerItem.setSelected(true);
        }
    }

    private void onCellPressed(final int row, final int col, final MouseEvent e) {
        if (this.game.getStatus() == GameStatus.WON || this.game.getStatus() == GameStatus.LOST) {
            return;
        }
        this.pressRow = row;
        this.pressCol = col;

        if (SwingUtilities.isMiddleMouseButton(e)) {
            this.mouseDownOnCell = true;
            this.chordArmed = true;
            this.showChordPreview(row, col);
            this.updateFace();
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            this.leftButtonDown = true;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            this.rightButtonDown = true;
        }
        // Some platforms deliver one event with both buttons; also track sequential press.
        if (this.leftButtonDown && this.rightButtonDown) {
            this.chordArmed = true;
        }
        if (this.leftButtonDown || this.chordArmed) {
            this.mouseDownOnCell = true;
            if (this.chordArmed) {
                this.showChordPreview(row, col);
            }
            this.updateFace();
        }
    }

    private void onCellReleased(final int row, final int col, final MouseEvent e) {
        if (this.game.getStatus() == GameStatus.WON || this.game.getStatus() == GameStatus.LOST) {
            this.resetPointerState();
            this.updateFace();
            return;
        }

        final boolean over = this.isOverCell(e);
        final int targetRow = over ? row : this.pressRow;
        final int targetCol = over ? col : this.pressCol;

        if (SwingUtilities.isMiddleMouseButton(e)) {
            if (over && this.chordArmed) {
                this.applyChord(targetRow, targetCol);
            }
            this.resetPointerState();
            this.refreshUi();
            return;
        }

        if (SwingUtilities.isLeftMouseButton(e)) {
            this.leftButtonDown = false;
        }
        if (SwingUtilities.isRightMouseButton(e)) {
            this.rightButtonDown = false;
        }

        if (this.chordArmed) {
            if (over && !this.chordConsumed) {
                this.applyChord(targetRow, targetCol);
                this.chordConsumed = true;
            }
            if (!this.leftButtonDown && !this.rightButtonDown) {
                this.resetPointerState();
                this.refreshUi();
            } else {
                this.mouseDownOnCell = this.leftButtonDown || this.rightButtonDown;
                this.clearChordPreview();
                this.updateFace();
            }
            return;
        }

        // Single-button actions only when a chord was never armed.
        if (over && SwingUtilities.isLeftMouseButton(e)) {
            this.applyOpen(targetRow, targetCol);
        } else if (over && SwingUtilities.isRightMouseButton(e)) {
            if (this.game.toggleFlag(targetRow, targetCol, this.marksEnabled)) {
                this.refreshUi();
            } else {
                this.updateFace();
            }
        }

        if (!this.leftButtonDown && !this.rightButtonDown) {
            this.resetPointerState();
            this.updateFace();
        }
    }

    private void onCellExited() {
        if (this.leftButtonDown || this.rightButtonDown || this.chordArmed) {
            this.resetPointerState();
            this.refreshUi();
        }
    }

    private void applyOpen(final int row, final int col) {
        final boolean changed = this.game.open(row, col);
        if (changed) {
            this.maybeStartTimer();
            this.refreshUi();
            this.afterMove();
        } else {
            this.updateFace();
        }
    }

    private void applyChord(final int row, final int col) {
        final boolean changed = this.game.chord(row, col);
        if (changed) {
            this.maybeStartTimer();
            this.refreshUi();
            this.afterMove();
        } else {
            this.clearChordPreview();
            this.updateFace();
        }
    }

    private void afterMove() {
        if (this.game.getStatus() == GameStatus.WON) {
            this.stopTimer(false);
            this.handleWin();
        } else if (this.game.getStatus() == GameStatus.LOST) {
            this.stopTimer(false);
        }
    }

    private void showChordPreview(final int row, final int col) {
        this.clearChordPreview();
        this.previewRow = row;
        this.previewCol = col;
        if (this.cellButtons == null || !this.game.isInBounds(row, col)) {
            return;
        }
        // Depress covered, unflagged neighbors around the chording cell (XP-style hint).
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                final int nr = row + dr;
                final int nc = col + dc;
                if (!this.game.isInBounds(nr, nc)) {
                    continue;
                }
                final Cell neighbor = this.game.getCell(nr, nc);
                if (!neighbor.isOpen() && !neighbor.isFlagged()) {
                    this.cellButtons[nr][nc].setPressedPreview(true);
                }
            }
        }
    }

    private void clearChordPreview() {
        if (this.previewRow < 0 || this.cellButtons == null) {
            this.previewRow = -1;
            this.previewCol = -1;
            return;
        }
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                final int nr = this.previewRow + dr;
                final int nc = this.previewCol + dc;
                if (this.game.isInBounds(nr, nc)) {
                    this.cellButtons[nr][nc].setPressedPreview(false);
                }
            }
        }
        this.previewRow = -1;
        this.previewCol = -1;
    }

    private void resetPointerState() {
        this.leftButtonDown = false;
        this.rightButtonDown = false;
        this.chordArmed = false;
        this.chordConsumed = false;
        this.mouseDownOnCell = false;
        this.pressRow = -1;
        this.pressCol = -1;
        this.clearChordPreview();
    }

    private void setMarksEnabled(final boolean enabled) {
        this.marksEnabled = enabled;
        if (!enabled) {
            this.game.clearAllQuestionMarks();
            this.refreshUi();
        }
        this.saveMarksEnabled();
    }

    private void handleWin() {
        if (!this.boardSpec.tracksBestTime()) {
            return;
        }
        final Difficulty difficulty = this.boardSpec.getDifficulty();
        if (!this.bestTimes.isRecord(difficulty, this.elapsedSeconds)) {
            return;
        }
        final String name = JOptionPane.showInputDialog(
                this,
                "You have the fastest time for " + difficulty.getDisplayName() + " level.\n"
                        + "Please enter your name.",
                "Congratulations",
                JOptionPane.QUESTION_MESSAGE
        );
        this.bestTimes.setRecord(
                difficulty,
                this.elapsedSeconds,
                name == null ? BestTimes.DEFAULT_NAME : name
        );
        BestTimesDialog.show(this, this.bestTimes);
    }

    private boolean isOverCell(final MouseEvent e) {
        final Component source = (Component) e.getSource();
        final Point p = e.getPoint();
        return p.x >= 0 && p.y >= 0 && p.x < source.getWidth() && p.y < source.getHeight();
    }

    private void maybeStartTimer() {
        if (!this.timerRunning && this.game.isMinesPlaced()) {
            this.timerRunning = true;
            this.swingTimer.start();
        }
    }

    private void stopTimer(final boolean resetSeconds) {
        this.swingTimer.stop();
        this.timerRunning = false;
        if (resetSeconds) {
            this.elapsedSeconds = 0;
            this.timerCounter.setValue(0);
        }
    }

    private void refreshUi() {
        this.mineCounter.setValue(this.game.getRemainingMines());
        if (!this.timerRunning) {
            this.timerCounter.setValue(this.elapsedSeconds);
        }
        for (int r = 0; r < this.game.getRows(); r++) {
            for (int c = 0; c < this.game.getCols(); c++) {
                this.cellButtons[r][c].refresh();
            }
        }
        this.updateFace();
    }

    private void updateFace() {
        if (this.game.getStatus() == GameStatus.WON) {
            this.faceButton.setText("😎");
        } else if (this.game.getStatus() == GameStatus.LOST) {
            this.faceButton.setText("😵");
        } else if (this.mouseDownOnCell) {
            this.faceButton.setText("😮");
        } else {
            this.faceButton.setText("🙂");
        }
    }

    private BoardSpec loadBoardSpec() {
        final String mode = this.config.get(CONFIG_MODE, Difficulty.BEGINNER.name());
        if ("CUSTOM".equals(mode)) {
            try {
                final int rows = Integer.parseInt(this.config.get(CONFIG_CUSTOM_ROWS, "9"));
                final int cols = Integer.parseInt(this.config.get(CONFIG_CUSTOM_COLS, "9"));
                final int mines = Integer.parseInt(this.config.get(CONFIG_CUSTOM_MINES, "10"));
                return BoardSpec.custom(rows, cols, mines);
            } catch (final RuntimeException e) {
                return BoardSpec.of(Difficulty.BEGINNER);
            }
        }
        try {
            return BoardSpec.of(Difficulty.valueOf(mode));
        } catch (final IllegalArgumentException e) {
            return BoardSpec.of(Difficulty.BEGINNER);
        }
    }

    private void saveBoardSpec() {
        if (this.boardSpec.isCustom()) {
            this.config.set(CONFIG_MODE, "CUSTOM");
            this.config.set(CONFIG_CUSTOM_ROWS, String.valueOf(this.boardSpec.getRows()));
            this.config.set(CONFIG_CUSTOM_COLS, String.valueOf(this.boardSpec.getCols()));
            this.config.set(CONFIG_CUSTOM_MINES, String.valueOf(this.boardSpec.getMines()));
        } else {
            this.config.set(CONFIG_MODE, this.boardSpec.getDifficulty().name());
        }
        this.config.save();
    }

    private boolean loadMarksEnabled() {
        return Boolean.parseBoolean(this.config.get(CONFIG_MARKS, "true"));
    }

    private void saveMarksEnabled() {
        this.config.set(CONFIG_MARKS, String.valueOf(this.marksEnabled));
        this.config.save();
    }

    private void exit() {
        this.stopTimer(false);
        this.saveBoardSpec();
        this.saveMarksEnabled();
        this.dispose();
    }

    private final class CellButton extends JButton {
        private final int row;
        private final int col;
        private boolean pressedPreview;

        private CellButton(final int row, final int col) {
            this.row = row;
            this.col = col;
            this.setPreferredSize(new Dimension(24, 24));
            this.setMargin(new Insets(0, 0, 0, 0));
            this.setFocusable(false);
            this.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            this.setBackground(PANEL_GRAY);
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    MinesweeperFrame.this.onCellPressed(row, col, e);
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    MinesweeperFrame.this.onCellReleased(row, col, e);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    MinesweeperFrame.this.onCellExited();
                }
            });
            this.refresh();
        }

        private void setPressedPreview(final boolean pressed) {
            if (this.pressedPreview == pressed) {
                return;
            }
            this.pressedPreview = pressed;
            this.refresh();
        }

        private void refresh() {
            final Cell cell = MinesweeperFrame.this.game.getCell(this.row, this.col);
            final GameStatus status = MinesweeperFrame.this.game.getStatus();

            if (cell.isOpen()) {
                this.setBorder(BorderFactory.createLineBorder(new Color(0x80, 0x80, 0x80)));
                this.setBackground(PANEL_GRAY);
                if (cell.isMine()) {
                    final boolean exploded = status == GameStatus.LOST
                            && this.row == MinesweeperFrame.this.game.getExplodedRow()
                            && this.col == MinesweeperFrame.this.game.getExplodedCol();
                    this.setBackground(exploded ? Color.RED : PANEL_GRAY);
                    this.setText("✱");
                    this.setForeground(Color.BLACK);
                } else if (cell.getAdjacentMines() > 0) {
                    final int n = cell.getAdjacentMines();
                    this.setText(String.valueOf(n));
                    this.setForeground(NUMBER_COLORS[n]);
                } else {
                    this.setText("");
                }
            } else {
                if (this.pressedPreview) {
                    this.setBorder(BorderFactory.createLineBorder(new Color(0x80, 0x80, 0x80)));
                } else {
                    this.setBorder(BorderFactory.createRaisedBevelBorder());
                }
                this.setBackground(PANEL_GRAY);
                if (cell.isFlagged()) {
                    this.setText("🚩");
                    this.setForeground(Color.RED);
                    if (status == GameStatus.LOST && !cell.isMine()) {
                        this.setText("✗");
                        this.setForeground(Color.RED);
                    }
                } else if (cell.isQuestionMarked()) {
                    this.setText("?");
                    this.setForeground(Color.BLACK);
                } else if (status == GameStatus.LOST && cell.isMine()) {
                    this.setBorder(BorderFactory.createLineBorder(new Color(0x80, 0x80, 0x80)));
                    this.setText("✱");
                    this.setForeground(Color.BLACK);
                } else {
                    this.setText("");
                }
            }
        }
    }
}
