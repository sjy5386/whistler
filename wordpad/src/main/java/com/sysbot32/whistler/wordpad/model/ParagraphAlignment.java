package com.sysbot32.whistler.wordpad.model;

import javax.swing.text.StyleConstants;

public enum ParagraphAlignment {
    LEFT(StyleConstants.ALIGN_LEFT),
    CENTER(StyleConstants.ALIGN_CENTER),
    RIGHT(StyleConstants.ALIGN_RIGHT);

    private final int swingConstant;

    ParagraphAlignment(final int swingConstant) {
        this.swingConstant = swingConstant;
    }

    public int getSwingConstant() {
        return this.swingConstant;
    }

    public static ParagraphAlignment fromSwing(final int swingConstant) {
        return switch (swingConstant) {
            case StyleConstants.ALIGN_CENTER -> CENTER;
            case StyleConstants.ALIGN_RIGHT -> RIGHT;
            default -> LEFT;
        };
    }
}
