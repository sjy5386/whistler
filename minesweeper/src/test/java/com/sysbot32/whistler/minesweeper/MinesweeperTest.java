package com.sysbot32.whistler.minesweeper;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinesweeperTest {
    @Test
    void beginnerPresetDimensions() {
        final Minesweeper game = new Minesweeper(Difficulty.BEGINNER);
        assertEquals(9, game.getRows());
        assertEquals(9, game.getCols());
        assertEquals(10, game.getMineCount());
        assertEquals(GameStatus.READY, game.getStatus());
        assertFalse(game.isMinesPlaced());
    }

    @Test
    void intermediateAndExpertPresets() {
        final Minesweeper intermediate = new Minesweeper(Difficulty.INTERMEDIATE);
        assertEquals(16, intermediate.getRows());
        assertEquals(16, intermediate.getCols());
        assertEquals(40, intermediate.getMineCount());

        final Minesweeper expert = new Minesweeper(Difficulty.EXPERT);
        assertEquals(16, expert.getRows());
        assertEquals(30, expert.getCols());
        assertEquals(99, expert.getMineCount());
    }

    @Test
    void rejectsInvalidBoard() {
        assertThrows(IllegalArgumentException.class, () -> new Minesweeper(0, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> new Minesweeper(5, 5, 25));
        assertThrows(IllegalArgumentException.class, () -> new Minesweeper(5, 5, -1));
    }

    @Test
    void firstClickIsAlwaysSafe() {
        for (int seed = 0; seed < 50; seed++) {
            final Minesweeper game = new Minesweeper(9, 9, 10, new Random(seed));
            assertTrue(game.open(4, 4));
            assertFalse(game.getCell(4, 4).isMine());
            assertTrue(game.getCell(4, 4).isOpen());
            assertTrue(game.isMinesPlaced());
            assertEquals(10, countMines(game));
        }
    }

    @Test
    void openFloodFillsZeroRegions() {
        // Fixed layout via custom placement simulation: use tiny board with seed that
        // places mines away from a corner zero region is hard to guarantee — instead
        // open every non-mine after known placement by using mineCount 0.
        final Minesweeper empty = new Minesweeper(3, 3, 0, new Random(1));
        assertTrue(empty.open(1, 1));
        assertEquals(GameStatus.WON, empty.getStatus());
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                assertTrue(empty.getCell(r, c).isOpen());
            }
        }
    }

    @Test
    void adjacentMineCountsAreCorrect() {
        final Minesweeper game = new Minesweeper(3, 3, 8, new Random(0));
        // First click center: only safe cell, all others mines → instant win
        assertTrue(game.open(1, 1));
        assertEquals(8, game.getCell(1, 1).getAdjacentMines());
        assertEquals(GameStatus.WON, game.getStatus());
        assertEquals(1, game.getOpenSafeCount());
    }

    @Test
    void openingMineLoses() {
        final Minesweeper game = new Minesweeper(2, 2, 1, new Random(42));
        assertTrue(game.open(0, 0));
        int mineRow = -1;
        int mineCol = -1;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (game.getCell(r, c).isMine()) {
                    mineRow = r;
                    mineCol = c;
                }
            }
        }
        game.reset();
        // Place then hit mine: open a safe cell first, then open the mine cell on a fresh
        // game that still has mines. After reset, open safe corner then find mine again.
        final Minesweeper g2 = new Minesweeper(2, 2, 1, new Random(42));
        g2.open(0, 0);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (g2.getCell(r, c).isMine()) {
                    assertTrue(g2.open(r, c));
                    assertEquals(GameStatus.LOST, g2.getStatus());
                    assertEquals(r, g2.getExplodedRow());
                    assertEquals(c, g2.getExplodedCol());
                    return;
                }
            }
        }
        throw new AssertionError("Expected a mine after first click");
    }

    @Test
    void markCycleIsFlagThenQuestionThenClear() {
        final Minesweeper game = new Minesweeper(Difficulty.BEGINNER);
        assertEquals(10, game.getRemainingMines());

        assertTrue(game.toggleFlag(0, 0));
        assertTrue(game.getCell(0, 0).isFlagged());
        assertFalse(game.getCell(0, 0).isQuestionMarked());
        assertEquals(9, game.getRemainingMines());

        assertTrue(game.toggleFlag(0, 0));
        assertFalse(game.getCell(0, 0).isFlagged());
        assertTrue(game.getCell(0, 0).isQuestionMarked());
        assertEquals(10, game.getRemainingMines());

        assertTrue(game.toggleFlag(0, 0));
        assertFalse(game.getCell(0, 0).isFlagged());
        assertFalse(game.getCell(0, 0).isQuestionMarked());
        assertEquals(10, game.getRemainingMines());
    }

    @Test
    void cannotOpenFlaggedCell() {
        final Minesweeper game = new Minesweeper(5, 5, 3, new Random(1));
        assertTrue(game.toggleFlag(2, 2));
        assertFalse(game.open(2, 2));
        assertFalse(game.isMinesPlaced());
        assertEquals(GameStatus.PLAYING, game.getStatus());
    }

    @Test
    void canOpenQuestionMarkedCell() {
        final Minesweeper game = new Minesweeper(5, 5, 3, new Random(1));
        assertTrue(game.toggleFlag(2, 2));
        assertTrue(game.toggleFlag(2, 2));
        assertTrue(game.getCell(2, 2).isQuestionMarked());
        assertTrue(game.open(2, 2));
        assertTrue(game.getCell(2, 2).isOpen());
        assertFalse(game.getCell(2, 2).isQuestionMarked());
    }

    @Test
    void winWhenAllSafeCellsOpen() {
        final Minesweeper game = new Minesweeper(2, 2, 1, new Random(7));
        game.open(0, 0);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (!game.getCell(r, c).isMine() && !game.getCell(r, c).isOpen()) {
                    game.open(r, c);
                }
            }
        }
        assertEquals(GameStatus.WON, game.getStatus());
        assertEquals(0, game.getRemainingMines());
    }

    @Test
    void resetClearsBoard() {
        final Minesweeper game = new Minesweeper(5, 5, 5, new Random(3));
        game.open(0, 0);
        game.toggleFlag(1, 1);
        game.reset();
        assertEquals(GameStatus.READY, game.getStatus());
        assertFalse(game.isMinesPlaced());
        assertEquals(0, game.getFlagCount());
        assertEquals(0, game.getOpenSafeCount());
        assertFalse(game.getCell(0, 0).isOpen());
    }

    @Test
    void outOfBoundsThrows() {
        final Minesweeper game = new Minesweeper(3, 3, 1);
        assertThrows(IndexOutOfBoundsException.class, () -> game.open(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> game.toggleFlag(0, 3));
    }

    @Test
    void noActionsAfterGameOver() {
        final Minesweeper game = new Minesweeper(2, 2, 1, new Random(42));
        game.open(0, 0);
        int mr = 0;
        int mc = 0;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                if (game.getCell(r, c).isMine()) {
                    mr = r;
                    mc = c;
                }
            }
        }
        game.open(mr, mc);
        assertEquals(GameStatus.LOST, game.getStatus());
        assertFalse(game.open(0, 0));
        assertFalse(game.toggleFlag(0, 1));
    }

    private static int countMines(final Minesweeper game) {
        int count = 0;
        for (int r = 0; r < game.getRows(); r++) {
            for (int c = 0; c < game.getCols(); c++) {
                if (game.getCell(r, c).isMine()) {
                    count++;
                }
            }
        }
        return count;
    }
}
