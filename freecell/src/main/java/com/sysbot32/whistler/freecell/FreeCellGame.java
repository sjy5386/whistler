package com.sysbot32.whistler.freecell;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Pure FreeCell rules: deal, legal moves (including supermove), win detection, undo.
 */
@Getter
public class FreeCellGame {
    public static final int FREE_CELL_COUNT = 4;
    public static final int FOUNDATION_COUNT = 4;
    public static final int CASCADE_COUNT = 8;
    public static final int DECK_SIZE = 52;

    private final Card[] freeCells = new Card[FREE_CELL_COUNT];
    private final List<List<Card>> foundations = new ArrayList<>(FOUNDATION_COUNT);
    private final List<List<Card>> cascades = new ArrayList<>(CASCADE_COUNT);
    private final Deque<Move> undoStack = new ArrayDeque<>();

    private GameStatus status = GameStatus.PLAYING;
    private final long seed;
    private int moveCount;

    public FreeCellGame() {
        this(System.nanoTime());
    }

    public FreeCellGame(final long seed) {
        this(seed, new Random(seed));
    }

    public FreeCellGame(final long seed, final Random random) {
        Objects.requireNonNull(random, "random");
        this.seed = seed;
        for (int i = 0; i < FOUNDATION_COUNT; i++) {
            this.foundations.add(new ArrayList<>());
        }
        for (int i = 0; i < CASCADE_COUNT; i++) {
            this.cascades.add(new ArrayList<>());
        }
        this.deal(Deck.createShuffled(random));
    }

    /**
     * Builds a game from an explicit 52-card deal order (cascade fill left-to-right, top-to-bottom).
     * Intended for tests and fixtures.
     */
    public static FreeCellGame fromDeal(final List<Card> cards) {
        Objects.requireNonNull(cards, "cards");
        if (cards.size() != DECK_SIZE) {
            throw new IllegalArgumentException("Deal must contain exactly " + DECK_SIZE + " cards");
        }
        final FreeCellGame game = new FreeCellGame(0L, new Random(0));
        // Constructor already dealt; clear and re-deal with the given order.
        game.clearBoard();
        game.deal(cards);
        game.undoStack.clear();
        game.moveCount = 0;
        game.status = GameStatus.PLAYING;
        return game;
    }

    /**
     * Empty board for carefully constructed test positions.
     */
    public static FreeCellGame emptyBoard() {
        final FreeCellGame game = new FreeCellGame(0L, new Random(0));
        game.clearBoard();
        game.undoStack.clear();
        game.moveCount = 0;
        game.status = GameStatus.PLAYING;
        return game;
    }

    private void clearBoard() {
        for (int i = 0; i < FREE_CELL_COUNT; i++) {
            this.freeCells[i] = null;
        }
        for (final List<Card> foundation : this.foundations) {
            foundation.clear();
        }
        for (final List<Card> cascade : this.cascades) {
            cascade.clear();
        }
    }

    private void deal(final List<Card> cards) {
        for (int i = 0; i < cards.size(); i++) {
            this.cascades.get(i % CASCADE_COUNT).add(cards.get(i));
        }
    }

    public Optional<Card> getFreeCell(final int index) {
        this.checkFreeCellIndex(index);
        return Optional.ofNullable(this.freeCells[index]);
    }

    public List<Card> getFoundation(final int index) {
        this.checkFoundationIndex(index);
        return Collections.unmodifiableList(this.foundations.get(index));
    }

    public List<Card> getCascade(final int index) {
        this.checkCascadeIndex(index);
        return Collections.unmodifiableList(this.cascades.get(index));
    }

    public Optional<Card> peekFoundation(final int index) {
        this.checkFoundationIndex(index);
        final List<Card> pile = this.foundations.get(index);
        if (pile.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pile.get(pile.size() - 1));
    }

