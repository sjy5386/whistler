package com.sysbot32.whistler.minesweeper;

import com.sysbot32.whistler.config.PropertiesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestTimesTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsAreAnonymous999() {
        final BestTimes bestTimes = new BestTimes(new PropertiesConfig(this.tempDir.resolve("m.properties")));
        assertEquals(999, bestTimes.getTime(Difficulty.BEGINNER));
        assertEquals("Anonymous", bestTimes.getName(Difficulty.BEGINNER));
        assertTrue(bestTimes.isRecord(Difficulty.BEGINNER, 100));
        assertFalse(bestTimes.isRecord(Difficulty.BEGINNER, 999));
    }

    @Test
    void setRecordPersistsAcrossReload() {
        final Path file = this.tempDir.resolve("m.properties");
        final BestTimes written = new BestTimes(new PropertiesConfig(file));
        written.setRecord(Difficulty.INTERMEDIATE, 42, "Ada");

        final BestTimes loaded = new BestTimes(new PropertiesConfig(file));
        assertEquals(42, loaded.getTime(Difficulty.INTERMEDIATE));
        assertEquals("Ada", loaded.getName(Difficulty.INTERMEDIATE));
        assertTrue(loaded.isRecord(Difficulty.INTERMEDIATE, 41));
        assertFalse(loaded.isRecord(Difficulty.INTERMEDIATE, 42));
        assertFalse(loaded.isRecord(Difficulty.INTERMEDIATE, 43));
    }

    @Test
    void blankNameBecomesAnonymous() {
        final BestTimes bestTimes = new BestTimes(new PropertiesConfig(this.tempDir.resolve("m.properties")));
        bestTimes.setRecord(Difficulty.EXPERT, 120, "   ");
        assertEquals("Anonymous", bestTimes.getName(Difficulty.EXPERT));
    }

    @Test
    void resetClearsAllDifficulties() {
        final Path file = this.tempDir.resolve("m.properties");
        final BestTimes bestTimes = new BestTimes(new PropertiesConfig(file));
        bestTimes.setRecord(Difficulty.BEGINNER, 10, "A");
        bestTimes.setRecord(Difficulty.INTERMEDIATE, 20, "B");
        bestTimes.setRecord(Difficulty.EXPERT, 30, "C");
        bestTimes.reset();

        final BestTimes reloaded = new BestTimes(new PropertiesConfig(file));
        for (final Difficulty difficulty : Difficulty.values()) {
            assertEquals(999, reloaded.getTime(difficulty));
            assertEquals("Anonymous", reloaded.getName(difficulty));
        }
    }
}
