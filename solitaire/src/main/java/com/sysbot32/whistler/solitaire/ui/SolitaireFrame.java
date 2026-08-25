package com.sysbot32.whistler.solitaire.ui;

import com.sysbot32.whistler.card.Card;
import com.sysbot32.whistler.solitaire.model.PileRef;
import com.sysbot32.whistler.solitaire.model.PileType;
import com.sysbot32.whistler.solitaire.model.ScoringMode;
import com.sysbot32.whistler.solitaire.model.SolitaireGame;
import com.sysbot32.whistler.solitaire.model.SolitaireOptions;
import com.sysbot32.whistler.solitaire.model.TableauCard;

import com.sysbot32.whistler.config.Config;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Klondike shell: stock + waste + four foundations on top, seven tableau piles below.
 * Click source then destination; drag-and-drop is also supported. Click the stock to
 * draw (or recycle the waste when the stock is empty). Double-click sends a card home.
 */
public class SolitaireFrame extends JFrame {
    private static final String TITLE = "Solitaire";
    private static final Color FELT_GREEN = new Color(0x00, 0x80, 0x00);
    private static final Color CARD_FACE = new Color(0xFF, 0xFF, 0xF0);
    private static final Color CARD_SELECTED = new Color(0xFF, 0xFF, 0x99);
    private static final Color CARD_BORDER = new Color(0x20, 0x20, 0x20);
    private static final Color EMPTY_SLOT = new Color(0x00, 0x6B, 0x00);
    private static final Color RED_INK = new Color(0xC0, 0x00, 0x00);
    private static final Color BLACK_INK = new Color(0x10, 0x10, 0x10);

    static final int CARD_WIDTH = 72;
    static final int CARD_HEIGHT = 100;
    static final int CARD_GAP = 8;
    static final int FACE_UP_OVERLAP = 24;
    static final int FACE_DOWN_OVERLAP = 12;
    static final int TOP_ROW_GAP = 28;
    static final int WASTE_FAN = 16;

    private final SolitaireOptions options;
    private SolitaireGame game;

    private final BoardPanel boardPanel = new BoardPanel();
    private final JPanel statusBar = new JPanel(new BorderLayout());
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel timeLabel = new JLabel("Time: 0");

    private Selection selection;
    private boolean winDialogShown;
    private int elapsedSeconds;
    private boolean timerRunning;
    private final Timer clock;
    private JMenuItem undoItem;