    public Optional<Card> peekCascade(final int index) {
        this.checkCascadeIndex(index);
        final List<Card> pile = this.cascades.get(index);
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

    public int countEmptyFreeCells() {
        int count = 0;
        for (final Card cell : this.freeCells) {
            if (cell == null) {
                count++;
            }
        }
        return count;
    }

    public int countEmptyCascades() {
        int count = 0;
        for (final List<Card> cascade : this.cascades) {
            if (cascade.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Classic FreeCell supermove capacity.
     * <p>
     * {@code max = (emptyFreeCells + 1) * 2^emptyCascades}, where empty cascades
     * exclude the destination when the destination is an empty cascade (it cannot
     * serve as an intermediate helper column).
     */
    public int maxMovableCards(final boolean destinationIsEmptyCascade) {
        int emptyCascades = this.countEmptyCascades();
        if (destinationIsEmptyCascade) {
            emptyCascades = Math.max(0, emptyCascades - 1);
        }
        final int emptyFreeCells = this.countEmptyFreeCells();
        // Cap at deck size; 2^emptyCascades grows fast but emptyCascades ≤ 7 in practice.
        if (emptyCascades >= 10) {
            return DECK_SIZE;
        }
        return (emptyFreeCells + 1) * (1 << emptyCascades);
    }

    /**
     * Length of the valid descending alternating sequence ending at the cascade top,
     * starting from {@code startFromTop} cards from the top (1 = only top card).
     * Returns 0 if the sequence is invalid.
     */
    public int validSequenceLengthFromTop(final int cascadeIndex, final int cardsFromTop) {
        this.checkCascadeIndex(cascadeIndex);
        if (cardsFromTop < 1) {
            return 0;
        }
        final List<Card> cascade = this.cascades.get(cascadeIndex);
        if (cardsFromTop > cascade.size()) {
            return 0;
        }
        final int start = cascade.size() - cardsFromTop;
        for (int i = start; i < cascade.size() - 1; i++) {
            final Card lower = cascade.get(i + 1);
            final Card upper = cascade.get(i);
            if (!lower.canPlaceOnCascade(upper)) {
                return 0;
            }
        }
        return cardsFromTop;
    }

    /**
     * Maximum legal sequence length that can be selected from the top of a cascade
     * (valid alternating run, not yet limited by supermove destination).
     */
    public int maxSelectableFromCascade(final int cascadeIndex) {
        this.checkCascadeIndex(cascadeIndex);
        final List<Card> cascade = this.cascades.get(cascadeIndex);
        if (cascade.isEmpty()) {
            return 0;
        }
        int length = 1;
        for (int i = cascade.size() - 1; i > 0; i--) {
            final Card lower = cascade.get(i);
            final Card upper = cascade.get(i - 1);
            if (!lower.canPlaceOnCascade(upper)) {
                break;
            }
            length++;
        }
        return length;
    }

    public boolean canMove(final PileRef from, final int cardCount, final PileRef to) {
        if (this.status == GameStatus.WON) {
            return false;
        }
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
        return this.canPlace(moving, to, from);
    }

    /**
     * Applies a move. Returns {@code true} if the board changed.
     */
    public boolean move(final PileRef from, final int cardCount, final PileRef to) {
        if (!this.canMove(from, cardCount, to)) {
            return false;
        }
        final List<Card> moving = this.takeCards(from, cardCount);
        this.placeCards(moving, to);
        this.undoStack.push(new Move(from, to, cardCount));
        this.moveCount++;
        this.updateWinStatus();
        return true;
    }

    /**
     * Tries to move the single accessible card at {@code from} onto any legal foundation.
     * Preferred for double-click / auto-move UX.
     */
    public boolean tryMoveToFoundation(final PileRef from) {
        if (this.status == GameStatus.WON) {
            return false;
        }
        if (!this.canTake(from, 1)) {
            return false;
        }
        for (int i = 0; i < FOUNDATION_COUNT; i++) {
            final PileRef foundation = PileRef.foundation(i);
            if (this.move(from, 1, foundation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * After a successful user move, auto-moves any cards that can safely go to foundations
     * when it is the unique minimal next rank overall (simple helper; optional UI use).
     * This implementation only auto-moves when a card can go to foundation and its rank
     * is at most one above the lowest foundation top (or Ace). Keeps play conservative.
     */
    public int autoMoveToFoundations() {
        if (this.status == GameStatus.WON) {
            return 0;
        }
        int moved = 0;
        boolean progress;
        do {
            progress = false;
            // Free cells
            for (int i = 0; i < FREE_CELL_COUNT; i++) {
                final Card card = this.freeCells[i];
                if (card == null) {
                    continue;
                }
                if (this.isSafeAutoFoundation(card)
                        && this.tryMoveToFoundation(PileRef.freeCell(i))) {
                    moved++;
                    progress = true;
                    break;
                }
            }
            if (progress) {
                continue;
            }
            // Cascade tops
            for (int i = 0; i < CASCADE_COUNT; i++) {
                final Optional<Card> top = this.peekCascade(i);
                if (top.isEmpty()) {
                    continue;
                }
                if (this.isSafeAutoFoundation(top.get())
                        && this.tryMoveToFoundation(PileRef.cascade(i))) {
                    moved++;
                    progress = true;
                    break;
                }
            }
        } while (progress);
        return moved;
    }

    /**
     * Classic safe auto-move: Aces always; otherwise only when both opposite-color
     * foundations are built to at least {@code card.rank - 1}
     * ({@code card.rank <= min(opposite foundations) + 1}, empty suit = 0).
     */
    private boolean isSafeAutoFoundation(final Card card) {
        if (card.getRank() == Rank.ACE) {
            return true;
        }
        final int minOpposite = card.isRed()
                ? Math.min(this.foundationRank(Suit.CLUBS), this.foundationRank(Suit.SPADES))
                : Math.min(this.foundationRank(Suit.DIAMONDS), this.foundationRank(Suit.HEARTS));
        return card.getRank().getValue() <= minOpposite + 1;
    }

    /** Highest rank of {@code suit} currently on any foundation, or 0 if none. */
    private int foundationRank(final Suit suit) {
        for (final List<Card> foundation : this.foundations) {
            if (foundation.isEmpty()) {
                continue;
            }
            // Each foundation holds a single suit once started
            if (foundation.get(0).getSuit() == suit) {
                return foundation.get(foundation.size() - 1).getRank().getValue();
            }
        }
        return 0;
    }

    public boolean canUndo() {
        return !this.undoStack.isEmpty() && this.status != GameStatus.WON;
    }

    /**
     * Undoes the last move. Win state is not undone once set (game finished).
     * Returns {@code true} if a move was undone.
     */
    public boolean undo() {
        if (!this.canUndo()) {
            return false;
        }
        final Move last = this.undoStack.pop();
        final List<Card> cards = this.takeCards(last.getTo(), last.getCardCount());
        this.placeCards(cards, last.getFrom());
        this.moveCount = Math.max(0, this.moveCount - 1);
        this.status = GameStatus.PLAYING;
        return true;
    }

    private boolean canTake(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case FREE_CELL -> {
                this.checkFreeCellIndex(from.getIndex());
                yield cardCount == 1 && this.freeCells[from.getIndex()] != null;
            }
            case FOUNDATION -> {
                this.checkFoundationIndex(from.getIndex());
                // Foundations are not a source for normal play; still allow for undo path only via takeCards.
                yield false;
            }
            case CASCADE -> {
                this.checkCascadeIndex(from.getIndex());
                yield this.validSequenceLengthFromTop(from.getIndex(), cardCount) == cardCount;
            }
        };
    }

    private List<Card> peekMovingCards(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case FREE_CELL -> {
                final Card card = this.freeCells[from.getIndex()];
                yield card == null ? List.of() : List.of(card);
            }
            case FOUNDATION -> List.of();
            case CASCADE -> {
                final List<Card> cascade = this.cascades.get(from.getIndex());
                final int start = cascade.size() - cardCount;
                yield List.copyOf(cascade.subList(start, cascade.size()));
            }
        };
    }

    private boolean canPlace(final List<Card> moving, final PileRef to, final PileRef from) {
        final Card first = moving.get(0);
        return switch (to.getType()) {
            case FREE_CELL -> {
                this.checkFreeCellIndex(to.getIndex());
                yield moving.size() == 1 && this.freeCells[to.getIndex()] == null;
            }
            case FOUNDATION -> {
                this.checkFoundationIndex(to.getIndex());
                if (moving.size() != 1) {
                    yield false;
                }
                final Optional<Card> top = this.peekFoundation(to.getIndex());
                yield first.canPlaceOnFoundation(top.orElse(null));
            }
            case CASCADE -> {
                this.checkCascadeIndex(to.getIndex());
                final List<Card> cascade = this.cascades.get(to.getIndex());
                final boolean emptyDest = cascade.isEmpty();
                if (!emptyDest && !first.canPlaceOnCascade(cascade.get(cascade.size() - 1))) {
                    yield false;
                }
                if (moving.size() == 1) {
                    yield true;
                }
                // Supermove: multi-card moves only from cascade to cascade.
                if (from.getType() != PileType.CASCADE) {
                    yield false;
                }
                final int max = this.maxMovableCards(emptyDest);
                yield moving.size() <= max;
            }
        };
    }

    private List<Card> takeCards(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case FREE_CELL -> {
                final Card card = this.freeCells[from.getIndex()];
                this.freeCells[from.getIndex()] = null;
                yield new ArrayList<>(List.of(card));
            }
            case FOUNDATION -> {
                final List<Card> pile = this.foundations.get(from.getIndex());
                final List<Card> taken = new ArrayList<>(cardCount);
                for (int i = 0; i < cardCount; i++) {
                    taken.add(0, pile.remove(pile.size() - 1));
                }
                yield taken;
            }
            case CASCADE -> {
                final List<Card> cascade = this.cascades.get(from.getIndex());
                final int start = cascade.size() - cardCount;
                final List<Card> taken = new ArrayList<>(cascade.subList(start, cascade.size()));
                cascade.subList(start, cascade.size()).clear();
                yield taken;
            }
        };
    }

    private void placeCards(final List<Card> cards, final PileRef to) {
        switch (to.getType()) {
            case FREE_CELL -> {
                if (cards.size() != 1) {
                    throw new IllegalStateException("Free cell accepts one card");
                }
                this.freeCells[to.getIndex()] = cards.get(0);
            }
            case FOUNDATION -> {
                this.foundations.get(to.getIndex()).addAll(cards);
            }
            case CASCADE -> {
                this.cascades.get(to.getIndex()).addAll(cards);
            }
        }
    }

    private void updateWinStatus() {
        if (this.getFoundationCardCount() == DECK_SIZE) {
            this.status = GameStatus.WON;
        }
    }

    private void checkFreeCellIndex(final int index) {
        if (index < 0 || index >= FREE_CELL_COUNT) {
            throw new IndexOutOfBoundsException("Free cell index: " + index);
        }
    }

    private void checkFoundationIndex(final int index) {
        if (index < 0 || index >= FOUNDATION_COUNT) {
            throw new IndexOutOfBoundsException("Foundation index: " + index);
        }
    }

    private void checkCascadeIndex(final int index) {
        if (index < 0 || index >= CASCADE_COUNT) {
            throw new IndexOutOfBoundsException("Cascade index: " + index);
        }
    }

    // --- Test / fixture helpers (not used by normal play) ---

    /** Places a card into an empty free cell. For fixtures only. */
    public void putFreeCellForTest(final int index, final Card card) {
        this.checkFreeCellIndex(index);
        if (this.freeCells[index] != null) {
            throw new IllegalStateException("Free cell already occupied: " + index);
        }
        this.freeCells[index] = Objects.requireNonNull(card, "card");
    }

    /** Appends a card to a cascade. For fixtures only. */
    public void pushCascadeForTest(final int index, final Card card) {
        this.checkCascadeIndex(index);
        this.cascades.get(index).add(Objects.requireNonNull(card, "card"));
    }

    /** Appends a card to a foundation without rule checks. For fixtures only. */
    public void pushFoundationForTest(final int index, final Card card) {
        this.checkFoundationIndex(index);
        this.foundations.get(index).add(Objects.requireNonNull(card, "card"));
        this.updateWinStatus();
    }
}
