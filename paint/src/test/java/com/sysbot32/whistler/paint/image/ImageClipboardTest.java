package com.sysbot32.whistler.paint.image;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageClipboardTest {
    @Test
    void transferableRoundTripPreservesPixels() {
        final BufferedImage source = BitmapOps.createBlank(12, 8);
        source.setRGB(3, 4, Color.MAGENTA.getRGB());
        final Transferable transferable = ImageClipboard.asTransferable(source);
        final BufferedImage restored = ImageClipboard.fromTransferable(transferable);
        assertNotNull(restored);
        assertEquals(12, restored.getWidth());
        assertEquals(8, restored.getHeight());
        assertEquals(Color.MAGENTA.getRGB(), restored.getRGB(3, 4));
    }

    @Test
    void fromTransferableReturnsNullForEmpty() {
        assertNull(ImageClipboard.fromTransferable(null));
    }
}
