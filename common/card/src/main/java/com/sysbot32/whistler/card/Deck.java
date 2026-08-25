package com.sysbot32.whistler.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Standard 52-card deck (no jokers) with injectable {@link Random} for reproducible shuffles.
 */
public final class Deck {
    public static final int SIZE = 52;

    private Deck() {
    }

    public static List<Card> createOrdered() {
        final List<Card> cards = new ArrayList<>(SIZE);
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
