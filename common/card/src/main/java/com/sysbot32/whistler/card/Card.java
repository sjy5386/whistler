package com.sysbot32.whistler.card;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Identity of one card in a standard 52-card deck (no jokers).
 * Empty-pile acceptance is a per-game rule — do not treat
 * {@link #canPlaceOnCascade(Card)} with a {@code null} target as Klondike law.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class Card {
    private final Suit suit;
    private final Rank rank;

    public boolean isRed() {
        return this.suit.isRed();
    }

    public boolean isBlack() {
        return this.suit.isBlack();
    }

    public boolean isOppositeColor(final Card other) {
        return this.isRed() != other.isRed();
    }

    /**
     * Whether this card can be stacked onto {@code target} as a descending
     * alternating-color build (one rank lower, opposite color).
     * <p>
     * A {@code null} target means an empty FreeCell cascade (any card). Klondike
     * empty tableau must not call this with {@code null}; that game accepts only a King.
     */
    public boolean canPlaceOnCascade(final Card target) {
        if (target == null) {
            return true;
        }
        return this.rank.getValue() == target.rank.getValue() - 1
                && this.isOppositeColor(target);
    }

    /**
     * Whether this card can be placed onto a foundation whose current top is {@code top}
     * ({@code null} means empty foundation — Ace only). Same-suit ascending Ace→King.
     */
    public boolean canPlaceOnFoundation(final Card top) {
        if (top == null) {
            return this.rank == Rank.ACE;
        }
        return this.suit == top.suit
                && this.rank.getValue() == top.rank.getValue() + 1;
    }

    public String displayLabel() {
        return this.rank.getLabel() + this.suit.getSymbol();
    }

    @Override
    public String toString() {
        return this.displayLabel();
    }
}
