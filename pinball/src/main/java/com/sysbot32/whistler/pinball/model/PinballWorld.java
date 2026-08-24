package com.sysbot32.whistler.pinball.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic pinball simulation: gravity toward the drain, walls, plunger
 * launch, independently raised flippers, pop bumpers, score, and ball count.
 * Table coordinates: +x right, +y upfield (away from the drain at y = 0).
 */
@Getter
public final class PinballWorld {
    public static final double TABLE_WIDTH = 200.0;
    public static final double TABLE_HEIGHT = 400.0;
    public static final double BALL_RADIUS = 5.0;
    public static final int STARTING_BALLS = 3;
    public static final double LAUNCH_LANE_LEFT = 178.0;
    public static final double DRAIN_LEFT = 72.0;
    public static final double DRAIN_RIGHT = 108.0;
    public static final double DRAIN_Y = 10.0;
    public static final double GRAVITY = 320.0;
    public static final double LAUNCH_SPEED_MIN = 280.0;
    public static final double LAUNCH_SPEED_MAX = 780.0;
    public static final int BUMPER_SCORE = 100;
    public static final double LEFT_FLIPPER_PIVOT_X = 50.0;
    public static final double RIGHT_FLIPPER_PIVOT_X = 130.0;
    public static final double FLIPPER_PIVOT_Y = 34.0;
    public static final double FLIPPER_LENGTH = 34.0;
    public static final double FLIPPER_RADIUS = 4.0;

    private static final double MAX_STEP = 1.0 / 240.0;
    private static final double DAMPING = 0.12;
    private static final double MAX_SPEED = 1000.0;
    private static final double WALL_RESTITUTION = 0.48;
    private static final double BUMPER_BOOST = 170.0;
    private static final double BUMPER_RESTITUTION = 1.15;
    private static final double FLIPPER_HOLD_KICK = 220.0;
    private static final double FLIPPER_SWING_KICK = 14.0;
    private static final double PLUNGER_REST_Y = 14.0;
    private static final double PLUNGER_DRAW = 16.0;

    private final List<Wall> walls;
    private final List<Bumper> bumpers;
    private final Flipper leftFlipper;
    private final Flipper rightFlipper;

    private double ballX;
    private double ballY;
    private double ballVx;
    private double ballVy;
    private final double ballRadius = BALL_RADIUS;

    private int score;
    private int ballsRemaining;
    private GameStatus status;
    private double plungerPull;
    private boolean inPlunger;

    public PinballWorld() {
        this.walls = new ArrayList<>();
        this.bumpers = new ArrayList<>();
        final double leftRest = Math.toRadians(-24.0);
        final double leftRaised = Math.toRadians(52.0);
        this.leftFlipper = new Flipper(
                LEFT_FLIPPER_PIVOT_X,
                FLIPPER_PIVOT_Y,
                FLIPPER_LENGTH,
                FLIPPER_RADIUS,
                leftRest,
                leftRaised
        );
        this.rightFlipper = new Flipper(
                RIGHT_FLIPPER_PIVOT_X,
                FLIPPER_PIVOT_Y,
                FLIPPER_LENGTH,
                FLIPPER_RADIUS,
                Math.PI - leftRest,
                Math.PI - leftRaised
        );
        this.buildPlayfield();
        this.reset();
    }

    public List<Wall> getWalls() {
        return Collections.unmodifiableList(this.walls);
    }

    public List<Bumper> getBumpers() {
        return Collections.unmodifiableList(this.bumpers);
    }

    public boolean isGameOver() {
        return this.status == GameStatus.GAME_OVER;
    }

    public boolean isInPlunger() {
        return this.inPlunger;
    }

    public void reset() {
        this.score = 0;
        this.ballsRemaining = STARTING_BALLS;
        this.status = GameStatus.PLAYING;
        this.leftFlipper.setRaised(false);
        this.rightFlipper.setRaised(false);
        this.leftFlipper.tick(1.0);
        this.rightFlipper.tick(1.0);
        this.clearContacts();
        this.serveBall();
    }

