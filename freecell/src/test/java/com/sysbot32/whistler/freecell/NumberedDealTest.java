package com.sysbot32.whistler.freecell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Golden vectors from Rosetta Code “Deal cards for FreeCell” (numbered-deal LCG).
 */
class NumberedDealTest {
    @Test
    void gameOneDealOrder() {
        assertEquals(
                "JD 2D 9H JC 5D 7H 7C 5H "
                        + "KD KC 9S 5S AD QC KH 3H "
                        + "2S KS 9D QD JS AS AH 3C "
                        + "4C 5C TS QH 4H AC 4D 7S "
                        + "3S TD 4S TH 8H 2C JH 7D "
                        + "6D 8S 8D QS 6C 3D 8C TC "
                        + "6S 9C 2H 6H",
                formatDeal(NumberedDeal.deal(1))
        );
    }

    @Test
    void game617DealOrder() {
        assertEquals(
                "7D AD 5C 3S 5S 8C 2D AH "
                        + "TD 7S QD AC 6D 8H AS KH "
                        + "TH QC 3H 9D 6S 8D 3D TC "
                        + "KD 5H 9S 3C 8S 7H 4D JS "
                        + "4C QS 9C 9H 7C 6H 2C 2S "
                        + "4S TS 2H 5D JC 6C JH QH "
                        + "JD KS KC 4H",
                formatDeal(NumberedDeal.deal(617))
        );
    }

    @Test
    void game11982DealOrder() {
        // Well-known unsolvable classic numbered deal
        assertEquals(
                "AH AS 4H AC 2D 6S TS JS "
                        + "3D 3H QS QC 8S 7H AD KS "
                        + "KD 6H 5S 4D 9H JH 9S 3C "
                        + "JC 5D 5C 8C 9D TD KH 7C "
                        + "6C 2C TH QH 6D TC 4S 7S "
                        + "JD 7D 8H 9C 2H QD 4C 5H "
                        + "KC 8D 2S 3S",
                formatDeal(NumberedDeal.deal(11982))
        );
    }

    @Test
    void dealContainsAllFiftyTwoCards() {
        final List<Card> deal = NumberedDeal.deal(42);
        assertEquals(52, deal.size());
        assertEquals(52, new HashSet<>(deal).size());
    }

    @Test
    void rejectsNonPositiveGameNumber() {
        assertThrows(IllegalArgumentException.class, () -> NumberedDeal.deal(0));
        assertThrows(IllegalArgumentException.class, () -> NumberedDeal.deal(-5));
    }

    @Test
    void packedIndexRoundTrip() {
        for (int i = 0; i < 52; i++) {
            final Card card = NumberedDeal.fromPackedIndex(i);
            assertEquals(i, NumberedDeal.toPackedIndex(card));
        }
        assertEquals(new Card(Suit.CLUBS, Rank.ACE), NumberedDeal.fromPackedIndex(0));
        assertEquals(new Card(Suit.SPADES, Rank.KING), NumberedDeal.fromPackedIndex(51));
    }

    private static String formatDeal(final List<Card> cards) {
        return cards.stream()
                .map(NumberedDealTest::asciiLabel)
                .collect(Collectors.joining(" "));
    }

    private static String asciiLabel(final Card card) {
        final char suit = switch (card.getSuit()) {
            case CLUBS -> 'C';
            case DIAMONDS -> 'D';
            case HEARTS -> 'H';
            case SPADES -> 'S';
        };
        // Classic notation uses T for ten, not "10"
        final String rank = card.getRank() == Rank.TEN ? "T" : card.getRank().getLabel();
        return rank + suit;
    }
}
