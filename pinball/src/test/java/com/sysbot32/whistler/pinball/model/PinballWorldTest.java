package com.sysbot32.whistler.pinball.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinballWorldTest {
    @Test
    void gameStartsWithAtLeastThreeBallsOnThePlunger() {
        final PinballWorld world = new PinballWorld();
        assertEquals(PinballWorld.STARTING_BALLS, world.getBallsRemaining());
        assertTrue(world.getBallsRemaining() >= 3);
        assertEquals(0, world.getScore());
        assertEquals(GameStatus.PLAYING, world.getStatus());
        assertTrue(world.isInPlunger());
        assertFalse(world.isGameOver());
        assertEquals(4, world.getBumpers().size());
    }

    @Test
    void tableExposesSpaceCadetFeatures() {
        final PinballWorld world = new PinballWorld();
        assertEquals(4, world.getBumpers().size());
        assertEquals(TableFeature.Kind.LAUNCH_RAMP, world.getLaunchRamp().getKind());
        assertEquals(2, world.getReentryLanes().size());
        assertEquals(TableFeature.Kind.CENTER_MEDAL, world.getCenterMedal().getKind());
        assertEquals(3, world.getWormholes().size());
        assertEquals(TableFeature.Kind.HYPERSPACE, world.getHyperspace().getKind());
        assertEquals(3, world.getMissionTargets().size());
    }

    @Test
    void gravityPullsBallTowardTheDrain() {
        final PinballWorld world = new PinballWorld();
        world.placeBall(48.0, 180.0);
        world.setBallVelocity(0.0, 0.0);
        final double y0 = world.getBallY();
        world.tick(0.12);
        assertTrue(world.getBallY() < y0, "y after tick=" + world.getBallY() + " start=" + y0);
        assertTrue(world.getBallVy() < 0.0, "vy=" + world.getBallVy());
    }

    @Test
    void plungerReleaseImpartsUpfieldVelocity() {
        final PinballWorld world = new PinballWorld();
        world.setPlungerPull(0.4);
        world.releasePlunger();
        assertFalse(world.isInPlunger());
        assertTrue(world.getBallVy() > 0.0, "launch vy=" + world.getBallVy());
        assertTrue(world.getBallVy() >= PinballWorld.LAUNCH_SPEED_MIN);
    }

    @Test
    void strongerPlungerPullProducesLargerLaunchSpeed() {
        final PinballWorld weak = new PinballWorld();
        weak.setPlungerPull(0.2);
        weak.releasePlunger();
        final double weakSpeed = Math.hypot(weak.getBallVx(), weak.getBallVy());

        final PinballWorld strong = new PinballWorld();
        strong.setPlungerPull(0.85);
        strong.releasePlunger();
        final double strongSpeed = Math.hypot(strong.getBallVx(), strong.getBallVy());

        assertTrue(weak.getBallVy() > 0.0);
        assertTrue(strongSpeed > weakSpeed, "strong=" + strongSpeed + " weak=" + weakSpeed);
    }

    @Test
    void raisedLeftFlipperBatsBallAwayFromTheDrain() {
        final PinballWorld world = new PinballWorld();
        final Flipper left = world.getLeftFlipper();
        final double mid = 0.62;
        final double ang = left.getRestAngle();
        final double x = left.getPivotX() + Math.cos(ang) * left.getLength() * mid;
        final double y = left.getPivotY() + Math.sin(ang) * left.getLength() * mid;
        world.placeBall(x, y);
        world.setBallVelocity(0.0, -25.0);
        final double vy0 = world.getBallVy();
        world.setLeftFlipperRaised(true);
        for (int i = 0; i < 18; i++) {
            world.tick(1.0 / 60.0);
        }
        assertTrue(world.getBallVy() > vy0, "vy=" + world.getBallVy() + " start=" + vy0);
        assertTrue(world.getBallVy() > 0.0, "expected upfield velocity, vy=" + world.getBallVy());
    }

    @Test
    void raisedRightFlipperBatsBallAwayFromTheDrain() {
        final PinballWorld world = new PinballWorld();
        final Flipper right = world.getRightFlipper();
        final double mid = 0.62;
        final double ang = right.getRestAngle();
        final double x = right.getPivotX() + Math.cos(ang) * right.getLength() * mid;
        final double y = right.getPivotY() + Math.sin(ang) * right.getLength() * mid;
        world.placeBall(x, y);
        world.setBallVelocity(0.0, -25.0);
        final double vy0 = world.getBallVy();
        world.setRightFlipperRaised(true);
        for (int i = 0; i < 18; i++) {
            world.tick(1.0 / 60.0);
        }
        assertTrue(world.getBallVy() > vy0, "vy=" + world.getBallVy() + " start=" + vy0);
        assertTrue(world.getBallVy() > 0.0, "expected upfield velocity, vy=" + world.getBallVy());
    }

    @Test
    void bumperContactIncreasesScoreAndBoostsBallAway() {
        final PinballWorld world = new PinballWorld();
        final Bumper bumper = world.getBumpers().getFirst();
        final double overlap = (bumper.getRadius() + PinballWorld.BALL_RADIUS) * 0.45;
        world.placeBall(bumper.getX() + overlap, bumper.getY());
        world.setBallVelocity(-40.0, 0.0);
        final int score0 = world.getScore();
        world.tick(1.0 / 60.0);
        assertTrue(world.getScore() > score0, "score=" + world.getScore());
        assertEquals(score0 + PinballWorld.BUMPER_SCORE, world.getScore());
        final double away = (world.getBallX() - bumper.getX()) * world.getBallVx()
                + (world.getBallY() - bumper.getY()) * world.getBallVy();
        assertTrue(away > 0.0, "away=" + away + " vx=" + world.getBallVx() + " vy=" + world.getBallVy());
    }

    @Test
    void drainDecrementsBallsRemaining() {
        final PinballWorld world = new PinballWorld();
        assertEquals(3, world.getBallsRemaining());
        world.placeBall((PinballWorld.DRAIN_LEFT + PinballWorld.DRAIN_RIGHT) * 0.5, 4.0);
        world.tick(1.0 / 60.0);
        assertEquals(2, world.getBallsRemaining());
        assertFalse(world.isGameOver());
        assertTrue(world.isInPlunger());
    }

    @Test
    void lastBallDrainSetsGameOverAndFreezesScore() {
        final PinballWorld world = new PinballWorld();
        final double drainX = (PinballWorld.DRAIN_LEFT + PinballWorld.DRAIN_RIGHT) * 0.5;
        world.placeBall(drainX, 4.0);
        world.tick(1.0 / 60.0);
        assertEquals(2, world.getBallsRemaining());
        world.placeBall(drainX, 4.0);
        world.tick(1.0 / 60.0);
        assertEquals(1, world.getBallsRemaining());
        world.placeBall(drainX, 4.0);
        world.tick(1.0 / 60.0);
        assertEquals(0, world.getBallsRemaining());
        assertTrue(world.isGameOver());
        assertEquals(GameStatus.GAME_OVER, world.getStatus());

        final int frozen = world.getScore();
        final double x = world.getBallX();
        final double y = world.getBallY();
        world.tick(1.0);
        world.tick(1.0);
        assertEquals(frozen, world.getScore());
        assertEquals(0, world.getBallsRemaining());
        assertEquals(x, world.getBallX());
        assertEquals(y, world.getBallY());
        assertTrue(world.isGameOver());
    }

    @Test
    void wallsKeepAnUnlaunchedInPlayBallOnTheTableUntilItDrains() {
        final PinballWorld world = new PinballWorld();
        world.placeBall(40.0, 200.0);
        world.setBallVelocity(180.0, 40.0);
        boolean stayedOnTable = true;
        for (int i = 0; i < 90; i++) {
            world.tick(1.0 / 60.0);
            if (world.getBallsRemaining() < PinballWorld.STARTING_BALLS) {
                break;
            }
            if (world.getBallX() < -PinballWorld.BALL_RADIUS
                    || world.getBallX() > PinballWorld.TABLE_WIDTH + PinballWorld.BALL_RADIUS
                    || world.getBallY() > PinballWorld.TABLE_HEIGHT + 20.0) {
                stayedOnTable = false;
                break;
            }
        }
        assertTrue(stayedOnTable);
    }
}