    public void placeBall(final double x, final double y) {
        this.inPlunger = false;
        this.plungerPull = 0.0;
        this.ballX = x;
        this.ballY = y;
        this.ballVx = 0.0;
        this.ballVy = 0.0;
        this.clearContacts();
    }

    public void setBallVelocity(final double vx, final double vy) {
        this.ballVx = vx;
        this.ballVy = vy;
    }

    public void setPlungerPull(final double pull) {
        this.plungerPull = clamp(pull, 0.0, 1.0);
        if (this.inPlunger) {
            this.ballY = this.plungerBallY();
        }
    }

    public void pullPlunger(final double dt) {
        if (!this.inPlunger || this.status == GameStatus.GAME_OVER) {
            return;
        }
        this.setPlungerPull(this.plungerPull + dt / 0.85);
    }

    public void releasePlunger() {
        if (this.status == GameStatus.GAME_OVER) {
            this.plungerPull = 0.0;
            return;
        }
        if (!this.inPlunger) {
            this.plungerPull = 0.0;
            return;
        }
        final double speed = LAUNCH_SPEED_MIN + this.plungerPull * (LAUNCH_SPEED_MAX - LAUNCH_SPEED_MIN);
        this.ballVx = 0.0;
        this.ballVy = speed;
        this.ballX = this.laneCenter();
        this.ballY = this.plungerBallY();
        this.inPlunger = false;
        this.plungerPull = 0.0;
    }

    public void setLeftFlipperRaised(final boolean raised) {
        this.leftFlipper.setRaised(raised);
    }

    public void setRightFlipperRaised(final boolean raised) {
        this.rightFlipper.setRaised(raised);
    }

    public void tick(final double dt) {
        if (this.status == GameStatus.GAME_OVER) {
            return;
        }
        if (dt <= 0.0) {
            return;
        }
        double remaining = dt;
        while (remaining > 1e-12) {
            final double step = Math.min(MAX_STEP, remaining);
            this.step(step);
            remaining -= step;
            if (this.status == GameStatus.GAME_OVER) {
                return;
            }
        }
    }

    private void step(final double dt) {
        this.leftFlipper.tick(dt);
        this.rightFlipper.tick(dt);

        if (this.inPlunger) {
            this.ballX = this.laneCenter();
            this.ballY = this.plungerBallY();
            this.ballVx = 0.0;
            this.ballVy = 0.0;
            return;
        }

        this.ballVy -= GRAVITY * dt;
        this.ballVx *= Math.exp(-DAMPING * dt);
        this.ballVy *= Math.exp(-DAMPING * dt);
        final double speed = Math.hypot(this.ballVx, this.ballVy);
        if (speed > MAX_SPEED) {
            this.ballVx *= MAX_SPEED / speed;
            this.ballVy *= MAX_SPEED / speed;
        }
        this.ballX += this.ballVx * dt;
        this.ballY += this.ballVy * dt;

        for (final Wall wall : this.walls) {
            this.collideSegment(wall.x1(), wall.y1(), wall.x2(), wall.y2(), 0.0, wall.restitution());
        }
        for (final Bumper bumper : this.bumpers) {
            this.collideBumper(bumper);
        }
        this.collideFlipper(this.leftFlipper);
        this.collideFlipper(this.rightFlipper);

        if (this.ballX >= LAUNCH_LANE_LEFT && this.ballY < PLUNGER_REST_Y + this.ballRadius + 4.0 && this.ballVy <= 0.0) {
            this.serveBall();
            return;
        }
        if (this.isInDrain()) {
            this.drain();
        }
    }

    private boolean isInDrain() {
        if (this.inPlunger) {
            return false;
        }
        if (this.ballX >= LAUNCH_LANE_LEFT) {
            return false;
        }
        return this.ballY < DRAIN_Y;
    }

