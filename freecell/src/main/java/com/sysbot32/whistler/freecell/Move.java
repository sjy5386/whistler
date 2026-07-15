package com.sysbot32.whistler.freecell;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A completed move of one or more cards between piles (used for undo).
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class Move {
    private final PileRef from;
    private final PileRef to;
    private final int cardCount;

    @Override
    public String toString() {
        return "Move{" + this.from + " -> " + this.to + " x" + this.cardCount + "}";
    }
}
