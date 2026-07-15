package com.sysbot32.whistler.freecell;

import com.sysbot32.whistler.config.Config;
import lombok.Getter;

import java.util.Objects;

/**
 * Classic FreeCell statistics: session, totals, and streaks.
 * Layout matches the original Statistics dialog fields.
 */
@Getter
public final class FreeCellStatistics {
    private static final String KEY_TOTAL_WON = "stats.gamesWon";
    private static final String KEY_TOTAL_LOST = "stats.gamesLost";
    private static final String KEY_BEST_WINS = "stats.bestWinStreak";
    private static final String KEY_BEST_LOSSES = "stats.bestLossStreak";
    private static final String KEY_CURRENT = "stats.currentStreak";
    private static final String KEY_CURRENT_IS_WIN = "stats.currentStreakIsWin";

    private final Config config;

    /** In-memory only; reset when the app starts. */
    private int sessionWon;
    private int sessionLost;

    private int totalWon;
    private int totalLost;
    private int bestWinStreak;
    private int bestLossStreak;
    /** Length of the active consecutive win or loss run. */
    private int currentStreak;
    private boolean currentStreakIsWin;

    public FreeCellStatistics(final Config config) {
        this.config = Objects.requireNonNull(config, "config");
        this.sessionWon = 0;
        this.sessionLost = 0;
        this.totalWon = parseInt(config.get(KEY_TOTAL_WON), 0);
        this.totalLost = parseInt(config.get(KEY_TOTAL_LOST), 0);
        this.bestWinStreak = parseInt(config.get(KEY_BEST_WINS), 0);
        this.bestLossStreak = parseInt(config.get(KEY_BEST_LOSSES), 0);
        this.currentStreak = parseInt(config.get(KEY_CURRENT), 0);
        this.currentStreakIsWin = parseBool(config.get(KEY_CURRENT_IS_WIN), true);
        // Migrate legacy "gamesPlayed" not needed for display.
    }

    public void recordWin() {
        this.sessionWon++;
        this.totalWon++;
        if (this.currentStreakIsWin || this.currentStreak == 0) {
            this.currentStreak++;
        } else {
            this.currentStreak = 1;
        }
        this.currentStreakIsWin = true;
        if (this.currentStreak > this.bestWinStreak) {
            this.bestWinStreak = this.currentStreak;
        }
        this.persist();
    }

    public void recordLoss() {
        this.sessionLost++;
        this.totalLost++;
        if (!this.currentStreakIsWin || this.currentStreak == 0) {
            // continuing a loss streak, or starting fresh after zero
            if (!this.currentStreakIsWin && this.currentStreak > 0) {
                this.currentStreak++;
            } else {
                this.currentStreak = 1;
            }
        } else {
            this.currentStreak = 1;
        }
        this.currentStreakIsWin = false;
        if (this.currentStreak > this.bestLossStreak) {
            this.bestLossStreak = this.currentStreak;
        }
        this.persist();
    }

    /**
     * Abandoned mid-play: does not count as win/loss (classic behaviour).
     * Session and totals unchanged; active streak is cleared.
     */
    public void recordAbandoned() {
        this.currentStreak = 0;
        this.currentStreakIsWin = true;
        this.persist();
    }

    public void reset() {
        this.sessionWon = 0;
        this.sessionLost = 0;
        this.totalWon = 0;
        this.totalLost = 0;
        this.bestWinStreak = 0;
        this.bestLossStreak = 0;
        this.currentStreak = 0;
        this.currentStreakIsWin = true;
        this.persist();
    }

    public int sessionTotal() {
        return this.sessionWon + this.sessionLost;
    }

    public int totalFinished() {
        return this.totalWon + this.totalLost;
    }

    public double sessionWinPercentage() {
        return percentage(this.sessionWon, this.sessionTotal());
    }

    public double totalWinPercentage() {
        return percentage(this.totalWon, this.totalFinished());
    }

    /** Status-bar convenience: finished games this install (totals). */
    public int getGamesWon() {
        return this.totalWon;
    }

    public int getGamesLost() {
        return this.totalLost;
    }

    public int getGamesPlayed() {
        return this.totalFinished();
    }

    private void persist() {
        this.config.set(KEY_TOTAL_WON, Integer.toString(this.totalWon));
        this.config.set(KEY_TOTAL_LOST, Integer.toString(this.totalLost));
        this.config.set(KEY_BEST_WINS, Integer.toString(this.bestWinStreak));
        this.config.set(KEY_BEST_LOSSES, Integer.toString(this.bestLossStreak));
        this.config.set(KEY_CURRENT, Integer.toString(this.currentStreak));
        this.config.set(KEY_CURRENT_IS_WIN, Boolean.toString(this.currentStreakIsWin));
        this.config.save();
    }

    private static double percentage(final int won, final int total) {
        if (total <= 0) {
            return 0.0;
        }
        return 100.0 * won / total;
    }

    private static int parseInt(final String raw, final int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean parseBool(final String raw, final boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw);
    }
}
