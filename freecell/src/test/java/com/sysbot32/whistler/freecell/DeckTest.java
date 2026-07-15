package com.sysbot32.whistler.freecell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeckTest {
    @Test
    void orderedDeckHas52UniqueCards() {
        final List<Card> cards = Deck.createOrdered();
        assertEquals(52, cards.size());
        assertEquals(52, new HashSet<>(cards).size());
    }

    @Test
    void shuffleIsReproducibleWithSameSeed() {
        final List<Card> a = Deck.createShuffled(new Random(42));
        final List<Card> b = Deck.createShuffled(new Random(42));
        final List<Card> c = Deck.createShuffled(new Random(43));
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void shuffledDeckStillHasAllCards() {
        final Set<Card> expected = new HashSet<>(Deck.createOrdered());
        final Set<Card> actual = new HashSet<>(Deck.createShuffled(new Random(7)));
        assertEquals(expected, actual);
    }
}
