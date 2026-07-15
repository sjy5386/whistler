package com.sysbot32.whistler.freecell;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * One atomic card transfer between piles.
 * Undo groups one player move plus any following auto-foundation transfers into a single step.
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
