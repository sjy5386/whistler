package com.sysbot32.whistler.freecell;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Rank {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");

    private final int value;
    private final String label;

    public Rank next() {
        final int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length) {
            return null;
        }
        return values()[ordinal];
    }

    public Rank previous() {
        final int ordinal = this.ordinal() - 1;
        if (ordinal < 0) {
            return null;
        }
        return values()[ordinal];
    }
}
