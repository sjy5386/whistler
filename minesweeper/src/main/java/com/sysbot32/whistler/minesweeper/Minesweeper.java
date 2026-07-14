package com.sysbot32.whistler.minesweeper;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Pure Minesweeper rules: first click is always safe, left-open / flag toggle,
 * flood-fill zeros, win when every non-mine cell is open.
 */
@Getter
public class Minesweeper {
    private static final int[] DR = {-1, -1, -1, 0, 0, 1, 1, 1};
    private static final int[] DC = {-1, 0, 1, -1, 1, -1, 0, 1};

    private final int rows;
    private final int cols;
    private final int mineCount;
    private final Random random;
    private final Cell[][] cells;

    private GameStatus status = GameStatus.READY;
    private boolean minesPlaced;
    private int flagCount;
    private int openSafeCount;
    private int explodedRow = -1;
    private int explodedCol = -1;

    public Minesweeper(final Difficulty difficulty) {
        this(difficulty.getRows(), difficulty.getCols(), difficulty.getMines(), new Random());
    }

    public Minesweeper(final int rows, final int cols, final int mineCount) {
        this(rows, cols, mineCount, new Random());
    }

    public Minesweeper(final int rows, final int cols, final int mineCount, final Random random) {
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException("Board dimensions must be positive");
        }
        if (mineCount < 0 || mineCount >= rows * cols) {
            throw new IllegalArgumentException("Mine count must be in [0, rows * cols)");
        }
        this.rows = rows;
        this.cols = cols;
        this.mineCount = mineCount;
        this.random = Objects.requireNonNull(random, "random");
        this.cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                this.cells[r][c] = new Cell();
            }
        }
    }

    public Cell getCell(final int row, final int col) {
        this.checkBounds(row, col);
        return this.cells[row][col];
    }

    public int getRemainingMines() {
        return this.mineCount - this.flagCount;
    }

    public int getSafeCellCount() {
        return this.rows * this.cols - this.mineCount;
    }

    public boolean isInBounds(final int row, final int col) {
        return row >= 0 && row < this.rows && col >= 0 && col < this.cols;
    }

    /**
     * Opens a covered cell. Mines are placed on the first successful open so that
     * the clicked cell is never a mine.
     *
     * @return {@code true} if the board state changed
     */
    public boolean open(final int row, final int col) {
        this.checkBounds(row, col);
        if (this.status == GameStatus.WON || this.status == GameStatus.LOST) {
            return false;
        }
        final Cell cell = this.cells[row][col];
        if (cell.isOpen() || cell.isFlagged()) {
            return false;
        }

        if (!this.minesPlaced) {
            this.placeMines(row, col);
            this.status = GameStatus.PLAYING;
        }

        if (cell.isMine()) {
            cell.open();
            this.explodedRow = row;
            this.explodedCol = col;
            this.revealAllMines();
            this.status = GameStatus.LOST;
            return true;
        }

        this.floodOpen(row, col);
        if (this.openSafeCount >= this.getSafeCellCount()) {
            this.flagAllMines();
            this.status = GameStatus.WON;
        }
        return true;
    }

    /**
     * Toggles a flag on a covered cell. Ignored after the game ends or on open cells.
     *
     * @return {@code true} if the flag state changed
     */
    public boolean toggleFlag(final int row, final int col) {
        this.checkBounds(row, col);
        if (this.status == GameStatus.WON || this.status == GameStatus.LOST) {
            return false;
        }
        final Cell cell = this.cells[row][col];
        if (cell.isOpen()) {
            return false;
        }
        final boolean wasFlagged = cell.isFlagged();
        cell.toggleFlag();
        if (cell.isFlagged() && !wasFlagged) {
            this.flagCount++;
        } else if (!cell.isFlagged() && wasFlagged) {
            this.flagCount--;
        }
        if (this.status == GameStatus.READY) {
            this.status = GameStatus.PLAYING;
        }
        return true;
    }

    public void reset() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                this.cells[r][c].reset();
            }
        }
        this.status = GameStatus.READY;
        this.minesPlaced = false;
        this.flagCount = 0;
        this.openSafeCount = 0;
        this.explodedRow = -1;
        this.explodedCol = -1;
    }

    private void placeMines(final int safeRow, final int safeCol) {
        final List<int[]> candidates = new ArrayList<>(this.rows * this.cols - 1);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                if (r == safeRow && c == safeCol) {
                    continue;
                }
                candidates.add(new int[]{r, c});
            }
        }
        Collections.shuffle(candidates, this.random);
        for (int i = 0; i < this.mineCount; i++) {
            final int[] pos = candidates.get(i);
            this.cells[pos[0]][pos[1]].setMine(true);
        }
        this.computeAdjacentMines();
        this.minesPlaced = true;
    }

    private void computeAdjacentMines() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                if (this.cells[r][c].isMine()) {
                    this.cells[r][c].setAdjacentMines(0);
                    continue;
                }
                int count = 0;
                for (int i = 0; i < DR.length; i++) {
                    final int nr = r + DR[i];
                    final int nc = c + DC[i];
                    if (this.isInBounds(nr, nc) && this.cells[nr][nc].isMine()) {
                        count++;
                    }
                }
                this.cells[r][c].setAdjacentMines(count);
            }
        }
    }

    private void floodOpen(final int startRow, final int startCol) {
        final int[] rowQueue = new int[this.rows * this.cols];
        final int[] colQueue = new int[this.rows * this.cols];
        int head = 0;
        int tail = 0;

        this.openSafeCell(startRow, startCol);
        if (this.cells[startRow][startCol].getAdjacentMines() == 0) {
            rowQueue[tail] = startRow;
            colQueue[tail] = startCol;
            tail++;
        }

        while (head < tail) {
            final int r = rowQueue[head];
            final int c = colQueue[head];
            head++;
            for (int i = 0; i < DR.length; i++) {
                final int nr = r + DR[i];
                final int nc = c + DC[i];
                if (!this.isInBounds(nr, nc)) {
                    continue;
                }
                final Cell neighbor = this.cells[nr][nc];
                if (neighbor.isOpen() || neighbor.isFlagged() || neighbor.isMine()) {
                    continue;
                }
                this.openSafeCell(nr, nc);
                if (neighbor.getAdjacentMines() == 0) {
                    rowQueue[tail] = nr;
                    colQueue[tail] = nc;
                    tail++;
                }
            }
        }
    }

    private void openSafeCell(final int row, final int col) {
        final Cell cell = this.cells[row][col];
        if (cell.isOpen() || cell.isFlagged() || cell.isMine()) {
            return;
        }
        cell.open();
        this.openSafeCount++;
    }

    private void revealAllMines() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                final Cell cell = this.cells[r][c];
                if (cell.isMine() && !cell.isFlagged()) {
                    cell.open();
                }
            }
        }
    }

    private void flagAllMines() {
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                final Cell cell = this.cells[r][c];
                if (cell.isMine() && !cell.isFlagged()) {
                    cell.toggleFlag();
                    this.flagCount++;
                }
            }
        }
    }

    private void checkBounds(final int row, final int col) {
        if (!this.isInBounds(row, col)) {
            throw new IndexOutOfBoundsException("Cell out of bounds: (" + row + ", " + col + ")");
        }
    }
}
