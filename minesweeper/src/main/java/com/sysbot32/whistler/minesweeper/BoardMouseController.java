package com.sysbot32.whistler.minesweeper;

import java.util.Optional;

/**
 * Board pointer state machine (open / flag / chord), independent of Swing so
 * press–drag–release transitions can be unit-tested.
 */
public final class BoardMouseController {
    public enum Button {
        LEFT,
        RIGHT,
        MIDDLE
    }

    public enum ActionType {
        NONE,
        OPEN,
        FLAG,
        CHORD
    }

    public enum PreviewMode {
        NONE,
        /** Depress the covered cell under the cursor (left-button press). */
        CELL,
        /** Depress unflagged neighbors of the hover cell (chord). */
        CHORD_NEIGHBORS
    }

    public record Action(ActionType type, int row, int col) {
        public static Action none() {
            return new Action(ActionType.NONE, -1, -1);
        }

        public static Action of(final ActionType type, final int row, final int col) {
            return new Action(type, row, col);
        }
    }

    private boolean leftDown;
    private boolean rightDown;
    private boolean middleDown;
    private boolean chordArmed;
    private boolean chordConsumed;
    private int hoverRow = -1;
    private int hoverCol = -1;

    public void press(final Button button, final int row, final int col) {
        this.setHover(row, col);
        switch (button) {
            case LEFT -> this.leftDown = true;
            case RIGHT -> this.rightDown = true;
            case MIDDLE -> {
                this.middleDown = true;
                this.chordArmed = true;
            }
        }
        if (this.leftDown && this.rightDown) {
            this.chordArmed = true;
        }
    }

    /**
     * Updates the cell under the cursor while a button is held.
     * No-op when no button is down.
     */
    public void hover(final int row, final int col) {
        if (!this.isAnyButtonDown()) {
            return;
        }
        this.setHover(row, col);
    }

    /**
     * Pointer left the board while a button is held.
     */
    public void hoverOutside() {
        if (!this.isAnyButtonDown()) {
            return;
        }
        this.hoverRow = -1;
        this.hoverCol = -1;
    }

    /**
     * Completes a button release using the current hover target.
     */
    public Action release(final Button button) {
        final boolean over = this.isOverBoard();
        final int row = this.hoverRow;
        final int col = this.hoverCol;

        if (button == Button.MIDDLE) {
            Action action = Action.none();
            if (over && this.chordArmed && !this.chordConsumed) {
                action = Action.of(ActionType.CHORD, row, col);
            }
            this.reset();
            return action;
        }

        if (button == Button.LEFT) {
            this.leftDown = false;
        } else if (button == Button.RIGHT) {
            this.rightDown = false;
        }

        if (this.chordArmed) {
            Action action = Action.none();
            // First chord-button release ends the gesture: chord only if still over the board.
            // Releasing outside cancels and must not allow a later re-entry release to chord.
            if (!this.chordConsumed) {
                if (over) {
                    action = Action.of(ActionType.CHORD, row, col);
                }
                this.chordConsumed = true;
            }
            if (!this.isAnyButtonDown()) {
                this.reset();
            }
            return action;
        }

        Action action = Action.none();
        if (over) {
            if (button == Button.LEFT) {
                action = Action.of(ActionType.OPEN, row, col);
            } else if (button == Button.RIGHT) {
                action = Action.of(ActionType.FLAG, row, col);
            }
        }

        if (!this.isAnyButtonDown()) {
            this.reset();
        }
        return action;
    }

    public void reset() {
        this.leftDown = false;
        this.rightDown = false;
        this.middleDown = false;
        this.chordArmed = false;
        this.chordConsumed = false;
        this.hoverRow = -1;
        this.hoverCol = -1;
    }

    public boolean isAnyButtonDown() {
        return this.leftDown || this.rightDown || this.middleDown;
    }

    public boolean isOverBoard() {
        return this.hoverRow >= 0 && this.hoverCol >= 0;
    }

    /**
     * Classic face "ooh" while left or chord is active over a cell.
     */
    public boolean isFacePressed() {
        if (!this.isOverBoard()) {
            return false;
        }
        return this.leftDown || this.chordArmed;
    }

    public boolean isChordArmed() {
        return this.chordArmed;
    }

    public boolean isChordConsumed() {
        return this.chordConsumed;
    }

    public boolean isLeftDown() {
        return this.leftDown;
    }

    public boolean isRightDown() {
        return this.rightDown;
    }

    public Optional<int[]> hoverCell() {
        if (!this.isOverBoard()) {
            return Optional.empty();
        }
        return Optional.of(new int[]{this.hoverRow, this.hoverCol});
    }

    public PreviewMode previewMode() {
        if (!this.isAnyButtonDown() || !this.isOverBoard()) {
            return PreviewMode.NONE;
        }
        if (this.chordArmed) {
            return this.chordConsumed ? PreviewMode.NONE : PreviewMode.CHORD_NEIGHBORS;
        }
        if (this.leftDown) {
            return PreviewMode.CELL;
        }
        return PreviewMode.NONE;
    }

    private void setHover(final int row, final int col) {
        this.hoverRow = row;
        this.hoverCol = col;
    }
}
