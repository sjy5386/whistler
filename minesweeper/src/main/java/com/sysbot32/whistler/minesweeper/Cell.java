package com.sysbot32.whistler.minesweeper;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Cell {
    @Setter
    private boolean mine;
    private boolean open;
    private boolean flagged;
    @Setter
    private int adjacentMines;

    public void open() {
        if (this.flagged) {
            return;
        }
        this.open = true;
    }

    public void toggleFlag() {
        if (this.open) {
            return;
        }
        this.flagged = !this.flagged;
    }

    void reset() {
        this.mine = false;
        this.open = false;
        this.flagged = false;
        this.adjacentMines = 0;
    }
}
