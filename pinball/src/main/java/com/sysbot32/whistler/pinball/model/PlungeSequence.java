package com.sysbot32.whistler.pinball.model;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Drives a full plunger launch on the shipped simulation and records the ball
 * until the first drain or a tick cap.
 */
public final class PlungeSequence {
    private PlungeSequence() {
    }

    public static String run(final double pull, final int maxTicks, final double dt) {
        final PinballWorld world = new PinballWorld();
        world.setPlungerPull(pull);
        world.releasePlunger();
        final StringBuilder sb = new StringBuilder();
        sb.append("# tick x y vx vy score balls gameOver inPlunger\n");
        append(sb, 0, world);
        for (int i = 1; i <= maxTicks; i++) {
            world.tick(dt);
            append(sb, i, world);
            if (world.isGameOver() || world.getBallsRemaining() < PinballWorld.STARTING_BALLS) {
                break;
            }
        }
        return sb.toString();
    }

    public static void main(final String[] args) throws Exception {
        final String text = run(1.0, 1800, 1.0 / 60.0);
        if (args.length == 0) {
            System.out.print(text);
        } else {
            Files.writeString(Path.of(args[0]), text);
        }
    }

    private static void append(final StringBuilder sb, final int tick, final PinballWorld world) {
        sb.append(tick)
                .append(' ').append(fmt(world.getBallX()))
                .append(' ').append(fmt(world.getBallY()))
                .append(' ').append(fmt(world.getBallVx()))
                .append(' ').append(fmt(world.getBallVy()))
                .append(' ').append(world.getScore())
                .append(' ').append(world.getBallsRemaining())
                .append(' ').append(world.isGameOver())
                .append(' ').append(world.isInPlunger())
                .append('\n');
    }

    private static String fmt(final double value) {
        return String.format(java.util.Locale.ROOT, "%.5f", value);
    }
}
