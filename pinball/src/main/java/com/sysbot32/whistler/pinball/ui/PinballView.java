package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.Bumper;
import com.sysbot32.whistler.pinball.model.Flipper;
import com.sysbot32.whistler.pinball.model.PinballWorld;
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
import java.util.Random;

/**
 * Perspective-3D playfield: foreshortened table, raised rails, cylindrical
 * bumpers, flippers, plunger lane, and a HUD for score / balls remaining.
 */
public final class PinballView extends JPanel {
    static final int PREFERRED_WIDTH = 540;
    static final int PREFERRED_HEIGHT = 800;

    private static final Color SPACE_TOP = new Color(0x05, 0x07, 0x16);
    private static final Color SPACE_BOTTOM = new Color(0x12, 0x18, 0x3A);
    private static final Color TABLE_NEAR = new Color(0x1B, 0x3A, 0x7A);
    private static final Color TABLE_FAR = new Color(0x0C, 0x18, 0x3C);
    private static final Color LANE = new Color(0x14, 0x2A, 0x58);
    private static final Color RAIL_GOLD = new Color(0xD4, 0xB1, 0x2A);
    private static final Color RAIL_DARK = new Color(0x7A, 0x5A, 0x10);
    private static final Color FLIPPER_BODY = new Color(0xF0, 0xC4, 0x2E);
    private static final Color FLIPPER_EDGE = new Color(0x8A, 0x6A, 0x08);
    private static final Color HUD_BG = new Color(0, 0, 0, 150);
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
        this.stars = new Point2D.Double[90];
        final Random rng = new Random(3);
        for (int i = 0; i < this.stars.length; i++) {
            this.stars[i] = new Point2D.Double(rng.nextDouble(), rng.nextDouble());
        }
    }

    public TableCamera getCamera() {
        return this.camera;
    }

    public Point2D.Double project(final double tableX, final double tableY, final double heightAboveTable) {
        this.camera.setViewport(Math.max(1, this.getWidth()), Math.max(1, this.getHeight()));
        return this.camera.project(tableX, tableY, heightAboveTable);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            this.camera.setViewport(this.getWidth(), this.getHeight());
            this.paintSpace(g2);
            this.paintCabinet(g2);
            this.paintTable(g2);
            this.paintLane(g2);
            this.paintDrain(g2);
            this.paintWalls(g2);
            this.paintBumpers(g2);
            this.paintPlunger(g2);
            this.paintFlipper(g2, this.world.getLeftFlipper());
            this.paintFlipper(g2, this.world.getRightFlipper());
            this.paintBall(g2);
            this.paintHud(g2);
        } finally {
            g2.dispose();
        }
    }

    private void paintSpace(final Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, SPACE_TOP, 0, this.getHeight(), SPACE_BOTTOM));
        g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        g2.setColor(Color.WHITE);
        for (final Point2D.Double star : this.stars) {
            final int x = (int) (star.x * this.getWidth());
            final int y = (int) (star.y * this.getHeight());
            g2.fillRect(x, y, 2, 2);
        }
    }

    private void paintCabinet(final Graphics2D g2) {
        final double w = PinballWorld.TABLE_WIDTH;
        final double h = PinballWorld.TABLE_HEIGHT;
        final Path2D outer = new Path2D.Double();
        final Point2D.Double[] rim = {
                this.camera.project(-8, -18, 0),
                this.camera.project(w + 8, -18, 0),
                this.camera.project(w + 8, h + 8, 22),
                this.camera.project(-8, h + 8, 22)
        };
        move(outer, rim[0]);
        line(outer, rim[1]);
        line(outer, rim[2]);
        line(outer, rim[3]);
        outer.closePath();
        g2.setColor(new Color(0x2A, 0x1C, 0x10));
        g2.fill(outer);
        g2.setColor(new Color(0x5A, 0x3A, 0x18));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(outer);

        final Path2D glass = new Path2D.Double();
        final Point2D.Double g0 = this.camera.project(10, h + 2, 4);
        final Point2D.Double g1 = this.camera.project(w - 10, h + 2, 4);
        final Point2D.Double g2p = this.camera.project(w - 10, h + 2, 52);
        final Point2D.Double g3 = this.camera.project(10, h + 2, 52);
        move(glass, g0);
        line(glass, g1);
        line(glass, g2p);
        line(glass, g3);
        glass.closePath();
        g2.setPaint(new GradientPaint(
                (float) g3.x, (float) g3.y, new Color(0x40, 0x18, 0x70),
                (float) g0.x, (float) g0.y, new Color(0x18, 0x30, 0x80)
        ));
        g2.fill(glass);
        g2.setColor(new Color(0xC0, 0xA0, 0x40));
        g2.setStroke(new BasicStroke(2f));
        g2.draw(glass);
        g2.setColor(new Color(0xF4, 0xE0, 0x80));
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        final String title = "3D PINBALL";
        final int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (int) ((g0.x + g1.x) / 2.0 - tw / 2.0), (int) ((g2p.y + g1.y) / 2.0));
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
        final double h = PinballWorld.TABLE_HEIGHT;
        move(lane, this.camera.project(left, 2, 0.4));
        line(lane, this.camera.project(w, 2, 0.4));
        line(lane, this.camera.project(w, h - 4, 0.4));
        line(lane, this.camera.project(left, 350, 0.4));
        lane.closePath();
        g2.setColor(LANE);
        g2.fill(lane);
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
            final double x = bumper.getX() + Math.cos(a) * r;
            final double y = bumper.getY() + Math.sin(a) * r;
            final Point2D.Double p = this.camera.project(x, y, 0);
            if (i == 0) {
                move(side, p);
            } else {
                line(side, p);
            }
        }
        for (int i = sides; i >= 0; i--) {
            final double a = (Math.PI * 2.0 * i) / sides;
            final double x = bumper.getX() + Math.cos(a) * r;
            final double y = bumper.getY() + Math.sin(a) * r;
            line(side, this.camera.project(x, y, 16));
        }
        g2.setColor(new Color(0x80, 0x20, 0x18));
        g2.fill(side);

        final Path2D top = new Path2D.Double();
        for (int i = 0; i <= sides; i++) {
            final double a = (Math.PI * 2.0 * i) / sides;
            final double x = bumper.getX() + Math.cos(a) * r;
            final double y = bumper.getY() + Math.sin(a) * r;
            final Point2D.Double p = this.camera.project(x, y, 16);
            if (i == 0) {
                move(top, p);
            } else {
                line(top, p);
            }
        }
        top.closePath();
        final Point2D.Double c = this.camera.project(bumper.getX(), bumper.getY(), 16);
        g2.setColor(bumper.isContacting() ? new Color(0xFF, 0xEE, 0x66) : new Color(0xE8, 0x3A, 0x2A));
        g2.fill(top);
        g2.setColor(new Color(0xFF, 0xC8, 0x40));
        final double cap = r * 0.45 * this.camera.scaleAt(bumper.getY(), 16);
        g2.fill(new Ellipse2D.Double(c.x - cap, c.y - cap, cap * 2, cap * 2));
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

    private void paintHud(final Graphics2D g2) {
        final int barH = 44;
        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, this.getWidth(), barH + 18);
        g2.setColor(new Color(0xF4, 0xE0, 0x70));
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2.drawString(String.format("SCORE  %06d", this.world.getScore()), 16, 28);
        final String balls = "BALLS  " + this.world.getBallsRemaining();
        final int bw = g2.getFontMetrics().stringWidth(balls);
        g2.drawString(balls, this.getWidth() - bw - 16, 28);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(0xC8, 0xD0, 0xE8));
        g2.drawString("Z / ←  left flipper    / / →  right flipper    hold SPACE  plunger    F2  new game", 16, barH + 10);

        if (this.world.isGameOver()) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, this.getHeight() / 2 - 40, this.getWidth(), 80);
            g2.setColor(new Color(0xFF, 0xCC, 0x44));
            g2.setFont(new Font("SansSerif", Font.BOLD, 36));
            final String over = "GAME OVER";
            final int ow = g2.getFontMetrics().stringWidth(over);
            g2.drawString(over, (this.getWidth() - ow) / 2, this.getHeight() / 2 + 12);
        }
    }

    private static void move(final Path2D path, final Point2D.Double p) {
        path.moveTo(p.x, p.y);
    }

    private static void line(final Path2D path, final Point2D.Double p) {
        path.lineTo(p.x, p.y);
    }
}