    private void drain() {
        this.ballsRemaining -= 1;
        this.clearContacts();
        if (this.ballsRemaining <= 0) {
            this.ballsRemaining = 0;
            this.status = GameStatus.GAME_OVER;
            this.ballVx = 0.0;
            this.ballVy = 0.0;
            return;
        }
        this.serveBall();
    }

    private void serveBall() {
        this.inPlunger = true;
        this.plungerPull = 0.0;
        this.ballX = this.laneCenter();
        this.ballY = this.plungerBallY();
        this.ballVx = 0.0;
        this.ballVy = 0.0;
        this.leftFlipper.setContacting(false);
        this.rightFlipper.setContacting(false);
    }

    private void collideBumper(final Bumper bumper) {
        final double dx = this.ballX - bumper.getX();
        final double dy = this.ballY - bumper.getY();
        final double dist = Math.hypot(dx, dy);
        final double minDist = this.ballRadius + bumper.getRadius();
        if (dist >= minDist) {
            bumper.setContacting(false);
            return;
        }
        double nx;
        double ny;
        if (dist < 1e-9) {
            nx = 0.0;
            ny = 1.0;
        } else {
            nx = dx / dist;
            ny = dy / dist;
        }
        final double overlap = minDist - dist;
        this.ballX += nx * overlap;
        this.ballY += ny * overlap;
        final double vn = this.ballVx * nx + this.ballVy * ny;
        if (vn < 0.0) {
            this.ballVx -= (1.0 + BUMPER_RESTITUTION) * vn * nx;
            this.ballVy -= (1.0 + BUMPER_RESTITUTION) * vn * ny;
        }
        if (!bumper.isContacting()) {
            this.score += BUMPER_SCORE;
            this.ballVx += nx * BUMPER_BOOST;
            this.ballVy += ny * BUMPER_BOOST;
        }
        bumper.setContacting(true);
    }

    private void collideFlipper(final Flipper flipper) {
        final double x1 = flipper.getPivotX();
        final double y1 = flipper.getPivotY();
        final double x2 = flipper.getTipX();
        final double y2 = flipper.getTipY();
        final double px = this.ballX;
        final double py = this.ballY;
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double len2 = dx * dx + dy * dy;
        final double t = len2 < 1e-12 ? 0.0 : clamp(((px - x1) * dx + (py - y1) * dy) / len2, 0.0, 1.0);
        final double cx = x1 + t * dx;
        final double cy = y1 + t * dy;
        double nx = px - cx;
        double ny = py - cy;
        final double dist = Math.hypot(nx, ny);
        final double minDist = this.ballRadius + flipper.getRadius();
        final boolean nowContact = dist < minDist;
        if (!nowContact) {
            flipper.setContacting(false);
            return;
        }
        if (dist < 1e-9) {
            nx = -Math.sin(flipper.getAngle());
            ny = Math.cos(flipper.getAngle());
            if (ny < 0.0) {
                nx = -nx;
                ny = -ny;
            }
        } else {
            nx /= dist;
            ny /= dist;
        }
        final double overlap = minDist - dist;
        this.ballX += nx * overlap;
        this.ballY += ny * overlap;

        final double relX = cx - x1;
        final double relY = cy - y1;
        final double omega = flipper.getAngularVelocity();
        final double pointVx = -omega * relY;
        final double pointVy = omega * relX;
        final double rvx = this.ballVx - pointVx;
        final double rvy = this.ballVy - pointVy;
        final double vn = rvx * nx + rvy * ny;
        if (vn < 0.0) {
            this.ballVx -= (1.0 + 0.35) * vn * nx;
            this.ballVy -= (1.0 + 0.35) * vn * ny;
        }

        if (!flipper.isContacting() && (flipper.isRaised() || Math.abs(omega) > 1.5)) {
            double fx = -Math.sin(flipper.getAngle());
            double fy = Math.cos(flipper.getAngle());
            if (fy < 0.0) {
                fx = -fx;
                fy = -fy;
            }
            final double kick = FLIPPER_HOLD_KICK + Math.abs(omega) * FLIPPER_SWING_KICK;
            this.ballVx += fx * kick;
            this.ballVy += fy * kick;
        }
        flipper.setContacting(true);
    }

