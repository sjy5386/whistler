package com.sysbot32.whistler.minesweeper.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardSpecTest {
    @Test
    void presetTracksBestTime() {
        final BoardSpec beginner = BoardSpec.of(Difficulty.BEGINNER);
        assertEquals(9, beginner.getRows());
        assertEquals(9, beginner.getCols());
        assertEquals(10, beginner.getMines());
        assertFalse(beginner.isCustom());
        assertTrue(beginner.tracksBestTime());
        assertEquals(Difficulty.BEGINNER, beginner.getDifficulty());
    }

    @Test
    void customDoesNotTrackBestTime() {
        final BoardSpec custom = BoardSpec.custom(12, 20, 40);
        assertTrue(custom.isCustom());
        assertFalse(custom.tracksBestTime());
        assertEquals(12, custom.getRows());
        assertEquals(20, custom.getCols());
        assertEquals(40, custom.getMines());
    }

    @Test
    void customRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> BoardSpec.custom(8, 9, 10));
        assertThrows(IllegalArgumentException.class, () -> BoardSpec.custom(9, 8, 10));
        assertThrows(IllegalArgumentException.class, () -> BoardSpec.custom(25, 9, 10));
        assertThrows(IllegalArgumentException.class, () -> BoardSpec.custom(9, 31, 10));
        assertThrows(IllegalArgumentException.class, () -> BoardSpec.custom(9, 9, 9));
        assertThrows(IllegalArgumentException.class, () -> BoardSpec.custom(9, 9, 81));
    }

    @Test
    void maxMinesIsOneLessThanCells() {
        assertEquals(80, BoardSpec.maxMines(9, 9));
    }
}
