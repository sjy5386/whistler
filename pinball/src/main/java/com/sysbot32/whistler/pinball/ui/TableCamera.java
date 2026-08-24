package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.PinballWorld;

import java.awt.geom.Point2D;

/**
 * Perspective camera looking up an inclined pinball table. Table (x, y) maps to
 * world (x, y*sin(incline), y*cos(incline)); +y on the table is upfield.
 */
public final class TableCamera {
    private static final double INCLINE = 0.12;
    private static final double FOCAL = 430.0;

    private final double camX;
    private final double camY;
    private final double camZ;
    private final double fx;
    private final double fy;
    private final double fz;
    private final double rx;
    private final double ry;
    private final double rz;
    private final double ux;
    private final double uy;
    private final double uz;
    private final double sinIncline;
    private final double cosIncline;

    private int width = 540;
    private int height = 800;

    public TableCamera() {
        this.sinIncline = Math.sin(INCLINE);
        this.cosIncline = Math.cos(INCLINE);
        this.camX = PinballWorld.TABLE_WIDTH * 0.5;
        this.camY = 132.0;
        this.camZ = -125.0;
        final double lookX = PinballWorld.TABLE_WIDTH * 0.5;
        final double lookY = 8.0;
        final double lookZ = 175.0;
        final double lx = lookX - this.camX;
        final double ly = lookY - this.camY;
        final double lz = lookZ - this.camZ;
        final double fl = Math.sqrt(lx * lx + ly * ly + lz * lz);
        this.fx = lx / fl;
        this.fy = ly / fl;
        this.fz = lz / fl;
        // right = normalize(cross(worldUp, forward))
        final double rx0 = this.fz;
        final double rz0 = -this.fx;
        final double rl = Math.hypot(rx0, rz0);
        this.rx = rx0 / rl;
        this.ry = 0.0;
        this.rz = rz0 / rl;
        // up = cross(forward, right)
        this.ux = this.fy * this.rz - this.fz * this.ry;
        this.uy = this.fz * this.rx - this.fx * this.rz;
        this.uz = this.fx * this.ry - this.fy * this.rx;
    }

    public void setViewport(final int width, final int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public Point2D.Double project(final double tableX, final double tableY, final double heightAboveTable) {
        final double wx = tableX;
        final double wy = heightAboveTable + tableY * this.sinIncline;
        final double wz = tableY * this.cosIncline;
        final double dx = wx - this.camX;
        final double dy = wy - this.camY;
        final double dz = wz - this.camZ;
        final double cx = dx * this.rx + dy * this.ry + dz * this.rz;
        final double cy = dx * this.ux + dy * this.uy + dz * this.uz;
        double cz = dx * this.fx + dy * this.fy + dz * this.fz;
        if (cz < 4.0) {
            cz = 4.0;
        }
        final double s = FOCAL / cz;
        final double sx = this.width * 0.5 + cx * s;
        final double sy = this.height * 0.62 - cy * s;
        return new Point2D.Double(sx, sy);
    }

    public double scaleAt(final double tableY, final double heightAboveTable) {
        final Point2D.Double a = this.project(0.0, tableY, heightAboveTable);
        final Point2D.Double b = this.project(1.0, tableY, heightAboveTable);
        return Math.hypot(b.x - a.x, b.y - a.y);
    }
}
