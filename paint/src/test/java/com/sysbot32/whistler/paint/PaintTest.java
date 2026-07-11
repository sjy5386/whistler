package com.sysbot32.whistler.paint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
