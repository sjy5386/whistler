package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.PinballWorld;

import java.awt.geom.Point2D;

/**
 * Overhead-ish perspective of the inclined table, then fitted so the playfield
 * fills the viewport instead of sitting in a sea of sky.
 */
public final class TableCamera {
    private static final double INCLINE = 0.06;
    private static final double FOCAL = 520.0;
    private static final double MARGIN = 0.045;

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

    private int width = 617;
    private int height = 720;
    private double fitScale = 1.0;
    private double fitOx;
    private double fitOy;

    public TableCamera() {
        this.sinIncline = Math.sin(INCLINE);
        this.cosIncline = Math.cos(INCLINE);
        this.camX = PinballWorld.TABLE_WIDTH * 0.5;
        this.camY = 255.0;
        this.camZ = -35.0;
        final double lookX = PinballWorld.TABLE_WIDTH * 0.5;
        final double lookY = 0.0;
        final double lookZ = 210.0;
        final double lx = lookX - this.camX;
        final double ly = lookY - this.camY;
        final double lz = lookZ - this.camZ;
        final double fl = Math.sqrt(lx * lx + ly * ly + lz * lz);
        this.fx = lx / fl;
        this.fy = ly / fl;
        this.fz = lz / fl;
        final double rx0 = this.fz;
        final double rz0 = -this.fx;
        final double rl = Math.hypot(rx0, rz0);
        this.rx = rx0 / rl;
        this.ry = 0.0;
        this.rz = rz0 / rl;
        this.ux = this.fy * this.rz - this.fz * this.ry;
        this.uy = this.fz * this.rx - this.fx * this.rz;
        this.uz = this.fx * this.ry - this.fy * this.rx;
        this.recomputeFit();
    }

    public void setViewport(final int width, final int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.recomputeFit();
    }

    public Point2D.Double project(final double tableX, final double tableY, final double heightAboveTable) {
        final Point2D.Double raw = this.rawProject(tableX, tableY, heightAboveTable);
        return new Point2D.Double(raw.x * this.fitScale + this.fitOx, raw.y * this.fitScale + this.fitOy);
    }

    public double scaleAt(final double tableY, final double heightAboveTable) {
        final Point2D.Double a = this.project(0.0, tableY, heightAboveTable);
        final Point2D.Double b = this.project(1.0, tableY, heightAboveTable);
        return Math.hypot(b.x - a.x, b.y - a.y);
    }

    private void recomputeFit() {
        final Point2D.Double nL = this.rawProject(0.0, 0.0, 0.0);
        final Point2D.Double nR = this.rawProject(PinballWorld.TABLE_WIDTH, 0.0, 0.0);
        final Point2D.Double fL = this.rawProject(0.0, PinballWorld.TABLE_HEIGHT, 18.0);
        final Point2D.Double fR = this.rawProject(PinballWorld.TABLE_WIDTH, PinballWorld.TABLE_HEIGHT, 18.0);
        final double minX = Math.min(Math.min(nL.x, nR.x), Math.min(fL.x, fR.x));
        final double maxX = Math.max(Math.max(nL.x, nR.x), Math.max(fL.x, fR.x));
        final double minY = Math.min(Math.min(nL.y, nR.y), Math.min(fL.y, fR.y));
        final double maxY = Math.max(Math.max(nL.y, nR.y), Math.max(fL.y, fR.y));
        final double bw = Math.max(1.0, maxX - minX);
        final double bh = Math.max(1.0, maxY - minY);
        final double usableW = this.width * (1.0 - 2.0 * MARGIN);
        final double usableH = this.height * (1.0 - 2.0 * MARGIN);
        this.fitScale = Math.min(usableW / bw, usableH / bh);
        final double cx = (minX + maxX) * 0.5;
        final double cy = (minY + maxY) * 0.5;
        this.fitOx = this.width * 0.5 - cx * this.fitScale;
        this.fitOy = this.height * 0.5 - cy * this.fitScale;
    }

    private Point2D.Double rawProject(final double tableX, final double tableY, final double heightAboveTable) {
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
        return new Point2D.Double(cx * s, -cy * s);
    }
}
