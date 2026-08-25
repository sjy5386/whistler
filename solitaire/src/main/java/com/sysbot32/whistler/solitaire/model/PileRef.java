package com.sysbot32.whistler.solitaire.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Identifies a stock, waste, foundation, or tableau pile by type and index.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class PileRef {
    private final PileType type;
    private final int index;

    public static PileRef stock() {
        return new PileRef(PileType.STOCK, 0);
    }

    public static PileRef waste() {
        return new PileRef(PileType.WASTE, 0);
    }

    public static PileRef foundation(final int index) {
        return new PileRef(PileType.FOUNDATION, index);
    }

    public static PileRef tableau(final int index) {
        return new PileRef(PileType.TABLEAU, index);
    }

    @Override
    public String toString() {
        return this.type + "[" + this.index + "]";
    }
}
