package com.sysbot32.whistler.paint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.Polygon;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaintTest {
    @Test
    void defaultConstructorUsesBlankCanvas() {
        final Paint paint = new Paint();
        assertEquals(Paint.DEFAULT_WIDTH, paint.getWidth());
        assertEquals(Paint.DEFAULT_HEIGHT, paint.getHeight());
        assertFalse(paint.isEdited());
        assertNull(paint.getPath());
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(0, 0));
        assertEquals(PaintTool.PENCIL, paint.getTool());
        assertEquals(Color.BLACK.getRGB(), paint.getForeground().getRGB());
        assertEquals(Color.WHITE.getRGB(), paint.getBackground().getRGB());
    }

    @Test
    void createNewResetsPathAndDirtyFlag() {
        final Paint paint = new Paint(8, 8);
        paint.setEdited(true);
        paint.createNew();
        assertEquals(Paint.DEFAULT_WIDTH, paint.getWidth());
        assertEquals(Paint.DEFAULT_HEIGHT, paint.getHeight());
        assertFalse(paint.isEdited());
        assertNull(paint.getPath());
    }

    @Test
    void createNewWithSizeRejectsNonPositive() {
        final Paint paint = new Paint();
        assertThrows(IllegalArgumentException.class, () -> paint.createNew(0, 10));
        assertThrows(IllegalArgumentException.class, () -> paint.createNew(10, -1));
    }

    @Test
    void clearImageFillsWhiteAndMarksEdited() {
        final Paint paint = new Paint(20, 10);
        paint.getImage().setRGB(5, 5, Color.RED.getRGB());
        paint.clearImage();
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(5, 5));
        assertTrue(paint.isEdited());
    }

    @Test
    void leftAndRightButtonColorsAffectFreehand() {
        final Paint paint = new Paint(30, 30);
        paint.setForeground(Color.RED);
        paint.setBackground(Color.BLUE);
        paint.pushUndo();
        paint.strokePencil(0, 0, 10, 0, false);
        assertEquals(Color.RED.getRGB(), paint.getImage().getRGB(5, 0));
        paint.strokePencil(0, 5, 10, 5, true);
        assertEquals(Color.BLUE.getRGB(), paint.getImage().getRGB(5, 5));
    }

    @Test
    void fillAndPickColorUseButtonSemantics() {
        final Paint paint = new Paint(20, 20);
        paint.setForeground(Color.GREEN);
        paint.fillAt(2, 2, false);
        assertEquals(Color.GREEN.getRGB(), paint.getImage().getRGB(10, 10));

        paint.getImage().setRGB(3, 3, Color.ORANGE.getRGB());
        paint.pickAt(3, 3, false);
        assertEquals(Color.ORANGE.getRGB(), paint.getForeground().getRGB());
        paint.getImage().setRGB(4, 4, Color.PINK.getRGB());
        paint.setTool(PaintTool.PICK_COLOR);
        paint.pickAt(4, 4, true);
        assertEquals(Color.PINK.getRGB(), paint.getBackground().getRGB());
    }

    @Test
    void shapesAndTextMutateDocument() {
        final Paint paint = new Paint(60, 60);
        paint.setForeground(Color.BLACK);
        paint.setBackground(Color.YELLOW);
        paint.setFillStyle(FillStyle.OUTLINE_FILL);
        paint.drawRectangleShape(5, 5, 25, 25, false, false);
        assertEquals(Color.YELLOW.getRGB(), paint.getImage().getRGB(15, 15));

        paint.drawEllipseShape(30, 30, 50, 50, true, false); // square-constrained circle
        paint.drawLineShape(0, 40, 20, 40, false, false);
        assertEquals(Color.BLACK.getRGB(), paint.getImage().getRGB(10, 40));

        paint.drawTextAt("A", 40, 15);
        assertTrue(paint.isEdited());
    }

    @Test
    void rightButtonShapeUsesBackgroundForOutline() {
        final Paint paint = new Paint(40, 40);
        paint.setForeground(Color.RED);
        paint.setBackground(Color.BLUE);
        paint.setFillStyle(FillStyle.OUTLINE);

        paint.drawRectangleShape(5, 5, 20, 20, false, true);
        assertEquals(Color.BLUE.getRGB(), paint.getImage().getRGB(5, 5));
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(12, 12));

        final Paint ellipse = new Paint(40, 40);
        ellipse.setForeground(Color.RED);
        ellipse.setBackground(Color.BLUE);
        ellipse.setFillStyle(FillStyle.OUTLINE);
        ellipse.drawEllipseShape(5, 5, 30, 25, false, true);
        // sample a pixel on the ellipse boundary region
        boolean foundBlue = false;
        for (int y = 5; y <= 25; y++) {
            for (int x = 5; x <= 30; x++) {
                if (ellipse.getImage().getRGB(x, y) == Color.BLUE.getRGB()) {
                    foundBlue = true;
                    break;
                }
            }
        }
        assertTrue(foundBlue);

        final Paint poly = new Paint(40, 40);
        poly.setForeground(Color.RED);
        poly.setBackground(Color.GREEN);
        poly.setFillStyle(FillStyle.OUTLINE);
        poly.drawPolygonShape(new int[]{5, 30, 5}, new int[]{5, 15, 30}, 3, true);
        assertEquals(Color.GREEN.getRGB(), poly.getImage().getRGB(5, 5));

        final Paint rounded = new Paint(40, 40);
        rounded.setForeground(Color.RED);
        rounded.setBackground(Color.MAGENTA);
        rounded.setFillStyle(FillStyle.OUTLINE);
        rounded.drawRoundedRectangleShape(4, 4, 28, 28, false, true);
        assertEquals(Color.MAGENTA.getRGB(), rounded.getImage().getRGB(4, 16));
    }

    @Test
    void rightButtonShapeSwapsFillColors() {
        final Paint paint = new Paint(40, 40);
        paint.setForeground(Color.RED);
        paint.setBackground(Color.YELLOW);
        paint.setFillStyle(FillStyle.OUTLINE_FILL);
        // right: outline=BG (yellow), fill=FG (red)
        paint.drawRectangleShape(5, 5, 25, 25, false, true);
        assertEquals(Color.YELLOW.getRGB(), paint.getImage().getRGB(5, 5));
        assertEquals(Color.RED.getRGB(), paint.getImage().getRGB(15, 15));
    }

    @Test
    void curveAndPolygonRasterize() {
        final Paint paint = new Paint(50, 50);
        paint.setForeground(Color.RED);
        paint.drawCurveShape(0, 40, 40, 40, 10, 5, 30, 5, false);
        boolean curve = false;
        for (int y = 0; y < 50; y++) {
            for (int x = 0; x < 50; x++) {
                if (paint.getImage().getRGB(x, y) == Color.RED.getRGB()) {
                    curve = true;
                    break;
                }
            }
        }
        assertTrue(curve);

        paint.setForeground(Color.BLUE);
        paint.setFillStyle(FillStyle.FILL);
        paint.drawPolygonShape(new int[]{5, 20, 5}, new int[]{5, 10, 20}, 3, false);
        assertEquals(Color.BLUE.getRGB(), paint.getImage().getRGB(8, 10));
    }

    @Test
    void undoRestoresPreviousBitmapAcrossMultipleLevels() {
        final Paint paint = new Paint(10, 10);
        paint.pushUndo();
        paint.strokePencil(0, 0, 5, 0, false);
        paint.pushUndo();
        paint.strokePencil(0, 5, 5, 5, false);
        paint.pushUndo();
        paint.strokePencil(0, 9, 5, 9, false);

        assertEquals(Color.BLACK.getRGB(), paint.getImage().getRGB(2, 9));
        paint.undo();
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(2, 9));
        assertEquals(Color.BLACK.getRGB(), paint.getImage().getRGB(2, 5));
        paint.undo();
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(2, 5));
        paint.undo();
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(2, 0));
        assertFalse(paint.canUndo());
    }

    @Test
    void undoStackCapsAtThree() {
        final Paint paint = new Paint(8, 8);
        for (int i = 0; i < 5; i++) {
            paint.pushUndo();
            paint.strokePencil(i, 0, i, 0, false);
        }
        int undos = 0;
        while (paint.canUndo()) {
            paint.undo();
            undos++;
        }
        assertEquals(Paint.MAX_UNDO, undos);
    }

    @Test
    void rectangularSelectionCutCopyPasteAndClear() {
        final Paint paint = new Paint(30, 30);
        paint.setForeground(Color.RED);
        paint.pushUndo();
        BitmapOps.fillRect(paint.getImage(), 5, 5, 6, 6, Color.RED);

        paint.beginRectangularSelection(5, 5, 11, 11);
        assertTrue(paint.getSelection().isActive());
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(6, 6)); // hole filled with bg

        paint.copySelection();
        assertNotNull(paint.getClipboard());
        paint.moveSelection(15, 15);
        paint.commitSelectionIfAny();
        assertEquals(Color.RED.getRGB(), paint.getImage().getRGB(16, 16));

        paint.beginRectangularSelection(15, 15, 21, 21);
        paint.cutSelection();
        assertFalse(paint.getSelection().isActive());
        paint.pasteClipboard();
        assertTrue(paint.getSelection().isActive());
        paint.clearSelection();
        assertFalse(paint.getSelection().isActive());
    }

    @Test
    void freeFormSelectionExtractsRegion() {
        final Paint paint = new Paint(30, 30);
        BitmapOps.fillRect(paint.getImage(), 0, 0, 30, 30, Color.CYAN);
        final Polygon poly = new Polygon(new int[]{5, 15, 5}, new int[]{5, 10, 20}, 3);
        paint.beginFreeFormSelection(poly);
        assertTrue(paint.getSelection().isActive());
        assertTrue(paint.getSelection().getWidth() >= 1);
    }

    @Test
    void drawOpaqueFalseSkipsBackgroundKeyOnCommit() {
        final Paint paint = new Paint(20, 20);
        paint.setBackground(Color.WHITE);
        paint.setDrawOpaque(false);
        BitmapOps.fillRect(paint.getImage(), 0, 0, 20, 20, Color.GREEN);
        paint.beginRectangularSelection(0, 0, 5, 5);
        // floating has green; put white into floating corner via clipboard path
        paint.getSelection().getFloating().setRGB(0, 0, Color.WHITE.getRGB());
        paint.getSelection().getFloating().setRGB(1, 1, Color.BLUE.getRGB());
        paint.moveSelection(10, 10);
        paint.commitSelectionIfAny();
        assertEquals(Color.GREEN.getRGB(), paint.getImage().getRGB(10, 10)); // white key skipped
        assertEquals(Color.BLUE.getRGB(), paint.getImage().getRGB(11, 11));
    }

    @Test
    void imageOpsChangeDimensionsAndPixels() {
        final Paint paint = new Paint(10, 8);
        paint.getImage().setRGB(0, 0, Color.RED.getRGB());
        paint.flipHorizontal();
        assertEquals(Color.RED.getRGB(), paint.getImage().getRGB(9, 0));

        paint.attributes(20, 12);
        assertEquals(20, paint.getWidth());
        assertEquals(12, paint.getHeight());

        paint.invertColors();
        paint.stretch(50, 50);
        assertEquals(10, paint.getWidth());
        assertEquals(6, paint.getHeight());
    }

    @Test
    void attributesResizeKeepsContentAndFillsNewAreaWhite() {
        final Paint paint = new Paint(8, 6);
        paint.getImage().setRGB(1, 1, Color.BLUE.getRGB());
        paint.attributes(12, 10);
        assertEquals(12, paint.getWidth());
        assertEquals(10, paint.getHeight());
        assertEquals(Color.BLUE.getRGB(), paint.getImage().getRGB(1, 1));
        assertEquals(Color.WHITE.getRGB(), paint.getImage().getRGB(11, 9));

        paint.attributes(4, 4);
        assertEquals(4, paint.getWidth());
        assertEquals(4, paint.getHeight());
        assertEquals(Color.BLUE.getRGB(), paint.getImage().getRGB(1, 1));
    }

    @Test
    void zoomLevelsAcceptedAndInvalidRejected() {
        final Paint paint = new Paint();
        paint.setZoom(2);
        assertEquals(2, paint.getZoom());
        paint.setZoom(8);
        assertEquals(8, paint.getZoom());
        assertThrows(IllegalArgumentException.class, () -> paint.setZoom(3));
    }

    @Test
    void saveAndOpenRoundTrip(@TempDir final Path dir) throws IOException {
        final Path file = dir.resolve("canvas.png");
        final Paint paint = new Paint(16, 12);
        paint.getImage().setRGB(3, 4, Color.BLUE.getRGB());
        paint.setEdited(true);
        paint.save(file);

        assertFalse(paint.isEdited());
        assertEquals(file, paint.getPath());

        final Paint loaded = new Paint(file);
        assertEquals(16, loaded.getWidth());
        assertEquals(12, loaded.getHeight());
        assertEquals(Color.BLUE.getRGB(), loaded.getImage().getRGB(3, 4));
        assertFalse(loaded.isEdited());
        assertEquals(file, loaded.getPath());
    }

    @Test
    void saveSupportsClassicFormats(@TempDir final Path dir) throws IOException {
        final Paint paint = new Paint(8, 8);
        paint.getImage().setRGB(2, 2, Color.RED.getRGB());
        for (final String name : new String[]{"a.png", "b.jpg", "c.bmp", "d.gif"}) {
            final Path file = dir.resolve(name);
            paint.save(file);
            final Paint loaded = new Paint(file);
            assertEquals(8, loaded.getWidth());
            assertEquals(8, loaded.getHeight());
            // lossy/indexed formats may quantize; PNG/BMP should keep red closely
            if (name.endsWith(".png") || name.endsWith(".bmp")) {
                assertEquals(Color.RED.getRGB(), loaded.getImage().getRGB(2, 2));
            }
        }
        // TIFF if writer present
        final Path tiff = dir.resolve("e.tiff");
        try {
            paint.save(tiff);
            final Paint loaded = new Paint(tiff);
            assertEquals(8, loaded.getWidth());
        } catch (final IOException ex) {
            // platform may lack TIFF writer — still acceptable per plan
            assertTrue(ex.getMessage().contains("tiff") || ex.getMessage().contains("writer")
                    || ex.getMessage().contains("No writer"));
        }
    }

    @Test
    void allSixteenToolsAreSelectable() {
        final Paint paint = new Paint();
        assertEquals(16, PaintTool.values().length);
        for (final PaintTool tool : PaintTool.values()) {
            paint.setTool(tool);
            assertEquals(tool, paint.getTool());
            assertNotNull(tool.displayName());
        }
    }
}
