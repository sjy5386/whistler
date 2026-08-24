package com.sysbot32.whistler.pinball.ui;

import com.sysbot32.whistler.pinball.model.PinballWorld;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinballViewTest {
    @Test
    void playfieldIsForeshortenedInPerspectiveNotTopDown() {
        final PinballView view = new PinballView(new PinballWorld());
        view.setSize(PinballView.PREFERRED_WIDTH, PinballView.PREFERRED_HEIGHT);
        final Point2D.Double nearLeft = view.project(0.0, 0.0, 0.0);
        final Point2D.Double nearRight = view.project(PinballWorld.TABLE_WIDTH, 0.0, 0.0);
        final Point2D.Double farLeft = view.project(0.0, PinballWorld.TABLE_HEIGHT, 0.0);
        final Point2D.Double farRight = view.project(PinballWorld.TABLE_WIDTH, PinballWorld.TABLE_HEIGHT, 0.0);
        final double nearWidth = nearRight.x - nearLeft.x;
        final double farWidth = farRight.x - farLeft.x;
        assertTrue(nearWidth > 40.0, "near width=" + nearWidth);
        assertTrue(farWidth < nearWidth * 0.85, "far=" + farWidth + " near=" + nearWidth);
        assertTrue(farLeft.y < nearLeft.y, "far should be higher on screen farY=" + farLeft.y + " nearY=" + nearLeft.y);
    }

    @Test
    void paintRendersScoreHudAndPlayfieldPixels() {
        final PinballWorld world = new PinballWorld();
        final PinballView view = new PinballView(world);
        view.setSize(PinballView.PREFERRED_WIDTH, PinballView.PREFERRED_HEIGHT);
        final BufferedImage image = new BufferedImage(
                PinballView.PREFERRED_WIDTH,
                PinballView.PREFERRED_HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );
        view.paint(image.getGraphics());

        int nonBlack = 0;
        int yellowIsh = 0;
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                final int rgb = image.getRGB(x, y) & 0xFFFFFF;
                if (rgb != 0) {
                    nonBlack++;
                }
                final int r = (rgb >> 16) & 0xFF;
                final int g = (rgb >> 8) & 0xFF;
                final int b = rgb & 0xFF;
                if (r > 180 && g > 140 && b < 140) {
                    yellowIsh++;
                }
            }
        }
        assertTrue(nonBlack > 200, "playfield should not be empty, nonBlack=" + nonBlack);
        assertTrue(yellowIsh > 5, "gold rails / HUD expected, yellowIsh=" + yellowIsh);
        assertEquals(PinballWorld.STARTING_BALLS, world.getBallsRemaining());
    }

    @Test
    void frameTitleIs3dPinball() {
        assertEquals("3D Pinball", PinballFrame.TITLE);
    }
}