    private void collideSegment(
            final double x1,
            final double y1,
            final double x2,
            final double y2,
            final double radius,
            final double restitution
    ) {
        final double px = this.ballX;
        final double py = this.ballY;
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double len2 = dx * dx + dy * dy;
        double t = 0.0;
        if (len2 >= 1e-12) {
            t = clamp(((px - x1) * dx + (py - y1) * dy) / len2, 0.0, 1.0);
        }
        final double cx = x1 + t * dx;
        final double cy = y1 + t * dy;
        double nx = px - cx;
        double ny = py - cy;
        final double dist = Math.hypot(nx, ny);
        final double minDist = this.ballRadius + radius;
        if (dist >= minDist) {
            return;
        }
        if (dist < 1e-9) {
            final double len = Math.sqrt(len2);
            if (len < 1e-9) {
                nx = 0.0;
                ny = 1.0;
            } else {
                nx = -dy / len;
                ny = dx / len;
            }
        } else {
            nx /= dist;
            ny /= dist;
        }
        final double overlap = minDist - dist;
        this.ballX += nx * overlap;
        this.ballY += ny * overlap;
        final double vn = this.ballVx * nx + this.ballVy * ny;
        if (vn < 0.0) {
            this.ballVx -= (1.0 + restitution) * vn * nx;
            this.ballVy -= (1.0 + restitution) * vn * ny;
        }
    }

    private void buildPlayfield() {
        final double w = TABLE_WIDTH;
        final double h = TABLE_HEIGHT;
        final double e = WALL_RESTITUTION;
        this.walls.add(new Wall(0.0, 18.0, 0.0, h, e));
        this.walls.add(new Wall(0.0, h, w, h, e));
        this.walls.add(new Wall(w, 0.0, w, h, e));
        this.walls.add(new Wall(LAUNCH_LANE_LEFT, 16.0, LAUNCH_LANE_LEFT, 350.0, e));
        this.walls.add(new Wall(w, 350.0, LAUNCH_LANE_LEFT - 22.0, h, 0.32));
        this.walls.add(new Wall(0.0, 55.0, LEFT_FLIPPER_PIVOT_X - 5.0, FLIPPER_PIVOT_Y + 2.0, e));
        this.walls.add(new Wall(LAUNCH_LANE_LEFT, 55.0, RIGHT_FLIPPER_PIVOT_X + 5.0, FLIPPER_PIVOT_Y + 2.0, e));
        this.walls.add(new Wall(LAUNCH_LANE_LEFT, 2.0, w, 2.0, 0.2));
        this.walls.add(new Wall(0.0, h - 8.0, 28.0, h, e));
        this.walls.add(new Wall(8.0, 210.0, 28.0, 160.0, 0.7));
        this.walls.add(new Wall(LAUNCH_LANE_LEFT - 8.0, 210.0, LAUNCH_LANE_LEFT - 28.0, 160.0, 0.7));

        this.bumpers.add(new Bumper(78.0, 288.0, 12.0));
        this.bumpers.add(new Bumper(100.0, 318.0, 12.0));
        this.bumpers.add(new Bumper(122.0, 288.0, 12.0));
    }

    private void clearContacts() {
        for (final Bumper bumper : this.bumpers) {
            bumper.setContacting(false);
        }
        this.leftFlipper.setContacting(false);
        this.rightFlipper.setContacting(false);
    }

    private double laneCenter() {
        return (LAUNCH_LANE_LEFT + TABLE_WIDTH) * 0.5;
    }

    private double plungerBallY() {
        return PLUNGER_REST_Y - this.plungerPull * PLUNGER_DRAW + this.ballRadius;
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }
}
