package com.sysbot32.whistler.paint.image;

import com.sysbot32.whistler.paint.model.BrushShape;
import com.sysbot32.whistler.paint.model.FillStyle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Random;

/**
 * Pure bitmap mutations for classic Paint tools (testable without Swing).
 */
public final class BitmapOps {
    private BitmapOps() {
    }

    public static BufferedImage copyOf(final BufferedImage source) {
        final BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D g = copy.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }

    public static BufferedImage createBlank(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image size must be positive: " + width + "x" + height);
        }
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fillRect(image, 0, 0, width, height, Color.WHITE);
        return image;
    }

    public static void fillRect(
            final BufferedImage image,
            final int x,
            final int y,
            final int width,
            final int height,
            final Color color
    ) {
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(x, y, width, height);
        } finally {
            g.dispose();
        }
    }

    public static void drawPencil(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color color
    ) {
        drawThickLine(image, x0, y0, x1, y1, color, 1);
    }

    public static void drawBrush(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color color,
            final int size,
            final BrushShape shape
    ) {
        bresenham(x0, y0, x1, y1, (x, y) -> stampBrush(image, x, y, color, size, shape));
    }

    public static void drawEraser(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color background,
            final int size
    ) {
        drawBrush(image, x0, y0, x1, y1, background, size, BrushShape.SQUARE);
    }

    /**
     * Color eraser: within the brush footprint, replace {@code match} pixels with {@code replacement}.
     */
    public static void drawColorEraser(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color match,
            final Color replacement,
            final int size
    ) {
        final int matchRgb = match.getRGB();
        final int replaceRgb = replacement.getRGB();
        final int half = Math.max(1, size) / 2;
        bresenham(x0, y0, x1, y1, (x, y) -> {
            for (int dy = -half; dy <= half; dy++) {
                for (int dx = -half; dx <= half; dx++) {
                    final int px = x + dx;
                    final int py = y + dy;
                    if (inBounds(image, px, py) && image.getRGB(px, py) == matchRgb) {
                        image.setRGB(px, py, replaceRgb);
                    }
                }
            }
        });
    }

    public static void drawAirbrush(
            final BufferedImage image,
            final int x,
            final int y,
            final Color color,
            final int radius,
            final int density,
            final long seed
    ) {
        final Random random = new Random(seed ^ (((long) x) << 32) ^ y ^ color.getRGB());
        final int rgb = color.getRGB();
        final int r = Math.max(1, radius);
        for (int i = 0; i < density; i++) {
            final double angle = random.nextDouble() * Math.PI * 2;
            final double dist = random.nextDouble() * r;
            final int px = x + (int) Math.round(Math.cos(angle) * dist);
            final int py = y + (int) Math.round(Math.sin(angle) * dist);
            if (inBounds(image, px, py)) {
                image.setRGB(px, py, rgb);
            }
        }
    }

    public static void floodFill(final BufferedImage image, final int x, final int y, final Color fill) {
        if (!inBounds(image, x, y)) {
            return;
        }
        final int target = image.getRGB(x, y);
        final int replacement = fill.getRGB();
        if (target == replacement) {
            return;
        }
        final int w = image.getWidth();
        final int h = image.getHeight();
        final ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{x, y});
        while (!queue.isEmpty()) {
            final int[] p = queue.removeFirst();
            final int cx = p[0];
            final int cy = p[1];
            if (!inBounds(image, cx, cy) || image.getRGB(cx, cy) != target) {
                continue;
            }
            image.setRGB(cx, cy, replacement);
            queue.add(new int[]{cx + 1, cy});
            queue.add(new int[]{cx - 1, cy});
            queue.add(new int[]{cx, cy + 1});
            queue.add(new int[]{cx, cy - 1});
            // prevent unbounded growth on huge floods by scanning scanline style on wide runs
            if (queue.size() > w * h) {
                break;
            }
        }
    }

    public static Color pickColor(final BufferedImage image, final int x, final int y) {
        if (!inBounds(image, x, y)) {
            throw new IllegalArgumentException("Out of bounds: " + x + "," + y);
        }
        return new Color(image.getRGB(x, y), true);
    }

    public static void drawLine(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color color,
            final int thickness
    ) {
        drawThickLine(image, x0, y0, x1, y1, color, thickness);
    }

    public static void drawCurve(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final int c1x,
            final int c1y,
            final int c2x,
            final int c2y,
            final Color color,
            final int thickness
    ) {
        final Graphics2D g = createDrawingGraphics(image);
        try {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1, thickness), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            final Path2D path = new Path2D.Double();
            path.moveTo(x0, y0);
            path.curveTo(c1x, c1y, c2x, c2y, x1, y1);
            g.draw(path);
        } finally {
            g.dispose();
        }
    }

    public static void drawRectangle(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color foreground,
            final Color background,
            final FillStyle style,
            final int thickness
    ) {
        final int x = Math.min(x0, x1);
        final int y = Math.min(y0, y1);
        final int w = Math.abs(x1 - x0);
        final int h = Math.abs(y1 - y0);
        paintShape(image, new java.awt.Rectangle(x, y, w, h), foreground, background, style, thickness);
    }

    public static void drawEllipse(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color foreground,
            final Color background,
            final FillStyle style,
            final int thickness
    ) {
        final int x = Math.min(x0, x1);
        final int y = Math.min(y0, y1);
        final int w = Math.abs(x1 - x0);
        final int h = Math.abs(y1 - y0);
        paintShape(image, new Ellipse2D.Double(x, y, w, h), foreground, background, style, thickness);
    }

    public static void drawRoundedRectangle(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color foreground,
            final Color background,
            final FillStyle style,
            final int thickness
    ) {
        final int x = Math.min(x0, x1);
        final int y = Math.min(y0, y1);
        final int w = Math.abs(x1 - x0);
        final int h = Math.abs(y1 - y0);
        final double arc = Math.min(w, h) * 0.4;
        paintShape(image, new RoundRectangle2D.Double(x, y, w, h, arc, arc), foreground, background, style, thickness);
    }

    public static void drawPolygon(
            final BufferedImage image,
            final int[] xs,
            final int[] ys,
            final int n,
            final Color foreground,
            final Color background,
            final FillStyle style,
            final int thickness
    ) {
        if (n < 2) {
            return;
        }
        paintShape(image, new Polygon(xs, ys, n), foreground, background, style, thickness);
    }

    public static void drawText(
            final BufferedImage image,
            final String text,
            final int x,
            final int y,
            final Font font,
            final Color foreground,
            final Color background,
            final boolean opaque
    ) {
        drawText(image, text, x, y, font, foreground, background, opaque, false);
    }

    public static void drawText(
            final BufferedImage image,
            final String text,
            final int x,
            final int y,
            final Font font,
            final Color foreground,
            final Color background,
            final boolean opaque,
            final boolean underline
    ) {
        final Graphics2D g = createDrawingGraphics(image);
        try {
            g.setFont(font);
            final var metrics = g.getFontMetrics();
            final int textWidth = metrics.stringWidth(text);
            final int textHeight = metrics.getHeight();
            if (opaque) {
                g.setColor(background);
                g.fillRect(x, y - metrics.getAscent(), textWidth, textHeight);
            }
            g.setColor(foreground);
            g.drawString(text, x, y);
            if (underline && textWidth > 0) {
                final int uy = y + Math.max(1, metrics.getDescent() / 2);
                g.drawLine(x, uy, x + textWidth, uy);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Rasterize multi-line text into a box (classic Paint Text tool commit).
     */
    public static void drawTextBlock(
            final BufferedImage image,
            final String text,
            final int boxX,
            final int boxY,
            final int boxWidth,
            final int boxHeight,
            final Font font,
            final Color foreground,
            final Color background,
            final boolean opaque
    ) {
        drawTextBlock(image, text, boxX, boxY, boxWidth, boxHeight, font, foreground, background, opaque, false);
    }

    public static void drawTextBlock(
            final BufferedImage image,
            final String text,
            final int boxX,
            final int boxY,
            final int boxWidth,
            final int boxHeight,
            final Font font,
            final Color foreground,
            final Color background,
            final boolean opaque,
            final boolean underline
    ) {
        if (Objects.isNull(text) || text.isEmpty() || boxWidth <= 0 || boxHeight <= 0) {
            return;
        }
        final Graphics2D g = createDrawingGraphics(image);
        try {
            final Shape oldClip = g.getClip();
            g.setClip(boxX, boxY, boxWidth, boxHeight);
            if (opaque) {
                g.setColor(background);
                g.fillRect(boxX, boxY, boxWidth, boxHeight);
            }
            g.setFont(font);
            g.setColor(foreground);
            final var metrics = g.getFontMetrics();
            int lineY = boxY + metrics.getAscent();
            final int lineHeight = Math.max(1, metrics.getHeight());
            for (final String line : text.split("\n", -1)) {
                if (lineY - metrics.getAscent() >= boxY + boxHeight) {
                    break;
                }
                g.drawString(line, boxX, lineY);
                if (underline) {
                    final int lineWidth = metrics.stringWidth(line);
                    if (lineWidth > 0) {
                        final int uy = lineY + Math.max(1, metrics.getDescent() / 2);
                        g.drawLine(boxX, uy, boxX + lineWidth, uy);
                    }
                }
                lineY += lineHeight;
            }
            g.setClip(oldClip);
        } finally {
            g.dispose();
        }
    }

    public static void invertColors(final BufferedImage image) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int argb = image.getRGB(x, y);
                final int a = (argb >>> 24) & 0xff;
                final int r = 255 - ((argb >>> 16) & 0xff);
                final int g = 255 - ((argb >>> 8) & 0xff);
                final int b = 255 - (argb & 0xff);
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    public static BufferedImage flipHorizontal(final BufferedImage source) {
        final int w = source.getWidth();
        final int h = source.getHeight();
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, w, 0, 0, h, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static BufferedImage flipVertical(final BufferedImage source) {
        final int w = source.getWidth();
        final int h = source.getHeight();
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, h, w, 0, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static BufferedImage rotate90Clockwise(final BufferedImage source) {
        final int w = source.getWidth();
        final int h = source.getHeight();
        final BufferedImage out = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        try {
            g.translate(h, 0);
            g.rotate(Math.PI / 2);
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static BufferedImage rotate180(final BufferedImage source) {
        return rotate90Clockwise(rotate90Clockwise(source));
    }

    public static BufferedImage rotate270Clockwise(final BufferedImage source) {
        return rotate90Clockwise(rotate180(source));
    }

    public static BufferedImage stretch(
            final BufferedImage source,
            final int horizontalPercent,
            final int verticalPercent
    ) {
        final int newW = Math.max(1, Math.round(source.getWidth() * horizontalPercent / 100f));
        final int newH = Math.max(1, Math.round(source.getHeight() * verticalPercent / 100f));
        final BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(source, 0, 0, newW, newH, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static BufferedImage skew(
            final BufferedImage source,
            final int horizontalDegrees,
            final int verticalDegrees
    ) {
        final double hx = Math.tan(Math.toRadians(horizontalDegrees));
        final double vy = Math.tan(Math.toRadians(verticalDegrees));
        final int w = source.getWidth();
        final int h = source.getHeight();
        final int extraW = (int) Math.ceil(Math.abs(hx) * h);
        final int extraH = (int) Math.ceil(Math.abs(vy) * w);
        final int newW = w + extraW;
        final int newH = h + extraH;
        final BufferedImage out = createBlank(newW, newH);
        final Graphics2D g = out.createGraphics();
        try {
            final AffineTransform tx = new AffineTransform();
            final double xOff = hx < 0 ? extraW : 0;
            final double yOff = vy < 0 ? extraH : 0;
            tx.translate(xOff, yOff);
            tx.shear(hx, vy);
            g.drawImage(source, tx, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    public static BufferedImage extractRect(
            final BufferedImage source,
            final int x,
            final int y,
            final int width,
            final int height
    ) {
        final int sx = clamp(x, 0, source.getWidth() - 1);
        final int sy = clamp(y, 0, source.getHeight() - 1);
        final int sw = Math.max(1, Math.min(width, source.getWidth() - sx));
        final int sh = Math.max(1, Math.min(height, source.getHeight() - sy));
        final BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    /**
     * Free-form extract: copies pixels inside the polygon into a tight bounding image
     * with transparent outside; returns [image, minX, minY].
     */
    public static Object[] extractFreeForm(final BufferedImage source, final Polygon polygon) {
        final var bounds = polygon.getBounds();
        final int minX = Math.max(0, bounds.x);
        final int minY = Math.max(0, bounds.y);
        final int maxX = Math.min(source.getWidth(), bounds.x + bounds.width);
        final int maxY = Math.min(source.getHeight(), bounds.y + bounds.height);
        final int w = Math.max(1, maxX - minX);
        final int h = Math.max(1, maxY - minY);
        final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if (polygon.contains(x, y)) {
                    out.setRGB(x - minX, y - minY, source.getRGB(x, y));
                }
            }
        }
        return new Object[]{out, minX, minY};
    }

    public static void clearRect(
            final BufferedImage image,
            final int x,
            final int y,
            final int width,
            final int height,
            final Color color
    ) {
        fillRect(image, x, y, width, height, color);
    }

    public static void clearPolygon(
            final BufferedImage image,
            final Polygon polygon,
            final Color color
    ) {
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            g.fillPolygon(polygon);
        } finally {
            g.dispose();
        }
    }

    public static void composite(
            final BufferedImage dest,
            final BufferedImage floating,
            final int destX,
            final int destY,
            final boolean drawOpaque,
            final Color transparentKey
    ) {
        final int key = transparentKey.getRGB();
        for (int y = 0; y < floating.getHeight(); y++) {
            for (int x = 0; x < floating.getWidth(); x++) {
                final int dx = destX + x;
                final int dy = destY + y;
                if (!inBounds(dest, dx, dy)) {
                    continue;
                }
                final int argb = floating.getRGB(x, y);
                final int alpha = (argb >>> 24) & 0xff;
                if (alpha == 0) {
                    continue;
                }
                if (!drawOpaque && argb == key) {
                    continue;
                }
                dest.setRGB(dx, dy, argb);
            }
        }
    }

    public static int[] constrainLine(final int x0, final int y0, final int x1, final int y1) {
        final int dx = x1 - x0;
        final int dy = y1 - y0;
        if (dx == 0 && dy == 0) {
            return new int[]{x1, y1};
        }
        final double angle = Math.toDegrees(Math.atan2(dy, dx));
        final double snapped = Math.round(angle / 45.0) * 45.0;
        final double rad = Math.toRadians(snapped);
        final double length = Math.hypot(dx, dy);
        final int nx = x0 + (int) Math.round(Math.cos(rad) * length);
        final int ny = y0 + (int) Math.round(Math.sin(rad) * length);
        return new int[]{nx, ny};
    }

    public static int[] constrainSquare(final int x0, final int y0, final int x1, final int y1) {
        final int dx = x1 - x0;
        final int dy = y1 - y0;
        final int side = Math.max(Math.abs(dx), Math.abs(dy));
        final int sx = dx < 0 ? -side : side;
        final int sy = dy < 0 ? -side : side;
        return new int[]{x0 + sx, y0 + sy};
    }

    private static void paintShape(
            final BufferedImage image,
            final Shape shape,
            final Color foreground,
            final Color background,
            final FillStyle style,
            final int thickness
    ) {
        final Graphics2D g = createDrawingGraphics(image);
        try {
            g.setStroke(new BasicStroke(Math.max(1, thickness), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            switch (style) {
                case OUTLINE -> {
                    g.setColor(foreground);
                    g.draw(shape);
                }
                case OUTLINE_FILL -> {
                    g.setColor(background);
                    g.fill(shape);
                    g.setColor(foreground);
                    g.draw(shape);
                }
                case FILL -> {
                    g.setColor(foreground);
                    g.fill(shape);
                }
            }
        } finally {
            g.dispose();
        }
    }

    private static void drawThickLine(
            final BufferedImage image,
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final Color color,
            final int thickness
    ) {
        final Graphics2D g = createDrawingGraphics(image);
        try {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1, thickness), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(x0, y0, x1, y1));
        } finally {
            g.dispose();
        }
    }

    private static void stampBrush(
            final BufferedImage image,
            final int x,
            final int y,
            final Color color,
            final int size,
            final BrushShape shape
    ) {
        final int s = Math.max(1, size);
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(color);
            switch (shape) {
                case ROUND -> g.fillOval(x - s / 2, y - s / 2, s, s);
                case SQUARE -> g.fillRect(x - s / 2, y - s / 2, s, s);
                case FORWARD_SLASH -> {
                    g.setStroke(new BasicStroke(Math.max(1, s / 2f)));
                    g.drawLine(x - s / 2, y + s / 2, x + s / 2, y - s / 2);
                }
                case BACKWARD_SLASH -> {
                    g.setStroke(new BasicStroke(Math.max(1, s / 2f)));
                    g.drawLine(x - s / 2, y - s / 2, x + s / 2, y + s / 2);
                }
            }
        } finally {
            g.dispose();
        }
    }

    private static Graphics2D createDrawingGraphics(final BufferedImage image) {
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        return g;
    }

    private static boolean inBounds(final BufferedImage image, final int x, final int y) {
        return x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight();
    }

    private static int clamp(final int v, final int min, final int max) {
        return Math.max(min, Math.min(max, v));
    }

    @FunctionalInterface
    private interface PointConsumer {
        void accept(int x, int y);
    }

    private static void bresenham(
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final PointConsumer consumer
    ) {
        int x = x0;
        int y = y0;
        final int dx = Math.abs(x1 - x0);
        final int dy = Math.abs(y1 - y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            consumer.accept(x, y);
            if (x == x1 && y == y1) {
                break;
            }
            final int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
