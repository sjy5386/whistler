package com.sysbot32.whistler.minesweeper.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Difficulty {
    BEGINNER(9, 9, 10, "Beginner"),
    INTERMEDIATE(16, 16, 40, "Intermediate"),
    EXPERT(16, 30, 99, "Expert");

    private final int rows;
    private final int cols;
    private final int mines;
    private final String displayName;
}
