package com.sysbot32.whistler.freecell.card;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardTest {
    @Test
    void colorsAndLabels() {
        final Card heart = new Card(Suit.HEARTS, Rank.ACE);
        final Card spade = new Card(Suit.SPADES, Rank.KING);
        assertTrue(heart.isRed());
        assertTrue(spade.isBlack());
        assertTrue(heart.isOppositeColor(spade));
        assertEquals("A♥", heart.displayLabel());
        assertEquals("K♠", spade.displayLabel());
    }

    @Test
    void cascadePlacementAlternatingDescending() {
        final Card blackFive = new Card(Suit.CLUBS, Rank.FIVE);
        final Card redSix = new Card(Suit.HEARTS, Rank.SIX);
        final Card blackSix = new Card(Suit.SPADES, Rank.SIX);
        final Card redFour = new Card(Suit.DIAMONDS, Rank.FOUR);

        assertTrue(blackFive.canPlaceOnCascade(redSix));
        assertFalse(blackFive.canPlaceOnCascade(blackSix));
        assertFalse(redFour.canPlaceOnCascade(redSix));
        assertTrue(blackFive.canPlaceOnCascade(null));
    }

    @Test
    void foundationPlacementAscendingSameSuit() {
        final Card ace = new Card(Suit.DIAMONDS, Rank.ACE);
        final Card two = new Card(Suit.DIAMONDS, Rank.TWO);
        final Card three = new Card(Suit.DIAMONDS, Rank.THREE);
        final Card twoHearts = new Card(Suit.HEARTS, Rank.TWO);

        assertTrue(ace.canPlaceOnFoundation(null));
        assertFalse(two.canPlaceOnFoundation(null));
        assertTrue(two.canPlaceOnFoundation(ace));
        assertTrue(three.canPlaceOnFoundation(two));
        assertFalse(twoHearts.canPlaceOnFoundation(ace));
        assertFalse(three.canPlaceOnFoundation(ace));
    }
}
