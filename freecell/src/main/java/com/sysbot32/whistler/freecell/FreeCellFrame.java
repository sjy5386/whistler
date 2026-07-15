package com.sysbot32.whistler.freecell;

import com.sysbot32.whistler.config.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Classic FreeCell shell: free cells + foundations on top, eight cascades below.
 * Interaction: click source then destination (original style) and/or drag-and-drop;
 * double-click sends a card to an empty free cell.
 */
public class FreeCellFrame extends JFrame {
    private static final Color FELT_GREEN = new Color(0x00, 0x80, 0x00);
    private static final Color PANEL_EDGE = new Color(0x00, 0x60, 0x00);
    private static final Color CARD_FACE = new Color(0xFF, 0xFF, 0xF0);
    private static final Color CARD_SELECTED = new Color(0xFF, 0xFF, 0x99);
    private static final Color CARD_BORDER = new Color(0x20, 0x20, 0x20);
    private static final Color EMPTY_SLOT = new Color(0x00, 0x6B, 0x00);
    private static final Color RED_INK = new Color(0xC0, 0x00, 0x00);
    private static final Color BLACK_INK = new Color(0x10, 0x10, 0x10);

    static final int CARD_WIDTH = 72;
    static final int CARD_HEIGHT = 100;
    static final int CARD_GAP = 8;
    /** Gap between free-cell group and foundation group (XP logo sits here). */
    static final int TOP_CENTER_GAP = 56;
    static final int CASCADE_OVERLAP = 24;
    static final int KING_BADGE_SIZE = 44;

    private final Config config;
    private final FreeCellOptions options;
    private final FreeCellStatistics statistics;
    private FreeCellGame game;

    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel statusLabel = new JLabel(" ");

    private Selection selection;
    private boolean endDialogShown;
    private boolean gameCounted;

    private JMenuItem restartItem;
    private JMenuItem undoItem;

