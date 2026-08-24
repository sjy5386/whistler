package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.Bumper;
import com.sysbot32.whistler.pinball.model.Flipper;
import com.sysbot32.whistler.pinball.model.PinballWorld;
import com.sysbot32.whistler.pinball.model.TableFeature;
import com.sysbot32.whistler.pinball.model.Wall;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * Space Cadet split layout: perspective playfield on the left, backglass /
 * status panel on the right.
 */
public final class PinballView extends JPanel {
    static final int PREFERRED_WIDTH = 980;
    static final int PREFERRED_HEIGHT = 720;
    static final double TABLE_SPLIT = 0.63;

    private static final Color SPACE_TOP = new Color(0x04, 0x06, 0x14);
    private static final Color SPACE_BOTTOM = new Color(0x16, 0x10, 0x38);
    private static final Color TABLE_NEAR = new Color(0x1A, 0x24, 0x6A);
    private static final Color TABLE_FAR = new Color(0x2A, 0x0C, 0x4A);
    private static final Color LANE = new Color(0x18, 0x20, 0x48);
    private static final Color RAIL_GOLD = new Color(0xD4, 0xB1, 0x2A);
    private static final Color RAIL_DARK = new Color(0x7A, 0x5A, 0x10);
    private static final Color FLIPPER_BODY = new Color(0xF0, 0xC4, 0x2E);
    private static final Color FLIPPER_EDGE = new Color(0x8A, 0x6A, 0x08);
    private static final Color PANEL_BG = new Color(0x10, 0x14, 0x12);
    private static final Color PANEL_EDGE = new Color(0x6A, 0x5A, 0x18);
    private static final double WALL_HEIGHT = 14.0;
    private static final double BALL_HEIGHT_CENTER = PinballWorld.BALL_RADIUS;

    private final PinballWorld world;
    private final TableCamera camera = new TableCamera();
    private final Point2D.Double[] stars;

    public PinballView(final PinballWorld world) {
        this.world = world;
        this.setBackground(SPACE_TOP);
        this.setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        this.setFocusable(true);
        this.stars = new Point2D.Double[80];
        final Random rng = new Random(3);
        for (int i = 0; i < this.stars.length; i++) {
            this.stars[i] = new Point2D.Double(rng.nextDouble(), rng.nextDouble());
        }
    }

    public int getTablePanelWidth() {
        final int w = Math.max(1, this.getWidth());
        return Math.max(1, (int) Math.round(w * TABLE_SPLIT));
    }

    public TableCamera getCamera() {
        return this.camera;
    }

