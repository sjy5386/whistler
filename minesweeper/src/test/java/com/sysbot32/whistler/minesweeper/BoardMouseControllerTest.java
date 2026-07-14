package com.sysbot32.whistler.minesweeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sysbot32.whistler.minesweeper.BoardMouseController.ActionType.CHORD;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.ActionType.FLAG;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.ActionType.NONE;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.ActionType.OPEN;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.Button.LEFT;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.Button.MIDDLE;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.Button.RIGHT;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.PreviewMode.CELL;
import static com.sysbot32.whistler.minesweeper.BoardMouseController.PreviewMode.CHORD_NEIGHBORS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardMouseControllerTest {
    private BoardMouseController pointer;

    @BeforeEach
    void setUp() {
        this.pointer = new BoardMouseController();
    }

    @Test
    void leftClickOpensCellUnderCursor() {
        this.pointer.press(LEFT, 1, 2);
        assertEquals(CELL, this.pointer.previewMode());
        assertTrue(this.pointer.isFacePressed());

        final BoardMouseController.Action action = this.pointer.release(LEFT);
        assertEquals(OPEN, action.type());
        assertEquals(1, action.row());
        assertEquals(2, action.col());
        assertFalse(this.pointer.isAnyButtonDown());
    }

    @Test
    void leaveAndReenterThenReleaseOpensOriginalHoverTarget() {
        this.pointer.press(LEFT, 0, 0);
        this.pointer.hoverOutside();
        assertFalse(this.pointer.isFacePressed());
        assertEquals(BoardMouseController.PreviewMode.NONE, this.pointer.previewMode());

        this.pointer.hover(0, 0);
        assertTrue(this.pointer.isFacePressed());
        assertEquals(CELL, this.pointer.previewMode());

        final BoardMouseController.Action action = this.pointer.release(LEFT);
        assertEquals(OPEN, action.type());
        assertEquals(0, action.row());
        assertEquals(0, action.col());
    }

    @Test
    void dragToAdjacentCellOpensReleaseCell() {
        this.pointer.press(LEFT, 0, 0);
        this.pointer.hover(1, 1);
        assertEquals(CELL, this.pointer.previewMode());
        assertTrue(this.pointer.hoverCell().isPresent());
        assertEquals(1, this.pointer.hoverCell().get()[0]);
        assertEquals(1, this.pointer.hoverCell().get()[1]);

        final BoardMouseController.Action action = this.pointer.release(LEFT);
        assertEquals(OPEN, action.type());
        assertEquals(1, action.row());
        assertEquals(1, action.col());
    }

    @Test
    void releaseOutsideBoardCancelsOpen() {
        this.pointer.press(LEFT, 2, 2);
        this.pointer.hoverOutside();
        final BoardMouseController.Action action = this.pointer.release(LEFT);
        assertEquals(NONE, action.type());
        assertFalse(this.pointer.isAnyButtonDown());
    }

    @Test
    void rightClickFlagsWhenOverCell() {
        this.pointer.press(RIGHT, 3, 4);
        assertFalse(this.pointer.isFacePressed()); // right-only does not ooh
        assertEquals(BoardMouseController.PreviewMode.NONE, this.pointer.previewMode());

        final BoardMouseController.Action action = this.pointer.release(RIGHT);
        assertEquals(FLAG, action.type());
        assertEquals(3, action.row());
        assertEquals(4, action.col());
    }

    @Test
    void chordFiresOnFirstButtonReleaseAndNotAgain() {
        this.pointer.press(LEFT, 1, 1);
        this.pointer.press(RIGHT, 1, 1);
        assertTrue(this.pointer.isChordArmed());
        assertEquals(CHORD_NEIGHBORS, this.pointer.previewMode());

        final BoardMouseController.Action first = this.pointer.release(LEFT);
        assertEquals(CHORD, first.type());
        assertEquals(1, first.row());
        assertEquals(1, first.col());
        assertTrue(this.pointer.isChordConsumed());
        assertTrue(this.pointer.isRightDown());
        assertEquals(BoardMouseController.PreviewMode.NONE, this.pointer.previewMode());

        final BoardMouseController.Action second = this.pointer.release(RIGHT);
        assertEquals(NONE, second.type());
        assertFalse(this.pointer.isAnyButtonDown());
    }

    @Test
    void chordUsesHoverCellAfterDrag() {
        this.pointer.press(LEFT, 0, 0);
        this.pointer.press(RIGHT, 0, 0);
        this.pointer.hover(2, 3);

        final BoardMouseController.Action action = this.pointer.release(RIGHT);
        assertEquals(CHORD, action.type());
        assertEquals(2, action.row());
        assertEquals(3, action.col());
    }

    @Test
    void chordCancelledWhenReleasedOutside() {
        this.pointer.press(LEFT, 1, 1);
        this.pointer.press(RIGHT, 1, 1);
        this.pointer.hoverOutside();

        final BoardMouseController.Action first = this.pointer.release(LEFT);
        assertEquals(NONE, first.type());
        assertTrue(this.pointer.isRightDown());
        assertTrue(this.pointer.isChordConsumed());

        final BoardMouseController.Action second = this.pointer.release(RIGHT);
        assertEquals(NONE, second.type());
        assertFalse(this.pointer.isAnyButtonDown());
    }

    @Test
    void chordStaysCancelledAfterOutsideReleaseThenReenter() {
        this.pointer.press(LEFT, 1, 1);
        this.pointer.press(RIGHT, 1, 1);
        this.pointer.hoverOutside();

        assertEquals(NONE, this.pointer.release(LEFT).type());
        assertTrue(this.pointer.isChordConsumed());

        // Still holding right: re-enter board and release — must not chord.
        this.pointer.hover(2, 2);
        final BoardMouseController.Action second = this.pointer.release(RIGHT);
        assertEquals(NONE, second.type());
        assertFalse(this.pointer.isAnyButtonDown());
    }

    @Test
    void middleClickChordsOnRelease() {
        this.pointer.press(MIDDLE, 4, 5);
        assertTrue(this.pointer.isChordArmed());
        assertEquals(CHORD_NEIGHBORS, this.pointer.previewMode());

        final BoardMouseController.Action action = this.pointer.release(MIDDLE);
        assertEquals(CHORD, action.type());
        assertEquals(4, action.row());
        assertEquals(5, action.col());
        assertFalse(this.pointer.isAnyButtonDown());
    }

    @Test
    void middleClickOutsideCancels() {
        this.pointer.press(MIDDLE, 0, 0);
        this.pointer.hoverOutside();
        assertEquals(NONE, this.pointer.release(MIDDLE).type());
    }

    @Test
    void hoverIgnoredWhenNoButtonDown() {
        this.pointer.hover(5, 5);
        assertFalse(this.pointer.isOverBoard());
        assertEquals(NONE, this.pointer.release(LEFT).type());
    }
}
