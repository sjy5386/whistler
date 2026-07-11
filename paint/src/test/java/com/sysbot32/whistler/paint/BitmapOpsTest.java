package com.sysbot32.whistler.paint;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitmapOpsTest {
    @Test
    void pencilStrokeWritesForegroundPixels() {
        final BufferedImage image = BitmapOps.createBlank(20, 20);
        BitmapOps.drawPencil(image, 0, 5, 10, 5, Color.RED);
        assertEquals(Color.RED.getRGB(), image.getRGB(0, 5));
        assertEquals(Color.RED.getRGB(), image.getRGB(5, 5));
        assertEquals(Color.RED.getRGB(), image.getRGB(10, 5));
        assertEquals(Color.WHITE.getRGB(), image.getRGB(5, 6));
    }

    @Test
    void brushUsesChosenColorAndSize() {
        final BufferedImage image = BitmapOps.createBlank(40, 40);
        BitmapOps.drawBrush(image, 20, 20, 20, 20, Color.BLUE, 6, BrushShape.SQUARE);
        assertEquals(Color.BLUE.getRGB(), image.getRGB(20, 20));
        assertEquals(Color.BLUE.getRGB(), image.getRGB(22, 20));
    }

    @Test
    void eraserPaintsBackgroundAndColorEraserReplacesMatch() {
        final BufferedImage image = BitmapOps.createBlank(30, 30);
        BitmapOps.drawBrush(image, 10, 10, 20, 10, Color.GREEN, 4, BrushShape.SQUARE);
        BitmapOps.drawEraser(image, 10, 10, 20, 10, Color.WHITE, 6);
        assertEquals(Color.WHITE.getRGB(), image.getRGB(15, 10));

        BitmapOps.drawBrush(image, 5, 20, 15, 20, Color.RED, 3, BrushShape.SQUARE);
        BitmapOps.drawColorEraser(image, 5, 20, 15, 20, Color.RED, Color.YELLOW, 5);
        assertEquals(Color.YELLOW.getRGB(), image.getRGB(10, 20));
    }

    @Test
    void floodFillFillsConnectedRegion() {
        final BufferedImage image = BitmapOps.createBlank(10, 10);
        // border box in black
        BitmapOps.drawRectangle(image, 1, 1, 8, 8, Color.BLACK, Color.WHITE, FillStyle.OUTLINE, 1);
        BitmapOps.floodFill(image, 4, 4, Color.CYAN);
        assertEquals(Color.CYAN.getRGB(), image.getRGB(4, 4));
        assertEquals(Color.CYAN.getRGB(), image.getRGB(5, 5));
        // outside remains white
        assertEquals(Color.WHITE.getRGB(), image.getRGB(0, 0));
    }

    @Test
    void pickColorReadsPixel() {
        final BufferedImage image = BitmapOps.createBlank(5, 5);
        image.setRGB(2, 3, Color.MAGENTA.getRGB());
        assertEquals(Color.MAGENTA.getRGB(), BitmapOps.pickColor(image, 2, 3).getRGB());
    }

    @Test
    void shapesRasterizeWithFillStyles() {
        final BufferedImage outline = BitmapOps.createBlank(40, 40);
        BitmapOps.drawRectangle(outline, 5, 5, 25, 25, Color.BLACK, Color.RED, FillStyle.OUTLINE, 1);
        assertEquals(Color.BLACK.getRGB(), outline.getRGB(5, 5));
        assertEquals(Color.WHITE.getRGB(), outline.getRGB(15, 15));

        final BufferedImage filled = BitmapOps.createBlank(40, 40);
        BitmapOps.drawEllipse(filled, 5, 5, 30, 25, Color.BLUE, Color.YELLOW, FillStyle.FILL, 1);
        assertEquals(Color.BLUE.getRGB(), filled.getRGB(17, 15));

        final BufferedImage both = BitmapOps.createBlank(40, 40);
        BitmapOps.drawRoundedRectangle(both, 2, 2, 30, 30, Color.BLACK, Color.ORANGE, FillStyle.OUTLINE_FILL, 1);
        assertEquals(Color.ORANGE.getRGB(), both.getRGB(15, 15));
    }

    @Test
    void lineAndCurveWritePixels() {
        final BufferedImage image = BitmapOps.createBlank(50, 50);
        BitmapOps.drawLine(image, 0, 0, 20, 0, Color.BLACK, 2);
        assertEquals(Color.BLACK.getRGB(), image.getRGB(10, 0));

        BitmapOps.drawCurve(image, 0, 40, 40, 40, 10, 10, 30, 10, Color.RED, 2);
        // curve bows upward — sample near control region
        boolean found = false;
        for (int y = 10; y <= 40; y++) {
            for (int x = 0; x <= 40; x++) {
                if (image.getRGB(x, y) == Color.RED.getRGB()) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    void polygonAndTextRasterize() {
        final BufferedImage image = BitmapOps.createBlank(80, 80);
        BitmapOps.drawPolygon(
                image,
                new int[]{10, 40, 10},
                new int[]{10, 20, 40},
                3,
                Color.BLACK,
                Color.GREEN,
                FillStyle.OUTLINE_FILL,
                1
        );
        assertEquals(Color.GREEN.getRGB(), image.getRGB(15, 20));

        BitmapOps.drawText(image, "Hi", 50, 40, new Font(Font.MONOSPACED, Font.PLAIN, 12), Color.BLUE, Color.WHITE, true);
        boolean textPixel = false;
        for (int y = 25; y < 45; y++) {
            for (int x = 50; x < 75; x++) {
                if (image.getRGB(x, y) == Color.BLUE.getRGB()) {
                    textPixel = true;
                    break;
                }
            }
        }
        assertTrue(textPixel);
    }

    @Test
    void invertFlipRotateStretchChangePixels() {
        final BufferedImage image = BitmapOps.createBlank(4, 2);
        image.setRGB(0, 0, Color.BLACK.getRGB());
        image.setRGB(3, 1, Color.WHITE.getRGB());

        BitmapOps.invertColors(image);
        assertEquals(Color.WHITE.getRGB(), image.getRGB(0, 0) | 0xff000000);
        // white inverted is black
        assertEquals(0xff000000, image.getRGB(3, 1));

        final BufferedImage flipped = BitmapOps.flipHorizontal(image);
        assertEquals(image.getRGB(0, 0), flipped.getRGB(3, 0));

        final BufferedImage rotated = BitmapOps.rotate90Clockwise(image);
        assertEquals(2, rotated.getWidth());
        assertEquals(4, rotated.getHeight());

        final BufferedImage stretched = BitmapOps.stretch(image, 200, 100);
        assertEquals(8, stretched.getWidth());
        assertEquals(2, stretched.getHeight());
    }

    @Test
    void selectionExtractAndCompositeRespectTransparentKey() {
        final BufferedImage source = BitmapOps.createBlank(20, 20);
        BitmapOps.fillRect(source, 5, 5, 5, 5, Color.RED);
        final BufferedImage floating = BitmapOps.extractRect(source, 5, 5, 5, 5);
        assertEquals(Color.RED.getRGB(), floating.getRGB(0, 0));

        final BufferedImage dest = BitmapOps.createBlank(20, 20);
        BitmapOps.composite(dest, floating, 10, 10, true, Color.WHITE);
        assertEquals(Color.RED.getRGB(), dest.getRGB(10, 10));

        final BufferedImage withKey = BitmapOps.createBlank(3, 3);
        withKey.setRGB(1, 1, Color.BLUE.getRGB());
        // leave (0,0) white as key
        final BufferedImage dest2 = BitmapOps.createBlank(10, 10);
        dest2.setRGB(0, 0, Color.GREEN.getRGB());
        BitmapOps.composite(dest2, withKey, 0, 0, false, Color.WHITE);
        assertEquals(Color.GREEN.getRGB(), dest2.getRGB(0, 0));
        assertEquals(Color.BLUE.getRGB(), dest2.getRGB(1, 1));
    }

    @Test
    void freeFormExtractKeepsInterior() {
        final BufferedImage source = BitmapOps.createBlank(20, 20);
        BitmapOps.fillRect(source, 0, 0, 20, 20, Color.YELLOW);
        final Polygon poly = new Polygon(new int[]{2, 10, 2}, new int[]{2, 6, 12}, 3);
        final Object[] extracted = BitmapOps.extractFreeForm(source, poly);
        final BufferedImage floating = (BufferedImage) extracted[0];
        assertTrue(floating.getWidth() >= 1);
        assertTrue(floating.getHeight() >= 1);
        // at least one non-transparent yellow-ish pixel
        boolean any = false;
        for (int y = 0; y < floating.getHeight(); y++) {
            for (int x = 0; x < floating.getWidth(); x++) {
                if ((floating.getRGB(x, y) >>> 24) != 0) {
                    any = true;
                    break;
                }
            }
        }
        assertTrue(any);
    }

    @Test
    void constrainHelpersSnapGeometry() {
        final int[] line = BitmapOps.constrainLine(0, 0, 10, 1);
        assertEquals(0, line[1]); // near-horizontal snaps to axis

        final int[] square = BitmapOps.constrainSquare(0, 0, 10, 4);
        assertEquals(10, Math.abs(square[0]));
        assertEquals(10, Math.abs(square[1]));
    }

    @Test
    void airbrushWritesSomePixels() {
        final BufferedImage image = BitmapOps.createBlank(40, 40);
        BitmapOps.drawAirbrush(image, 20, 20, Color.RED, 10, 80, 42L);
        int painted = 0;
        for (int y = 10; y < 30; y++) {
            for (int x = 10; x < 30; x++) {
                if (image.getRGB(x, y) == Color.RED.getRGB()) {
                    painted++;
                }
            }
        }
        assertTrue(painted > 0);
        assertNotEquals(20 * 20, painted); // not a solid fill
    }
}
