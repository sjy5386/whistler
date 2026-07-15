package com.sysbot32.whistler.freecell;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure FreeCell rules: deal, legal moves (including supermove), win detection, undo.
 * <p>
 * Deals use the classic numbered FreeCell algorithm ({@link NumberedDeal}).
 * <p>
 * Classic FreeCell fact-check (official help + original {@code freecell.exe} playtest):
 * <ul>
 *   <li>Home/foundation cards cannot be moved back into free cells or cascades.
 *       Official play help only lists cascade bottoms and free cells as sources;
 *       original binary confirmed home cells are not takeable.</li>
 *   <li>After a player move, safe cards auto-transfer to home cells. Undo rewinds
 *       that player move and its following auto-transfers as one step (not per card).</li>
 * </ul>
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
    /**
     * Each entry is one undoable player step: the voluntary move plus any auto-moves
     * that followed it (classic FreeCell batches those on a single Undo).
     */
    private final Deque<List<Move>> undoStack = new ArrayDeque<>();

    private GameStatus status = GameStatus.PLAYING;
    /** Numbered deal id (1…), or 0 for fixture boards. */
    private final int gameNumber;
    private int moveCount;

    /** New game with a random classic numbered deal (1…32000). */
    public FreeCellGame() {
        this(randomClassicGameNumber());
    }

    /**
     * New game using the classic numbered deal for {@code gameNumber}
     * (compatible with common FreeCell game-number layouts).
     */
    public FreeCellGame(final int gameNumber) {
        if (gameNumber < 1) {
            throw new IllegalArgumentException("Game number must be >= 1: " + gameNumber);
        }
        this.gameNumber = gameNumber;
        this.initPiles();
        this.deal(NumberedDeal.deal(gameNumber));
    }

    /** Fixture constructor: empty board, optional game-number label for tests. */
    private FreeCellGame(final int gameNumber, final boolean fixture) {
        this.gameNumber = gameNumber;
        this.initPiles();
    }

    private void initPiles() {
        for (int i = 0; i < FOUNDATION_COUNT; i++) {
            this.foundations.add(new ArrayList<>());
        }
        for (int i = 0; i < CASCADE_COUNT; i++) {
            this.cascades.add(new ArrayList<>());
        }
    }

    private static int randomClassicGameNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(NumberedDeal.STANDARD_MAX_GAME);
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
        final FreeCellGame game = new FreeCellGame(0, true);
        game.deal(cards);
        return game;
    }

    /**
     * Empty board for carefully constructed test positions.
     */
    public static FreeCellGame emptyBoard() {
        return new FreeCellGame(0, true);
    }

    private void deal(final List<Card> cards) {
        for (int i = 0; i < cards.size(); i++) {
            this.cascades.get(i % CASCADE_COUNT).add(cards.get(i));
        }
    }

    /** Alias kept for older call sites / status text. */
    public long getSeed() {
        return this.gameNumber;
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
     * Applies a player move and records it as a new undo step.
     * Call {@link #autoMoveToFoundations()} afterward so safe home transfers attach to this step.
     *
     * @return {@code true} if the board changed
     */
    public boolean move(final PileRef from, final int cardCount, final PileRef to) {
        final Move applied = this.applyMove(from, cardCount, to);
        if (applied == null) {
            return false;
        }
        final List<Move> turn = new ArrayList<>(4);
        turn.add(applied);
        this.undoStack.push(turn);
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
     * Auto-moves safe cards to foundations (classic "unneeded card" transfer).
     * <p>
     * Safe rule: Aces always; otherwise only when both opposite-color foundations are built
     * high enough that the card cannot be needed for tableau packing
     * ({@code rank <= min(opposite foundation ranks) + 1}, empty suit = 0).
     * <p>
     * Any transfers performed here are appended to the <em>current</em> undo step so that
     * one Undo rewinds the player move and all following auto-moves together (verified on
     * original classic FreeCell).
     *
     * @return number of cards moved to foundations
     */
    public int autoMoveToFoundations() {
        if (this.status == GameStatus.WON) {
            return 0;
        }
        final List<Move> autos = new ArrayList<>();
        boolean progress;
        do {
            progress = false;
            for (int i = 0; i < FREE_CELL_COUNT; i++) {
                final Card card = this.freeCells[i];
                if (card == null || !this.isSafeAutoFoundation(card)) {
                    continue;
                }
                final Move applied = this.tryApplyToAnyFoundation(PileRef.freeCell(i));
                if (applied != null) {
                    autos.add(applied);
                    progress = true;
                    break;
                }
            }
            if (progress) {
                continue;
            }
            for (int i = 0; i < CASCADE_COUNT; i++) {
                final Optional<Card> top = this.peekCascade(i);
                if (top.isEmpty() || !this.isSafeAutoFoundation(top.get())) {
                    continue;
                }
                final Move applied = this.tryApplyToAnyFoundation(PileRef.cascade(i));
                if (applied != null) {
                    autos.add(applied);
                    progress = true;
                    break;
                }
            }
        } while (progress);

        if (!autos.isEmpty()) {
            if (!this.undoStack.isEmpty()) {
                this.undoStack.peek().addAll(autos);
            } else {
                // Auto-only batch (e.g. tests calling auto-move without a prior user move).
                this.undoStack.push(new ArrayList<>(autos));
            }
        }
        return autos.size();
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
     * Undoes the last player step (voluntary move + any auto home transfers in that step).
     * Win state is not undone once set (game finished).
     *
     * @return {@code true} if a step was undone
     */
    public boolean undo() {
        if (!this.canUndo()) {
            return false;
        }
        final List<Move> turn = this.undoStack.pop();
        for (int i = turn.size() - 1; i >= 0; i--) {
            final Move step = turn.get(i);
            final List<Card> cards = this.takeCards(step.getTo(), step.getCardCount());
            this.placeCards(cards, step.getFrom());
            this.moveCount = Math.max(0, this.moveCount - 1);
        }
        this.status = GameStatus.PLAYING;
        return true;
    }

    /**
     * Applies a legal move without creating a new undo step. Updates {@link #moveCount}
     * and win status. Returns the recorded {@link Move}, or {@code null} if illegal.
     */
    private Move applyMove(final PileRef from, final int cardCount, final PileRef to) {
        if (!this.canMove(from, cardCount, to)) {
            return null;
        }
        final List<Card> moving = this.takeCards(from, cardCount);
        this.placeCards(moving, to);
        this.moveCount++;
        this.updateWinStatus();
        return new Move(from, to, cardCount);
    }

    private Move tryApplyToAnyFoundation(final PileRef from) {
        if (!this.canTake(from, 1)) {
            return null;
        }
        for (int i = 0; i < FOUNDATION_COUNT; i++) {
            final Move applied = this.applyMove(from, 1, PileRef.foundation(i));
            if (applied != null) {
                return applied;
            }
        }
        return null;
    }

    private boolean canTake(final PileRef from, final int cardCount) {
        return switch (from.getType()) {
            case FREE_CELL -> {
                this.checkFreeCellIndex(from.getIndex());
                yield cardCount == 1 && this.freeCells[from.getIndex()] != null;
            }
            case FOUNDATION -> {
                this.checkFoundationIndex(from.getIndex());
                // Classic FreeCell: home cells are sinks only (cannot take back into play).
                // Undo still uses takeCards() directly when reversing a recorded step.
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
