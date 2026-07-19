package com.sysbot32.whistler.paint.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Objects;

/**
 * Printable wrapper + layout helpers for the Paint document bitmap.
 */
public final class ImagePrintSupport implements Printable {
    private final BufferedImage image;

    public ImagePrintSupport(final BufferedImage image) {
        this.image = Objects.requireNonNull(image);
    }

    /**
     * Fit {@code imageW×imageH} into the page imageable area, preserving aspect ratio, top-left aligned.
     */
    public static Rectangle fitToPage(
            final PageFormat pageFormat,
            final int imageWidth,
            final int imageHeight
    ) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalArgumentException("Image size must be positive");
        }
        final double areaX = pageFormat.getImageableX();
        final double areaY = pageFormat.getImageableY();
        final double areaW = pageFormat.getImageableWidth();
        final double areaH = pageFormat.getImageableHeight();
        final double scale = Math.min(areaW / imageWidth, areaH / imageHeight);
        final int drawW = Math.max(1, (int) Math.round(imageWidth * scale));
        final int drawH = Math.max(1, (int) Math.round(imageHeight * scale));
        return new Rectangle((int) Math.round(areaX), (int) Math.round(areaY), drawW, drawH);
    }

    @Override
    public int print(final Graphics graphics, final PageFormat pageFormat, final int pageIndex)
            throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        final Graphics2D g2 = (Graphics2D) graphics;
        final Rectangle dest = fitToPage(pageFormat, this.image.getWidth(), this.image.getHeight());
        g2.drawImage(
                this.image,
                dest.x,
                dest.y,
                dest.x + dest.width,
                dest.y + dest.height,
                0,
                0,
                this.image.getWidth(),
                this.image.getHeight(),
                null
        );
        return PAGE_EXISTS;
    }
}
