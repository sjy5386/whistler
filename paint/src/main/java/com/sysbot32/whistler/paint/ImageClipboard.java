package com.sysbot32.whistler.paint;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * System clipboard helpers for image cut/copy/paste (classic Paint interop).
 */
public final class ImageClipboard {
    private ImageClipboard() {
    }

    public static void copyImage(final BufferedImage image) {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(asTransferable(image), null);
    }

    public static BufferedImage pasteImage() {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        return fromTransferable(clipboard.getContents(null));
    }

    public static boolean hasImage() {
        try {
            final Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            return Objects.nonNull(contents) && contents.isDataFlavorSupported(DataFlavor.imageFlavor);
        } catch (final IllegalStateException ex) {
            return false;
        }
    }

    public static Transferable asTransferable(final BufferedImage image) {
        return new ImageTransferable(BitmapOps.copyOf(Objects.requireNonNull(image)));
    }

    public static BufferedImage fromTransferable(final Transferable transferable) {
        if (Objects.isNull(transferable) || !transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return null;
        }
        try {
            final Object data = transferable.getTransferData(DataFlavor.imageFlavor);
            if (data instanceof BufferedImage buffered) {
                return toArgb(buffered);
            }
            if (data instanceof Image image) {
                return toArgb(image);
            }
            return null;
        } catch (final UnsupportedFlavorException | IOException | IllegalStateException ex) {
            return null;
        }
    }

    private static BufferedImage toArgb(final Image source) {
        final int w = Math.max(1, source.getWidth(null));
        final int h = Math.max(1, source.getHeight(null));
        final BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final java.awt.Graphics2D g = argb.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return argb;
    }

    private static final class ImageTransferable implements Transferable {
        private final BufferedImage image;

        private ImageTransferable(final BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(final DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return this.image;
        }
    }
}
