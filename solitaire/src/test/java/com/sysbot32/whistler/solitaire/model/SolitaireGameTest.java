package com.sysbot32.whistler.solitaire.model;

import com.sysbot32.whistler.card.Card;
import com.sysbot32.whistler.card.Deck;
import com.sysbot32.whistler.card.Rank;
import com.sysbot32.whistler.card.Suit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolitaireGameTest {
    private static Card card(final Suit suit, final Rank rank) {
        return new Card(suit, rank);
    }

    @Test
    void dealUsesFiftyTwoUniqueCardsWithClassicLayout() {
        final SolitaireGame game = SolitaireGame.fromDeal(Deck.createOrdered());
        final Set<Card> seen = new HashSet<>();
        int tableauTotal = 0;
        for (int i = 0; i < SolitaireGame.TABLEAU_COUNT; i++) {
            final List<TableauCard> pile = game.getTableau(i);
            assertEquals(i + 1, pile.size(), "tableau " + i + " size");
            tableauTotal += pile.size();
            for (int c = 0; c < pile.size(); c++) {
                final TableauCard sitting = pile.get(c);
                final boolean expectFaceUp = c == pile.size() - 1;
                assertEquals(expectFaceUp, sitting.isFaceUp(), "tableau " + i + " card " + c);
                assertTrue(seen.add(sitting.getCard()), "duplicate " + sitting.getCard());
            }
        }
        assertEquals(28, tableauTotal);
        assertEquals(24, game.getStock().size());
        assertTrue(game.getWaste().isEmpty());
        for (int i = 0; i < SolitaireGame.FOUNDATION_COUNT; i++) {
            assertTrue(game.getFoundation(i).isEmpty());
        }
        for (final Card stockCard : game.getStock()) {
            assertTrue(seen.add(stockCard), "duplicate stock " + stockCard);
        }
        assertEquals(52, seen.size());
        assertEquals(GameStatus.PLAYING, game.getStatus());
        assertFalse(game.isWon());
    }

    @Test
    void shuffledDealStillHasClassicShape() {
        final SolitaireGame game = new SolitaireGame(new Random(42));
        int total = game.getStock().size() + game.getWaste().size();
        for (int i = 0; i < SolitaireGame.TABLEAU_COUNT; i++) {
            assertEquals(i + 1, game.getTableau(i).size());
            total += game.getTableau(i).size();
            assertTrue(game.peekTableau(i).orElseThrow().isFaceUp());
            for (int c = 0; c < i; c++) {
                assertFalse(game.getTableau(i).get(c).isFaceUp());
            }
        }
        for (int i = 0; i < SolitaireGame.FOUNDATION_COUNT; i++) {
            total += game.getFoundation(i).size();
        }
        assertEquals(52, total);
        assertEquals(24, game.getStock().size());
        assertTrue(game.getWaste().isEmpty());
    }

    @Test
    void sameSeedProducesSameDeal() {
        final SolitaireGame a = new SolitaireGame(new Random(99));
        final SolitaireGame b = new SolitaireGame(new Random(99));
        final SolitaireGame c = new SolitaireGame(new Random(100));
        for (int i = 0; i < SolitaireGame.TABLEAU_COUNT; i++) {
            assertEquals(a.getTableau(i), b.getTableau(i));
        }
        assertEquals(a.getStock(), b.getStock());
        assertNotEquals(a.getStock(), c.getStock());
    }

    @Test
    void fromDealRejectsWrongSize() {
        assertThrows(IllegalArgumentException.class, () -> SolitaireGame.fromDeal(List.of()));
        final List<Card> tooMany = new ArrayList<>(Deck.createOrdered());
        tooMany.add(card(Suit.HEARTS, Rank.ACE));
        assertThrows(IllegalArgumentException.class, () -> SolitaireGame.fromDeal(tooMany));
        final List<Card> dups = new ArrayList<>(Deck.createOrdered());
        dups.set(51, dups.get(0));
        assertThrows(IllegalArgumentException.class, () -> SolitaireGame.fromDeal(dups));
    }

    @Test
    void tableauBuildsDownAlternatingColor() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.HEARTS, Rank.SIX), true);
        game.pushTableauForTest(1, card(Suit.CLUBS, Rank.FIVE), true);
        game.pushTableauForTest(2, card(Suit.DIAMONDS, Rank.FIVE), true);
        game.pushTableauForTest(3, card(Suit.SPADES, Rank.FOUR), true);

        assertTrue(game.canMove(PileRef.tableau(1), 1, PileRef.tableau(0)));
        assertFalse(game.canMove(PileRef.tableau(2), 1, PileRef.tableau(0)));
        assertFalse(game.canMove(PileRef.tableau(3), 1, PileRef.tableau(0)));

        assertTrue(game.move(PileRef.tableau(1), 1, PileRef.tableau(0)));
        assertEquals(2, game.getTableau(0).size());
        assertEquals(card(Suit.CLUBS, Rank.FIVE), game.peekTableau(0).orElseThrow().getCard());
        assertTrue(game.getTableau(1).isEmpty());
        assertFalse(game.move(PileRef.tableau(2), 1, PileRef.tableau(0)));
    }

    @Test
    void emptyTableauAcceptsOnlyKingOrKingHeadedRun() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.HEARTS, Rank.QUEEN), true);
        game.pushTableauForTest(1, card(Suit.SPADES, Rank.KING), true);
        game.pushTableauForTest(1, card(Suit.HEARTS, Rank.QUEEN), true);
        game.pushTableauForTest(1, card(Suit.CLUBS, Rank.JACK), true);
        game.pushTableauForTest(2, card(Suit.DIAMONDS, Rank.FIVE), true);

        // Empty-pile rule is Klondike's (King only), not Card.canPlaceOnCascade(null).
        assertFalse(game.canMove(PileRef.tableau(0), 1, PileRef.tableau(3)));
        assertFalse(game.canMove(PileRef.tableau(2), 1, PileRef.tableau(3)));
        assertFalse(game.move(PileRef.tableau(0), 1, PileRef.tableau(3)));

        assertTrue(game.canMove(PileRef.tableau(1), 3, PileRef.tableau(3)));
        assertTrue(game.move(PileRef.tableau(1), 3, PileRef.tableau(3)));
        assertEquals(3, game.getTableau(3).size());
        assertEquals(card(Suit.SPADES, Rank.KING), game.getTableau(3).get(0).getCard());
        assertEquals(card(Suit.CLUBS, Rank.JACK), game.peekTableau(3).orElseThrow().getCard());
        assertTrue(game.getTableau(3).stream().allMatch(TableauCard::isFaceUp));
    }

    @Test
    void emptyTableauRejectsNonKingHeadedSequence() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.HEARTS, Rank.QUEEN), true);
        game.pushTableauForTest(0, card(Suit.SPADES, Rank.JACK), true);

        assertEquals(2, game.maxSelectableFromTableau(0));
        assertFalse(game.canMove(PileRef.tableau(0), 2, PileRef.tableau(1)));
        assertFalse(game.move(PileRef.tableau(0), 2, PileRef.tableau(1)));
        assertEquals(2, game.getTableau(0).size());
        assertTrue(game.getTableau(1).isEmpty());
    }

    @Test
    void faceUpRunMovesAsAUnitOntoMatchingTableau() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.DIAMONDS, Rank.QUEEN), true);
        game.pushTableauForTest(1, card(Suit.SPADES, Rank.JACK), true);
        game.pushTableauForTest(1, card(Suit.HEARTS, Rank.TEN), true);
        game.pushTableauForTest(1, card(Suit.CLUBS, Rank.NINE), true);

        assertTrue(game.canMove(PileRef.tableau(1), 3, PileRef.tableau(0)));
        assertFalse(game.canMove(PileRef.tableau(1), 2, PileRef.tableau(0)));
        assertTrue(game.move(PileRef.tableau(1), 3, PileRef.tableau(0)));
        assertEquals(4, game.getTableau(0).size());
        assertEquals(card(Suit.CLUBS, Rank.NINE), game.peekTableau(0).orElseThrow().getCard());
        assertTrue(game.getTableau(1).isEmpty());
    }

    @Test
    void brokenOrFaceDownSequenceCannotMoveAsAUnit() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.HEARTS, Rank.TEN), true);
        game.pushTableauForTest(1, card(Suit.SPADES, Rank.KING), true);
        game.pushTableauForTest(1, card(Suit.CLUBS, Rank.QUEEN), true);
        game.pushTableauForTest(2, card(Suit.DIAMONDS, Rank.SIX), false);
        game.pushTableauForTest(2, card(Suit.CLUBS, Rank.FIVE), true);

        assertEquals(0, game.validFaceUpSequenceLengthFromTop(1, 2));
        assertFalse(game.canMove(PileRef.tableau(1), 2, PileRef.tableau(0)));
        assertFalse(game.canMove(PileRef.tableau(2), 2, PileRef.tableau(0)));
        assertFalse(game.move(PileRef.tableau(2), 1, PileRef.tableau(0)));
    }

    @Test
    void uncoveringTableauTurnsNewlyExposedCardFaceUp() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.CLUBS, Rank.FIVE), false);
        game.pushTableauForTest(0, card(Suit.HEARTS, Rank.SIX), true);
        game.pushTableauForTest(1, card(Suit.SPADES, Rank.SEVEN), true);

        assertFalse(game.getTableau(0).get(0).isFaceUp());
        assertTrue(game.move(PileRef.tableau(0), 1, PileRef.tableau(1)));
        assertEquals(1, game.getTableau(0).size());
        assertTrue(game.peekTableau(0).orElseThrow().isFaceUp());
        assertEquals(card(Suit.CLUBS, Rank.FIVE), game.peekTableau(0).orElseThrow().getCard());
    }

    @Test
    void foundationBuildsAceUpSameSuitAndRejectsOthers() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushTableauForTest(0, card(Suit.DIAMONDS, Rank.ACE), true);
        game.pushTableauForTest(1, card(Suit.DIAMONDS, Rank.TWO), true);
        game.pushTableauForTest(2, card(Suit.DIAMONDS, Rank.THREE), true);
        game.pushTableauForTest(3, card(Suit.HEARTS, Rank.TWO), true);
        game.pushTableauForTest(4, card(Suit.DIAMONDS, Rank.FIVE), true);

        assertFalse(game.canMove(PileRef.tableau(1), 1, PileRef.foundation(0)));
        assertTrue(game.move(PileRef.tableau(0), 1, PileRef.foundation(0)));
        assertTrue(game.move(PileRef.tableau(1), 1, PileRef.foundation(0)));
        assertTrue(game.move(PileRef.tableau(2), 1, PileRef.foundation(0)));
        assertFalse(game.move(PileRef.tableau(3), 1, PileRef.foundation(0)));
        assertFalse(game.move(PileRef.tableau(4), 1, PileRef.foundation(0)));
        assertEquals(3, game.getFoundation(0).size());
        assertEquals(card(Suit.DIAMONDS, Rank.THREE), game.peekFoundation(0).orElseThrow());
    }

    @Test
    void onlyWasteTopIsPlayableToTableauOrFoundation() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushWasteForTest(card(Suit.HEARTS, Rank.TWO));
        game.pushWasteForTest(card(Suit.CLUBS, Rank.ACE));
        game.pushTableauForTest(0, card(Suit.SPADES, Rank.THREE), true);

        assertFalse(game.canMove(PileRef.waste(), 2, PileRef.foundation(0)));
        assertFalse(game.canMove(PileRef.waste(), 1, PileRef.tableau(0)));
        assertTrue(game.canMove(PileRef.waste(), 1, PileRef.foundation(0)));
        assertTrue(game.move(PileRef.waste(), 1, PileRef.foundation(0)));
        assertEquals(card(Suit.CLUBS, Rank.ACE), game.peekFoundation(0).orElseThrow());
        assertEquals(card(Suit.HEARTS, Rank.TWO), game.peekWaste().orElseThrow());
        assertFalse(game.canMove(PileRef.waste(), 1, PileRef.foundation(0)));
        assertTrue(game.move(PileRef.waste(), 1, PileRef.tableau(0)));
        assertTrue(game.getWaste().isEmpty());
        assertEquals(card(Suit.HEARTS, Rank.TWO), game.peekTableau(0).orElseThrow().getCard());
    }

    @Test
    void wasteKingPlaysToEmptyTableau() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushWasteForTest(card(Suit.HEARTS, Rank.QUEEN));
        assertFalse(game.move(PileRef.waste(), 1, PileRef.tableau(0)));
        game.pushWasteForTest(card(Suit.SPADES, Rank.KING));
        assertTrue(game.move(PileRef.waste(), 1, PileRef.tableau(0)));
        assertEquals(card(Suit.SPADES, Rank.KING), game.peekTableau(0).orElseThrow().getCard());
        assertEquals(card(Suit.HEARTS, Rank.QUEEN), game.peekWaste().orElseThrow());
    }

    @Test
    void drawMovesStockTopOntoWaste() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushStockForTest(card(Suit.CLUBS, Rank.TEN));
        game.pushStockForTest(card(Suit.HEARTS, Rank.JACK));

        assertTrue(game.draw());
        assertEquals(1, game.getStock().size());
        assertEquals(card(Suit.HEARTS, Rank.JACK), game.peekWaste().orElseThrow());
        assertEquals(card(Suit.CLUBS, Rank.TEN), game.peekStock().orElseThrow());
        assertTrue(game.draw());
        assertTrue(game.getStock().isEmpty());
        assertEquals(card(Suit.CLUBS, Rank.TEN), game.peekWaste().orElseThrow());
        assertFalse(game.draw());
        assertEquals(2, game.getWaste().size());
    }

    @Test
    void recycleRestoresDrawOrderAndRefusesWhenStockStillHasCards() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        final Card firstDrawn = card(Suit.DIAMONDS, Rank.ACE);
        final Card secondDrawn = card(Suit.CLUBS, Rank.TWO);
        final Card thirdDrawn = card(Suit.HEARTS, Rank.THREE);
        game.pushStockForTest(thirdDrawn);
        game.pushStockForTest(secondDrawn);
        game.pushStockForTest(firstDrawn);

        assertTrue(game.draw());
        assertTrue(game.draw());
        game.pushStockForTest(card(Suit.SPADES, Rank.FOUR));
        assertFalse(game.recycleWaste());
        assertTrue(game.draw());
        assertTrue(game.draw());
        assertTrue(game.getStock().isEmpty());

        assertTrue(game.recycleWaste());
        assertTrue(game.getWaste().isEmpty());
        assertEquals(firstDrawn, game.peekStock().orElseThrow());
        assertTrue(game.draw());
        assertEquals(firstDrawn, game.peekWaste().orElseThrow());
        assertFalse(game.recycleWaste());
        assertFalse(game.getStock().isEmpty());
    }

    @Test
    void drawOrRecycleDrawsThenRecycles() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        game.pushStockForTest(card(Suit.SPADES, Rank.KING));
        assertTrue(game.drawOrRecycle());
        assertTrue(game.getStock().isEmpty());
        assertEquals(1, game.getWaste().size());
        assertTrue(game.drawOrRecycle());
        assertTrue(game.getWaste().isEmpty());
        assertEquals(1, game.getStock().size());
        assertFalse(SolitaireGame.emptyBoard().drawOrRecycle());
    }

    @Test
    void notWonUntilAllFiftyTwoCardsSitOnFoundationsViaMoves() {
        final SolitaireGame game = SolitaireGame.emptyBoard();
        assertFalse(game.isWon());
        int moved = 0;
        for (int suitIndex = 0; suitIndex < Suit.values().length; suitIndex++) {
            final Suit suit = Suit.values()[suitIndex];
            for (final Rank rank : Rank.values()) {
                game.pushTableauForTest(0, card(suit, rank), true);
                assertTrue(
                        game.move(PileRef.tableau(0), 1, PileRef.foundation(suitIndex)),
                        "place " + rank + " of " + suit
                );
                moved++;
                if (moved < 52) {
                    assertFalse(game.isWon(), "must not win at " + moved + " cards");
                    assertEquals(GameStatus.PLAYING, game.getStatus());
                }
            }
        }
        assertEquals(52, moved);
        assertEquals(52, game.getFoundationCardCount());
        assertTrue(game.isWon());
        assertEquals(GameStatus.WON, game.getStatus());
        game.pushTableauForTest(1, card(Suit.HEARTS, Rank.ACE), true);
        assertFalse(game.move(PileRef.tableau(1), 1, PileRef.tableau(2)));
        assertFalse(game.draw());
    }

    @Test
    void dealtGameIsNotWon() {
        final SolitaireGame game = new SolitaireGame(new Random(1));
        assertFalse(game.isWon());
        assertEquals(0, game.getFoundationCardCount());
    }
}
