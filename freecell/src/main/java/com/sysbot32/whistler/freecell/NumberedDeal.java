package com.sysbot32.whistler.freecell;

import java.util.ArrayList;
import java.util.List;

/**
 * Numbered FreeCell deal generator (classic 1…32000 / extended 1…1_000_000 style).
 * <p>
 * Uses the widely published LCG shuffle for numbered FreeCell hands
 * (Jim Horne; also documented as Rosetta Code “Deal cards for FreeCell”):
 * <ul>
 *   <li>{@code state ← (214013 · state + 2531011) mod 2³¹}</li>
 *   <li>{@code rand ← state ≫ 16} (0…32767)</li>
 *   <li>Deck order: Ace♣…King♠, ranks outer / suits CDHS</li>
 *   <li>Fisher–Yates with that RNG; deal left-to-right, row by row across 8 columns</li>
 * </ul>
 */
public final class NumberedDeal {
    /** Classic Windows FreeCell range (1…32000). */
    public static final int STANDARD_MAX_GAME = 32_000;
    /** Extended range used by later Windows FreeCell builds (1…1_000_000). */
    public static final int EXTENDED_MAX_GAME = 1_000_000;

    private static final int LCG_MULT = 214_013;
    private static final int LCG_INCR = 2_531_011;
    private static final int LCG_MASK = 0x7FFF_FFFF;

    private NumberedDeal() {
    }

    /**
     * Returns 52 cards in numbered-deal order for {@code gameNumber}
     * (index 0 is the first card dealt to cascade 0).
     */
    public static List<Card> deal(final int gameNumber) {
        if (gameNumber < 1) {
            throw new IllegalArgumentException("Game number must be >= 1: " + gameNumber);
        }

        // Remaining pack as packed indices 0…51: rank*4 + suit (C=0,D=1,H=2,S=3).
        final int[] pack = new int[52];
        for (int i = 0; i < 52; i++) {
            pack[i] = i;
        }

        int state = gameNumber;
        final List<Card> dealt = new ArrayList<>(52);
        for (int remaining = 52; remaining > 0; remaining--) {
            state = nextState(state);
            final int r = state >>> 16;
            final int index = r % remaining;
            dealt.add(fromPackedIndex(pack[index]));
            pack[index] = pack[remaining - 1];
        }
        return dealt;
    }

    private static int nextState(final int state) {
        // 32-bit LCG then keep low 31 bits (classic FreeCell numbered-deal RNG).
        return (int) (((long) state * LCG_MULT + LCG_INCR) & LCG_MASK);
    }

    /**
     * Packed index 0 = Ace of Clubs … 51 = King of Spades (rank major, suit CDHS).
     */
    static Card fromPackedIndex(final int index) {
        if (index < 0 || index > 51) {
            throw new IllegalArgumentException("Card index out of range: " + index);
        }
        final Rank rank = Rank.values()[index / 4];
        final Suit suit = Suit.values()[index % 4];
        return new Card(suit, rank);
    }

    /** Inverse of {@link #fromPackedIndex(int)} for tests. */
    static int toPackedIndex(final Card card) {
        return card.getRank().ordinal() * 4 + card.getSuit().ordinal();
    }
}
