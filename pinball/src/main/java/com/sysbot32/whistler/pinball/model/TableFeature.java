package com.sysbot32.whistler.pinball.model;

import lombok.Getter;

/**
 * Named playfield object: ramp, lanes, medal, wormholes, hyperspace, mission
 * targets. Circle collider on the table plane.
 */
@Getter
public final class TableFeature {
    public enum Kind {
        LAUNCH_RAMP,
        REENTRY_LANE,
        CENTER_MEDAL,
        WORMHOLE,
        HYPERSPACE,
        MISSION_TARGET
    }

    private final Kind kind;
    private final double x;
    private final double y;
    private final double radius;
    private boolean contacting;
    private boolean lit;

    public TableFeature(final Kind kind, final double x, final double y, final double radius) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    void setContacting(final boolean contacting) {
        this.contacting = contacting;
    }

    void setLit(final boolean lit) {
        this.lit = lit;
    }
}
