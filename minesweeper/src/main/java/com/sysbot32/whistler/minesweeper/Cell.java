package com.sysbot32.whistler.minesweeper;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Cell {
    @Setter
    private boolean mine;
    private boolean open;
    private boolean flagged;
    private boolean questionMarked;
    @Setter
    private int adjacentMines;

    public void open() {
        if (this.flagged) {
            return;
        }
        this.questionMarked = false;
        this.open = true;
    }

    /**
     * Cycles covered-cell marks.
     * <ul>
     *   <li>Marks on: none → flag → question → none</li>
     *   <li>Marks off: none ↔ flag (question marks are cleared)</li>
     * </ul>
     */
    public void cycleMark(final boolean marksEnabled) {
        if (this.open) {
            return;
        }
        if (!marksEnabled) {
            if (this.questionMarked) {
                this.questionMarked = false;
                this.flagged = false;
                return;
            }
            this.flagged = !this.flagged;
            return;
        }
        if (!this.flagged && !this.questionMarked) {
            this.flagged = true;
        } else if (this.flagged) {
            this.flagged = false;
            this.questionMarked = true;
        } else {
            this.questionMarked = false;
        }
    }

    /**
     * Forces a flag mark (used when auto-flagging mines on win).
     */
    public void forceFlag() {
        if (this.open) {
            return;
        }
        this.questionMarked = false;
        this.flagged = true;
    }

    public void clearQuestionMark() {
        this.questionMarked = false;
    }

    void reset() {
        this.mine = false;
        this.open = false;
        this.flagged = false;
        this.questionMarked = false;
        this.adjacentMines = 0;
    }
}
