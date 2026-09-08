package com.sysbot32.whistler.wordpad.model;

public enum MeasurementUnit {
    INCHES(72f, "Inches"),
    CENTIMETERS(72f / 2.54f, "Centimeters"),
    POINTS(1f, "Points"),
    PICAS(12f, "Picas");

    private final float pointsPerUnit;
    private final String displayName;

    MeasurementUnit(final float pointsPerUnit, final String displayName) {
        this.pointsPerUnit = pointsPerUnit;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public float toPoints(final float units) {
        return units * this.pointsPerUnit;
    }

    public float fromPoints(final float points) {
        return points / this.pointsPerUnit;
    }
}
