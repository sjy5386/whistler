package com.sysbot32.whistler.minesweeper.model;

import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

/**
 * Board size and mine count for a game. Standard presets track best times;
 * custom boards do not.
 */
@Getter
public final class BoardSpec {
    public static final int MIN_ROWS = 9;
    public static final int MAX_ROWS = 24;
    public static final int MIN_COLS = 9;
    public static final int MAX_COLS = 30;
    public static final int MIN_MINES = 10;

    private final Difficulty difficulty;
    private final int rows;
    private final int cols;
    private final int mines;

    private BoardSpec(final Difficulty difficulty, final int rows, final int cols, final int mines) {
        this.difficulty = difficulty;
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
    }

    public static BoardSpec of(final Difficulty difficulty) {
        Objects.requireNonNull(difficulty, "difficulty");
        return new BoardSpec(difficulty, difficulty.getRows(), difficulty.getCols(), difficulty.getMines());
    }

    public static BoardSpec custom(final int rows, final int cols, final int mines) {
        validateCustom(rows, cols, mines);
        return new BoardSpec(null, rows, cols, mines);
    }

    public static void validateCustom(final int rows, final int cols, final int mines) {
        if (rows < MIN_ROWS || rows > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "Height must be between " + MIN_ROWS + " and " + MAX_ROWS);
        }
        if (cols < MIN_COLS || cols > MAX_COLS) {
            throw new IllegalArgumentException(
                    "Width must be between " + MIN_COLS + " and " + MAX_COLS);
        }
        final int maxMines = maxMines(rows, cols);
        if (mines < MIN_MINES || mines > maxMines) {
            throw new IllegalArgumentException(
                    "Mines must be between " + MIN_MINES + " and " + maxMines);
        }
    }

    public static int maxMines(final int rows, final int cols) {
        return Math.max(MIN_MINES, rows * cols - 1);
    }

    public boolean isCustom() {
        return this.difficulty == null;
    }

    public boolean tracksBestTime() {
        return this.difficulty != null;
    }

    public Optional<Difficulty> difficulty() {
        return Optional.ofNullable(this.difficulty);
    }

    public String displayName() {
        if (this.difficulty != null) {
            return this.difficulty.getDisplayName();
        }
        return "Custom (" + this.rows + "×" + this.cols + ", " + this.mines + ")";
    }
}
