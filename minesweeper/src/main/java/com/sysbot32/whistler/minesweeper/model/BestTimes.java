package com.sysbot32.whistler.minesweeper.model;

import com.sysbot32.whistler.config.Config;

import java.util.Objects;

/**
 * Fastest clear times for standard difficulties, stored via {@link Config}.
 */
public class BestTimes {
    public static final int DEFAULT_TIME = 999;
    public static final String DEFAULT_NAME = "Anonymous";

    private final Config config;

    public BestTimes(final Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public int getTime(final Difficulty difficulty) {
        this.requireStandard(difficulty);
        final String raw = this.config.get(timeKey(difficulty), String.valueOf(DEFAULT_TIME));
        try {
            final int value = Integer.parseInt(raw);
            return Math.max(0, Math.min(DEFAULT_TIME, value));
        } catch (final NumberFormatException e) {
            return DEFAULT_TIME;
        }
    }

    public String getName(final Difficulty difficulty) {
        this.requireStandard(difficulty);
        final String name = this.config.get(nameKey(difficulty), DEFAULT_NAME);
        return (name == null || name.isBlank()) ? DEFAULT_NAME : name;
    }

    public boolean isRecord(final Difficulty difficulty, final int seconds) {
        this.requireStandard(difficulty);
        if (seconds < 0 || seconds > DEFAULT_TIME) {
            return false;
        }
        return seconds < this.getTime(difficulty);
    }

    public void setRecord(final Difficulty difficulty, final int seconds, final String name) {
        this.requireStandard(difficulty);
        if (seconds < 0 || seconds > DEFAULT_TIME) {
            throw new IllegalArgumentException("Time must be in [0, " + DEFAULT_TIME + "]");
        }
        final String resolved = (name == null || name.isBlank()) ? DEFAULT_NAME : name.strip();
        this.config.set(timeKey(difficulty), String.valueOf(seconds));
        this.config.set(nameKey(difficulty), resolved);
        this.config.save();
    }

    public void reset() {
        for (final Difficulty difficulty : Difficulty.values()) {
            this.config.set(timeKey(difficulty), String.valueOf(DEFAULT_TIME));
            this.config.set(nameKey(difficulty), DEFAULT_NAME);
        }
        this.config.save();
    }

    private void requireStandard(final Difficulty difficulty) {
        Objects.requireNonNull(difficulty, "difficulty");
    }

    private static String timeKey(final Difficulty difficulty) {
        return "best." + difficulty.name().toLowerCase() + ".time";
    }

    private static String nameKey(final Difficulty difficulty) {
        return "best." + difficulty.name().toLowerCase() + ".name";
    }
}
