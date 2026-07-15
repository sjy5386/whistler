package com.sysbot32.whistler.freecell;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Identifies a free cell, foundation, or cascade pile by type and index.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class PileRef {
    private final PileType type;
    private final int index;

    public static PileRef freeCell(final int index) {
        return new PileRef(PileType.FREE_CELL, index);
    }

    public static PileRef foundation(final int index) {
        return new PileRef(PileType.FOUNDATION, index);
    }

    public static PileRef cascade(final int index) {
        return new PileRef(PileType.CASCADE, index);
    }

    @Override
    public String toString() {
        return this.type + "[" + this.index + "]";
    }
}
