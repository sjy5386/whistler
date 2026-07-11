package com.sysbot32.whistler.paint;

import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Domain model for the classic Paint document (bitmap + dirty flag + path).
 */
@Getter
public class Paint {
    public static final int DEFAULT_WIDTH = 400;
    public static final int DEFAULT_HEIGHT = 300;

    private BufferedImage image;
    private boolean edited = false;
    private Path path = null;

    public Paint() {
        this.createNew();
    }

    public Paint(final int width, final int height) {
        this.image = createBlankImage(width, height);
    }

    public Paint(final Path path) {
        try {
            this.open(path);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createNew() {
        this.image = createBlankImage(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.path = null;
        this.edited = false;
    }

    public void createNew(final int width, final int height) {
        this.image = createBlankImage(width, height);
        this.path = null;
        this.edited = false;
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

    public BufferedImage open(final Path path) throws IOException {
        final BufferedImage loaded = ImageIO.read(path.toFile());
        if (Objects.isNull(loaded)) {
            throw new IOException("Unsupported or unreadable image: " + path);
        }
        this.image = toArgb(loaded);
        this.path = path;
        this.edited = false;
        return this.image;
    }

    public void save(final Path path) throws IOException {
        final String format = formatNameFor(path);
        if (!ImageIO.write(this.image, format, path.toFile())) {
            throw new IOException("No writer for format '" + format + "': " + path);
        }
        this.edited = false;
        this.path = path;
    }

    public void clearImage() {
        final Graphics2D g = this.image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());
        } finally {
            g.dispose();
        }
        this.edited = true;
    }

    private static BufferedImage createBlankImage(final int width, final int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image size must be positive: " + width + "x" + height);
        }
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage toArgb(final BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        final BufferedImage argb = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D g = argb.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return argb;
    }

    private static String formatNameFor(final Path path) {
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
            default -> "png";
        };
    }
}
