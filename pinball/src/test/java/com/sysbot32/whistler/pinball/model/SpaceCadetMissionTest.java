package com.sysbot32.whistler.pinball.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaceCadetMissionTest {
    @Test
    void startStateIsCadetAwaitingDeployment() {
        final PinballWorld world = new PinballWorld();
        assertEquals(Rank.CADET, world.getRank());
        assertEquals(Mission.NONE, world.getMission());
        assertFalse(world.isMissionActive());
        assertEquals(PinballWorld.STATUS_AWAITING, world.getStatusLine());
        assertEquals(0, world.getMissionsCompleted());
        assertEquals(1, world.getBallNumber());
        assertEquals(0.0, world.getFuel());
    }

    @Test
    void missionTargetsThenLaunchRampStartsTargetPractice() {
        final PinballWorld world = new PinballWorld();
        this.hitAllMissionTargets(world);
        assertEquals(Mission.NONE, world.getMission());
        this.hit(world, world.getLaunchRamp());
        assertEquals(Mission.TARGET_PRACTICE, world.getMission());
        assertTrue(world.isMissionActive());
        assertEquals(PinballWorld.STATUS_TARGET_PRACTICE, world.getStatusLine());
        assertEquals(PinballWorld.FUEL_MAX, world.getFuel(), 1e-6);
        assertEquals(Rank.CADET, world.getRank());
    }

    @Test
    void launchRampWithoutArmedTargetsDoesNotStartAMission() {
        final PinballWorld world = new PinballWorld();
        this.hit(world, world.getLaunchRamp());
        assertEquals(Mission.NONE, world.getMission());
        assertEquals(PinballWorld.STATUS_AWAITING, world.getStatusLine());
    }

    @Test
    void enoughAttackBumperHitsCompleteTargetPracticeAndPromote() {
        final PinballWorld world = new PinballWorld();
        this.hitAllMissionTargets(world);
        this.hit(world, world.getLaunchRamp());
        assertEquals(Mission.TARGET_PRACTICE, world.getMission());

        for (int i = 0; i < PinballWorld.TARGET_PRACTICE_HITS; i++) {
            final Bumper bumper = world.getBumpers().get(i % world.getBumpers().size());
            world.placeBall(bumper.getX() + (bumper.getRadius() + PinballWorld.BALL_RADIUS) * 0.4, bumper.getY());
            world.tick(1.0 / 60.0);
        }

        assertEquals(Mission.NONE, world.getMission());
        assertFalse(world.isMissionActive());
        assertEquals(1, world.getMissionsCompleted());
        assertEquals(Rank.ENSIGN, world.getRank());
        assertEquals(PinballWorld.STATUS_MISSION_COMPLETE, world.getStatusLine());
    }

    @Test
    void fuelDepletesAndAbortsAnActiveMission() {
        final PinballWorld world = new PinballWorld();
        this.hitAllMissionTargets(world);
        this.hit(world, world.getLaunchRamp());
        assertTrue(world.isMissionActive());
        world.placeBall(60.0, 130.0);
        world.setBallVelocity(0.0, 0.0);
        world.tick(PinballWorld.FUEL_MAX / PinballWorld.FUEL_BURN_PER_SECOND + 0.5);
        assertFalse(world.isMissionActive());
        assertEquals(Mission.NONE, world.getMission());
        assertEquals(PinballWorld.STATUS_OUT_OF_FUEL, world.getStatusLine());
        assertEquals(Rank.CADET, world.getRank());
        assertEquals(0, world.getMissionsCompleted());
        assertEquals(0.0, world.getFuel());
    }

    private void hitAllMissionTargets(final PinballWorld world) {
        for (final TableFeature target : world.getMissionTargets()) {
            this.hit(world, target);
            assertTrue(target.isLit());
        }
    }

    private void hit(final PinballWorld world, final TableFeature feature) {
        world.placeBall(feature.getX(), feature.getY());
        world.tick(1.0 / 60.0);
    }
}
