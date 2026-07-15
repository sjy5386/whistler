package com.sysbot32.whistler.freecell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Standard 52-card deck with injectable {@link Random} for reproducible shuffles.
 */
public final class Deck {
    private Deck() {
    }

    public static List<Card> createOrdered() {
        final List<Card> cards = new ArrayList<>(52);
        for (final Suit suit : Suit.values()) {
            for (final Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        return cards;
    }

    public static List<Card> createShuffled(final Random random) {
        Objects.requireNonNull(random, "random");
        final List<Card> cards = createOrdered();
        Collections.shuffle(cards, random);
        return cards;
    }
}
