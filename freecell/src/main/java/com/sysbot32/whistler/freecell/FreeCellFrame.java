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
 * Interaction: click source then destination, drag-and-drop, double-click to foundation.
 */
public class FreeCellFrame extends JFrame {
    private static final String TITLE = "FreeCell";
    private static final String CONFIG_GAMES_WON = "stats.gamesWon";
    private static final String CONFIG_GAMES_PLAYED = "stats.gamesPlayed";

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
    static final int CASCADE_OVERLAP = 24;

    private final Config config;
    private FreeCellGame game;

    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel statusLabel = new JLabel(" ");

    private Selection selection;
    private boolean winDialogShown;
    private boolean gameCounted;

    public FreeCellFrame(final Config config) {
        super(TITLE);
        this.config = Objects.requireNonNull(config, "config");
        this.game = new FreeCellGame();

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

        final JMenuItem undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoItem.addActionListener(e -> this.undo());

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());

        gameMenu.add(newItem);
        gameMenu.add(undoItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutItem = new JMenuItem("About FreeCell...", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "FreeCell\nWhistler — Windows XP classic reimplementation\n\n"
                        + "Click a card, then click a destination (or drag).\n"
                        + "Double-click: move to foundation if legal.\n"
                        + "Empty free cells / columns enable supermoves.\n"
                        + "F2: new game\n"
                        + "Ctrl/Cmd+Z: undo",
                "About FreeCell",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void newGame() {
        if (!this.gameCounted && this.game.getMoveCount() > 0 && !this.game.isWon()) {
            this.incrementStat(CONFIG_GAMES_PLAYED);
            this.gameCounted = true;
        }
        this.game = new FreeCellGame();
        this.selection = null;
        this.winDialogShown = false;
        this.gameCounted = false;
        this.boardPanel.clearDrag();
        this.refreshStatus();
        this.boardPanel.repaint();
    }

    private void undo() {
        if (this.game.undo()) {
            this.selection = null;
            this.boardPanel.clearDrag();
            this.refreshStatus();
            this.boardPanel.repaint();
        }
    }

    private void exit() {
        if (!this.gameCounted && this.game.getMoveCount() > 0 && !this.game.isWon()) {
            this.incrementStat(CONFIG_GAMES_PLAYED);
            this.config.save();
        }
        this.dispose();
    }

    private void refreshStatus() {
        final String won = this.config.get(CONFIG_GAMES_WON, "0");
        final String played = this.config.get(CONFIG_GAMES_PLAYED, "0");
        this.statusLabel.setText(String.format(
                "Moves: %d   Seed: %d   Won: %s / Played: %s%s",
                this.game.getMoveCount(),
                this.game.getSeed(),
                won,
                played,
                this.game.isWon() ? "   — You win!" : ""
        ));
    }

    private void incrementStat(final String key) {
        final int value = Integer.parseInt(this.config.get(key, "0"));
        this.config.set(key, Integer.toString(value + 1));
        this.config.save();
    }

    private void onWin() {
        if (this.winDialogShown) {
            return;
        }
        this.winDialogShown = true;
        if (!this.gameCounted) {
            this.incrementStat(CONFIG_GAMES_PLAYED);
            this.incrementStat(CONFIG_GAMES_WON);
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

    private void applyMove(final PileRef from, final int cardCount, final PileRef to) {
        if (this.game.move(from, cardCount, to)) {
            this.game.autoMoveToFoundations();
            this.selection = null;
            this.refreshStatus();
            this.boardPanel.repaint();
            if (this.game.isWon()) {
                this.onWin();
            }
        }
    }

    private void trySelectOrMove(final Hit hit, final boolean doubleClick) {
        if (hit == null || this.game.isWon()) {
            return;
        }
        if (doubleClick) {
            final PileRef from = hit.pile();
            final int count = hit.cardCount();
            if (count == 1 && this.game.tryMoveToFoundation(from)) {
                this.game.autoMoveToFoundations();
                this.selection = null;
                this.refreshStatus();
                this.boardPanel.repaint();
                if (this.game.isWon()) {
                    this.onWin();
                }
            }
            return;
        }

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

        if (this.game.canMove(this.selection.pile(), this.selection.cardCount(), hit.pile())) {
            this.applyMove(this.selection.pile(), this.selection.cardCount(), hit.pile());
        } else if (hit.cardCount() > 0 && this.isSelectable(hit)) {
            this.selection = new Selection(hit.pile(), hit.cardCount());
            this.boardPanel.repaint();
        } else {
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

        BoardPanel() {
            final MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (e.getButton() != MouseEvent.BUTTON1 || FreeCellFrame.this.game.isWon()) {
                        return;
                    }
                    BoardPanel.this.pressPoint = e.getPoint();
                    BoardPanel.this.dragMoved = false;
                    final Hit hit = BoardPanel.this.hitTest(e.getX(), e.getY());
                    if (hit != null && hit.cardCount() > 0 && FreeCellFrame.this.isSelectable(hit)) {
                        BoardPanel.this.dragSelection = new Selection(hit.pile(), hit.cardCount());
                        BoardPanel.this.dragCards = BoardPanel.this.cardsFor(hit.pile(), hit.cardCount());
                        BoardPanel.this.dragPoint = e.getPoint();
                        FreeCellFrame.this.selection = BoardPanel.this.dragSelection;
                        BoardPanel.this.repaint();
                    } else {
                        BoardPanel.this.dragSelection = null;
                        BoardPanel.this.dragCards = List.of();
                    }
                }

                @Override
                public void mouseDragged(final MouseEvent e) {
                    if (BoardPanel.this.dragSelection == null) {
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
                        BoardPanel.this.dragPoint = e.getPoint();
                        BoardPanel.this.repaint();
                    }
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
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
            final int width = FreeCellGame.CASCADE_COUNT * CARD_WIDTH
                    + (FreeCellGame.CASCADE_COUNT - 1) * CARD_GAP
                    + 24;
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

        private void paintCard(final Graphics2D g2, final int x, final int y, final Card card, final boolean selected) {
            g2.setColor(selected ? CARD_SELECTED : CARD_FACE);
            g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);

            final Color ink = card.isRed() ? RED_INK : BLACK_INK;
            g2.setColor(ink);

            final String rank = card.getRank().getLabel();
            final String suit = card.getSuit().getSymbol();

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g2.drawString(rank, x + 6, y + 18);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g2.drawString(suit, x + 6, y + 34);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            final FontMetrics fm = g2.getFontMetrics();
            final String center = suit;
            g2.drawString(center, x + (CARD_WIDTH - fm.stringWidth(center)) / 2,
                    y + CARD_HEIGHT / 2 + fm.getAscent() / 2 - 4);

            // Bottom-right corner (mirrored feel)
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            final FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(rank, x + CARD_WIDTH - 6 - fm2.stringWidth(rank), y + CARD_HEIGHT - 20);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            final FontMetrics fm3 = g2.getFontMetrics();
            g2.drawString(suit, x + CARD_WIDTH - 6 - fm3.stringWidth(suit), y + CARD_HEIGHT - 6);
        }

        private Layout computeLayout() {
            final int totalWidth = FreeCellGame.CASCADE_COUNT * CARD_WIDTH
                    + (FreeCellGame.CASCADE_COUNT - 1) * CARD_GAP;
            final int originX = Math.max(0, (this.getWidth() - totalWidth) / 2);
            final int topY = 4;
            final int cascadeTopY = topY + CARD_HEIGHT + 20;
            return new Layout(originX, topY, cascadeTopY, totalWidth);
        }

        private record Layout(int originX, int topY, int cascadeTopY, int totalWidth) {
            Rectangle freeCell(final int i) {
                final int x = this.originX + i * (CARD_WIDTH + CARD_GAP);
                return new Rectangle(x, this.topY, CARD_WIDTH, CARD_HEIGHT);
            }

            Rectangle foundation(final int i) {
                // Right half of top row
                final int start = this.originX + 4 * (CARD_WIDTH + CARD_GAP);
                final int x = start + i * (CARD_WIDTH + CARD_GAP);
                return new Rectangle(x, this.topY, CARD_WIDTH, CARD_HEIGHT);
            }

            int cascadeX(final int i) {
                return this.originX + i * (CARD_WIDTH + CARD_GAP);
            }
        }
    }
}
