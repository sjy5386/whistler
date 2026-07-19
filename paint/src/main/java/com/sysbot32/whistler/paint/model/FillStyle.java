package com.sysbot32.whistler.paint.model;

/**
 * Shape fill style under the toolbox (classic Paint three-mode selector).
 */
public enum FillStyle {
    /** Outline only (foreground stroke). */
    OUTLINE,
    /** Outline (foreground) + fill (background). */
    OUTLINE_FILL,
    /** Filled shape only (foreground fill, no outline). */
    FILL
}
