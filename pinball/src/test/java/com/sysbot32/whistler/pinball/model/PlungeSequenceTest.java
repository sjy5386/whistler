package com.sysbot32.whistler.pinball.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlungeSequenceTest {
    @Test
    void fullPlungeSequenceMovesThenFallsTowardDrainTwice() {
        final String shot1 = PlungeSequence.run(1.0, 1800, 1.0 / 60.0);
        final String shot2 = PlungeSequence.run(1.0, 1800, 1.0 / 60.0);
        assertShotMovesAndFalls(shot1);
        assertShotMovesAndFalls(shot2);
        assertEquals(shot1, shot2, "identical pull on a fresh world must be deterministic");
    }

    private static void assertShotMovesAndFalls(final String log) {
        final String[] lines = log.strip().split("\n");
        assertTrue(lines.length > 8, "too few samples: " + lines.length);

        double spawnX = Double.NaN;
        double spawnY = Double.NaN;
        double maxY = Double.NEGATIVE_INFINITY;
        int maxYTick = 0;
        boolean moved = false;
        boolean fellAfterApex = false;
        int samples = 0;

        for (final String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            final String[] p = line.split(" ");
            final int tick = Integer.parseInt(p[0]);
            final double x = Double.parseDouble(p[1]);
            final double y = Double.parseDouble(p[2]);
            samples++;
            if (samples == 1) {
                spawnX = x;
                spawnY = y;
            } else if (Math.hypot(x - spawnX, y - spawnY) > 4.0) {
                moved = true;
            }
            if (y > maxY) {
                maxY = y;
                maxYTick = tick;
            }
        }
        assertTrue(moved, "ball stuck at spawn " + spawnX + "," + spawnY);
        assertTrue(maxY > spawnY + 20.0, "launch did not travel upfield maxY=" + maxY + " spawnY=" + spawnY);

        double yAtApex = Double.NaN;
        double yLater = Double.NaN;
        for (final String line : lines) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            final String[] p = line.split(" ");
            final int tick = Integer.parseInt(p[0]);
            final double y = Double.parseDouble(p[2]);
            if (tick == maxYTick) {
                yAtApex = y;
            }
            if (tick == maxYTick + 20) {
                yLater = y;
            }
        }
        if (Double.isNaN(yLater)) {
            // Sequence ended (drain) before +20 ticks; compare last y to apex.
            for (int i = lines.length - 1; i >= 0; i--) {
                if (lines[i].startsWith("#") || lines[i].isBlank()) {
                    continue;
                }
                yLater = Double.parseDouble(lines[i].split(" ")[2]);
                break;
            }
        }
        assertTrue(yLater < yAtApex, "gravity after launch: later y=" + yLater + " apex=" + yAtApex);
        fellAfterApex = yLater < yAtApex;
        assertTrue(fellAfterApex);
    }
}
