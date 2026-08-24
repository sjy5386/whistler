package com.sysbot32.whistler.pinball.model;

import lombok.Getter;

@Getter
public final class Bumper {
    private final double x;
    private final double y;
    private final double radius;
    private boolean contacting;

    public Bumper(final double x, final double y, final double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    void setContacting(final boolean contacting) {
        this.contacting = contacting;
    }
}
