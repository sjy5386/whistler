package com.sysbot32.whistler.pinball.model;

import lombok.Getter;

/**
 * Rotating capsule (pivot + length + radius). Angles are in radians from +x,
 * with +y upfield (away from the drain).
 */
@Getter
public final class Flipper {
    private static final double SWING_SPEED = 22.0;

    private final double pivotX;
    private final double pivotY;
    private final double length;
    private final double radius;
    private final double restAngle;
    private final double raisedAngle;

    private double angle;
    private double angularVelocity;
    private boolean raised;
    private boolean contacting;

    public Flipper(
            final double pivotX,
            final double pivotY,
            final double length,
            final double radius,
            final double restAngle,
            final double raisedAngle
    ) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.length = length;
        this.radius = radius;
        this.restAngle = restAngle;
        this.raisedAngle = raisedAngle;
        this.angle = restAngle;
    }

    public void setRaised(final boolean raised) {
        this.raised = raised;
    }

    void setContacting(final boolean contacting) {
        this.contacting = contacting;
    }

    public double getTipX() {
        return this.pivotX + Math.cos(this.angle) * this.length;
    }

    public double getTipY() {
        return this.pivotY + Math.sin(this.angle) * this.length;
    }

    void tick(final double dt) {
        final double target = this.raised ? this.raisedAngle : this.restAngle;
        final double previous = this.angle;
        final double maxDelta = SWING_SPEED * dt;
        final double diff = target - this.angle;
        if (diff > maxDelta) {
            this.angle += maxDelta;
        } else if (diff < -maxDelta) {
            this.angle -= maxDelta;
        } else {
            this.angle = target;
        }
        this.angularVelocity = dt > 0.0 ? (this.angle - previous) / dt : 0.0;
    }
}