    public Point2D.Double project(final double tableX, final double tableY, final double heightAboveTable) {
        this.camera.setViewport(this.getTablePanelWidth(), Math.max(1, this.getHeight()));
        return this.camera.project(tableX, tableY, heightAboveTable);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            final int split = this.getTablePanelWidth();
            this.camera.setViewport(split, this.getHeight());

            final var oldClip = g2.getClip();
            g2.setClip(0, 0, split, this.getHeight());
            this.paintSpace(g2, split);
            this.paintCabinet(g2);
            this.paintTable(g2);
            this.paintLane(g2);
            this.paintTableArt(g2);
            this.paintDrain(g2);
            this.paintFeatures(g2);
            this.paintWalls(g2);
            this.paintBumpers(g2);
            this.paintPlunger(g2);
            this.paintFlipper(g2, this.world.getLeftFlipper());
            this.paintFlipper(g2, this.world.getRightFlipper());
            this.paintBall(g2);
            g2.setClip(oldClip);

            this.paintBackglass(g2, split);
        } finally {
            g2.dispose();
        }
    }

    private void paintSpace(final Graphics2D g2, final int split) {
        g2.setPaint(new GradientPaint(0, 0, SPACE_TOP, 0, this.getHeight(), SPACE_BOTTOM));
        g2.fillRect(0, 0, split, this.getHeight());
        g2.setColor(Color.WHITE);
        for (final Point2D.Double star : this.stars) {
            final int x = (int) (star.x * split);
            final int y = (int) (star.y * this.getHeight());
            g2.fillRect(x, y, 2, 2);
        }
    }

    private void paintCabinet(final Graphics2D g2) {
        final double w = PinballWorld.TABLE_WIDTH;
        final double h = PinballWorld.TABLE_HEIGHT;
        final Path2D outer = new Path2D.Double();
        move(outer, this.camera.project(-8, -18, 0));
        line(outer, this.camera.project(w + 8, -18, 0));
        line(outer, this.camera.project(w + 8, h + 8, 18));
        line(outer, this.camera.project(-8, h + 8, 18));
        outer.closePath();
        g2.setColor(new Color(0x3A, 0x32, 0x38));
        g2.fill(outer);
        g2.setColor(new Color(0x6A, 0x62, 0x68));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(outer);
    }

    private void paintTable(final Graphics2D g2) {
        final double w = PinballWorld.TABLE_WIDTH;
        final double h = PinballWorld.TABLE_HEIGHT;
        final Path2D table = new Path2D.Double();
        final Point2D.Double nL = this.camera.project(0, 0, 0);
        final Point2D.Double nR = this.camera.project(w, 0, 0);
        final Point2D.Double fR = this.camera.project(w, h, 0);
        final Point2D.Double fL = this.camera.project(0, h, 0);
        move(table, nL);
        line(table, nR);
        line(table, fR);
        line(table, fL);
        table.closePath();
        g2.setPaint(new GradientPaint(
                (float) nL.x, (float) nL.y, TABLE_NEAR,
                (float) fL.x, (float) fL.y, TABLE_FAR
        ));
        g2.fill(table);
    }

    private void paintLane(final Graphics2D g2) {
        final Path2D lane = new Path2D.Double();
        final double left = PinballWorld.LAUNCH_LANE_LEFT;
        final double w = PinballWorld.TABLE_WIDTH;
        move(lane, this.camera.project(left, 2, 0.4));
        line(lane, this.camera.project(w, 2, 0.4));
        line(lane, this.camera.project(w, 350, 0.4));
        line(lane, this.camera.project(left, 350, 0.4));
        lane.closePath();
        g2.setColor(LANE);
        g2.fill(lane);
    }

    private void paintTableArt(final Graphics2D g2) {
        this.fillPoly(g2, new Color(0x6A, 0x28, 0x88, 180),
                this.camera.project(8, 200, 0.6),
                this.camera.project(46, 200, 0.6),
                this.camera.project(62, 340, 0.6),
                this.camera.project(10, 330, 0.6));
        this.fillPoly(g2, new Color(0xE0, 0xC0, 0x40, 200),
                this.camera.project(16, 210, 1.0),
                this.camera.project(46, 210, 1.0),
                this.camera.project(56, 325, 1.0),
                this.camera.project(22, 322, 1.0));
        this.fillPoly(g2, new Color(0x20, 0xC8, 0xD0, 140),
                this.camera.project(10, 70, 0.5),
                this.camera.project(36, 70, 0.5),
                this.camera.project(42, 120, 0.5),
                this.camera.project(12, 118, 0.5));
        this.fillPoly(g2, new Color(0x20, 0xC8, 0xD0, 140),
                this.camera.project(138, 70, 0.5),
                this.camera.project(168, 70, 0.5),
                this.camera.project(166, 118, 0.5),
                this.camera.project(132, 120, 0.5));

        final Point2D.Double medal = this.camera.project(100, 172, 0.8);
        final double ms = 16 * this.camera.scaleAt(172, 0.8);
        g2.setColor(new Color(0x18, 0x60, 0x88));
        g2.fill(new Ellipse2D.Double(medal.x - ms, medal.y - ms * 0.72, ms * 2, ms * 1.44));
        g2.setColor(new Color(0x40, 0xE0, 0xE8));
        g2.setStroke(new BasicStroke(2.2f));
        g2.draw(new Ellipse2D.Double(medal.x - ms * 0.72, medal.y - ms * 0.52, ms * 1.44, ms * 1.04));
        g2.setColor(new Color(0xF0, 0xD0, 0x50));
        g2.draw(new Ellipse2D.Double(medal.x - ms * 0.42, medal.y - ms * 0.30, ms * 0.84, ms * 0.60));
        for (int i = 0; i < 12; i++) {
            final double a = i * Math.PI * 2.0 / 12.0;
            final double lx = 100 + Math.cos(a) * 24;
            final double ly = 172 + Math.sin(a) * 24;
            final Point2D.Double lp = this.camera.project(lx, ly, 1.2);
            final double ls = 2.2 * this.camera.scaleAt(ly, 1.2);
            g2.setColor(i % 2 == 0 ? new Color(0xFF, 0xEE, 0x66) : new Color(0x44, 0xCC, 0xFF));
            g2.fill(new Ellipse2D.Double(lp.x - ls, lp.y - ls, ls * 2, ls * 2));
        }
    }

    private void paintDrain(final Graphics2D g2) {
        final Path2D drain = new Path2D.Double();
        move(drain, this.camera.project(PinballWorld.DRAIN_LEFT, 0, 0));
        line(drain, this.camera.project(PinballWorld.DRAIN_RIGHT, 0, 0));
        line(drain, this.camera.project(PinballWorld.DRAIN_RIGHT, PinballWorld.DRAIN_Y, 0));
        line(drain, this.camera.project(PinballWorld.DRAIN_LEFT, PinballWorld.DRAIN_Y, 0));
        drain.closePath();
        g2.setColor(new Color(0x04, 0x06, 0x10));
        g2.fill(drain);
    }

    private void paintFeatures(final Graphics2D g2) {
        for (final TableFeature feature : this.world.getFeatures()) {
            this.paintFeature(g2, feature);
        }
    }

    private void paintFeature(final Graphics2D g2, final TableFeature feature) {
        final Point2D.Double c = this.camera.project(feature.getX(), feature.getY(), 2.0);
        final double s = feature.getRadius() * this.camera.scaleAt(feature.getY(), 2.0);
        switch (feature.getKind()) {
            case WORMHOLE -> {
                g2.setColor(new Color(0x08, 0x04, 0x14));
                g2.fill(new Ellipse2D.Double(c.x - s, c.y - s * 0.7, s * 2, s * 1.4));
                g2.setColor(new Color(0xC8, 0x40, 0xE0));
                g2.setStroke(new BasicStroke(2.4f));
                g2.draw(new Ellipse2D.Double(c.x - s, c.y - s * 0.7, s * 2, s * 1.4));
                g2.setColor(new Color(0xF4, 0xD0, 0x40));
                g2.draw(new Ellipse2D.Double(c.x - s * 0.65, c.y - s * 0.45, s * 1.3, s * 0.9));
            }
            case HYPERSPACE -> {
                g2.setColor(new Color(0x10, 0x40, 0x70));
                g2.fill(new Ellipse2D.Double(c.x - s * 1.3, c.y - s * 0.55, s * 2.6, s * 1.1));
                g2.setColor(new Color(0x40, 0xE8, 0xFF));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(c.x - s * 1.3, c.y - s * 0.55, s * 2.6, s * 1.1));
                g2.setColor(new Color(0xCC, 0xFF, 0xFF));
                g2.fill(new Ellipse2D.Double(c.x - s * 0.45, c.y - s * 0.45, s * 0.9, s * 0.9));
            }
            case MISSION_TARGET -> {
                g2.setColor(feature.isLit() ? new Color(0xFF, 0xEE, 0x44) : new Color(0xC0, 0x88, 0x18));
                g2.fill(new Ellipse2D.Double(c.x - s, c.y - s * 1.4, s * 2, s * 2.8));
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(c.x - s * 0.35, c.y - s * 0.7, s * 0.7, s * 0.7));
            }
            case LAUNCH_RAMP, REENTRY_LANE, CENTER_MEDAL -> {
                // painted in table art
            }
        }
    }

    private void paintWalls(final Graphics2D g2) {
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (final Wall wall : this.world.getWalls()) {
            this.paintRaisedSegment(g2, wall.x1(), wall.y1(), wall.x2(), wall.y2(), WALL_HEIGHT, RAIL_GOLD, RAIL_DARK);
        }
    }

    private void paintRaisedSegment(
            final Graphics2D g2,
            final double x1,
            final double y1,
            final double x2,
            final double y2,
            final double height,
            final Color top,
            final Color side
    ) {
        final Point2D.Double a0 = this.camera.project(x1, y1, 0);
        final Point2D.Double b0 = this.camera.project(x2, y2, 0);
        final Point2D.Double a1 = this.camera.project(x1, y1, height);
        final Point2D.Double b1 = this.camera.project(x2, y2, height);
        final Path2D sidePath = new Path2D.Double();
        move(sidePath, a0);
        line(sidePath, b0);
        line(sidePath, b1);
        line(sidePath, a1);
        sidePath.closePath();
        g2.setColor(side);
        g2.fill(sidePath);
        g2.setColor(top);
        g2.draw(new Line2D.Double(a1, b1));
    }

    private void paintBumpers(final Graphics2D g2) {
        for (final Bumper bumper : this.world.getBumpers()) {
            this.paintBumper(g2, bumper);
        }
    }

    private void paintBumper(final Graphics2D g2, final Bumper bumper) {
        final int sides = 14;
        final double r = bumper.getRadius();
        final Path2D side = new Path2D.Double();
        for (int i = 0; i <= sides; i++) {
            final double a = (Math.PI * 2.0 * i) / sides;
            final Point2D.Double p = this.camera.project(
                    bumper.getX() + Math.cos(a) * r,
                    bumper.getY() + Math.sin(a) * r,
                    0
            );
            if (i == 0) {
                move(side, p);
            } else {
                line(side, p);
            }
        }
        for (int i = sides; i >= 0; i--) {
            final double a = (Math.PI * 2.0 * i) / sides;
            line(side, this.camera.project(
                    bumper.getX() + Math.cos(a) * r,
                    bumper.getY() + Math.sin(a) * r,
                    16
            ));
        }
        g2.setColor(new Color(0x80, 0x40, 0x10));
        g2.fill(side);

        final Path2D top = new Path2D.Double();
        for (int i = 0; i <= sides; i++) {
            final double a = (Math.PI * 2.0 * i) / sides;
            final Point2D.Double p = this.camera.project(
                    bumper.getX() + Math.cos(a) * r,
                    bumper.getY() + Math.sin(a) * r,
                    16
            );
            if (i == 0) {
                move(top, p);
            } else {
                line(top, p);
            }
        }
        top.closePath();
        final Point2D.Double c = this.camera.project(bumper.getX(), bumper.getY(), 16);
        g2.setColor(bumper.isContacting() ? new Color(0xFF, 0xEE, 0x66) : new Color(0xF0, 0xC0, 0x28));
        g2.fill(top);
        g2.setColor(new Color(0x88, 0x20, 0x80));
        final double cap = r * 0.42 * this.camera.scaleAt(bumper.getY(), 16);
        g2.fill(new Ellipse2D.Double(c.x - cap, c.y - cap, cap * 2, cap * 2));
        g2.setColor(new Color(0xFF, 0xF0, 0xA0));
        g2.fill(new Ellipse2D.Double(c.x - cap * 0.35, c.y - cap * 0.55, cap * 0.5, cap * 0.4));
    }

    private void paintPlunger(final Graphics2D g2) {
        final double pull = this.world.getPlungerPull();
        final double x = (PinballWorld.LAUNCH_LANE_LEFT + PinballWorld.TABLE_WIDTH) * 0.5;
        final double y1 = 2.0 - pull * 16.0;
        final double y2 = 28.0 - pull * 16.0;
        this.paintRaisedSegment(g2, x - 2.2, y1, x - 2.2, y2, 6, new Color(0xC0, 0xC6, 0xD0), new Color(0x60, 0x66, 0x70));
        this.paintRaisedSegment(g2, x + 2.2, y1, x + 2.2, y2, 6, new Color(0xC0, 0xC6, 0xD0), new Color(0x60, 0x66, 0x70));
        final Point2D.Double knob = this.camera.project(x, y1 - 2, 3);
        final double ks = 5 * this.camera.scaleAt(y1, 3);
        g2.setColor(new Color(0xAA, 0x22, 0x22));
        g2.fill(new Ellipse2D.Double(knob.x - ks, knob.y - ks, ks * 2, ks * 2));
    }

    private void paintFlipper(final Graphics2D g2, final Flipper flipper) {
        final int steps = 8;
        final Path2D top = new Path2D.Double();
        final Path2D side = new Path2D.Double();
        Point2D.Double firstLow = null;
        Point2D.Double lastLow = null;
        for (int i = 0; i <= steps; i++) {
            final double t = i / (double) steps;
            final double x = flipper.getPivotX() + Math.cos(flipper.getAngle()) * flipper.getLength() * t;
            final double y = flipper.getPivotY() + Math.sin(flipper.getAngle()) * flipper.getLength() * t;
            final double r = flipper.getRadius() * (1.0 - t * 0.35);
            final double nx = -Math.sin(flipper.getAngle());
            final double ny = Math.cos(flipper.getAngle());
            final Point2D.Double p = this.camera.project(x + nx * r, y + ny * r, 7);
            final Point2D.Double q = this.camera.project(x - nx * r, y - ny * r, 7);
            final Point2D.Double pl = this.camera.project(x + nx * r, y + ny * r, 0);
            if (i == 0) {
                move(top, p);
                firstLow = pl;
            } else {
                line(top, p);
            }
            lastLow = this.camera.project(x - nx * r, y - ny * r, 0);
            if (i == steps) {
                line(top, q);
            }
        }
        for (int i = steps; i >= 0; i--) {
            final double t = i / (double) steps;
            final double x = flipper.getPivotX() + Math.cos(flipper.getAngle()) * flipper.getLength() * t;
            final double y = flipper.getPivotY() + Math.sin(flipper.getAngle()) * flipper.getLength() * t;
            final double r = flipper.getRadius() * (1.0 - t * 0.35);
            final double nx = -Math.sin(flipper.getAngle());
            final double ny = Math.cos(flipper.getAngle());
            line(top, this.camera.project(x - nx * r, y - ny * r, 7));
        }
        top.closePath();
        if (firstLow != null && lastLow != null) {
            move(side, firstLow);
            line(side, lastLow);
            line(side, this.camera.project(flipper.getTipX(), flipper.getTipY(), 7));
            line(side, this.camera.project(flipper.getPivotX(), flipper.getPivotY(), 7));
            side.closePath();
            g2.setColor(FLIPPER_EDGE);
            g2.fill(side);
        }
        g2.setColor(FLIPPER_BODY);
        g2.fill(top);
        g2.setColor(FLIPPER_EDGE);
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(top);
        final Point2D.Double pivot = this.camera.project(flipper.getPivotX(), flipper.getPivotY(), 8);
        final double ps = 4.5 * this.camera.scaleAt(flipper.getPivotY(), 8);
        g2.setColor(new Color(0xEE, 0xEE, 0xF4));
        g2.fill(new Ellipse2D.Double(pivot.x - ps, pivot.y - ps, ps * 2, ps * 2));
    }

    private void paintBall(final Graphics2D g2) {
        final double x = this.world.getBallX();
        final double y = this.world.getBallY();
        final double r = this.world.getBallRadius();
        final Point2D.Double shadow = this.camera.project(x, y, 0);
        final double ss = r * 1.15 * this.camera.scaleAt(y, 0);
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fill(new Ellipse2D.Double(shadow.x - ss, shadow.y - ss * 0.55, ss * 2, ss * 1.1));

        final Point2D.Double c = this.camera.project(x, y, BALL_HEIGHT_CENTER);
        final double s = r * this.camera.scaleAt(y, BALL_HEIGHT_CENTER);
        g2.setPaint(new GradientPaint(
                (float) (c.x - s * 0.4), (float) (c.y - s * 0.5), Color.WHITE,
                (float) (c.x + s * 0.5), (float) (c.y + s * 0.6), new Color(0x6A, 0x72, 0x80)
        ));
        g2.fill(new Ellipse2D.Double(c.x - s, c.y - s, s * 2, s * 2));
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, 180));
        g2.fill(new Ellipse2D.Double(c.x - s * 0.45, c.y - s * 0.55, s * 0.55, s * 0.4));
    }

    private void paintBackglass(final Graphics2D g2, final int split) {
        final int x = split;
        final int w = this.getWidth() - split;
        final int h = this.getHeight();
        g2.setColor(PANEL_BG);
        g2.fillRect(x, 0, w, h);
        g2.setPaint(new GradientPaint(x, 0, new Color(0x2A, 0x28, 0x18), x + w, h, new Color(0x14, 0x22, 0x14)));
        g2.fillRect(x + 8, 8, w - 16, h - 16);
        g2.setColor(PANEL_EDGE);
        g2.setStroke(new BasicStroke(4f));
        g2.drawRect(x + 8, 8, w - 16, h - 16);

        final int cx = x + w / 2;
        g2.setColor(new Color(0xC8, 0xB0, 0xFF));
        g2.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 18));
        this.drawCentered(g2, "3D Pinball", cx, 48);
        g2.setColor(new Color(0x7A, 0xF0, 0x6A));
        g2.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 32));
        this.drawCentered(g2, "Space Cadet", cx, 86);

        this.paintCadetShip(g2, cx, 175);

        this.paintLedBox(g2, x + 28, 268, w - 56, 54, "BALL  " + this.world.getBallNumber());
        this.paintLedBox(g2, x + 28, 334, (w - 64) / 2, 48, String.valueOf(this.world.getMissionsCompleted()));
        this.paintLedBox(g2, x + 28 + (w - 64) / 2 + 8, 334, (w - 64) / 2, 48, String.valueOf(this.world.getScore()));

        g2.setColor(new Color(0xE8, 0xE0, 0xA0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        this.drawCentered(g2, this.world.getStatusLine(), cx, 430);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(new Color(0xA0, 0xC8, 0xFF));
        this.drawCentered(g2, "Rank  " + this.world.getRank().name(), cx, 456);

        if (this.world.isMissionActive()) {
            final int bx = x + 36;
            final int by = 480;
            final int bw = w - 72;
            g2.setColor(new Color(0x30, 0x30, 0x30));
            g2.fillRect(bx, by, bw, 16);
            final int fw = (int) Math.round(bw * Math.max(0.0, Math.min(1.0, this.world.getFuel() / PinballWorld.FUEL_MAX)));
            g2.setColor(new Color(0x40, 0xE0, 0x60));
            g2.fillRect(bx, by, fw, 16);
            g2.setColor(new Color(0xC0, 0xC0, 0xC0));
            g2.drawRect(bx, by, bw, 16);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("FUEL", bx, by - 4);
        }

        g2.setColor(new Color(0x88, 0x88, 0x90));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        this.drawCentered(g2, "Z / ←  left     / / →  right", cx, h - 48);
        this.drawCentered(g2, "hold SPACE plunger     F2 new", cx, h - 32);

        if (this.world.isGameOver()) {
            g2.setColor(new Color(0xFF, 0xCC, 0x44));
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            this.drawCentered(g2, "GAME OVER", cx, h - 80);
        }
    }

    private void paintCadetShip(final Graphics2D g2, final int cx, final int cy) {
        g2.setColor(new Color(0x60, 0x80, 0xA8));
        g2.fill(new Ellipse2D.Double(cx - 70, cy - 18, 140, 50));
        g2.setColor(new Color(0xC0, 0xD8, 0xF0));
        g2.fill(new Ellipse2D.Double(cx - 38, cy - 48, 76, 52));
        g2.setColor(new Color(0x40, 0x20, 0x70));
        g2.fill(new Ellipse2D.Double(cx - 16, cy - 38, 32, 28));
        g2.setColor(new Color(0xF0, 0xC8, 0x90));
        g2.fill(new Ellipse2D.Double(cx - 11, cy - 34, 22, 20));
        g2.setColor(new Color(0x30, 0x20, 0x18));
        g2.fill(new Ellipse2D.Double(cx - 5, cy - 28, 4, 4));
        g2.fill(new Ellipse2D.Double(cx + 3, cy - 28, 4, 4));
        g2.setColor(new Color(0x40, 0xE0, 0xA0));
        g2.fill(new Ellipse2D.Double(cx - 48, cy + 8, 16, 10));
        g2.fill(new Ellipse2D.Double(cx + 32, cy + 8, 16, 10));
        g2.setColor(new Color(0xE8, 0xE8, 0x90));
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        this.drawCentered(g2, "CADET", cx, cy + 48);
    }

    private void paintLedBox(final Graphics2D g2, final int x, final int y, final int w, final int h, final String text) {
        g2.setColor(new Color(0x08, 0x08, 0x08));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 8, 8));
        g2.setColor(new Color(0x40, 0x80, 0x40));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 8, 8));
        g2.setColor(new Color(0x40, 0xFF, 0x70));
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        final int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, x + (w - tw) / 2, y + h / 2 + 8);
    }

    private void drawCentered(final Graphics2D g2, final String text, final int cx, final int y) {
        final int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, cx - tw / 2, y);
    }

    private void fillPoly(final Graphics2D g2, final Color color, final Point2D.Double... pts) {
        final Path2D path = new Path2D.Double();
        move(path, pts[0]);
        for (int i = 1; i < pts.length; i++) {
            line(path, pts[i]);
        }
        path.closePath();
        g2.setColor(color);
        g2.fill(path);
    }

    private static void move(final Path2D path, final Point2D.Double p) {
        path.moveTo(p.x, p.y);
    }

    private static void line(final Path2D path, final Point2D.Double p) {
        path.lineTo(p.x, p.y);
    }
}
