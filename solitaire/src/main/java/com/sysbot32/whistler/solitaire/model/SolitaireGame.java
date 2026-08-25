package com.sysbot32.whistler.solitaire.model;

import com.sysbot32.whistler.card.Card;
import com.sysbot32.whistler.card.Deck;
import com.sysbot32.whistler.card.Rank;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure Klondike rules: deal, legal moves, draw/recycle, win detection.
 * <p>
 * Empty tableau accepts only a King (or a King-headed face-up run). That empty-pile
 * rule lives here — {@link Card#canPlaceOnCascade(Card)} with a {@code null} target
 * is FreeCell's empty-cascade law and is never used for empty tableau.
 */
@Getter
public class SolitaireGame {
    public static final int TABLEAU_COUNT = 7;
    public static final int FOUNDATION_COUNT = 4;
    public static final int DECK_SIZE = Deck.SIZE;
    public static final int TABLEAU_DEAL_COUNT = 28;
    public static final int STOCK_DEAL_COUNT = DECK_SIZE - TABLEAU_DEAL_COUNT;

    private final List<List<TableauCard>> tableau = new ArrayList<>(TABLEAU_COUNT);
    private final List<List<Card>> foundations = new ArrayList<>(FOUNDATION_COUNT);
    /** Bottom at index 0, drawable top at the end. */
    private final List<Card> stock = new ArrayList<>();
    /** Bottom at index 0, playable top at the end. */
    private final List<Card> waste = new ArrayList<>();

    private GameStatus status = GameStatus.PLAYING;
    private int moveCount;
    private int drawCount = 1;
    private ScoringMode scoring = ScoringMode.NONE;
    private int score;
    /** Completed trips through the stock (deal starts as pass 1). */
    private int passCount = 1;
    private int recycleCount;
    private Snapshot undoSnapshot;

    /** New game with a shuffled 52-card deck (Draw One, no scoring — tests / fixtures). */
    public SolitaireGame() {
        this(ThreadLocalRandom.current());
    }

    /** New game shuffled with the given RNG (reproducible when seeded). */
    public SolitaireGame(final Random random) {
        this(random, 1, ScoringMode.NONE, 0);
    }

    public SolitaireGame(final Random random, final int drawCount, final ScoringMode scoring,
                         final int startingScore) {
        Objects.requireNonNull(random, "random");
        this.initPiles();
        this.drawCount = this.normalizeDrawCount(drawCount);
        this.scoring = Objects.requireNonNull(scoring, "scoring");
        this.score = startingScore;
        this.deal(Deck.createShuffled(random));
    }

    public SolitaireGame(final SolitaireOptions options) {
        this(ThreadLocalRandom.current(),
                options.getDrawCount(),
                options.getScoring(),
                startingScore(options));
    }

    private SolitaireGame(final boolean fixture) {
        this.initPiles();
    }

    static int startingScore(final SolitaireOptions options) {
        if (options.getScoring() != ScoringMode.VEGAS) {
            return 0;
        }
        if (options.isKeepScore()) {
            return options.getVegasBank() - 52;
        }
        return -52;
    }

    private int normalizeDrawCount(final int count) {
        return count >= 3 ? 3 : 1;
    }

    private void initPiles() {
        for (int i = 0; i < TABLEAU_COUNT; i++) {
            this.tableau.add(new ArrayList<>());
        }
        for (int i = 0; i < FOUNDATION_COUNT; i++) {
            this.foundations.add(new ArrayList<>());
        }
    }

    /**
     * Builds a game from an explicit 52-card deal order.
     * Cards are dealt row-by-row into the seven tableau piles (pile {@code i} gets
     * {@code i} face-down cards then one face-up), remainder becomes the stock
     * (last remaining card is the stock top).
     */
    public static SolitaireGame fromDeal(final List<Card> cards) {
        Objects.requireNonNull(cards, "cards");
        if (cards.size() != DECK_SIZE) {
            throw new IllegalArgumentException("Deal must contain exactly " + DECK_SIZE + " cards");
        }
        final Set<Card> unique = new HashSet<>(cards);
        if (unique.size() != DECK_SIZE) {
            throw new IllegalArgumentException("Deal must contain 52 unique cards");
        }
        final SolitaireGame game = new SolitaireGame(true);
        game.deal(cards);
        return game;
    }

    /** Empty board for carefully constructed test positions. */
    public static SolitaireGame emptyBoard() {
        return new SolitaireGame(true);
    }

    private void deal(final List<Card> cards) {
        int n = 0;
        for (int row = 0; row < TABLEAU_COUNT; row++) {
            for (int pile = row; pile < TABLEAU_COUNT; pile++) {
                final boolean faceUp = pile == row;
                this.tableau.get(pile).add(new TableauCard(cards.get(n++), faceUp));
            }
        }
        for (int i = n; i < cards.size(); i++) {
            this.stock.add(cards.get(i));
        }
    }

    public List<TableauCard> getTableau(final int index) {
        this.checkTableauIndex(index);
        return Collections.unmodifiableList(this.tableau.get(index));
    }

    public List<Card> getFoundation(final int index) {
        this.checkFoundationIndex(index);
        return Collections.unmodifiableList(this.foundations.get(index));
    }

    public List<Card> getStock() {
        return Collections.unmodifiableList(this.stock);
    }

    public List<Card> getWaste() {
        return Collections.unmodifiableList(this.waste);
    }

    public Optional<Card> peekStock() {
        if (this.stock.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.stock.get(this.stock.size() - 1));
    }

    public Optional<Card> peekWaste() {
        if (this.waste.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.waste.get(this.waste.size() - 1));
    }

    public Optional<Card> peekFoundation(final int index) {
        this.checkFoundationIndex(index);
        final List<Card> pile = this.foundations.get(index);
        if (pile.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pile.get(pile.size() - 1));
    }

    public Optional<TableauCard> peekTableau(final int index) {
        this.checkTableauIndex(index);
        final List<TableauCard> pile = this.tableau.get(index);
        if (pile.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pile.get(pile.size() - 1));
    }

    public int getFoundationCardCount() {
        int total = 0;
        for (final List<Card> foundation : this.foundations) {
            total += foundation.size();
        }
        return total;
    }

    public boolean isWon() {
        return this.status == GameStatus.WON;
    }

    public boolean canUndo() {
        return this.undoSnapshot != null && this.status != GameStatus.WON;
    }

    /**
     * Undoes the last card moved or the last draw/recycle (classic single-step Undo).
     */
    public boolean undo() {
        if (!this.canUndo()) {
            return false;
        }
        this.undoSnapshot.restore(this);
        this.undoSnapshot = null;
        return true;
    }

    /** Rightmost (playable) waste cards, up to the current draw count — for Draw-Three fan. */
    public List<Card> getWasteFan() {
        final int n = Math.min(this.drawCount, this.waste.size());
        if (n == 0) {
            return List.of();
        }
        return List.copyOf(this.waste.subList(this.waste.size() - n, this.waste.size()));
    }

    /**
     * Length of the valid face-up descending alternating run ending at the tableau top,
     * starting from {@code cardsFromTop} cards from the top (1 = only top card).
     * Returns 0 if the sequence is invalid (includes any face-down card).
     */
    public int validFaceUpSequenceLengthFromTop(final int tableauIndex, final int cardsFromTop) {
        this.checkTableauIndex(tableauIndex);
        if (cardsFromTop < 1) {
            return 0;
        }
        final List<TableauCard> pile = this.tableau.get(tableauIndex);
        if (cardsFromTop > pile.size()) {
            return 0;
        }
        final int start = pile.size() - cardsFromTop;
        for (int i = start; i < pile.size(); i++) {
            if (!pile.get(i).isFaceUp()) {
                return 0;
            }
        }
        for (int i = start; i < pile.size() - 1; i++) {
            final Card lower = pile.get(i + 1).getCard();
            final Card upper = pile.get(i).getCard();
            if (!lower.canPlaceOnCascade(upper)) {
                return 0;
            }
        }
        return cardsFromTop;
    }

    /**
     * Longest legal face-up run selectable from the top of a tableau pile.
     */
    public int maxSelectableFromTableau(final int tableauIndex) {
        this.checkTableauIndex(tableauIndex);
        final List<TableauCard> pile = this.tableau.get(tableauIndex);
        if (pile.isEmpty() || !pile.get(pile.size() - 1).isFaceUp()) {
            return 0;
        }
        int length = 1;
        for (int i = pile.size() - 1; i > 0; i--) {
            final TableauCard lower = pile.get(i);
            final TableauCard upper = pile.get(i - 1);
            if (!upper.isFaceUp() || !lower.getCard().canPlaceOnCascade(upper.getCard())) {
                break;
            }
            length++;
        }
        return length;
    }

    public boolean canMove(final PileRef from, final int cardCount, final PileRef to) {
        if (this.status != GameStatus.PLAYING) {
            return false;
        }
        return this.isLegalMove(from, cardCount, to);
    }

    public boolean move(final PileRef from, final int cardCount, final PileRef to) {
        if (this.status != GameStatus.PLAYING) {
            return false;
        }
        if (!this.isLegalMove(from, cardCount, to)) {
            return false;
        }
        this.captureUndo();
        final List<Card> moving = this.takeCards(from, cardCount);
        this.placeCards(moving, to);
        this.uncoverIfNeeded(from);
        this.applyMoveScore(from, to);
        this.moveCount++;
        this.updateWinStatus();
        return true;
    }

    /**
     * Tries to move the single accessible card at {@code from} onto any legal foundation.
     */
    public boolean tryMoveToFoundation(final PileRef from) {
        if (this.status != GameStatus.PLAYING) {
            return false;
        }
        if (!this.canTake(from, 1)) {
            return false;
        }
        for (int i = 0; i < FOUNDATION_COUNT; i++) {
            if (this.move(from, 1, PileRef.foundation(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draw: move up to {@link #drawCount} cards from stock onto waste
     * (the new waste top is the playable card).
     *
     * @return {@code true} if at least one card was drawn
     */
    public boolean draw() {
        if (this.status != GameStatus.PLAYING) {
            return false;
        }
        if (this.stock.isEmpty()) {
            return false;
        }
        this.captureUndo();
        final int n = Math.min(this.drawCount, this.stock.size());
        for (int i = 0; i < n; i++) {
            this.waste.add(this.stock.remove(this.stock.size() - 1));
        }
        this.moveCount++;
        return true;
    }

    /**
     * When the stock is empty, turn the waste pile over to become the stock
     * (first-drawn waste card becomes the next stock top).
     * <p>
     * Vegas Draw One forbids recycling; Vegas Draw Three allows two redeals.
     *
     * @return {@code true} if the waste was recycled
     */
    public boolean recycleWaste() {
        if (this.status != GameStatus.PLAYING) {
            return false;
        }
        if (!this.stock.isEmpty() || this.waste.isEmpty()) {
            return false;
        }
        if (!this.canRecycle()) {
            return false;
        }
        this.captureUndo();
        Collections.reverse(this.waste);
        this.stock.addAll(this.waste);
        this.waste.clear();
        this.recycleCount++;
        this.passCount++;
        this.applyRecyclePenalty();
        this.moveCount++;
        return true;
    }

    public boolean canRecycle() {
        if (this.stock.isEmpty() && this.waste.isEmpty()) {
            return false;
        }
        if (this.scoring == ScoringMode.VEGAS) {
            if (this.drawCount == 1) {
                return false;
            }
            return this.recycleCount < 2;
        }
        return true;
    }

    /**
     * Click the stock: draw if it has cards, otherwise recycle the waste.
     */
    public boolean drawOrRecycle() {
        if (!this.stock.isEmpty()) {
            return this.draw();
        }
        return this.recycleWaste();
    }

    private boolean isLegalMove(final PileRef from, final int cardCount, final PileRef to) {
        if (from == null || to == null || from.equals(to) || cardCount < 1) {
            return false;
        }
        if (!this.canTake(from, cardCount)) {
            return false;
        }
        final List<Card> moving = this.peekMovingCards(from, cardCount);
        if (moving.isEmpty()) {
            return false;
        }
        return this.canPlace(moving, to);
    }

    private boolean canTake(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case STOCK -> false;
            case WASTE -> {
                this.checkStockWasteIndex(from.getIndex());
                yield cardCount == 1 && !this.waste.isEmpty();
            }
            case FOUNDATION -> {
                this.checkFoundationIndex(from.getIndex());
                yield cardCount == 1 && !this.foundations.get(from.getIndex()).isEmpty();
            }
            case TABLEAU -> {
                this.checkTableauIndex(from.getIndex());
                yield this.validFaceUpSequenceLengthFromTop(from.getIndex(), cardCount) == cardCount;
            }
        };
    }

    private List<Card> peekMovingCards(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case STOCK -> List.of();
            case WASTE -> {
                if (this.waste.isEmpty()) {
                    yield List.of();
                }
                yield List.of(this.waste.get(this.waste.size() - 1));
            }
            case FOUNDATION -> {
                final List<Card> pile = this.foundations.get(from.getIndex());
                if (pile.isEmpty()) {
                    yield List.of();
                }
                yield List.of(pile.get(pile.size() - 1));
            }
            case TABLEAU -> {
                final List<TableauCard> pile = this.tableau.get(from.getIndex());
                final int start = pile.size() - cardCount;
                final List<Card> cards = new ArrayList<>(cardCount);
                for (int i = start; i < pile.size(); i++) {
                    cards.add(pile.get(i).getCard());
                }
                yield List.copyOf(cards);
            }
        };
    }

    /**
     * Destination check. Empty tableau accepts only a King (or King-headed run).
     */
    private boolean canPlace(final List<Card> moving, final PileRef to) {
        final Card first = moving.get(0);
        return switch (to.getType()) {
            case STOCK, WASTE -> false;
            case FOUNDATION -> {
                this.checkFoundationIndex(to.getIndex());
                if (moving.size() != 1) {
                    yield false;
                }
                final Optional<Card> top = this.peekFoundation(to.getIndex());
                yield first.canPlaceOnFoundation(top.orElse(null));
            }
            case TABLEAU -> {
                this.checkTableauIndex(to.getIndex());
                final List<TableauCard> pile = this.tableau.get(to.getIndex());
                if (pile.isEmpty()) {
                    yield first.getRank() == Rank.KING;
                }
                final TableauCard destTop = pile.get(pile.size() - 1);
                if (!destTop.isFaceUp()) {
                    yield false;
                }
                yield first.canPlaceOnCascade(destTop.getCard());
            }
        };
    }

    private List<Card> takeCards(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case STOCK -> throw new IllegalStateException("Cannot take from stock; use draw()");
            case WASTE -> {
                final Card card = this.waste.remove(this.waste.size() - 1);
                yield new ArrayList<>(List.of(card));
            }
            case FOUNDATION -> {
                final List<Card> pile = this.foundations.get(from.getIndex());
                yield new ArrayList<>(List.of(pile.remove(pile.size() - 1)));
            }
            case TABLEAU -> {
                final List<TableauCard> pile = this.tableau.get(from.getIndex());
                final int start = pile.size() - cardCount;
                final List<Card> taken = new ArrayList<>(cardCount);
                for (int i = start; i < pile.size(); i++) {
                    taken.add(pile.get(i).getCard());
                }
                pile.subList(start, pile.size()).clear();
                yield taken;
            }
        };
    }

    private void placeCards(final List<Card> cards, final PileRef to) {
        switch (to.getType()) {
            case STOCK, WASTE -> throw new IllegalStateException("Cannot place onto " + to.getType());
            case FOUNDATION -> this.foundations.get(to.getIndex()).addAll(cards);
            case TABLEAU -> {
                final List<TableauCard> pile = this.tableau.get(to.getIndex());
                for (final Card card : cards) {
                    pile.add(new TableauCard(card, true));
                }
            }
        }
    }

    private void uncoverIfNeeded(final PileRef from) {
        if (from.getType() != PileType.TABLEAU) {
            return;
        }
        final List<TableauCard> pile = this.tableau.get(from.getIndex());
        if (pile.isEmpty()) {
            return;
        }
        final int last = pile.size() - 1;
        final TableauCard top = pile.get(last);
        if (!top.isFaceUp()) {
            pile.set(last, top.faceUp());
        }
    }

    private void updateWinStatus() {
        if (this.getFoundationCardCount() == DECK_SIZE) {
            this.status = GameStatus.WON;
        }
    }

    private void applyMoveScore(final PileRef from, final PileRef to) {
        if (this.scoring == ScoringMode.NONE) {
            return;
        }
        if (this.scoring == ScoringMode.VEGAS) {
            if (to.getType() == PileType.FOUNDATION) {
                this.score += 5;
            } else if (from.getType() == PileType.FOUNDATION) {
                this.score -= 5;
            }
            return;
        }
        // Standard
        if (to.getType() == PileType.FOUNDATION) {
            this.score += 10;
        }
        if (from.getType() == PileType.WASTE && to.getType() == PileType.TABLEAU) {
            this.score += 5;
        }
        if (from.getType() == PileType.FOUNDATION && to.getType() == PileType.TABLEAU) {
            this.score -= 15;
        }
    }

    private void applyRecyclePenalty() {
        if (this.scoring != ScoringMode.STANDARD) {
            return;
        }
        if (this.drawCount == 1 && this.passCount > 1) {
            this.score -= 100;
        } else if (this.drawCount == 3 && this.passCount > 3) {
            this.score -= 20;
        }
    }

    /**
     * Standard timed play: −2 points every 10 seconds.
     */
    public void applyTimePenalty() {
        if (this.status != GameStatus.PLAYING || this.scoring != ScoringMode.STANDARD) {
            return;
        }
        this.score -= 2;
    }

    /**
     * Timed Standard win bonus: 700,000 / seconds (classic Microsoft formula).
     */
    public void applyWinBonus(final int elapsedSeconds) {
        if (this.scoring != ScoringMode.STANDARD || elapsedSeconds <= 0) {
            return;
        }
        this.score += 700_000 / elapsedSeconds;
    }

    private void captureUndo() {
        this.undoSnapshot = Snapshot.capture(this);
    }

    private static final class Snapshot {
        private final List<List<TableauCard>> tableau;
        private final List<List<Card>> foundations;
        private final List<Card> stock;
        private final List<Card> waste;
        private final GameStatus status;
        private final int moveCount;
        private final int score;
        private final int passCount;
        private final int recycleCount;

        private Snapshot(final SolitaireGame game) {
            this.tableau = copyTableau(game.tableau);
            this.foundations = copyCards(game.foundations);
            this.stock = new ArrayList<>(game.stock);
            this.waste = new ArrayList<>(game.waste);
            this.status = game.status;
            this.moveCount = game.moveCount;
            this.score = game.score;
            this.passCount = game.passCount;
            this.recycleCount = game.recycleCount;
        }

        static Snapshot capture(final SolitaireGame game) {
            return new Snapshot(game);
        }

        void restore(final SolitaireGame game) {
            game.tableau.clear();
            game.tableau.addAll(copyTableau(this.tableau));
            game.foundations.clear();
            game.foundations.addAll(copyCards(this.foundations));
            game.stock.clear();
            game.stock.addAll(this.stock);
            game.waste.clear();
            game.waste.addAll(this.waste);
            game.status = this.status;
            game.moveCount = this.moveCount;
            game.score = this.score;
            game.passCount = this.passCount;
            game.recycleCount = this.recycleCount;
        }

        private static List<List<TableauCard>> copyTableau(final List<List<TableauCard>> source) {
            final List<List<TableauCard>> copy = new ArrayList<>(source.size());
            for (final List<TableauCard> pile : source) {
                copy.add(new ArrayList<>(pile));
            }
            return copy;
        }

        private static List<List<Card>> copyCards(final List<List<Card>> source) {
            final List<List<Card>> copy = new ArrayList<>(source.size());
            for (final List<Card> pile : source) {
                copy.add(new ArrayList<>(pile));
            }
            return copy;
        }
    }

    private void checkTableauIndex(final int index) {
        if (index < 0 || index >= TABLEAU_COUNT) {
            throw new IndexOutOfBoundsException("Tableau index: " + index);
        }
    }

    private void checkFoundationIndex(final int index) {
        if (index < 0 || index >= FOUNDATION_COUNT) {
            throw new IndexOutOfBoundsException("Foundation index: " + index);
        }
    }

    private void checkStockWasteIndex(final int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("Stock/waste index: " + index);
        }
    }

    // --- Test / fixture helpers (not used by normal play) ---

    /** Appends a tableau card with explicit face-up state. For fixtures only. */
    public void pushTableauForTest(final int index, final Card card, final boolean faceUp) {
        this.checkTableauIndex(index);
        this.tableau.get(index).add(new TableauCard(Objects.requireNonNull(card, "card"), faceUp));
    }

    /** Appends a card to a foundation without rule checks. For fixtures only. */
    public void pushFoundationForTest(final int index, final Card card) {
        this.checkFoundationIndex(index);
        this.foundations.get(index).add(Objects.requireNonNull(card, "card"));
        this.updateWinStatus();
    }

    /** Pushes a card onto the stock (new top). For fixtures only. */
    public void pushStockForTest(final Card card) {
        this.stock.add(Objects.requireNonNull(card, "card"));
    }

    /** Pushes a card onto the waste (new top). For fixtures only. */
    public void pushWasteForTest(final Card card) {
        this.waste.add(Objects.requireNonNull(card, "card"));
    }

    /** Draw One vs Draw Three. For fixtures only. */
    public void setDrawCountForTest(final int drawCount) {
        this.drawCount = this.normalizeDrawCount(drawCount);
    }

    /** Scoring system. For fixtures only. */
    public void setScoringForTest(final ScoringMode scoring) {
        this.scoring = Objects.requireNonNull(scoring, "scoring");
    }
}
