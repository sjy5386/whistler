package com.sysbot32.whistler.paint.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Classic Windows XP Paint toolbox (16 tools, left-to-right, top-to-bottom).
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum PaintTool {
    FREE_FORM_SELECT("Free-Form Select"),
    SELECT("Select"),
    ERASER("Eraser/Color Eraser"),
    FILL("Fill With Color"),
    PICK_COLOR("Pick Color"),
    MAGNIFIER("Magnifier"),
    PENCIL("Pencil"),
    BRUSH("Brush"),
    AIRBRUSH("Airbrush"),
    TEXT("Text"),
    LINE("Line"),
    CURVE("Curve"),
    RECTANGLE("Rectangle"),
    POLYGON("Polygon"),
    ELLIPSE("Ellipse"),
    ROUNDED_RECTANGLE("Rounded Rectangle");

    private final String displayName;
}