    public FreeCellFrame(final Config config) {
        super();
        this.config = Objects.requireNonNull(config, "config");
        this.options = new FreeCellOptions(this.config);
        this.statistics = new FreeCellStatistics(this.config);
        // Classic FreeCell: no deal until the player chooses New Game / Select Game.
        this.game = FreeCellGame.emptyBoard();

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.getContentPane().setBackground(FELT_GREEN);
        this.setLayout(new BorderLayout());

        this.setJMenuBar(this.createMenuBar());
        this.boardPanel.setBackground(FELT_GREEN);
        this.boardPanel.setBorder(new EmptyBorder(12, 12, 8, 12));
        this.add(this.boardPanel, BorderLayout.CENTER);

        this.statusLabel.setBorder(new EmptyBorder(0, 12, 8, 12));
        this.statusLabel.setForeground(Color.WHITE);
        this.statusLabel.setOpaque(true);
        this.statusLabel.setBackground(FELT_GREEN);
        this.add(this.statusLabel, BorderLayout.SOUTH);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                FreeCellFrame.this.exit();
            }
        });

        this.refreshStatus();
        this.pack();
        this.setMinimumSize(this.getPreferredSize());
        this.setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        final JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);

        final JMenuItem newItem = new JMenuItem("New Game", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        newItem.addActionListener(e -> this.newGame());

        final JMenuItem selectItem = new JMenuItem("Select Game...", KeyEvent.VK_S);
        selectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        selectItem.addActionListener(e -> this.selectGame());

        this.restartItem = new JMenuItem("Restart Game", KeyEvent.VK_R);
        this.restartItem.addActionListener(e -> this.restartSameGame());

        this.undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        this.undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
        this.undoItem.addActionListener(e -> this.undo());

        final JMenuItem statsItem = new JMenuItem("Statistics...", KeyEvent.VK_T);
        statsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        statsItem.addActionListener(e -> FreeCellStatisticsDialog.show(this, this.statistics));

        final JMenuItem optionsItem = new JMenuItem("Options...", KeyEvent.VK_O);
        optionsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        optionsItem.addActionListener(e -> FreeCellOptionsDialog.show(this, this.options));

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());

        gameMenu.add(newItem);
        gameMenu.add(selectItem);
        gameMenu.add(this.restartItem);
        gameMenu.addSeparator();
        gameMenu.add(statsItem);
        gameMenu.add(optionsItem);
        gameMenu.addSeparator();
        gameMenu.add(this.undoItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutItem = new JMenuItem("About FreeCell...", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "FreeCell\nWhistler — Windows XP classic reimplementation\n\n"
                        + "Click a card, then click a destination (original style).\n"
                        + "Drag-and-drop is also supported.\n"
                        + "Double-click: move to an empty free cell (if enabled in Options).\n"
                        + "Empty free cells / columns enable supermoves.\n"
                        + "F2: new game (random #1–32000)\n"
                        + "F3: select game\n"
                        + "F4: statistics\n"
                        + "F5: options\n"
                        + "F10: undo",
                "About FreeCell",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void newGame() {
        if (!this.confirmResignIfNeeded()) {
            return;
        }
        this.startGame(new FreeCellGame());
    }

    private void restartSameGame() {
        if (!this.isGameStarted()) {
            return;
        }
        if (!this.confirmResignIfNeeded()) {
            return;
        }
        this.startGame(new FreeCellGame(this.game.getGameNumber()));
    }

    private void selectGame() {
        if (!this.confirmResignIfNeeded()) {
            return;
        }
        final int current = Math.max(1, this.game.getGameNumber());
        final String input = (String) JOptionPane.showInputDialog(
                this,
                "Game number (1–" + NumberedDeal.EXTENDED_MAX_GAME + "):",
                "Select Game",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                Integer.toString(current)
        );
        if (input == null) {
            return;
        }
        final String trimmed = input.trim();
        final int number;
        try {
            number = Integer.parseInt(trimmed);
        } catch (final NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a whole number.", "Select Game",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (number < 1 || number > NumberedDeal.EXTENDED_MAX_GAME) {
            JOptionPane.showMessageDialog(
                    this,
                    "Game number must be between 1 and " + NumberedDeal.EXTENDED_MAX_GAME + ".",
                    "Select Game",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        this.startGame(new FreeCellGame(number));
    }

    /** Whether a numbered deal is on the board (not the idle empty shell). */
    private boolean isGameStarted() {
        return this.game.getGameNumber() > 0;
    }

    /**
     * @return {@code true} if it is OK to leave the current game
     */
    private boolean confirmResignIfNeeded() {
        // No deal yet — nothing to resign.
        if (!this.isGameStarted()) {
            return true;
        }
        // Classic FreeCell asks even when no moves have been made yet.
        if (this.gameCounted || this.game.isWon() || this.game.isLost()) {
            return true;
        }
        final int answer = JOptionPane.showConfirmDialog(
                this,
                "Do you want to resign this game?",
                "FreeCell",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return false;
        }
        // Resign counts as a loss (classic FreeCell statistics).
        this.statistics.recordLoss();
        this.gameCounted = true;
        return true;
    }

    private void startGame(final FreeCellGame next) {
        // Uncounted mid-game leave without resign dialog (should not happen after confirm).
        this.recordAbandonedIfNeeded();
        this.game = next;
        this.selection = null;
        this.endDialogShown = false;
        this.gameCounted = false;
        this.boardPanel.clearDrag();
        this.refreshStatus();
        this.boardPanel.repaint();
    }

    private void undo() {
        if (this.game.undo()) {
            this.endDialogShown = false;
            this.selection = null;
            this.boardPanel.clearDrag();
            this.refreshStatus();
            this.boardPanel.repaint();
        }
    }

    private void exit() {
        if (!this.confirmResignIfNeeded()) {
            return;
        }
        this.recordAbandonedIfNeeded();
        this.dispose();
    }

    private void recordAbandonedIfNeeded() {
        if (!this.isGameStarted()) {
            return;
        }
        if (!this.gameCounted && this.game.getMoveCount() > 0
                && !this.game.isWon() && !this.game.isLost()) {
            this.statistics.recordAbandoned();
            this.gameCounted = true;
        }
    }

    private void refreshStatus() {
        if (this.restartItem != null) {
            this.restartItem.setEnabled(this.isGameStarted());
        }
        if (this.undoItem != null) {
            this.undoItem.setEnabled(this.game.canUndo());
        }

        if (!this.isGameStarted()) {
            this.setTitle("FreeCell");
            this.statusLabel.setText(String.format(
                    "Session %d-%d   Total %d-%d   — Start a game (F2 / F3)",
                    this.statistics.getSessionWon(),
                    this.statistics.getSessionLost(),
                    this.statistics.getTotalWon(),
                    this.statistics.getTotalLost()
            ));
            return;
        }

        final int gameNumber = this.game.getGameNumber();
        this.setTitle("FreeCell Game #" + gameNumber);

        final String suffix;
        if (this.game.isWon()) {
            suffix = "   — You win!";
        } else if (this.game.isLost()) {
            suffix = "   — No more moves";
        } else {
            suffix = "";
        }
        this.statusLabel.setText(String.format(
                "Moves: %d   Game #%d   Session %d-%d   Total %d-%d%s",
                this.game.getMoveCount(),
                gameNumber,
                this.statistics.getSessionWon(),
                this.statistics.getSessionLost(),
                this.statistics.getTotalWon(),
                this.statistics.getTotalLost(),
                suffix
        ));
    }

    private void onWin() {
        if (this.endDialogShown) {
            return;
        }
        this.endDialogShown = true;
        if (!this.gameCounted) {
            this.statistics.recordWin();
            this.gameCounted = true;
        }
        this.refreshStatus();
        JOptionPane.showMessageDialog(
                this,
                "Congratulations! You won in " + this.game.getMoveCount() + " moves.",
                "FreeCell",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void onLoss() {
        if (this.endDialogShown) {
            return;
        }
        this.endDialogShown = true;
        if (!this.gameCounted) {
            this.statistics.recordLoss();
            this.gameCounted = true;
        }
        this.refreshStatus();
        JOptionPane.showMessageDialog(
                this,
                "There are no more legal moves.\n\nYou can Undo and try a different line of play.",
                "FreeCell",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void finishTurn() {
        this.game.autoMoveToFoundations();
        this.game.evaluateLoss();
        this.selection = null;
        this.refreshStatus();
        this.boardPanel.repaint();
        if (this.game.isWon()) {
            this.onWin();
        } else if (this.game.isLost()) {
            this.onLoss();
        }
    }

    private void applyMove(final PileRef from, final int cardCount, final PileRef to) {
        if (this.game.move(from, cardCount, to)) {
            this.finishTurn();
        } else {
            this.notifyIllegalMove();
        }
    }

    private void notifyIllegalMove() {
        if (!this.options.isMessagesOnIllegalMoves() || this.game.isWon() || this.game.isLost()) {
            return;
        }
        JOptionPane.showMessageDialog(
                this,
                "That move is not allowed.",
                "FreeCell",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void trySelectOrMove(final Hit hit, final boolean doubleClick) {
        if (hit == null || !this.isGameStarted() || this.game.isWon() || this.game.isLost()) {
            return;
        }
        if (doubleClick) {
            if (!this.options.isDoubleClickToFreeCell()) {
                return;
            }
            // Classic FreeCell: double-click parks a card in an empty free cell.
            final PileRef from = hit.pile();
            if (hit.cardCount() == 1 && this.game.tryMoveToEmptyFreeCell(from)) {
                this.finishTurn();
            }
            return;
        }

        // Click-source / click-destination (original FreeCell interaction).
        if (this.selection == null) {
            if (hit.cardCount() > 0 && this.isSelectable(hit)) {
                this.selection = new Selection(hit.pile(), hit.cardCount());
                this.boardPanel.repaint();
            }
            return;
        }

        // Second click: try move, or re-select
        if (this.selection.pile().equals(hit.pile())) {
            // Click same pile — adjust selection depth if cascade, else clear
            if (hit.pile().getType() == PileType.CASCADE && hit.cardCount() > 0) {
                this.selection = new Selection(hit.pile(), hit.cardCount());
            } else {
                this.selection = null;
            }
            this.boardPanel.repaint();
            return;
        }

        // Destination may be empty (free cell / cascade / foundation) — cardCount can be 0.
        if (this.game.canMove(this.selection.pile(), this.selection.cardCount(), hit.pile())) {
            this.applyMove(this.selection.pile(), this.selection.cardCount(), hit.pile());
        } else if (hit.cardCount() > 0 && this.isSelectable(hit)) {
            this.selection = new Selection(hit.pile(), hit.cardCount());
            this.boardPanel.repaint();
        } else {
            this.notifyIllegalMove();
            this.selection = null;
            this.boardPanel.repaint();
        }
    }

    private boolean isSelectable(final Hit hit) {
        if (hit.cardCount() < 1) {
            return false;
        }
        return switch (hit.pile().getType()) {
            case FREE_CELL -> this.game.getFreeCell(hit.pile().getIndex()).isPresent();
            // Classic FreeCell: home cells are not selectable sources (cannot take cards back).
            case FOUNDATION -> false;
            case CASCADE -> this.game.validSequenceLengthFromTop(
                    hit.pile().getIndex(), hit.cardCount()) == hit.cardCount();
        };
    }

    private record Selection(PileRef pile, int cardCount) {
    }

    private record Hit(PileRef pile, int cardCount) {
    }

    private final class BoardPanel extends JPanel {
        private Point pressPoint;
        private Point dragPoint;
        private Selection dragSelection;
        private List<Card> dragCards = List.of();
        private boolean dragMoved;
        /** Cursor position for king eye-tracking (null when mouse left the board). */
        private Point cursorPoint;

        BoardPanel() {
            final MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    BoardPanel.this.cursorPoint = e.getPoint();
                    if (e.getButton() != MouseEvent.BUTTON1
                            || !FreeCellFrame.this.isGameStarted()
                            || FreeCellFrame.this.game.isWon()
                            || FreeCellFrame.this.game.isLost()) {
                        return;
                    }
                    BoardPanel.this.pressPoint = e.getPoint();
                    BoardPanel.this.dragMoved = false;
                    final Hit hit = BoardPanel.this.hitTest(e.getX(), e.getY());
                    // Prepare drag only from a selectable card; click-to-empty destinations
                    // are handled on release without clearing an existing selection.
                    if (hit != null && hit.cardCount() > 0 && FreeCellFrame.this.isSelectable(hit)) {
                        BoardPanel.this.dragSelection = new Selection(hit.pile(), hit.cardCount());
                        BoardPanel.this.dragCards = BoardPanel.this.cardsFor(hit.pile(), hit.cardCount());
                        BoardPanel.this.dragPoint = e.getPoint();
                        // Do not steal an existing click-source selection until drag starts;
                        // press on another card will re-select on release if not dragged.
                        if (FreeCellFrame.this.selection == null) {
                            FreeCellFrame.this.selection = BoardPanel.this.dragSelection;
                            BoardPanel.this.repaint();
                        }
                    } else {
                        BoardPanel.this.dragSelection = null;
                        BoardPanel.this.dragCards = List.of();
                    }
                }

                @Override
                public void mouseDragged(final MouseEvent e) {
                    BoardPanel.this.cursorPoint = e.getPoint();
                    if (BoardPanel.this.dragSelection == null) {
                        BoardPanel.this.repaintKingBadge();
                        return;
                    }
                    if (BoardPanel.this.pressPoint != null) {
                        final int dx = e.getX() - BoardPanel.this.pressPoint.x;
                        final int dy = e.getY() - BoardPanel.this.pressPoint.y;
                        if (dx * dx + dy * dy > 16) {
                            BoardPanel.this.dragMoved = true;
                        }
                    }
                    if (BoardPanel.this.dragMoved) {
                        // Promote drag selection once movement exceeds threshold.
                        FreeCellFrame.this.selection = BoardPanel.this.dragSelection;
                        BoardPanel.this.dragPoint = e.getPoint();
                        BoardPanel.this.repaint();
                    } else {
                        BoardPanel.this.repaintKingBadge();
                    }
                }

                @Override
                public void mouseMoved(final MouseEvent e) {
                    BoardPanel.this.cursorPoint = e.getPoint();
                    BoardPanel.this.repaintKingBadge();
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    BoardPanel.this.cursorPoint = e.getPoint();
                    BoardPanel.this.repaintKingBadge();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    BoardPanel.this.cursorPoint = null;
                    BoardPanel.this.repaintKingBadge();
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                    BoardPanel.this.cursorPoint = e.getPoint();
                    if (e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    final Selection src = BoardPanel.this.dragSelection;
                    final boolean dragged = BoardPanel.this.dragMoved && src != null;
                    BoardPanel.this.clearDrag();

                    if (dragged) {
                        final Hit dest = BoardPanel.this.hitTest(e.getX(), e.getY());
                        if (dest != null
                                && FreeCellFrame.this.game.canMove(src.pile(), src.cardCount(), dest.pile())) {
                            FreeCellFrame.this.applyMove(src.pile(), src.cardCount(), dest.pile());
                        } else {
                            if (dest != null) {
                                FreeCellFrame.this.notifyIllegalMove();
                            }
                            FreeCellFrame.this.selection = src;
                            BoardPanel.this.repaint();
                        }
                        return;
                    }

                    final Hit hit = BoardPanel.this.hitTest(e.getX(), e.getY());
                    FreeCellFrame.this.trySelectOrMove(hit, e.getClickCount() >= 2);
                }
            };
            this.addMouseListener(mouse);
            this.addMouseMotionListener(mouse);
        }

        private void repaintKingBadge() {
            final Rectangle badge = this.computeLayout().kingBadge();
            // Slightly inflate so pupil movement doesn't leave trails
            this.repaint(badge.x - 2, badge.y - 2, badge.width + 4, badge.height + 4);
        }

        void clearDrag() {
            this.dragSelection = null;
            this.dragCards = List.of();
            this.dragPoint = null;
            this.pressPoint = null;
            this.dragMoved = false;
        }

        private List<Card> cardsFor(final PileRef pile, final int count) {
            return switch (pile.getType()) {
                case FREE_CELL -> FreeCellFrame.this.game.getFreeCell(pile.getIndex())
                        .map(List::of)
                        .orElse(List.of());
                case FOUNDATION -> {
                    final List<Card> f = FreeCellFrame.this.game.getFoundation(pile.getIndex());
                    if (f.isEmpty() || count < 1) {
                        yield List.of();
                    }
                    yield List.of(f.get(f.size() - 1));
                }
                case CASCADE -> {
                    final List<Card> c = FreeCellFrame.this.game.getCascade(pile.getIndex());
                    final int start = c.size() - count;
                    if (start < 0) {
                        yield List.of();
                    }
                    yield List.copyOf(c.subList(start, c.size()));
                }
            };
        }

        private Hit hitTest(final int x, final int y) {
            final Layout layout = this.computeLayout();
            // Free cells
            for (int i = 0; i < FreeCellGame.FREE_CELL_COUNT; i++) {
                final Rectangle r = layout.freeCell(i);
                if (r.contains(x, y)) {
                    final boolean has = FreeCellFrame.this.game.getFreeCell(i).isPresent();
                    return new Hit(PileRef.freeCell(i), has ? 1 : 0);
                }
            }
            // Foundations
            for (int i = 0; i < FreeCellGame.FOUNDATION_COUNT; i++) {
                final Rectangle r = layout.foundation(i);
                if (r.contains(x, y)) {
                    final boolean has = !FreeCellFrame.this.game.getFoundation(i).isEmpty();
                    return new Hit(PileRef.foundation(i), has ? 1 : 0);
                }
            }
            // Cascades — prefer topmost card in stack
            for (int i = 0; i < FreeCellGame.CASCADE_COUNT; i++) {
                final List<Card> cascade = FreeCellFrame.this.game.getCascade(i);
                final int baseX = layout.cascadeX(i);
                final int baseY = layout.cascadeTopY;
                if (cascade.isEmpty()) {
                    final Rectangle empty = new Rectangle(baseX, baseY, CARD_WIDTH, CARD_HEIGHT);
                    if (empty.contains(x, y)) {
                        return new Hit(PileRef.cascade(i), 0);
                    }
                    continue;
                }
                for (int c = cascade.size() - 1; c >= 0; c--) {
                    final int cardY = baseY + c * CASCADE_OVERLAP;
                    final int h = (c == cascade.size() - 1) ? CARD_HEIGHT : CASCADE_OVERLAP;
                    final Rectangle r = new Rectangle(baseX, cardY, CARD_WIDTH, h);
                    if (r.contains(x, y)) {
                        final int fromTop = cascade.size() - c;
                        return new Hit(PileRef.cascade(i), fromTop);
                    }
                }
            }
            return null;
        }

        @Override
        public Dimension getPreferredSize() {
            // Top row is wider: free cells + king gap + foundations
            final int topWidth = FreeCellGame.FREE_CELL_COUNT * CARD_WIDTH
                    + (FreeCellGame.FREE_CELL_COUNT - 1) * CARD_GAP
                    + TOP_CENTER_GAP
                    + FreeCellGame.FOUNDATION_COUNT * CARD_WIDTH
                    + (FreeCellGame.FOUNDATION_COUNT - 1) * CARD_GAP;
            final int cascadeWidth = FreeCellGame.CASCADE_COUNT * CARD_WIDTH
                    + (FreeCellGame.CASCADE_COUNT - 1) * CARD_GAP;
            final int width = Math.max(topWidth, cascadeWidth) + 24;
            final int height = CARD_HEIGHT + 24 + 12 * CASCADE_OVERLAP + CARD_HEIGHT + 24;
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Layout layout = this.computeLayout();
            final Selection sel = FreeCellFrame.this.selection;
            final boolean dragging = this.dragMoved
                    && this.dragSelection != null
                    && this.dragPoint != null;

            // Free cells
            for (int i = 0; i < FreeCellGame.FREE_CELL_COUNT; i++) {
                final Rectangle r = layout.freeCell(i);
                this.paintEmptySlot(g2, r, false);
                final Optional<Card> card = FreeCellFrame.this.game.getFreeCell(i);
                if (card.isPresent()) {
                    final boolean hide = dragging
                            && this.dragSelection.pile().getType() == PileType.FREE_CELL
                            && this.dragSelection.pile().getIndex() == i;
                    if (!hide) {
                        final boolean selected = sel != null
                                && sel.pile().getType() == PileType.FREE_CELL
                                && sel.pile().getIndex() == i;
                        this.paintCard(g2, r.x, r.y, card.get(), selected);
                    }
                }
            }

            // XP-style king badge between free cells and foundations
            this.paintKingBadge(g2, layout.kingBadge());

            // Foundations
            for (int i = 0; i < FreeCellGame.FOUNDATION_COUNT; i++) {
                final Rectangle r = layout.foundation(i);
                this.paintEmptySlot(g2, r, true);
                final List<Card> pile = FreeCellFrame.this.game.getFoundation(i);
                if (!pile.isEmpty()) {
                    this.paintCard(g2, r.x, r.y, pile.get(pile.size() - 1), false);
                }
            }

            // Cascades
            for (int i = 0; i < FreeCellGame.CASCADE_COUNT; i++) {
                final List<Card> cascade = FreeCellFrame.this.game.getCascade(i);
                final int baseX = layout.cascadeX(i);
                final int baseY = layout.cascadeTopY;
                if (cascade.isEmpty()) {
                    this.paintEmptySlot(g2, new Rectangle(baseX, baseY, CARD_WIDTH, CARD_HEIGHT), false);
                    continue;
                }
                final int hideFrom = dragging
                        && this.dragSelection.pile().getType() == PileType.CASCADE
                        && this.dragSelection.pile().getIndex() == i
                        ? cascade.size() - this.dragSelection.cardCount()
                        : cascade.size();
                for (int c = 0; c < cascade.size(); c++) {
                    if (c >= hideFrom) {
                        break;
                    }
                    final boolean selected = sel != null
                            && sel.pile().getType() == PileType.CASCADE
                            && sel.pile().getIndex() == i
                            && c >= cascade.size() - sel.cardCount()
                            && !dragging;
                    this.paintCard(g2, baseX, baseY + c * CASCADE_OVERLAP, cascade.get(c), selected);
                }
            }

            // Drag ghost
            if (dragging && !this.dragCards.isEmpty()) {
                int y = this.dragPoint.y - 10;
                final int x = this.dragPoint.x - CARD_WIDTH / 2;
                for (final Card card : this.dragCards) {
                    this.paintCard(g2, x, y, card, true);
                    y += CASCADE_OVERLAP;
                }
            }

            g2.dispose();
        }

        private void paintEmptySlot(final Graphics2D g2, final Rectangle r, final boolean foundation) {
            g2.setColor(EMPTY_SLOT);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(PANEL_EDGE);
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            if (foundation) {
                g2.setColor(new Color(255, 255, 255, 40));
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
                final String mark = "A";
                final FontMetrics fm = g2.getFontMetrics();
                g2.drawString(mark, r.x + (r.width - fm.stringWidth(mark)) / 2,
                        r.y + (r.height + fm.getAscent()) / 2 - 4);
            }
        }

        /**
         * Decorative king face between free cells and foundations (classic FreeCell chrome).
         * Not interactive — pure ornament.
         */
        private void paintKingBadge(final Graphics2D g2, final Rectangle r) {
            final int x = r.x;
            final int y = r.y;
            final int s = r.width;

            // Raised tile
            g2.setColor(new Color(0x00, 0x4A, 0x00));
            g2.fillRoundRect(x, y, s, s, 6, 6);
            g2.setColor(new Color(0x2A, 0x7A, 0x2A));
            g2.drawRoundRect(x, y, s - 1, s - 1, 6, 6);
            g2.setColor(new Color(0x00, 0x30, 0x00));
            g2.drawRoundRect(x + 1, y + 1, s - 3, s - 3, 5, 5);

            // Inner face card
            final int inset = 4;
            final int ix = x + inset;
            final int iy = y + inset;
            final int iw = s - inset * 2;
            final int ih = s - inset * 2;
            g2.setColor(new Color(0xFF, 0xF8, 0xDC));
            g2.fillRoundRect(ix, iy, iw, ih, 4, 4);
            g2.setColor(new Color(0xC9, 0xA2, 0x27));
            g2.drawRoundRect(ix, iy, iw - 1, ih - 1, 4, 4);

            final int cx = ix + iw / 2;
            final int cy = iy + ih / 2;

            // Crown
            final int crownY = iy + 3;
            final int[] cxPts = {cx - 10, cx - 6, cx - 3, cx, cx + 3, cx + 6, cx + 10, cx + 10, cx - 10};
            final int[] cyPts = {crownY + 8, crownY + 2, crownY + 7, crownY + 1, crownY + 7, crownY + 2, crownY + 8, crownY + 10, crownY + 10};
            g2.setColor(new Color(0xE8, 0xC4, 0x00));
            g2.fillPolygon(cxPts, cyPts, cxPts.length);
            g2.setColor(new Color(0xB8, 0x86, 0x0B));
            g2.drawPolygon(cxPts, cyPts, cxPts.length);
            g2.setColor(new Color(0xDC, 0x14, 0x3C));
            g2.fillOval(cx - 2, crownY, 4, 4);

            // Face
            g2.setColor(new Color(0xFF, 0xE0, 0xBD));
            g2.fillOval(cx - 9, cy - 4, 18, 16);

            // Eyes that follow the cursor (classic FreeCell easter egg)
            final int eyeY = cy;
            final int leftEyeX = cx - 5;
            final int rightEyeX = cx + 2;
            final int eyeW = 5;
            final int eyeH = 5;
            g2.setColor(Color.WHITE);
            g2.fillOval(leftEyeX - 1, eyeY - 1, eyeW, eyeH);
            g2.fillOval(rightEyeX - 1, eyeY - 1, eyeW, eyeH);

            // Eyes glance left/right only (classic FreeCell chrome).
            int pupilDx = 0;
            if (this.cursorPoint != null) {
                final double vx = this.cursorPoint.x - cx;
                if (Math.abs(vx) > 2) {
                    pupilDx = vx < 0 ? -2 : 2;
                }
            }
            g2.setColor(Color.BLACK);
            g2.fillOval(leftEyeX + pupilDx, eyeY, 3, 3);
            g2.fillOval(rightEyeX + pupilDx, eyeY, 3, 3);

            // Mustache / beard
            g2.setColor(new Color(0x5C, 0x40, 0x33));
            g2.fillOval(cx - 6, cy + 5, 12, 5);
            g2.fillOval(cx - 4, cy + 7, 8, 6);
        }

        private void paintCard(final Graphics2D g2, final int x, final int y, final Card card, final boolean selected) {
            g2.setColor(selected ? CARD_SELECTED : CARD_FACE);
            g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);

            final Color ink = card.isRed() ? RED_INK : BLACK_INK;
            g2.setColor(ink);

            final String rank = card.getRank().getLabel();
            final String suit = card.getSuit().getSymbol();

            // Top-left index
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g2.drawString(rank, x + 5, y + 15);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g2.drawString(suit, x + 5, y + 28);

            // Bottom-right index (upright, classic small-card feel)
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            final FontMetrics fmRank = g2.getFontMetrics();
            g2.drawString(rank, x + CARD_WIDTH - 5 - fmRank.stringWidth(rank), y + CARD_HEIGHT - 18);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            final FontMetrics fmSuit = g2.getFontMetrics();
            g2.drawString(suit, x + CARD_WIDTH - 5 - fmSuit.stringWidth(suit), y + CARD_HEIGHT - 5);

            final Rank r = card.getRank();
            if (r == Rank.ACE) {
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
                final FontMetrics fm = g2.getFontMetrics();
                g2.drawString(suit, x + (CARD_WIDTH - fm.stringWidth(suit)) / 2,
                        y + CARD_HEIGHT / 2 + fm.getAscent() / 2 - 6);
            } else if (r.ordinal() >= Rank.TWO.ordinal() && r.ordinal() <= Rank.TEN.ordinal()) {
                this.paintPips(g2, x, y, card);
            } else {
                this.paintFaceCard(g2, x, y, card);
            }
        }

        /**
         * Standard pip layout for 2–10 (count matches rank).
         * Coordinates are fractions of the card interior.
         */
        private void paintPips(final Graphics2D g2, final int x, final int y, final Card card) {
            final String suit = card.getSuit().getSymbol();
            final int n = card.getRank().getValue();
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, n >= 9 ? 13 : 15));
            final FontMetrics fm = g2.getFontMetrics();
            final int sw = fm.stringWidth(suit);
            final int sh = fm.getAscent();

            // Relative positions inside the face (0..1), classic pip grid.
            final float[][] pts = switch (n) {
                case 2 -> new float[][]{{0.5f, 0.28f}, {0.5f, 0.72f}};
                case 3 -> new float[][]{{0.5f, 0.26f}, {0.5f, 0.50f}, {0.5f, 0.74f}};
                case 4 -> new float[][]{
                        {0.32f, 0.28f}, {0.68f, 0.28f},
                        {0.32f, 0.72f}, {0.68f, 0.72f}
                };
                case 5 -> new float[][]{
                        {0.32f, 0.28f}, {0.68f, 0.28f},
                        {0.50f, 0.50f},
                        {0.32f, 0.72f}, {0.68f, 0.72f}
                };
                case 6 -> new float[][]{
                        {0.32f, 0.28f}, {0.68f, 0.28f},
                        {0.32f, 0.50f}, {0.68f, 0.50f},
                        {0.32f, 0.72f}, {0.68f, 0.72f}
                };
                case 7 -> new float[][]{
                        {0.32f, 0.26f}, {0.68f, 0.26f},
                        {0.50f, 0.38f},
                        {0.32f, 0.50f}, {0.68f, 0.50f},
                        {0.32f, 0.74f}, {0.68f, 0.74f}
                };
                case 8 -> new float[][]{
                        {0.32f, 0.24f}, {0.68f, 0.24f},
                        {0.50f, 0.36f},
                        {0.32f, 0.48f}, {0.68f, 0.48f},
                        {0.50f, 0.60f},
                        {0.32f, 0.76f}, {0.68f, 0.76f}
                };
                case 9 -> new float[][]{
                        {0.32f, 0.24f}, {0.68f, 0.24f},
                        {0.32f, 0.40f}, {0.68f, 0.40f},
                        {0.50f, 0.50f},
                        {0.32f, 0.60f}, {0.68f, 0.60f},
                        {0.32f, 0.76f}, {0.68f, 0.76f}
                };
                case 10 -> new float[][]{
                        {0.32f, 0.22f}, {0.68f, 0.22f},
                        {0.50f, 0.32f},
                        {0.32f, 0.40f}, {0.68f, 0.40f},
                        {0.32f, 0.58f}, {0.68f, 0.58f},
                        {0.50f, 0.68f},
                        {0.32f, 0.78f}, {0.68f, 0.78f}
                };
                default -> new float[0][];
            };

            final int left = x + 14;
            final int top = y + 18;
            final int w = CARD_WIDTH - 28;
            final int h = CARD_HEIGHT - 36;
            for (final float[] p : pts) {
                final int px = left + Math.round(p[0] * w) - sw / 2;
                final int py = top + Math.round(p[1] * h) + sh / 3;
                g2.drawString(suit, px, py);
            }
        }

        /** Simple Swing face art for J / Q / K (no bitmaps). */
        private void paintFaceCard(final Graphics2D g2, final int x, final int y, final Card card) {
            final Color ink = card.isRed() ? RED_INK : BLACK_INK;
            final int cx = x + CARD_WIDTH / 2;
            final int cy = y + CARD_HEIGHT / 2 + 4;

            // Panel behind figure
            g2.setColor(new Color(0xF5, 0xEB, 0xD0));
            g2.fillRoundRect(x + 16, y + 30, CARD_WIDTH - 32, CARD_HEIGHT - 48, 8, 8);
            g2.setColor(ink);
            g2.drawRoundRect(x + 16, y + 30, CARD_WIDTH - 32, CARD_HEIGHT - 48, 8, 8);

            // Head
            g2.setColor(new Color(0xFF, 0xE0, 0xBD));
            g2.fillOval(cx - 10, cy - 22, 20, 18);
            g2.setColor(ink);
            g2.drawOval(cx - 10, cy - 22, 20, 18);

            // Eyes
            g2.fillOval(cx - 5, cy - 15, 3, 3);
            g2.fillOval(cx + 2, cy - 15, 3, 3);

            switch (card.getRank()) {
                case JACK -> {
                    // Cap
                    g2.setColor(ink);
                    g2.fillRect(cx - 11, cy - 24, 22, 5);
                    g2.fillRect(cx - 8, cy - 28, 10, 5);
                    // Body
                    g2.setColor(new Color(0x3A, 0x6E, 0xA5));
                    g2.fillRoundRect(cx - 12, cy - 4, 24, 22, 6, 6);
                    g2.setColor(ink);
                    g2.drawRoundRect(cx - 12, cy - 4, 24, 22, 6, 6);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                    g2.drawString("J", cx - 3, cy + 10);
                }
                case QUEEN -> {
                    // Crown
                    g2.setColor(new Color(0xE8, 0xC4, 0x00));
                    final int[] xs = {cx - 11, cx - 7, cx - 3, cx, cx + 3, cx + 7, cx + 11, cx + 11, cx - 11};
                    final int[] ys = {cy - 20, cy - 28, cy - 22, cy - 29, cy - 22, cy - 28, cy - 20, cy - 18, cy - 18};
                    g2.fillPolygon(xs, ys, xs.length);
                    g2.setColor(ink);
                    g2.drawPolygon(xs, ys, xs.length);
                    // Gown
                    g2.setColor(new Color(0x8B, 0x00, 0x8B));
                    g2.fillRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                    g2.setColor(ink);
                    g2.drawRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                    g2.drawString("Q", cx - 4, cy + 11);
                }
                case KING -> {
                    // Crown
                    g2.setColor(new Color(0xE8, 0xC4, 0x00));
                    final int[] xs = {cx - 11, cx - 6, cx - 2, cx + 2, cx + 6, cx + 11, cx + 11, cx - 11};
                    final int[] ys = {cy - 20, cy - 28, cy - 22, cy - 29, cy - 22, cy - 28, cy - 18, cy - 18};
                    g2.fillPolygon(xs, ys, xs.length);
                    g2.setColor(new Color(0xDC, 0x14, 0x3C));
                    g2.fillOval(cx - 2, cy - 30, 4, 4);
                    // Robe
                    g2.setColor(new Color(0x8B, 0x00, 0x00));
                    g2.fillRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                    g2.setColor(ink);
                    g2.drawRoundRect(cx - 13, cy - 4, 26, 24, 8, 8);
                    // Beard
                    g2.setColor(new Color(0x5C, 0x40, 0x33));
                    g2.fillOval(cx - 7, cy - 8, 14, 8);
                    g2.setColor(ink);
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
                    g2.drawString("K", cx - 4, cy + 11);
                }
                default -> {
                    // no-op
                }
            }

            // Suit badge on figure
            g2.setColor(ink);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            final String suit = card.getSuit().getSymbol();
            final FontMetrics fm = g2.getFontMetrics();
            g2.drawString(suit, cx - fm.stringWidth(suit) / 2, cy + 28);
        }

        private Layout computeLayout() {
            final int topWidth = FreeCellGame.FREE_CELL_COUNT * CARD_WIDTH
                    + (FreeCellGame.FREE_CELL_COUNT - 1) * CARD_GAP
                    + TOP_CENTER_GAP
                    + FreeCellGame.FOUNDATION_COUNT * CARD_WIDTH
                    + (FreeCellGame.FOUNDATION_COUNT - 1) * CARD_GAP;
            final int cascadeWidth = FreeCellGame.CASCADE_COUNT * CARD_WIDTH
                    + (FreeCellGame.CASCADE_COUNT - 1) * CARD_GAP;
            final int boardWidth = Math.max(topWidth, cascadeWidth);
            final int originX = Math.max(0, (this.getWidth() - boardWidth) / 2);
            // Center the slightly shorter cascade row under the top row when widths differ
            final int cascadeOriginX = originX + Math.max(0, (boardWidth - cascadeWidth) / 2);
            final int topY = 4;
            final int cascadeTopY = topY + CARD_HEIGHT + 20;
            return new Layout(originX, cascadeOriginX, topY, cascadeTopY, boardWidth);
        }

        private record Layout(
                int originX,
                int cascadeOriginX,
                int topY,
                int cascadeTopY,
                int boardWidth
        ) {
            Rectangle freeCell(final int i) {
                final int x = this.originX + i * (CARD_WIDTH + CARD_GAP);
                return new Rectangle(x, this.topY, CARD_WIDTH, CARD_HEIGHT);
            }

            Rectangle foundation(final int i) {
                final int freeBlock = FreeCellGame.FREE_CELL_COUNT * CARD_WIDTH
                        + (FreeCellGame.FREE_CELL_COUNT - 1) * CARD_GAP;
                final int start = this.originX + freeBlock + TOP_CENTER_GAP;
                final int x = start + i * (CARD_WIDTH + CARD_GAP);
                return new Rectangle(x, this.topY, CARD_WIDTH, CARD_HEIGHT);
            }

            Rectangle kingBadge() {
                final int freeBlock = FreeCellGame.FREE_CELL_COUNT * CARD_WIDTH
                        + (FreeCellGame.FREE_CELL_COUNT - 1) * CARD_GAP;
                final int gapStart = this.originX + freeBlock;
                final int x = gapStart + (TOP_CENTER_GAP - KING_BADGE_SIZE) / 2;
                final int y = this.topY + (CARD_HEIGHT - KING_BADGE_SIZE) / 2;
                return new Rectangle(x, y, KING_BADGE_SIZE, KING_BADGE_SIZE);
            }

            int cascadeX(final int i) {
                return this.cascadeOriginX + i * (CARD_WIDTH + CARD_GAP);
            }
        }
    }
}
