package com.sysbot32.whistler.freecell;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Suit {
    CLUBS("♣", false),
    DIAMONDS("♦", true),
    HEARTS("♥", true),
    SPADES("♠", false);

    private final String symbol;
    private final boolean red;

    public boolean isBlack() {
        return !this.red;
    }
}
