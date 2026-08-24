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
        assertTrue(nearRight.x < view.getWidth() * 0.75, "table should sit in the left panel");
        final int h = view.getHeight();
        assertTrue(farLeft.y < h * 0.12, "table top should sit near the top, farY=" + farLeft.y);
        assertTrue(nearLeft.y > h * 0.82, "drain should sit near the bottom, nearY=" + nearLeft.y);
    }

    @Test
    void paintFillsLeftPlayfieldAndRightBackglassPanel() {
        final PinballWorld world = new PinballWorld();
        final PinballView view = new PinballView(world);
        view.setSize(PinballView.PREFERRED_WIDTH, PinballView.PREFERRED_HEIGHT);
        final BufferedImage image = new BufferedImage(
                PinballView.PREFERRED_WIDTH,
                PinballView.PREFERRED_HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );
        view.paint(image.getGraphics());

        final int split = view.getTablePanelWidth();
        int leftFilled = 0;
        int leftSamples = 0;
        int rightPanel = 0;
        int rightSamples = 0;
        int rightStarfield = 0;
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                final int rgb = image.getRGB(x, y) & 0xFFFFFF;
                final int r = (rgb >> 16) & 0xFF;
                final int g = (rgb >> 8) & 0xFF;
                final int b = rgb & 0xFF;
                final boolean darkBlueStarfield = r < 30 && g < 35 && b > 50 && b < 95 && b > r + 12 && b > g + 12;
                if (x < split) {
                    leftSamples++;
                    if (rgb != 0 && !(r < 20 && g < 20 && b < 30)) {
                        leftFilled++;
                    }
                }
                if (x > image.getWidth() * 2 / 3) {
                    rightSamples++;
                    if (!darkBlueStarfield && rgb != 0) {
                        rightPanel++;
                    }
                    if (darkBlueStarfield) {
                        rightStarfield++;
                    }
                }
            }
        }
        assertTrue(leftFilled > leftSamples * 0.25, "left playfield sparse filled=" + leftFilled + "/" + leftSamples);
        assertTrue(rightPanel > rightSamples * 0.40, "right third not a panel panel=" + rightPanel + "/" + rightSamples);
        assertTrue(rightStarfield < rightSamples * 0.20, "right third still starfield stars=" + rightStarfield + "/" + rightSamples);
        assertEquals(PinballWorld.STARTING_BALLS, world.getBallsRemaining());
        assertEquals(PinballWorld.STATUS_AWAITING, world.getStatusLine());
    }

    @Test
    void frameTitleNamesSpaceCadet() {
        assertEquals("3D Pinball: Space Cadet", PinballFrame.TITLE);
        assertTrue(PinballFrame.TITLE.contains("Space Cadet"));
    }
}