    public SolitaireFrame(final Config config) {
        super(TITLE);
        this.options = new SolitaireOptions(Objects.requireNonNull(config, "config"));
        this.game = new SolitaireGame(this.options);
        this.clock = new Timer(1000, e -> this.tickClock());

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.getContentPane().setBackground(FELT_GREEN);
        this.setLayout(new BorderLayout());

        this.setJMenuBar(this.createMenuBar());
        this.boardPanel.setBackground(FELT_GREEN);
        this.boardPanel.setBorder(new EmptyBorder(12, 12, 8, 12));
        this.add(this.boardPanel, BorderLayout.CENTER);

        this.statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.statusBar.setBackground(new Color(0xC0, 0xC0, 0xC0));
        this.scoreLabel.setBorder(new EmptyBorder(2, 8, 2, 8));
        this.timeLabel.setBorder(new EmptyBorder(2, 8, 2, 8));
        this.statusBar.add(this.scoreLabel, BorderLayout.WEST);
        this.statusBar.add(this.timeLabel, BorderLayout.EAST);
        this.add(this.statusBar, BorderLayout.SOUTH);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                SolitaireFrame.this.exit();
            }
        });

        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "stopWin");
        this.getRootPane().getActionMap().put("stopWin", new AbstractAction() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent e) {
                SolitaireFrame.this.boardPanel.stopWinAnimation();
            }
        });

        this.refreshStatus();
        this.pack();
        this.setMinimumSize(this.getPreferredSize());
        this.setLocationRelativeTo(null);
    }

    public SolitaireGame getGame() {
        return this.game;
    }

    private JMenuBar createMenuBar() {
        final JMenuBar menuBar = new JMenuBar();

        final JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);

        final JMenuItem dealItem = new JMenuItem("Deal", KeyEvent.VK_D);
        dealItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        dealItem.addActionListener(e -> this.newGame());

        this.undoItem = new JMenuItem("Undo", KeyEvent.VK_U);
        this.undoItem.addActionListener(e -> this.undo());

        final JMenuItem deckItem = new JMenuItem("Deck...", KeyEvent.VK_K);
        deckItem.addActionListener(e -> this.chooseDeck());

        final JMenuItem optionsItem = new JMenuItem("Options...", KeyEvent.VK_O);
        optionsItem.addActionListener(e -> this.showOptions());

        final JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> this.exit());

        gameMenu.add(dealItem);
        gameMenu.addSeparator();
        gameMenu.add(this.undoItem);
        gameMenu.add(deckItem);
        gameMenu.add(optionsItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);

        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        final JMenuItem aboutItem = new JMenuItem("About Solitaire...", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "Solitaire\nDeveloped for Microsoft by Wes Cherry\n\n"
                        + "Whistler — Windows XP classic reimplementation\n"
                        + "F2: Deal",
                "About Solitaire",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);

        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void newGame() {
        this.persistVegasBank();
        this.boardPanel.stopWinAnimation();
        this.game = new SolitaireGame(this.options);
        this.selection = null;
        this.winDialogShown = false;
        this.elapsedSeconds = 0;
        this.timerRunning = false;
        this.clock.stop();
        this.refreshStatus();
        this.boardPanel.repaint();
    }

    private void undo() {
        if (this.boardPanel.winAnimating) {
            return;
        }
        if (this.game.undo()) {
            this.selection = null;
            this.refreshStatus();
            this.boardPanel.repaint();
        }
    }

    private void chooseDeck() {
        final int picked = SolitaireDeckDialog.show(this, this.options.getDeckBack());
        if (picked >= 0) {
            this.options.setDeckBack(picked);
            this.boardPanel.repaint();
        }
    }

    private void showOptions() {
        final boolean redeal = SolitaireOptionsDialog.show(this, this.options);
        this.refreshStatus();
        if (redeal) {
            final int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Deal Again?",
                    TITLE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                this.newGame();
                return;
            }
        }
        this.boardPanel.repaint();
    }

    private void persistVegasBank() {
        if (this.options.getScoring() == ScoringMode.VEGAS && this.options.isKeepScore()) {
            this.options.setVegasBank(this.game.getScore());
        }
    }

    private void exit() {
        this.persistVegasBank();
        this.dispose();
        System.exit(0);
    }

    private void noteAction() {
        if (this.options.isTimed() && !this.timerRunning && !this.game.isWon()) {
            this.timerRunning = true;
            this.clock.start();
        }
    }

    private void tickClock() {
        if (!this.timerRunning || this.game.isWon()) {
            return;
        }
        this.elapsedSeconds++;
        if (this.elapsedSeconds % 10 == 0) {
            this.game.applyTimePenalty();
        }
        this.refreshStatus();
    }

    private void refreshStatus() {
        this.statusBar.setVisible(this.options.isStatusBar());
        final String scoreText = this.options.getScoring() == ScoringMode.NONE
                ? " "
                : "Score: " + this.game.getScore();
        this.scoreLabel.setText(scoreText);
        this.timeLabel.setText(this.options.isTimed() ? "Time: " + this.elapsedSeconds : " ");
        if (this.undoItem != null) {
            this.undoItem.setEnabled(this.game.canUndo() && !this.boardPanel.winAnimating);
        }
        this.setTitle(this.game.isWon() ? TITLE + " — Won" : TITLE);
    }

    private void afterTurn() {
        this.selection = null;
        this.noteAction();
        this.refreshStatus();
        this.boardPanel.repaint();
        if (this.game.isWon() && !this.winDialogShown) {
            this.winDialogShown = true;
            this.clock.stop();
            this.timerRunning = false;
            if (this.options.isTimed()) {
                this.game.applyWinBonus(Math.max(1, this.elapsedSeconds));
                this.refreshStatus();
            }
            this.persistVegasBank();
            this.boardPanel.startWinAnimation();
        }
    }

    private void applyMove(final PileRef from, final int cardCount, final PileRef to) {
        if (this.game.isWon()) {
            return;
        }
        if (this.game.move(from, cardCount, to)) {
            this.afterTurn();
        }
    }

    private void trySelectOrMove(final Hit hit, final boolean doubleClick) {
        if (hit == null || this.game.isWon()) {
            return;
        }
        if (hit.pile().getType() == PileType.STOCK) {
            this.game.drawOrRecycle();
            this.afterTurn();
            return;
        }
        if (doubleClick) {
            if (hit.cardCount() == 1 && this.isSelectable(hit)) {
                if (this.game.tryMoveToFoundation(hit.pile())) {
                    this.afterTurn();
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
        if (this.selection.pile().equals(hit.pile())) {
            if (hit.pile().getType() == PileType.TABLEAU && hit.cardCount() > 0) {
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
            case STOCK -> false;
            case WASTE -> this.game.peekWaste().isPresent();
            case FOUNDATION -> this.game.peekFoundation(hit.pile().getIndex()).isPresent();
            case TABLEAU -> this.game.validFaceUpSequenceLengthFromTop(
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
        private boolean winAnimating;
        private final List<Flyer> flyers = new ArrayList<>();
        private Timer winTimer;

        BoardPanel() {
            final MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (BoardPanel.this.winAnimating) {
                        BoardPanel.this.stopWinAnimation();
                        return;
                    }
                    if (e.getButton() != MouseEvent.BUTTON1 || SolitaireFrame.this.game.isWon()) {
                        return;
                    }
                    BoardPanel.this.pressPoint = e.getPoint();
                    BoardPanel.this.dragMoved = false;
                    final Hit hit = BoardPanel.this.hitTest(e.getX(), e.getY());
                    if (hit != null && hit.pile().getType() != PileType.STOCK
                            && hit.cardCount() > 0 && SolitaireFrame.this.isSelectable(hit)) {
                        BoardPanel.this.dragSelection = new Selection(hit.pile(), hit.cardCount());
                        BoardPanel.this.dragCards = BoardPanel.this.cardsFor(hit.pile(), hit.cardCount());
                        BoardPanel.this.dragPoint = e.getPoint();
                        if (SolitaireFrame.this.selection == null) {
                            SolitaireFrame.this.selection = BoardPanel.this.dragSelection;
                            BoardPanel.this.repaint();
                        }
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
                        SolitaireFrame.this.selection = BoardPanel.this.dragSelection;
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
                                && SolitaireFrame.this.game.canMove(
                                src.pile(), src.cardCount(), dest.pile())) {
                            SolitaireFrame.this.applyMove(src.pile(), src.cardCount(), dest.pile());
                        } else {
                            BoardPanel.this.repaint();
                        }
                        return;
                    }
                    SolitaireFrame.this.trySelectOrMove(
                            BoardPanel.this.hitTest(e.getX(), e.getY()),
                            e.getClickCount() >= 2
                    );
                }
            };
            this.addMouseListener(mouse);
            this.addMouseMotionListener(mouse);
        }

        private void clearDrag() {
            this.dragSelection = null;
            this.dragCards = List.of();
            this.dragMoved = false;
            this.pressPoint = null;
            this.dragPoint = null;
        }

        private List<Card> cardsFor(final PileRef pile, final int cardCount) {
            return switch (pile.getType()) {
                case WASTE -> SolitaireFrame.this.game.peekWaste().stream().toList();
                case FOUNDATION -> SolitaireFrame.this.game.peekFoundation(pile.getIndex()).stream().toList();
                case TABLEAU -> {
                    final List<TableauCard> cards = SolitaireFrame.this.game.getTableau(pile.getIndex());
                    final int start = cards.size() - cardCount;
                    yield cards.subList(start, cards.size()).stream().map(TableauCard::getCard).toList();
                }
                case STOCK -> List.of();
            };
        }

        @Override
        public Dimension getPreferredSize() {
            final int width = 12 * 2 + SolitaireGame.TABLEAU_COUNT * (CARD_WIDTH + CARD_GAP) - CARD_GAP;
            final int height = 12 + CARD_HEIGHT + TOP_ROW_GAP
                    + 12 * FACE_UP_OVERLAP + CARD_HEIGHT + 8;
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            this.paintStock(g2);
            this.paintWaste(g2);
            for (int i = 0; i < SolitaireGame.FOUNDATION_COUNT; i++) {
                this.paintFoundation(g2, i);
            }
            for (int i = 0; i < SolitaireGame.TABLEAU_COUNT; i++) {
                this.paintTableau(g2, i);
            }
            if (this.winAnimating) {
                for (final Flyer flyer : this.flyers) {
                    this.paintCard(g2, (int) flyer.x, (int) flyer.y, flyer.card, false);
                }
            } else if (this.dragMoved && this.dragPoint != null) {
                int y = this.dragPoint.y - 12;
                final int x = this.dragPoint.x - CARD_WIDTH / 2;
                if (SolitaireFrame.this.options.isOutlineDragging()) {
                    this.paintOutlineStack(g2, x, y, this.dragCards.size());
                    final Hit dest = this.hitTest(this.dragPoint.x, this.dragPoint.y);
                    if (dest != null && this.dragSelection != null
                            && SolitaireFrame.this.game.canMove(
                            this.dragSelection.pile(), this.dragSelection.cardCount(), dest.pile())) {
                        g2.setColor(new Color(255, 255, 180, 90));
                        final Rectangle highlight = this.slotRect(dest.pile());
                        g2.fillRoundRect(highlight.x, highlight.y, highlight.width, highlight.height, 10, 10);
                    }
                } else {
                    for (final Card card : this.dragCards) {
                        this.paintCard(g2, x, y, card, true);
                        y += FACE_UP_OVERLAP;
                    }
                }
            }
            g2.dispose();
        }

        private Rectangle slotRect(final PileRef pile) {
            return switch (pile.getType()) {
                case STOCK -> this.stockRect();
                case WASTE -> this.topWasteRect();
                case FOUNDATION -> this.foundationRect(pile.getIndex());
                case TABLEAU -> {
                    final List<TableauCard> cards = SolitaireFrame.this.game.getTableau(pile.getIndex());
                    final int x = this.tableauX(pile.getIndex());
                    final int y = cards.isEmpty()
                            ? this.tableauY()
                            : this.cardYInTableau(cards, cards.size() - 1, this.tableauY());
                    yield new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
                }
            };
        }

        private void paintOutlineStack(final Graphics2D g2, final int x, final int y, final int count) {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{4f, 4f}, 0));
            g2.setColor(Color.WHITE);
            int cy = y;
            for (int i = 0; i < count; i++) {
                g2.drawRoundRect(x, cy, CARD_WIDTH, CARD_HEIGHT, 10, 10);
                cy += FACE_UP_OVERLAP;
            }
            g2.setStroke(new BasicStroke(1f));
        }

        void startWinAnimation() {
            this.flyers.clear();
            for (int f = 0; f < SolitaireGame.FOUNDATION_COUNT; f++) {
                final List<Card> pile = SolitaireFrame.this.game.getFoundation(f);
                if (pile.isEmpty()) {
                    continue;
                }
                final Rectangle slot = this.foundationRect(f);
                final Card top = pile.get(pile.size() - 1);
                final Flyer flyer = new Flyer();
                flyer.card = top;
                flyer.x = slot.x;
                flyer.y = slot.y;
                flyer.vx = 6 + f * 2;
                flyer.vy = -8;
                this.flyers.add(flyer);
            }
            this.winAnimating = !this.flyers.isEmpty();
            if (!this.winAnimating) {
                return;
            }
            if (this.winTimer != null) {
                this.winTimer.stop();
            }
            this.winTimer = new Timer(16, e -> {
                final int ground = this.getHeight() - CARD_HEIGHT - 4;
                for (final Flyer flyer : this.flyers) {
                    flyer.x += flyer.vx;
                    flyer.y += flyer.vy;
                    flyer.vy += 0.7;
                    if (flyer.y >= ground) {
                        flyer.y = ground;
                        flyer.vy = -Math.abs(flyer.vy) * 0.86;
                    }
                    if (flyer.x > this.getWidth() - CARD_WIDTH) {
                        flyer.x = this.getWidth() - CARD_WIDTH;
                        flyer.vx = -Math.abs(flyer.vx);
                    } else if (flyer.x < 0) {
                        flyer.x = 0;
                        flyer.vx = Math.abs(flyer.vx);
                    }
                }
                this.repaint();
            });
            this.winTimer.start();
        }

        void stopWinAnimation() {
            this.winAnimating = false;
            this.flyers.clear();
            if (this.winTimer != null) {
                this.winTimer.stop();
            }
            this.repaint();
        }

        private void paintStock(final Graphics2D g2) {
            final Rectangle r = this.stockRect();
            if (SolitaireFrame.this.game.getStock().isEmpty()) {
                this.paintEmptyWell(g2, r);
                if (SolitaireFrame.this.game.canRecycle()
                        && !SolitaireFrame.this.game.getWaste().isEmpty()) {
                    g2.setColor(new Color(0xC8, 0xE0, 0xC8));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(r.x + 18, r.y + 30, r.width - 36, r.height - 60);
                    g2.setStroke(new BasicStroke(1f));
                }
            } else {
                this.paintCardBack(g2, r.x, r.y);
            }
        }

        private void paintWaste(final Graphics2D g2) {
            final List<Card> fan = SolitaireFrame.this.game.getWasteFan();
            if (fan.isEmpty()) {
                this.paintEmptyWell(g2, this.wasteRect());
                return;
            }
            final boolean selected = SolitaireFrame.this.selection != null
                    && SolitaireFrame.this.selection.pile().getType() == PileType.WASTE;
            final int hideTop = this.hideDrag(PileRef.waste(), 1) ? fan.size() - 1 : -1;
            for (int i = 0; i < fan.size(); i++) {
                if (i == hideTop) {
                    continue;
                }
                final int x = this.wasteRect().x + i * WASTE_FAN;
                this.paintCard(g2, x, this.wasteRect().y, fan.get(i), selected && i == fan.size() - 1);
            }
        }

        private void paintFoundation(final Graphics2D g2, final int index) {
            final Rectangle r = this.foundationRect(index);
            final var top = SolitaireFrame.this.game.peekFoundation(index);
            if (top.isEmpty()) {
                this.paintEmptyFoundation(g2, r);
                return;
            }
            final boolean selected = SolitaireFrame.this.selection != null
                    && SolitaireFrame.this.selection.pile().equals(PileRef.foundation(index));
            if (this.hideDrag(PileRef.foundation(index), 1)) {
                this.paintEmptyFoundation(g2, r);
                return;
            }
            this.paintCard(g2, r.x, r.y, top.get(), selected);
        }

        private void paintTableau(final Graphics2D g2, final int index) {
            final List<TableauCard> pile = SolitaireFrame.this.game.getTableau(index);
            final int baseX = this.tableauX(index);
            final int baseY = this.tableauY();
            if (pile.isEmpty()) {
                // XP: empty tableau is just felt, no placeholder hatch.
                return;
            }
            final int hideFrom = this.dragHideStart(PileRef.tableau(index));
            for (int c = 0; c < pile.size(); c++) {
                if (hideFrom >= 0 && c >= hideFrom) {
                    continue;
                }
                final int y = this.cardYInTableau(pile, c, baseY);
                final TableauCard sitting = pile.get(c);
                if (!sitting.isFaceUp()) {
                    this.paintCardBack(g2, baseX, y);
                    continue;
                }
                final boolean selected = this.isTableauCardSelected(index, pile.size() - c);
                this.paintCard(g2, baseX, y, sitting.getCard(), selected);
            }
        }

        private boolean isTableauCardSelected(final int pileIndex, final int cardsFromTopInclusive) {
            final Selection sel = SolitaireFrame.this.selection;
            if (sel == null || sel.pile().getType() != PileType.TABLEAU
                    || sel.pile().getIndex() != pileIndex) {
                return false;
            }
            return cardsFromTopInclusive <= sel.cardCount();
        }

        private boolean hideDrag(final PileRef pile, final int cardCount) {
            return this.dragMoved && this.dragSelection != null
                    && this.dragSelection.pile().equals(pile)
                    && this.dragSelection.cardCount() == cardCount;
        }

        private int dragHideStart(final PileRef pile) {
            if (!this.dragMoved || this.dragSelection == null
                    || !this.dragSelection.pile().equals(pile)) {
                return -1;
            }
            final int size = SolitaireFrame.this.game.getTableau(pile.getIndex()).size();
            return size - this.dragSelection.cardCount();
        }

        /** Dark well for empty stock / waste — no hatch. */
        private void paintEmptyWell(final Graphics2D g2, final Rectangle r) {
            g2.setColor(EMPTY_SLOT);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g2.setColor(new Color(0x00, 0x50, 0x00));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        }

        /**
         * XP {@code cards.dll} empty suit-stack: a clipped fine hatch inside the well.
         * Only foundations use this; tableau empties are bare felt.
         */
        private void paintEmptyFoundation(final Graphics2D g2, final Rectangle r) {
            this.paintEmptyWell(g2, r);
            final Shape clip = g2.getClip();
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                    r.x + 3, r.y + 3, r.width - 6, r.height - 6, 8, 8));
            g2.setColor(new Color(0x00, 0x58, 0x00));
            final int step = 6;
            for (int i = -r.height; i < r.width + r.height; i += step) {
                g2.drawLine(r.x + i, r.y + 3, r.x + i - r.height, r.y + r.height - 3);
            }
            g2.setClip(clip);
        }

        private void paintCardBack(final Graphics2D g2, final int x, final int y) {
            CardBack.at(SolitaireFrame.this.options.getDeckBack())
                    .paint(g2, x, y, CARD_WIDTH, CARD_HEIGHT);
        }

        private void paintCard(final Graphics2D g2, final int x, final int y, final Card card,
                               final boolean selected) {
            g2.setColor(selected ? CARD_SELECTED : CARD_FACE);
            g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);

            final Color ink = card.isRed() ? RED_INK : BLACK_INK;
            g2.setColor(ink);

            final String rank = card.getRank().getLabel();
            final String suit = card.getSuit().getSymbol();

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g2.drawString(rank, x + 5, y + 15);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g2.drawString(suit, x + 5, y + 28);

            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            final FontMetrics fmRank = g2.getFontMetrics();
            g2.drawString(rank, x + CARD_WIDTH - 5 - fmRank.stringWidth(rank), y + CARD_HEIGHT - 18);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            final FontMetrics fmSuit = g2.getFontMetrics();
            g2.drawString(suit, x + CARD_WIDTH - 5 - fmSuit.stringWidth(suit), y + CARD_HEIGHT - 5);

            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));
            final FontMetrics fm = g2.getFontMetrics();
            g2.drawString(suit, x + (CARD_WIDTH - fm.stringWidth(suit)) / 2,
                    y + CARD_HEIGHT / 2 + fm.getAscent() / 2 - 4);
        }

        private Rectangle stockRect() {
            return new Rectangle(this.originX(), this.topY(), CARD_WIDTH, CARD_HEIGHT);
        }

        private Rectangle wasteRect() {
            return new Rectangle(this.originX() + CARD_WIDTH + CARD_GAP, this.topY(),
                    CARD_WIDTH, CARD_HEIGHT);
        }

        private Rectangle topWasteRect() {
            final int fan = Math.max(0, SolitaireFrame.this.game.getWasteFan().size() - 1);
            final Rectangle base = this.wasteRect();
            return new Rectangle(base.x + fan * WASTE_FAN, base.y, CARD_WIDTH, CARD_HEIGHT);
        }

        private Rectangle foundationRect(final int index) {
            final int start = this.originX() + 3 * (CARD_WIDTH + CARD_GAP);
            return new Rectangle(start + index * (CARD_WIDTH + CARD_GAP), this.topY(),
                    CARD_WIDTH, CARD_HEIGHT);
        }

        private int tableauX(final int index) {
            return this.originX() + index * (CARD_WIDTH + CARD_GAP);
        }

        private int tableauY() {
            return this.topY() + CARD_HEIGHT + TOP_ROW_GAP;
        }

        private int originX() {
            final int width = SolitaireGame.TABLEAU_COUNT * (CARD_WIDTH + CARD_GAP) - CARD_GAP;
            return Math.max(12, (this.getWidth() - width) / 2);
        }

        private int topY() {
            return 12;
        }

        private int cardYInTableau(final List<TableauCard> pile, final int index, final int baseY) {
            int y = baseY;
            for (int i = 0; i < index; i++) {
                y += pile.get(i).isFaceUp() ? FACE_UP_OVERLAP : FACE_DOWN_OVERLAP;
            }
            return y;
        }

        private Hit hitTest(final int x, final int y) {
            if (this.stockRect().contains(x, y)) {
                return new Hit(PileRef.stock(), SolitaireFrame.this.game.getStock().isEmpty() ? 0 : 1);
            }
            final List<Card> fan = SolitaireFrame.this.game.getWasteFan();
            if (fan.isEmpty()) {
                if (this.wasteRect().contains(x, y)) {
                    return new Hit(PileRef.waste(), 0);
                }
            } else {
                for (int i = fan.size() - 1; i >= 0; i--) {
                    final Rectangle r = new Rectangle(
                            this.wasteRect().x + i * WASTE_FAN, this.wasteRect().y,
                            CARD_WIDTH, CARD_HEIGHT);
                    if (r.contains(x, y)) {
                        return new Hit(PileRef.waste(), i == fan.size() - 1 ? 1 : 0);
                    }
                }
            }
            for (int i = 0; i < SolitaireGame.FOUNDATION_COUNT; i++) {
                if (this.foundationRect(i).contains(x, y)) {
                    return new Hit(PileRef.foundation(i),
                            SolitaireFrame.this.game.peekFoundation(i).isPresent() ? 1 : 0);
                }
            }
            for (int i = 0; i < SolitaireGame.TABLEAU_COUNT; i++) {
                final Hit hit = this.hitTableau(i, x, y);
                if (hit != null) {
                    return hit;
                }
            }
            return null;
        }

        private Hit hitTableau(final int index, final int x, final int y) {
            final List<TableauCard> pile = SolitaireFrame.this.game.getTableau(index);
            final int baseX = this.tableauX(index);
            final int baseY = this.tableauY();
            if (pile.isEmpty()) {
                final Rectangle empty = new Rectangle(baseX, baseY, CARD_WIDTH, CARD_HEIGHT);
                if (empty.contains(x, y)) {
                    return new Hit(PileRef.tableau(index), 0);
                }
                return null;
            }
            for (int c = pile.size() - 1; c >= 0; c--) {
                final int cardY = this.cardYInTableau(pile, c, baseY);
                final int h = (c == pile.size() - 1) ? CARD_HEIGHT
                        : (pile.get(c).isFaceUp() ? FACE_UP_OVERLAP : FACE_DOWN_OVERLAP);
                final Rectangle r = new Rectangle(baseX, cardY, CARD_WIDTH, h);
                if (r.contains(x, y)) {
                    final int cardsFromTop = pile.size() - c;
                    return new Hit(PileRef.tableau(index), cardsFromTop);
                }
            }
            return null;
        }
    }

    private static final class Flyer {
        private Card card;
        private double x;
        private double y;
        private double vx;
        private double vy;
    }
}
