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

    public static final String STATUS_AWAITING = "Awaiting Deployment";
    public static final String STATUS_TARGET_PRACTICE = "Target Practice";
    public static final String STATUS_MISSION_COMPLETE = "Mission Complete";
    public static final String STATUS_OUT_OF_FUEL = "Out of Fuel";
    public static final double FUEL_MAX = 100.0;
    public static final double FUEL_BURN_PER_SECOND = 20.0;
    public static final int TARGET_PRACTICE_HITS = 8;
    public static final int MISSION_TARGETS_TO_ARM = 3;

    private final List<Wall> walls;
    private final List<Bumper> bumpers;
    private final List<TableFeature> features;
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

    private Rank rank;
    private Mission mission;
    private String statusLine;
    private double fuel;
    private int practiceHits;
    private int missionsCompleted;

    public PinballWorld() {
        this.walls = new ArrayList<>();
        this.bumpers = new ArrayList<>();
        this.features = new ArrayList<>();
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

    public List<TableFeature> getFeatures() {
        return Collections.unmodifiableList(this.features);
    }

    public List<TableFeature> featuresOf(final TableFeature.Kind kind) {
        final List<TableFeature> matched = new ArrayList<>();
        for (final TableFeature feature : this.features) {
            if (feature.getKind() == kind) {
                matched.add(feature);
            }
        }
        return Collections.unmodifiableList(matched);
    }

    public TableFeature getLaunchRamp() {
        return this.featuresOf(TableFeature.Kind.LAUNCH_RAMP).getFirst();
    }

    public TableFeature getCenterMedal() {
        return this.featuresOf(TableFeature.Kind.CENTER_MEDAL).getFirst();
    }

    public TableFeature getHyperspace() {
        return this.featuresOf(TableFeature.Kind.HYPERSPACE).getFirst();
    }

    public List<TableFeature> getWormholes() {
        return this.featuresOf(TableFeature.Kind.WORMHOLE);
    }

    public List<TableFeature> getReentryLanes() {
        return this.featuresOf(TableFeature.Kind.REENTRY_LANE);
    }

    public List<TableFeature> getMissionTargets() {
        return this.featuresOf(TableFeature.Kind.MISSION_TARGET);
    }

    public boolean isMissionActive() {
        return this.mission != Mission.NONE;
    }

    public int getBallNumber() {
        if (this.ballsRemaining <= 0) {
            return STARTING_BALLS;
        }
        return STARTING_BALLS - this.ballsRemaining + 1;
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
        this.rank = Rank.CADET;
        this.mission = Mission.NONE;
        this.statusLine = STATUS_AWAITING;
        this.fuel = 0.0;
        this.practiceHits = 0;
        this.missionsCompleted = 0;
        this.leftFlipper.setRaised(false);
        this.rightFlipper.setRaised(false);
        this.leftFlipper.tick(1.0);
        this.rightFlipper.tick(1.0);
        this.clearContacts();
        for (final TableFeature feature : this.features) {
            feature.setLit(false);
        }
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
        if (this.mission != Mission.NONE) {
            this.fuel -= FUEL_BURN_PER_SECOND * dt;
            if (this.fuel <= 0.0) {
                this.abortMission();
            }
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
        for (final TableFeature feature : this.features) {
            this.collideFeature(feature);
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
            if (this.mission == Mission.TARGET_PRACTICE) {
                this.practiceHits += 1;
                if (this.practiceHits >= TARGET_PRACTICE_HITS) {
                    this.completeMission();
                }
            }
        }
        bumper.setContacting(true);
    }

    private void collideFeature(final TableFeature feature) {
        final double dx = this.ballX - feature.getX();
        final double dy = this.ballY - feature.getY();
        final double dist = Math.hypot(dx, dy);
        final double minDist = this.ballRadius + feature.getRadius();
        if (dist >= minDist) {
            feature.setContacting(false);
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
        final boolean entered = !feature.isContacting();
        feature.setContacting(true);

        switch (feature.getKind()) {
            case MISSION_TARGET -> {
                this.separateAndBounce(nx, ny, minDist - dist, 0.35, 40.0);
                if (entered) {
                    feature.setLit(true);
                    this.score += 25;
                }
            }
            case LAUNCH_RAMP -> {
                this.separateAndBounce(nx, ny, minDist - dist, 0.25, 80.0);
                if (entered && this.mission == Mission.NONE && this.targetsArmed()) {
                    this.startTargetPractice();
                }
            }
            case CENTER_MEDAL -> {
                this.separateAndBounce(nx, ny, minDist - dist, 0.5, 60.0);
                if (entered) {
                    this.score += 10;
                }
            }
            case REENTRY_LANE -> {
                this.separateAndBounce(nx, ny, minDist - dist, 0.4, 90.0);
                if (entered) {
                    this.score += 15;
                }
            }
            case HYPERSPACE -> {
                if (entered) {
                    this.score += 500;
                    this.ballX = 100.0;
                    this.ballY = 210.0;
                    this.ballVx = -40.0;
                    this.ballVy = 280.0;
                    feature.setContacting(true);
                }
            }
            case WORMHOLE -> {
                if (entered) {
                    this.teleportWormhole(feature);
                }
            }
        }
    }

    private void teleportWormhole(final TableFeature from) {
        final List<TableFeature> holes = this.getWormholes();
        TableFeature dest = holes.getFirst();
        for (int i = 0; i < holes.size(); i++) {
            if (holes.get(i) == from) {
                dest = holes.get((i + 1) % holes.size());
                break;
            }
        }
        this.score += 75;
        this.ballX = dest.getX();
        this.ballY = dest.getY() - dest.getRadius() - this.ballRadius - 1.0;
        this.ballVx = 30.0;
        this.ballVy = -120.0;
        dest.setContacting(true);
        from.setContacting(true);
    }

    private void separateAndBounce(
            final double nx,
            final double ny,
            final double overlap,
            final double restitution,
            final double boost
    ) {
        this.ballX += nx * overlap;
        this.ballY += ny * overlap;
        final double vn = this.ballVx * nx + this.ballVy * ny;
        if (vn < 0.0) {
            this.ballVx -= (1.0 + restitution) * vn * nx;
            this.ballVy -= (1.0 + restitution) * vn * ny;
        }
        this.ballVx += nx * boost;
        this.ballVy += ny * boost;
    }

    private boolean targetsArmed() {
        int lit = 0;
        for (final TableFeature target : this.getMissionTargets()) {
            if (target.isLit()) {
                lit += 1;
            }
        }
        return lit >= MISSION_TARGETS_TO_ARM;
    }

    private void startTargetPractice() {
        this.mission = Mission.TARGET_PRACTICE;
        this.statusLine = STATUS_TARGET_PRACTICE;
        this.fuel = FUEL_MAX;
        this.practiceHits = 0;
        for (final TableFeature target : this.features) {
            if (target.getKind() == TableFeature.Kind.MISSION_TARGET) {
                target.setLit(false);
            }
        }
        this.score += 200;
    }

    private void completeMission() {
        this.mission = Mission.NONE;
        this.fuel = 0.0;
        this.practiceHits = 0;
        this.missionsCompleted += 1;
        if (this.rank == Rank.CADET) {
            this.rank = Rank.ENSIGN;
        } else if (this.rank == Rank.ENSIGN) {
            this.rank = Rank.LIEUTENANT;
        }
        this.statusLine = STATUS_MISSION_COMPLETE;
        this.score += 1000;
    }

    private void abortMission() {
        this.mission = Mission.NONE;
        this.fuel = 0.0;
        this.practiceHits = 0;
        this.statusLine = STATUS_OUT_OF_FUEL;
        for (final TableFeature target : this.features) {
            if (target.getKind() == TableFeature.Kind.MISSION_TARGET) {
                target.setLit(false);
            }
        }
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
        this.walls.add(new Wall(14.0, 205.0, 32.0, 330.0, 0.35));
        this.walls.add(new Wall(48.0, 205.0, 58.0, 330.0, 0.35));
        this.walls.add(new Wall(8.0, 78.0, 28.0, 118.0, 0.55));
        this.walls.add(new Wall(LAUNCH_LANE_LEFT - 8.0, 78.0, LAUNCH_LANE_LEFT - 28.0, 118.0, 0.55));

        this.bumpers.add(new Bumper(72.0, 318.0, 11.0));
        this.bumpers.add(new Bumper(100.0, 348.0, 11.0));
        this.bumpers.add(new Bumper(128.0, 318.0, 11.0));
        this.bumpers.add(new Bumper(100.0, 288.0, 11.0));

        this.features.add(new TableFeature(TableFeature.Kind.LAUNCH_RAMP, 34.0, 268.0, 12.0));
        this.features.add(new TableFeature(TableFeature.Kind.REENTRY_LANE, 24.0, 92.0, 8.0));
        this.features.add(new TableFeature(TableFeature.Kind.REENTRY_LANE, 154.0, 92.0, 8.0));
        this.features.add(new TableFeature(TableFeature.Kind.CENTER_MEDAL, 100.0, 172.0, 16.0));
        this.features.add(new TableFeature(TableFeature.Kind.WORMHOLE, 55.0, 222.0, 8.0));
        this.features.add(new TableFeature(TableFeature.Kind.WORMHOLE, 100.0, 214.0, 8.0));
        this.features.add(new TableFeature(TableFeature.Kind.WORMHOLE, 145.0, 222.0, 8.0));
        this.features.add(new TableFeature(TableFeature.Kind.HYPERSPACE, 158.0, 360.0, 9.0));
        this.features.add(new TableFeature(TableFeature.Kind.MISSION_TARGET, 18.0, 148.0, 5.5));
        this.features.add(new TableFeature(TableFeature.Kind.MISSION_TARGET, 18.0, 168.0, 5.5));
        this.features.add(new TableFeature(TableFeature.Kind.MISSION_TARGET, 18.0, 188.0, 5.5));
    }

    private void clearContacts() {
        for (final Bumper bumper : this.bumpers) {
            bumper.setContacting(false);
        }
        for (final TableFeature feature : this.features) {
            feature.setContacting(false);
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
