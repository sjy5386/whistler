package com.sysbot32.whistler.paint;

import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Classic Paint document + session state (colors, tool options, selection, undo, zoom).
 */
@Getter
public class Paint {
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 300;
    public static final int MAX_UNDO = 3;
    public static final int MAX_TOOL_SIZE = 100;
    public static final int[] ZOOM_LEVELS = {1, 2, 6, 8};
    public static final int[] LINE_WIDTHS = {1, 2, 3, 4, 5};
    public static final int[] ERASER_SIZES = {4, 6, 8, 10};
    public static final int[] BRUSH_SIZES = {2, 4, 6, 8};
    public static final int[] AIRBRUSH_RADII = {8, 16, 24};

    private BufferedImage image;
    private boolean edited = false;
    private Path path = null;

    private Color foreground = Color.BLACK;
    private Color background = Color.WHITE;
    private PaintTool tool = PaintTool.PENCIL;
    private FillStyle fillStyle = FillStyle.OUTLINE;
    private BrushShape brushShape = BrushShape.ROUND;
    private int lineWidth = 1;
    private int eraserSize = 8;
    private int brushSize = 4;
    private int airbrushRadius = 16;
    private int zoom = 1;
    private boolean drawOpaque = true;
    private Font textFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private boolean textUnderline = false;

    private final SelectionModel selection = new SelectionModel();
    private BufferedImage clipboard;

    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private PaintTool toolBeforePick = PaintTool.PENCIL;

    public Paint() {
        this.createNew();
    }

    public Paint(final int width, final int height) {
        this.image = BitmapOps.createBlank(width, height);
    }

    public Paint(final Path path) {
        try {
            this.open(path);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to open image: " + path, e);
        }
    }

