package com.sysbot32.whistler.freecell.model;

import com.sysbot32.whistler.config.PropertiesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeCellStatisticsTest {
    @TempDir
    Path tempDir;

    @Test
    void recordsSessionTotalAndStreaks() {
        final PropertiesConfig config = new PropertiesConfig(this.tempDir.resolve("fc.properties"));
        final FreeCellStatistics stats = new FreeCellStatistics(config);

        stats.recordWin();
        stats.recordWin();
        assertEquals(2, stats.getSessionWon());
        assertEquals(0, stats.getSessionLost());
        assertEquals(2, stats.getTotalWon());
        assertEquals(2, stats.getCurrentStreak());
        assertTrue(stats.isCurrentStreakIsWin());
        assertEquals(2, stats.getBestWinStreak());
        assertEquals(100.0, stats.sessionWinPercentage(), 0.01);
        assertEquals(100.0, stats.totalWinPercentage(), 0.01);

        stats.recordLoss();
        assertEquals(1, stats.getSessionLost());
        assertEquals(1, stats.getTotalLost());
        assertEquals(1, stats.getCurrentStreak());
        assertFalse(stats.isCurrentStreakIsWin());
        assertEquals(2, stats.getBestWinStreak());
        assertEquals(1, stats.getBestLossStreak());

        stats.recordLoss();
        assertEquals(2, stats.getBestLossStreak());
        assertEquals(2, stats.getCurrentStreak());

        stats.recordAbandoned();
        assertEquals(0, stats.getCurrentStreak());
        // abandoned does not change win/loss totals
        assertEquals(2, stats.getTotalWon());
        assertEquals(2, stats.getTotalLost());
    }

    @Test
    void clearResetsEverythingIncludingSession() {
        final PropertiesConfig config = new PropertiesConfig(this.tempDir.resolve("fc2.properties"));
        final FreeCellStatistics stats = new FreeCellStatistics(config);
        stats.recordWin();
        stats.recordLoss();
        stats.reset();
        assertEquals(0, stats.getSessionWon());
        assertEquals(0, stats.getSessionLost());
        assertEquals(0, stats.getTotalWon());
        assertEquals(0, stats.getTotalLost());
        assertEquals(0, stats.getBestWinStreak());
        assertEquals(0, stats.getBestLossStreak());
        assertEquals(0, stats.getCurrentStreak());
    }
}
