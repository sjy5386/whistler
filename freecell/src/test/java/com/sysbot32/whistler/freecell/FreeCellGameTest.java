package com.sysbot32.whistler.freecell;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeCellGameTest {
    @Test
    void dealDistributesFiftyTwoCardsFaceUpAcrossEightCascades() {
        final FreeCellGame game = new FreeCellGame(12345L);
        int total = 0;
        for (int i = 0; i < FreeCellGame.CASCADE_COUNT; i++) {
            final int size = game.getCascade(i).size();
            // 52 / 8 = 6 remainder 4 → first 4 cascades get 7, rest 6
            assertTrue(size == 6 || size == 7);
            total += size;
        }
        assertEquals(52, total);
        assertEquals(4, game.countEmptyFreeCells());
        assertEquals(0, game.countEmptyCascades());
        assertEquals(GameStatus.PLAYING, game.getStatus());
        assertEquals(0, game.getFoundationCardCount());
    }

    @Test
    void sameSeedProducesSameDeal() {
        final FreeCellGame a = new FreeCellGame(99L);
        final FreeCellGame b = new FreeCellGame(99L);
        for (int i = 0; i < FreeCellGame.CASCADE_COUNT; i++) {
            assertEquals(a.getCascade(i), b.getCascade(i));
        }
    }

    @Test
    void differentSeedsUsuallyDiffer() {
        final FreeCellGame a = new FreeCellGame(1L);
        final FreeCellGame b = new FreeCellGame(2L);
        boolean differ = false;
        for (int i = 0; i < FreeCellGame.CASCADE_COUNT; i++) {
            if (!a.getCascade(i).equals(b.getCascade(i))) {
                differ = true;
                break;
            }
        }
        assertTrue(differ);
    }

    @Test
    void moveSingleCardToEmptyFreeCell() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        final Card king = new Card(Suit.SPADES, Rank.KING);
        game.pushCascadeForTest(0, king);

        assertTrue(game.move(PileRef.cascade(0), 1, PileRef.freeCell(0)));
        assertTrue(game.getCascade(0).isEmpty());
        assertEquals(king, game.getFreeCell(0).orElseThrow());
        assertEquals(1, game.getMoveCount());
    }

    @Test
    void cannotMoveTwoCardsToFreeCell() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.SIX));
        game.pushCascadeForTest(0, new Card(Suit.CLUBS, Rank.FIVE));
        assertFalse(game.canMove(PileRef.cascade(0), 2, PileRef.freeCell(0)));
    }

    @Test
    void cascadeBuildAlternatingColors() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.SEVEN));
        game.pushCascadeForTest(1, new Card(Suit.SPADES, Rank.SIX));

        assertTrue(game.move(PileRef.cascade(1), 1, PileRef.cascade(0)));
        assertEquals(2, game.getCascade(0).size());
        assertEquals(Rank.SIX, game.getCascade(0).get(1).getRank());
    }

    @Test
    void cannotStackSameColorOnCascade() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.SEVEN));
        game.pushCascadeForTest(1, new Card(Suit.DIAMONDS, Rank.SIX));
        assertFalse(game.canMove(PileRef.cascade(1), 1, PileRef.cascade(0)));
    }

    @Test
    void foundationBuildsAceUpSameSuit() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.CLUBS, Rank.ACE));
        game.pushCascadeForTest(1, new Card(Suit.CLUBS, Rank.TWO));

        assertTrue(game.move(PileRef.cascade(0), 1, PileRef.foundation(0)));
        assertTrue(game.move(PileRef.cascade(1), 1, PileRef.foundation(0)));
        assertEquals(2, game.getFoundation(0).size());
        assertEquals(Rank.TWO, game.peekFoundation(0).orElseThrow().getRank());
    }

    @Test
    void emptyCascadeAcceptsAnyCard() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.THREE));
        assertTrue(game.move(PileRef.cascade(0), 1, PileRef.cascade(1)));
        assertTrue(game.getCascade(0).isEmpty());
        assertEquals(1, game.getCascade(1).size());
    }

    @Test
    void supermoveFormulaWithoutEmptyDestination() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        // 2 empty free cells, 1 empty cascade (cascades 1-7 empty → 7 empty, but we'll fill)
        // Start empty: 4 free cells empty, 8 cascades empty
        // max to non-empty dest: (4+1)*2^8 would use empty cascades = 8
        // Build a non-empty dest and limited helpers.
        game.pushCascadeForTest(0, new Card(Suit.SPADES, Rank.KING));
        // empty free = 4, empty cascades = 7 (cascade 0 occupied)
        // dest non-empty: max = 5 * 2^7 = 640 → capped by sequence
        assertEquals(5 * (1 << 7), game.maxMovableCards(false));
        // dest empty cascade: emptyCascades counted as 7-1=6 → 5 * 2^6
        assertEquals(5 * (1 << 6), game.maxMovableCards(true));
    }

    @Test
    void supermoveLimitsMultiCardMove() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        // Occupy all free cells → empty free cells = 0 → max = 1 * 2^emptyCascades
        game.putFreeCellForTest(0, new Card(Suit.HEARTS, Rank.ACE));
        game.putFreeCellForTest(1, new Card(Suit.DIAMONDS, Rank.ACE));
        game.putFreeCellForTest(2, new Card(Suit.CLUBS, Rank.ACE));
        game.putFreeCellForTest(3, new Card(Suit.SPADES, Rank.ACE));

        // Fill cascades 2–7 so empty cascades = 0.
        // Source cascade 0: legal run ♣5-♥4-♠3 (top). Dest cascade 1: ♦6.
        // Only the full 3-card run starts with ♣5, which fits on ♦6.
        for (int i = 2; i < 8; i++) {
            game.pushCascadeForTest(i, new Card(Suit.SPADES, Rank.KING));
        }
        game.pushCascadeForTest(0, new Card(Suit.CLUBS, Rank.FIVE));
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.FOUR));
        game.pushCascadeForTest(0, new Card(Suit.SPADES, Rank.THREE));
        game.pushCascadeForTest(1, new Card(Suit.DIAMONDS, Rank.SIX));

        // empty free = 0, empty cascades = 0 → max = 1
        assertEquals(1, game.maxMovableCards(false));
        assertFalse(game.canMove(PileRef.cascade(0), 1, PileRef.cascade(1))); // ♠3 on ♦6 illegal
        assertFalse(game.canMove(PileRef.cascade(0), 2, PileRef.cascade(1))); // rank/color + max
        assertFalse(game.canMove(PileRef.cascade(0), 3, PileRef.cascade(1))); // supermove blocks

        // Free one free cell → max = 2 — still cannot move 3
        assertTrue(game.move(PileRef.freeCell(0), 1, PileRef.foundation(0)));
        assertEquals(2, game.maxMovableCards(false));
        assertFalse(game.canMove(PileRef.cascade(0), 3, PileRef.cascade(1)));

        // Free second free cell → max = 3 — supermove allows the run
        assertTrue(game.move(PileRef.freeCell(1), 1, PileRef.foundation(1)));
        assertEquals(3, game.maxMovableCards(false));
        assertTrue(game.canMove(PileRef.cascade(0), 3, PileRef.cascade(1)));
        assertTrue(game.move(PileRef.cascade(0), 3, PileRef.cascade(1)));
        assertEquals(4, game.getCascade(1).size());
    }

    @Test
    void supermoveEmptyDestinationUsesOneFewerCascadeHelper() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        // 0 empty freecells, 1 empty cascade total which is the destination
        // → helpers = 0, max to empty cascade = 1
        for (int i = 0; i < 4; i++) {
            game.putFreeCellForTest(i, new Card(Suit.values()[i], Rank.ACE));
        }
        for (int i = 1; i < 8; i++) {
            // leave cascade 0 as source sequence, cascade 7 empty destination
            if (i == 7) {
                continue;
            }
            game.pushCascadeForTest(i, new Card(Suit.SPADES, Rank.KING));
        }
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.FIVE));
        game.pushCascadeForTest(0, new Card(Suit.CLUBS, Rank.FOUR));

        // empty free=0, empty cascades=1 (cascade 7)
        assertEquals(1, game.maxMovableCards(true)); // destination empty: (0+1)*2^0 = 1
        assertEquals(2, game.maxMovableCards(false)); // if dest were non-empty: (0+1)*2^1 = 2

        assertTrue(game.canMove(PileRef.cascade(0), 1, PileRef.cascade(7)));
        assertFalse(game.canMove(PileRef.cascade(0), 2, PileRef.cascade(7)));
    }

    @Test
    void multiCardMoveWhenSupermoveAllows() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        // Plenty of helpers
        game.pushCascadeForTest(0, new Card(Suit.SPADES, Rank.SEVEN));
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.SIX));
        game.pushCascadeForTest(0, new Card(Suit.CLUBS, Rank.FIVE));
        game.pushCascadeForTest(1, new Card(Suit.DIAMONDS, Rank.EIGHT));

        assertTrue(game.move(PileRef.cascade(0), 3, PileRef.cascade(1)));
        assertEquals(4, game.getCascade(1).size());
        assertTrue(game.getCascade(0).isEmpty());
    }

    @Test
    void invalidSequenceCannotMove() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.SPADES, Rank.SEVEN));
        game.pushCascadeForTest(0, new Card(Suit.SPADES, Rank.SIX)); // same color — broken
        game.pushCascadeForTest(1, new Card(Suit.HEARTS, Rank.SEVEN));

        assertEquals(1, game.maxSelectableFromCascade(0));
        assertFalse(game.canMove(PileRef.cascade(0), 2, PileRef.cascade(1)));
        assertTrue(game.canMove(PileRef.cascade(0), 1, PileRef.cascade(1))); // ♠6 on ♥7
    }

    @Test
    void winWhenAllFiftyTwoOnFoundations() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        int foundation = 0;
        int placed = 0;
        for (final Suit suit : Suit.values()) {
            for (final Rank rank : Rank.values()) {
                game.pushFoundationForTest(foundation, new Card(suit, rank));
                placed++;
            }
            foundation++;
        }
        assertEquals(52, placed);
        assertEquals(52, game.getFoundationCardCount());
        assertTrue(game.isWon());
        assertEquals(GameStatus.WON, game.getStatus());
        assertFalse(game.move(PileRef.cascade(0), 1, PileRef.freeCell(0)));
    }

    @Test
    void tryMoveToFoundationFromCascadeAndFreeCell() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.ACE));
        game.putFreeCellForTest(0, new Card(Suit.SPADES, Rank.ACE));

        assertTrue(game.tryMoveToFoundation(PileRef.cascade(0)));
        assertTrue(game.tryMoveToFoundation(PileRef.freeCell(0)));
        assertEquals(1, game.getFoundation(0).size());
        assertEquals(1, game.getFoundation(1).size());
    }

    @Test
    void undoRestoresPreviousState() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        final Card card = new Card(Suit.CLUBS, Rank.QUEEN);
        game.pushCascadeForTest(0, card);
        assertTrue(game.move(PileRef.cascade(0), 1, PileRef.freeCell(2)));
        assertTrue(game.canUndo());
        assertTrue(game.undo());
        assertEquals(card, game.peekCascade(0).orElseThrow());
        assertTrue(game.getFreeCell(2).isEmpty());
        assertEquals(0, game.getMoveCount());
        assertFalse(game.canUndo());
    }

    @Test
    void cannotTakeCardFromFoundation() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushFoundationForTest(0, new Card(Suit.HEARTS, Rank.ACE));
        game.pushFoundationForTest(0, new Card(Suit.HEARTS, Rank.TWO));
        assertFalse(game.canMove(PileRef.foundation(0), 1, PileRef.freeCell(0)));
        assertFalse(game.canMove(PileRef.foundation(0), 1, PileRef.cascade(0)));
        assertEquals(2, game.getFoundation(0).size());
    }

    @Test
    void undoBatchesUserMoveWithFollowingAutoFoundationMoves() {
        // User moves ♥A to free cell; auto-move then sends ♥A (and safe followers) home.
        // One Undo must reverse autos + the user move together (XP FreeCell behavior).
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.ACE));
        game.pushCascadeForTest(1, new Card(Suit.CLUBS, Rank.ACE));
        game.pushCascadeForTest(2, new Card(Suit.SPADES, Rank.ACE));
        // Opposite black Aces already home → red Two would auto if present; keep only Aces.
        assertTrue(game.move(PileRef.cascade(0), 1, PileRef.freeCell(0)));
        final int autos = game.autoMoveToFoundations();
        assertTrue(autos >= 1);
        assertTrue(game.getFreeCell(0).isEmpty());
        assertTrue(game.getFoundationCardCount() >= 1);
        final int movesAfter = game.getMoveCount();
        assertTrue(movesAfter >= 2); // user + at least one auto

        assertTrue(game.undo());
        assertEquals(0, game.getMoveCount());
        assertEquals(0, game.getFoundationCardCount());
        assertEquals(Rank.ACE, game.peekCascade(0).orElseThrow().getRank());
        assertTrue(game.getFreeCell(0).isEmpty());
        assertFalse(game.canUndo());
    }

    @Test
    void fromDealRejectsWrongSize() {
        assertThrows(IllegalArgumentException.class, () -> FreeCellGame.fromDeal(List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> FreeCellGame.fromDeal(Deck.createOrdered().subList(0, 10)));
    }

    @Test
    void fromDealUsesExactOrder() {
        final List<Card> ordered = Deck.createOrdered();
        final FreeCellGame game = FreeCellGame.fromDeal(ordered);
        // Deal goes left-to-right: card i → cascade i % 8
        assertEquals(ordered.get(0), game.getCascade(0).get(0));
        assertEquals(ordered.get(1), game.getCascade(1).get(0));
        assertEquals(ordered.get(8), game.getCascade(0).get(1));
    }

    @Test
    void randomInjectionMatchesSeedConstructor() {
        final long seed = 555L;
        final FreeCellGame a = new FreeCellGame(seed);
        final FreeCellGame b = new FreeCellGame(seed, new Random(seed));
        for (int i = 0; i < FreeCellGame.CASCADE_COUNT; i++) {
            assertEquals(a.getCascade(i), b.getCascade(i));
        }
    }

    @Test
    void autoMoveSendsAcesToFoundation() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.pushCascadeForTest(0, new Card(Suit.DIAMONDS, Rank.ACE));
        game.pushCascadeForTest(1, new Card(Suit.DIAMONDS, Rank.TWO));
        // Ace always auto-moves; Two waits until opposite-color foundations catch up (safe rule)
        final int moved = game.autoMoveToFoundations();
        assertEquals(1, moved);
        assertEquals(Rank.ACE, game.peekFoundation(0).orElseThrow().getRank());
        assertEquals(0, game.autoMoveToFoundations());
        assertEquals(Rank.TWO, game.peekCascade(1).orElseThrow().getRank());
    }

    @Test
    void autoMoveBuildsWhenSafeAgainstOppositeColor() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        // Black foundations at Ace → red Two is safe to auto-move (2 ≤ minBlack+1)
        game.pushFoundationForTest(0, new Card(Suit.CLUBS, Rank.ACE));
        game.pushFoundationForTest(1, new Card(Suit.SPADES, Rank.ACE));
        game.pushCascadeForTest(0, new Card(Suit.HEARTS, Rank.ACE));
        game.pushCascadeForTest(1, new Card(Suit.HEARTS, Rank.TWO));

        final int moved = game.autoMoveToFoundations();
        assertTrue(moved >= 2); // ♥A then ♥2
        assertEquals(Rank.TWO, game.peekFoundation(2).orElseGet(
                () -> game.peekFoundation(3).orElseThrow()).getRank());
    }

    @Test
    void freeCellToCascadeAndBack() {
        final FreeCellGame game = FreeCellGame.emptyBoard();
        game.putFreeCellForTest(0, new Card(Suit.HEARTS, Rank.NINE));
        game.pushCascadeForTest(0, new Card(Suit.CLUBS, Rank.TEN));
        assertTrue(game.move(PileRef.freeCell(0), 1, PileRef.cascade(0)));
        assertTrue(game.move(PileRef.cascade(0), 1, PileRef.freeCell(1)));
    }
}
