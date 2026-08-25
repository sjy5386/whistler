package com.sysbot32.whistler.solitaire.model;

import com.sysbot32.whistler.config.Config;

import lombok.Getter;

import java.util.Objects;

/**
 * XP Solitaire Options dialog, persisted via {@link Config}.
 * Defaults match the original: Draw Three, Standard scoring, timed, status bar, outline dragging.
 */
@Getter
public final class SolitaireOptions {
    private static final String KEY_DRAW_THREE = "options.drawThree";
    private static final String KEY_SCORING = "options.scoring";
    private static final String KEY_TIMED = "options.timed";
    private static final String KEY_STATUS = "options.statusBar";
    private static final String KEY_OUTLINE = "options.outlineDragging";
    private static final String KEY_KEEP_SCORE = "options.keepScore";
    private static final String KEY_DECK = "options.deckBack";
    private static final String KEY_VEGAS_BANK = "options.vegasBank";

    private final Config config;

    private boolean drawThree;
    private ScoringMode scoring;
    private boolean timed;
    private boolean statusBar;
    private boolean outlineDragging;
    private boolean keepScore;
    private int deckBack;
    private int vegasBank;

    public SolitaireOptions(final Config config) {
        this.config = Objects.requireNonNull(config, "config");
        this.drawThree = parseBool(config.get(KEY_DRAW_THREE), true);
        this.scoring = parseScoring(config.get(KEY_SCORING), ScoringMode.STANDARD);
        this.timed = parseBool(config.get(KEY_TIMED), true);
        this.statusBar = parseBool(config.get(KEY_STATUS), true);
        this.outlineDragging = parseBool(config.get(KEY_OUTLINE), true);
        this.keepScore = parseBool(config.get(KEY_KEEP_SCORE), false);
        this.deckBack = parseInt(config.get(KEY_DECK), 0);
        this.vegasBank = parseInt(config.get(KEY_VEGAS_BANK), 0);
    }

    public int getDrawCount() {
        return this.drawThree ? 3 : 1;
    }

    public void setDrawThree(final boolean value) {
        this.drawThree = value;
        this.persist();
    }

    public void setScoring(final ScoringMode value) {
        this.scoring = Objects.requireNonNull(value, "scoring");
        this.persist();
    }

    public void setTimed(final boolean value) {
        this.timed = value;
        this.persist();
    }

    public void setStatusBar(final boolean value) {
        this.statusBar = value;
        this.persist();
    }

    public void setOutlineDragging(final boolean value) {
        this.outlineDragging = value;
        this.persist();
    }

    public void setKeepScore(final boolean value) {
        this.keepScore = value;
        this.persist();
    }

    public void setDeckBack(final int value) {
        this.deckBack = Math.max(0, value);
        this.persist();
    }

    public void setVegasBank(final int value) {
        this.vegasBank = value;
        this.persist();
    }

    public void resetVegasBank() {
        this.vegasBank = 0;
        this.persist();
    }

    private void persist() {
        this.config.set(KEY_DRAW_THREE, Boolean.toString(this.drawThree));
        this.config.set(KEY_SCORING, this.scoring.name());
        this.config.set(KEY_TIMED, Boolean.toString(this.timed));
        this.config.set(KEY_STATUS, Boolean.toString(this.statusBar));
        this.config.set(KEY_OUTLINE, Boolean.toString(this.outlineDragging));
        this.config.set(KEY_KEEP_SCORE, Boolean.toString(this.keepScore));
        this.config.set(KEY_DECK, Integer.toString(this.deckBack));
        this.config.set(KEY_VEGAS_BANK, Integer.toString(this.vegasBank));
        this.config.save();
    }

    private static boolean parseBool(final String raw, final boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw);
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

    private static ScoringMode parseScoring(final String raw, final ScoringMode defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return ScoringMode.valueOf(raw.trim());
        } catch (final IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}
