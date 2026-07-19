package com.sysbot32.whistler.paint.model;

import com.sysbot32.whistler.paint.image.BitmapOps;

import lombok.Getter;

import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Floating selection state for rectangular / free-form Select tools.
 */
@Getter
public class SelectionModel {
    private BufferedImage floating;
    private int x;
    private int y;
    private boolean active;
    private Polygon freeFormMask;

    public void setRectangular(final BufferedImage floating, final int x, final int y) {
        this.floating = Objects.requireNonNull(floating);
        this.x = x;
        this.y = y;
        this.active = true;
        this.freeFormMask = null;
    }

    public void setFreeForm(final BufferedImage floating, final int x, final int y, final Polygon mask) {
        this.floating = Objects.requireNonNull(floating);
        this.x = x;
        this.y = y;
        this.active = true;
        this.freeFormMask = mask;
    }

    public void moveTo(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public void clear() {
        this.floating = null;
        this.x = 0;
        this.y = 0;
        this.active = false;
        this.freeFormMask = null;
    }

    public int getWidth() {
        return Objects.isNull(this.floating) ? 0 : this.floating.getWidth();
    }

    public int getHeight() {
        return Objects.isNull(this.floating) ? 0 : this.floating.getHeight();
    }

    public SelectionModel copy() {
        final SelectionModel copy = new SelectionModel();
        if (this.active && Objects.nonNull(this.floating)) {
            copy.floating = BitmapOps.copyOf(this.floating);
            copy.x = this.x;
            copy.y = this.y;
            copy.active = true;
            if (Objects.nonNull(this.freeFormMask)) {
                copy.freeFormMask = new Polygon(
                        this.freeFormMask.xpoints,
                        this.freeFormMask.ypoints,
                        this.freeFormMask.npoints
                );
            }
        }
        return copy;
    }
}