    public void createNew() {
        this.image = BitmapOps.createBlank(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.path = null;
        this.edited = false;
        this.selection.clear();
        this.undoStack.clear();
        this.redoStack.clear();
    }

    public void createNew(final int width, final int height) {
        this.image = BitmapOps.createBlank(width, height);
        this.path = null;
        this.edited = false;
        this.selection.clear();
        this.undoStack.clear();
        this.redoStack.clear();
    }

    public void setEdited(final boolean edited) {
        this.edited = edited;
    }

    public int getWidth() {
        return this.image.getWidth();
    }

    public int getHeight() {
        return this.image.getHeight();
    }

    public void setForeground(final Color foreground) {
        this.foreground = Objects.requireNonNull(foreground);
    }

    public void setBackground(final Color background) {
        this.background = Objects.requireNonNull(background);
    }

    public void setTool(final PaintTool tool) {
        commitSelectionIfAny();
        if (tool == PaintTool.PICK_COLOR && this.tool != PaintTool.PICK_COLOR) {
            this.toolBeforePick = this.tool;
        }
        this.tool = Objects.requireNonNull(tool);
    }

    public void setFillStyle(final FillStyle fillStyle) {
        this.fillStyle = Objects.requireNonNull(fillStyle);
    }

    public void setBrushShape(final BrushShape brushShape) {
        this.brushShape = Objects.requireNonNull(brushShape);
    }

    public void setLineWidth(final int lineWidth) {
        this.lineWidth = Math.max(1, lineWidth);
    }

    public void setEraserSize(final int eraserSize) {
        this.eraserSize = Math.max(1, eraserSize);
    }

    public void setBrushSize(final int brushSize) {
        this.brushSize = Math.max(1, brushSize);
    }

    public void setAirbrushRadius(final int airbrushRadius) {
        this.airbrushRadius = Math.max(1, airbrushRadius);
    }

    /**
     * Classic Ctrl+NumPad±: grow/shrink the active tool size (can exceed toolbar presets).
     *
     * @return the size after nudge, or {@code -1} if the current tool has no size
     */
    public int nudgeToolSize(final int delta) {
        return switch (this.tool) {
            case BRUSH -> {
                this.brushSize = clampToolSize(this.brushSize + delta);
                yield this.brushSize;
            }
            case ERASER -> {
                this.eraserSize = clampToolSize(this.eraserSize + delta);
                yield this.eraserSize;
            }
            case AIRBRUSH -> {
                this.airbrushRadius = clampToolSize(this.airbrushRadius + delta);
                yield this.airbrushRadius;
            }
            case LINE, CURVE, RECTANGLE, POLYGON, ELLIPSE, ROUNDED_RECTANGLE -> {
                this.lineWidth = clampToolSize(this.lineWidth + delta);
                yield this.lineWidth;
            }
            default -> -1;
        };
    }

    private static int clampToolSize(final int size) {
        return Math.max(1, Math.min(MAX_TOOL_SIZE, size));
    }

    public void setZoom(final int zoom) {
        for (final int level : ZOOM_LEVELS) {
            if (level == zoom) {
                this.zoom = zoom;
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported zoom: " + zoom);
    }

    public void setDrawOpaque(final boolean drawOpaque) {
        this.drawOpaque = drawOpaque;
    }

    public void setTextFont(final Font textFont) {
        this.textFont = Objects.requireNonNull(textFont);
    }

    public void setTextUnderline(final boolean textUnderline) {
        this.textUnderline = textUnderline;
    }

    /**
     * Font used for the live editor and rasterize, including underline attribute when enabled.
     */
    public Font getEffectiveTextFont() {
        final Map<TextAttribute, Object> attrs = new HashMap<>(this.textFont.getAttributes());
        if (this.textUnderline) {
            attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        } else {
            attrs.put(TextAttribute.UNDERLINE, -1);
        }
        return this.textFont.deriveFont(attrs);
    }

    public void pushUndo() {
        this.undoStack.addFirst(BitmapOps.copyOf(this.image));
        while (this.undoStack.size() > MAX_UNDO) {
            this.undoStack.removeLast();
        }
        // A new edit invalidates the classic Repeat/redo trail.
        this.redoStack.clear();
    }

    public boolean canUndo() {
        return !this.undoStack.isEmpty();
    }

    public boolean canRepeat() {
        return !this.redoStack.isEmpty();
    }

    public void undo() {
        if (this.undoStack.isEmpty()) {
            return;
        }
        this.selection.clear();
        this.redoStack.addFirst(BitmapOps.copyOf(this.image));
        while (this.redoStack.size() > MAX_UNDO) {
            this.redoStack.removeLast();
        }
        this.image = this.undoStack.removeFirst();
        this.edited = true;
    }

    /**
     * Classic Paint "Repeat" — redo the last undone change when available.
     */
    public void repeat() {
        if (this.redoStack.isEmpty()) {
            return;
        }
        this.selection.clear();
        this.undoStack.addFirst(BitmapOps.copyOf(this.image));
        while (this.undoStack.size() > MAX_UNDO) {
            this.undoStack.removeLast();
        }
        this.image = this.redoStack.removeFirst();
        this.edited = true;
    }

    public BufferedImage open(final Path path) throws IOException {
        final BufferedImage loaded = ImageIO.read(path.toFile());
        if (Objects.isNull(loaded)) {
            throw new IOException("Unsupported or unreadable image: " + path);
        }
        this.selection.clear();
        this.undoStack.clear();
        this.redoStack.clear();
        this.image = toArgb(loaded);
        this.path = path;
        this.edited = false;
        return this.image;
    }

    public void save(final Path path) throws IOException {
        commitSelectionIfAny();
        final String format = formatNameFor(path);
        final BufferedImage toWrite = prepareForSave(this.image, format);
        if (!ImageIO.write(toWrite, format, path.toFile())) {
            throw new IOException("No writer for format '" + format + "': " + path);
        }
        this.edited = false;
        this.path = path;
    }

    public void clearImage() {
        pushUndo();
        this.selection.clear();
        BitmapOps.fillRect(this.image, 0, 0, this.image.getWidth(), this.image.getHeight(), Color.WHITE);
        this.edited = true;
    }

    public void attributes(final int width, final int height) {
        pushUndo();
        this.selection.clear();
        final BufferedImage next = BitmapOps.createBlank(width, height);
        Graphics2DSafe.draw(next, this.image);
        this.image = next;
        this.edited = true;
    }

    public void invertColors() {
        pushUndo();
        applyToWorkingSurface(BitmapOps::invertColors);
        this.edited = true;
    }

    public void flipHorizontal() {
        pushUndo();
        transformWorking(BitmapOps::flipHorizontal);
        this.edited = true;
    }

    public void flipVertical() {
        pushUndo();
        transformWorking(BitmapOps::flipVertical);
        this.edited = true;
    }

    public void rotate90() {
        pushUndo();
        transformWorking(BitmapOps::rotate90Clockwise);
        this.edited = true;
    }

    public void rotate180() {
        pushUndo();
        transformWorking(BitmapOps::rotate180);
        this.edited = true;
    }

    public void rotate270() {
        pushUndo();
        transformWorking(BitmapOps::rotate270Clockwise);
        this.edited = true;
    }

    public void stretch(final int horizontalPercent, final int verticalPercent) {
        pushUndo();
        transformWorking(img -> BitmapOps.stretch(img, horizontalPercent, verticalPercent));
        this.edited = true;
    }

    public void skew(final int horizontalDegrees, final int verticalDegrees) {
        pushUndo();
        transformWorking(img -> BitmapOps.skew(img, horizontalDegrees, verticalDegrees));
        this.edited = true;
    }

    public Color colorForButton(final boolean rightButton) {
        return rightButton ? this.background : this.foreground;
    }

    public void strokePencil(final int x0, final int y0, final int x1, final int y1, final boolean rightButton) {
        BitmapOps.drawPencil(this.image, x0, y0, x1, y1, colorForButton(rightButton));
        this.edited = true;
    }

    public void strokeBrush(final int x0, final int y0, final int x1, final int y1, final boolean rightButton) {
        BitmapOps.drawBrush(
                this.image, x0, y0, x1, y1,
                colorForButton(rightButton), this.brushSize, this.brushShape
        );
        this.edited = true;
    }

    public void strokeEraser(final int x0, final int y0, final int x1, final int y1, final boolean rightButton) {
        if (rightButton) {
            BitmapOps.drawColorEraser(
                    this.image, x0, y0, x1, y1,
                    this.foreground, this.background, this.eraserSize
            );
        } else {
            BitmapOps.drawEraser(this.image, x0, y0, x1, y1, this.background, this.eraserSize);
        }
        this.edited = true;
    }

    public void strokeAirbrush(final int x, final int y, final boolean rightButton, final long seed) {
        BitmapOps.drawAirbrush(
                this.image, x, y, colorForButton(rightButton),
                this.airbrushRadius, 40, seed
        );
        this.edited = true;
    }

    public void fillAt(final int x, final int y, final boolean rightButton) {
        pushUndo();
        BitmapOps.floodFill(this.image, x, y, colorForButton(rightButton));
        this.edited = true;
    }

    public void pickAt(final int x, final int y, final boolean rightButton) {
        final Color picked = BitmapOps.pickColor(this.image, x, y);
        if (rightButton) {
            this.background = picked;
        } else {
            this.foreground = picked;
        }
        this.tool = this.toolBeforePick;
    }

    public void drawLineShape(
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final boolean rightButton,
            final boolean shift
    ) {
        int ex = x1;
        int ey = y1;
        if (shift) {
            final int[] c = BitmapOps.constrainLine(x0, y0, x1, y1);
            ex = c[0];
            ey = c[1];
        }
        pushUndo();
        BitmapOps.drawLine(this.image, x0, y0, ex, ey, colorForButton(rightButton), this.lineWidth);
        this.edited = true;
    }

    public void drawCurveShape(
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final int c1x,
            final int c1y,
            final int c2x,
            final int c2y,
            final boolean rightButton
    ) {
        pushUndo();
        BitmapOps.drawCurve(
                this.image, x0, y0, x1, y1, c1x, c1y, c2x, c2y,
                colorForButton(rightButton), this.lineWidth
        );
        this.edited = true;
    }

    /**
     * Classic Paint: left drag uses FG outline / BG fill; right drag swaps those roles.
     */
    public Color[] shapeColors(final boolean rightButton) {
        if (rightButton) {
            return new Color[]{this.background, this.foreground};
        }
        return new Color[]{this.foreground, this.background};
    }

    public void drawRectangleShape(
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final boolean shift,
            final boolean rightButton
    ) {
        int ex = x1;
        int ey = y1;
        if (shift) {
            final int[] c = BitmapOps.constrainSquare(x0, y0, x1, y1);
            ex = c[0];
            ey = c[1];
        }
        final Color[] colors = shapeColors(rightButton);
        pushUndo();
        BitmapOps.drawRectangle(
                this.image, x0, y0, ex, ey,
                colors[0], colors[1], this.fillStyle, this.lineWidth
        );
        this.edited = true;
    }

    public void drawEllipseShape(
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final boolean shift,
            final boolean rightButton
    ) {
        int ex = x1;
        int ey = y1;
        if (shift) {
            final int[] c = BitmapOps.constrainSquare(x0, y0, x1, y1);
            ex = c[0];
            ey = c[1];
        }
        final Color[] colors = shapeColors(rightButton);
        pushUndo();
        BitmapOps.drawEllipse(
                this.image, x0, y0, ex, ey,
                colors[0], colors[1], this.fillStyle, this.lineWidth
        );
        this.edited = true;
    }

    public void drawRoundedRectangleShape(
            final int x0,
            final int y0,
            final int x1,
            final int y1,
            final boolean shift,
            final boolean rightButton
    ) {
        int ex = x1;
        int ey = y1;
        if (shift) {
            final int[] c = BitmapOps.constrainSquare(x0, y0, x1, y1);
            ex = c[0];
            ey = c[1];
        }
        final Color[] colors = shapeColors(rightButton);
        pushUndo();
        BitmapOps.drawRoundedRectangle(
                this.image, x0, y0, ex, ey,
                colors[0], colors[1], this.fillStyle, this.lineWidth
        );
        this.edited = true;
    }

    public void drawPolygonShape(final int[] xs, final int[] ys, final int n, final boolean rightButton) {
        final Color[] colors = shapeColors(rightButton);
        pushUndo();
        BitmapOps.drawPolygon(
                this.image, xs, ys, n,
                colors[0], colors[1], this.fillStyle, this.lineWidth
        );
        this.edited = true;
    }

    public void drawTextAt(final String text, final int x, final int y) {
        if (Objects.isNull(text) || text.isEmpty()) {
            return;
        }
        pushUndo();
        BitmapOps.drawText(
                this.image, text, x, y, this.textFont,
                this.foreground, this.background, this.drawOpaque, this.textUnderline
        );
        this.edited = true;
    }

    public void drawTextBlock(
            final String text,
            final int boxX,
            final int boxY,
            final int boxWidth,
            final int boxHeight
    ) {
        if (Objects.isNull(text) || text.isEmpty()) {
            return;
        }
        pushUndo();
        BitmapOps.drawTextBlock(
                this.image,
                text,
                boxX,
                boxY,
                boxWidth,
                boxHeight,
                this.textFont,
                this.foreground,
                this.background,
                this.drawOpaque,
                this.textUnderline
        );
        this.edited = true;
    }

    public void beginRectangularSelection(final int x0, final int y0, final int x1, final int y1) {
        commitSelectionIfAny();
        pushUndo();
        final int x = Math.min(x0, x1);
        final int y = Math.min(y0, y1);
        final int w = Math.max(1, Math.abs(x1 - x0));
        final int h = Math.max(1, Math.abs(y1 - y0));
        final BufferedImage floating = BitmapOps.extractRect(this.image, x, y, w, h);
        BitmapOps.clearRect(this.image, x, y, w, h, this.background);
        this.selection.setRectangular(floating, x, y);
        this.edited = true;
    }

    public void beginFreeFormSelection(final Polygon polygon) {
        commitSelectionIfAny();
        if (polygon.npoints < 3) {
            return;
        }
        pushUndo();
        final Object[] extracted = BitmapOps.extractFreeForm(this.image, polygon);
        final BufferedImage floating = (BufferedImage) extracted[0];
        final int minX = (Integer) extracted[1];
        final int minY = (Integer) extracted[2];
        BitmapOps.clearPolygon(this.image, polygon, this.background);
        this.selection.setFreeForm(floating, minX, minY, polygon);
        this.edited = true;
    }

    public void moveSelection(final int x, final int y) {
        if (!this.selection.isActive()) {
            return;
        }
        this.selection.moveTo(x, y);
        this.edited = true;
    }

    /**
     * Classic stamp/trail: paint the floating selection onto the document without ending the selection.
     */
    public void stampSelection() {
        if (!this.selection.isActive()) {
            return;
        }
        BitmapOps.composite(
                this.image,
                this.selection.getFloating(),
                this.selection.getX(),
                this.selection.getY(),
                this.drawOpaque,
                this.background
        );
        this.edited = true;
    }

    /**
     * Move floating selection; with stamp ({@code ctrl}) or trail ({@code shift}), leave copies along the path.
     */
    public void moveSelection(final int x, final int y, final boolean stamp, final boolean trail) {
        if (!this.selection.isActive()) {
            return;
        }
        if ((stamp || trail)
                && (x != this.selection.getX() || y != this.selection.getY())) {
            stampSelection();
        }
        moveSelection(x, y);
    }

    public void commitSelectionIfAny() {
        if (!this.selection.isActive()) {
            return;
        }
        BitmapOps.composite(
                this.image,
                this.selection.getFloating(),
                this.selection.getX(),
                this.selection.getY(),
                this.drawOpaque,
                this.background
        );
        this.selection.clear();
        this.edited = true;
    }

    public void cutSelection() {
        if (!this.selection.isActive()) {
            return;
        }
        this.clipboard = BitmapOps.copyOf(this.selection.getFloating());
        pushSystemClipboard(this.clipboard);
        this.selection.clear();
        this.edited = true;
    }

    public void copySelection() {
        if (!this.selection.isActive()) {
            return;
        }
        this.clipboard = BitmapOps.copyOf(this.selection.getFloating());
        pushSystemClipboard(this.clipboard);
    }

    public boolean canPaste() {
        if (Objects.nonNull(this.clipboard)) {
            return true;
        }
        return ImageClipboard.hasImage();
    }

    public void pasteClipboard() {
        BufferedImage incoming = ImageClipboard.pasteImage();
        if (Objects.isNull(incoming) && Objects.nonNull(this.clipboard)) {
            incoming = BitmapOps.copyOf(this.clipboard);
        }
        if (Objects.isNull(incoming)) {
            return;
        }
        this.clipboard = BitmapOps.copyOf(incoming);
        commitSelectionIfAny();
        pushUndo();
        this.selection.setRectangular(BitmapOps.copyOf(incoming), 0, 0);
        this.edited = true;
    }

    private static void pushSystemClipboard(final BufferedImage image) {
        try {
            ImageClipboard.copyImage(image);
        } catch (final IllegalStateException ignored) {
            // Clipboard locked by another app — keep local clipboard only.
        }
    }

    public void clearSelection() {
        if (!this.selection.isActive()) {
            return;
        }
        this.selection.clear();
        this.edited = true;
    }

    public void selectAll() {
        beginRectangularSelection(0, 0, this.image.getWidth(), this.image.getHeight());
    }

    public void copySelectionTo(final Path path) throws IOException {
        if (!this.selection.isActive()) {
            throw new IllegalStateException("No selection");
        }
        final String format = formatNameFor(path);
        final BufferedImage toWrite = prepareForSave(this.selection.getFloating(), format);
        if (!ImageIO.write(toWrite, format, path.toFile())) {
            throw new IOException("No writer for format '" + format + "': " + path);
        }
    }

    public void pasteFrom(final Path path) throws IOException {
        final BufferedImage loaded = ImageIO.read(path.toFile());
        if (Objects.isNull(loaded)) {
            throw new IOException("Unsupported or unreadable image: " + path);
        }
        commitSelectionIfAny();
        pushUndo();
        this.selection.setRectangular(toArgb(loaded), 0, 0);
        this.edited = true;
    }

    private void applyToWorkingSurface(final ImageConsumer consumer) {
        if (this.selection.isActive()) {
            consumer.accept(this.selection.getFloating());
        } else {
            consumer.accept(this.image);
        }
    }

    private void transformWorking(final ImageTransformer transformer) {
        if (this.selection.isActive()) {
            final BufferedImage next = transformer.apply(this.selection.getFloating());
            this.selection.setRectangular(next, this.selection.getX(), this.selection.getY());
        } else {
            this.image = transformer.apply(this.image);
        }
    }

    private static BufferedImage toArgb(final BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return BitmapOps.copyOf(source);
        }
        final BufferedImage argb = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2DSafe.draw(argb, source);
        return argb;
    }

    private static BufferedImage prepareForSave(final BufferedImage source, final String format) {
        if ("jpg".equals(format) || "jpeg".equals(format) || "bmp".equals(format)) {
            final BufferedImage rgb = new BufferedImage(
                    source.getWidth(),
                    source.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            final java.awt.Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
            return rgb;
        }
        return source;
    }

    static String formatNameFor(final Path path) {
        final String name = path.getFileName().toString();
        final int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "png";
        }
        final String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpg", "jpeg" -> "jpg";
            case "bmp" -> "bmp";
            case "gif" -> "gif";
            case "tif", "tiff" -> "tiff";
            default -> "png";
        };
    }

    @FunctionalInterface
    private interface ImageConsumer {
        void accept(BufferedImage image);
    }

    @FunctionalInterface
    private interface ImageTransformer {
        BufferedImage apply(BufferedImage image);
    }

    /** Small helper to avoid repeating Graphics2D draw boilerplate. */
    private static final class Graphics2DSafe {
        private static void draw(final BufferedImage dest, final BufferedImage source) {
            final java.awt.Graphics2D g = dest.createGraphics();
            try {
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
        }
    }
}
