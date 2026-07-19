package com.sysbot32.whistler.paint.image;

import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagePrintSupportTest {
    @Test
    void fitToPageScalesDownToImageableArea() {
        final PageFormat pf = letterPage(72, 72, 468, 648); // 6.5" x 9" imageable
        final Rectangle dest = ImagePrintSupport.fitToPage(pf, 1000, 500);
        assertTrue(dest.width <= pf.getImageableWidth() + 0.5);
        assertTrue(dest.height <= pf.getImageableHeight() + 0.5);
        assertEquals(2.0, dest.width / (double) dest.height, 0.05); // aspect preserved
        assertEquals((int) Math.round(pf.getImageableX()), dest.x);
        assertEquals((int) Math.round(pf.getImageableY()), dest.y);
    }

    @Test
    void printableRendersSinglePage() throws Exception {
        final BufferedImage image = BitmapOps.createBlank(40, 20);
        image.setRGB(5, 5, 0xffff0000);
        final ImagePrintSupport printable = new ImagePrintSupport(image);
        final PageFormat pf = letterPage(36, 36, 540, 720);
        final BufferedImage page = new BufferedImage(612, 792, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = page.createGraphics();
        try {
            final int result = printable.print(g, pf, 0);
            assertEquals(Printable.PAGE_EXISTS, result);
            assertEquals(Printable.NO_SUCH_PAGE, printable.print(g, pf, 1));
        } finally {
            g.dispose();
        }
    }

    private static PageFormat letterPage(
            final double imageableX,
            final double imageableY,
            final double imageableW,
            final double imageableH
    ) {
        final Paper paper = new Paper();
        paper.setSize(612, 792); // letter points
        paper.setImageableArea(imageableX, imageableY, imageableW, imageableH);
        final PageFormat pf = new PageFormat();
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }
}
