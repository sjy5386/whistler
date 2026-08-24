package com.sysbot32.whistler.pinball.model;

/**
 * Line-segment collider on the table plane.
 */
public record Wall(double x1, double y1, double x2, double y2, double restitution) {
}
