package com.sysbot32.whistler.freecell;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
     * Whether this card can be stacked onto {@code target} in a cascade
     * (one rank lower, opposite color).
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
     * ({@code null} means empty foundation — Ace only).
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
