package com.sysbot32.whistler.paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Interactive drawing surface: maps mouse input to {@link Paint} tool operations.
 */
public class PaintCanvas extends JComponent {
    private Paint paint;
    private BiConsumer<Integer, Integer> statusListener = (x, y) -> {
    };

    private boolean drawing;
    private boolean rightButton;
    private boolean shiftDown;
    private int startX;
    private int startY;
    private int lastX;
    private int lastY;
    private int currX;
    private int currY;

    // Curve: after line drag, two control-point adjustments
    private int curvePhase; // 0 idle, 1 need ctrl1, 2 need ctrl2
    private int curveX0, curveY0, curveX1, curveY1, curveC1x, curveC1y;

    // Polygon points (image coords)
    private final List<Point> polygonPoints = new ArrayList<>();
    private boolean polygonRightButton;

    // Free-form select path
    private final List<Point> freeFormPoints = new ArrayList<>();

    // Moving selection
    private boolean movingSelection;
    private int moveGrabX;
    private int moveGrabY;

    // Text placement
    private boolean placingText;

    public PaintCanvas(final Paint paint) {
        this.paint = Objects.requireNonNull(paint);
        this.setBackground(new Color(128, 128, 128));
        this.setOpaque(true);
        this.syncPreferredSize();
        this.setFocusable(true);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                PaintCanvas.this.onPress(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                PaintCanvas.this.onRelease(e);
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                PaintCanvas.this.onClick(e);
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                PaintCanvas.this.onDrag(e);
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                PaintCanvas.this.onMove(e);
            }
        });
    }

    public void setPaint(final Paint paint) {
        this.paint = Objects.requireNonNull(paint);
        this.resetTransientState();
        this.syncPreferredSize();
        this.revalidate();
        this.repaint();
    }

    public void setStatusListener(final BiConsumer<Integer, Integer> statusListener) {
        this.statusListener = Objects.requireNonNull(statusListener);
    }

    public void syncPreferredSize() {
        final int z = this.paint.getZoom();
        this.setPreferredSize(new Dimension(
                this.paint.getWidth() * z,
                this.paint.getHeight() * z
        ));
    }

    public void notifyDocumentChanged() {
        this.syncPreferredSize();
        this.revalidate();
        this.repaint();
    }

    private void resetTransientState() {
        this.drawing = false;
        this.curvePhase = 0;
        this.polygonPoints.clear();
        this.freeFormPoints.clear();
        this.movingSelection = false;
        this.placingText = false;
    }

    private int imgX(final MouseEvent e) {
        return e.getX() / Math.max(1, this.paint.getZoom());
    }

    private int imgY(final MouseEvent e) {
        return e.getY() / Math.max(1, this.paint.getZoom());
    }

    private void onPress(final MouseEvent e) {
        this.requestFocusInWindow();
        final int x = imgX(e);
        final int y = imgY(e);
        this.rightButton = SwingUtilities.isRightMouseButton(e);
        this.shiftDown = e.isShiftDown();
        this.startX = x;
        this.startY = y;
        this.lastX = x;
        this.lastY = y;
        this.currX = x;
        this.currY = y;

        // Cancel stroke with opposite button (classic instant undo feel)
        if (this.drawing && this.rightButton != SwingUtilities.isRightMouseButton(e)
                && isFreehandTool(this.paint.getTool())) {
            if (this.paint.canUndo()) {
                this.paint.undo();
            }
            this.drawing = false;
            this.repaint();
            return;
        }

        final PaintTool tool = this.paint.getTool();

        if (this.paint.getSelection().isActive()
                && tool != PaintTool.SELECT && tool != PaintTool.FREE_FORM_SELECT
                && tool != PaintTool.TEXT) {
            // keep selection until another paint tool commits via Paint.setTool / ops
        }

        if ((tool == PaintTool.SELECT || tool == PaintTool.FREE_FORM_SELECT)
                && this.paint.getSelection().isActive()
                && hitSelection(x, y)) {
            this.movingSelection = true;
            this.moveGrabX = x - this.paint.getSelection().getX();
            this.moveGrabY = y - this.paint.getSelection().getY();
            this.drawing = true;
            return;
        }

        switch (tool) {
            case PENCIL -> {
                this.paint.pushUndo();
                this.paint.strokePencil(x, y, x, y, this.rightButton);
                this.drawing = true;
            }
            case BRUSH -> {
                this.paint.pushUndo();
                this.paint.strokeBrush(x, y, x, y, this.rightButton);
                this.drawing = true;
            }
            case ERASER -> {
                this.paint.pushUndo();
                this.paint.strokeEraser(x, y, x, y, this.rightButton);
                this.drawing = true;
            }
            case AIRBRUSH -> {
                this.paint.pushUndo();
                this.paint.strokeAirbrush(x, y, this.rightButton, System.nanoTime());
                this.drawing = true;
            }
            case FILL -> this.paint.fillAt(x, y, this.rightButton);
            case PICK_COLOR -> {
                this.paint.pickAt(x, y, this.rightButton);
                fireToolChanged();
            }
            case MAGNIFIER -> {
                final int[] levels = Paint.ZOOM_LEVELS;
                int idx = 0;
                for (int i = 0; i < levels.length; i++) {
                    if (levels[i] == this.paint.getZoom()) {
                        idx = i;
                        break;
                    }
                }
                if (this.rightButton) {
                    idx = Math.max(0, idx - 1);
                } else {
                    idx = Math.min(levels.length - 1, idx + 1);
                }
                this.paint.setZoom(levels[idx]);
                this.syncPreferredSize();
                this.revalidate();
            }
            case LINE, RECTANGLE, ELLIPSE, ROUNDED_RECTANGLE, SELECT -> this.drawing = true;
            case FREE_FORM_SELECT -> {
                this.freeFormPoints.clear();
                this.freeFormPoints.add(new Point(x, y));
                this.drawing = true;
            }
            case CURVE -> {
                if (this.curvePhase == 0) {
                    this.drawing = true;
                } else if (this.curvePhase == 1) {
                    this.curveC1x = x;
                    this.curveC1y = y;
                    this.drawing = true;
                } else if (this.curvePhase == 2) {
                    this.drawing = true;
                }
            }
            case POLYGON -> {
                if (this.polygonPoints.isEmpty()) {
                    this.polygonPoints.add(new Point(x, y));
                    this.polygonRightButton = this.rightButton;
                }
                this.drawing = true;
                this.currX = x;
                this.currY = y;
            }
            case TEXT -> {
                this.placingText = true;
                this.startX = x;
                this.startY = y;
                this.drawing = true;
            }
            default -> {
            }
        }
        this.repaint();
        fireDocumentChanged();
    }

    private void onDrag(final MouseEvent e) {
        final int x = imgX(e);
        final int y = imgY(e);
        this.shiftDown = e.isShiftDown();
        this.currX = x;
        this.currY = y;
        this.statusListener.accept(x, y);

        if (this.movingSelection) {
            this.paint.moveSelection(x - this.moveGrabX, y - this.moveGrabY);
            this.repaint();
            fireDocumentChanged();
            return;
        }
        if (!this.drawing) {
            this.repaint();
            return;
        }

        final PaintTool tool = this.paint.getTool();
        switch (tool) {
            case PENCIL -> this.paint.strokePencil(this.lastX, this.lastY, x, y, this.rightButton);
            case BRUSH -> this.paint.strokeBrush(this.lastX, this.lastY, x, y, this.rightButton);
            case ERASER -> this.paint.strokeEraser(this.lastX, this.lastY, x, y, this.rightButton);
            case AIRBRUSH -> this.paint.strokeAirbrush(x, y, this.rightButton, System.nanoTime());
            case FREE_FORM_SELECT -> this.freeFormPoints.add(new Point(x, y));
            default -> {
            }
        }
        this.lastX = x;
        this.lastY = y;
        this.repaint();
        fireDocumentChanged();
    }

    private void onRelease(final MouseEvent e) {
        final int x = imgX(e);
        final int y = imgY(e);
        this.shiftDown = e.isShiftDown();
        this.currX = x;
        this.currY = y;

        if (this.movingSelection) {
            this.movingSelection = false;
            this.drawing = false;
            this.repaint();
            fireDocumentChanged();
            return;
        }
        if (!this.drawing) {
            return;
        }

        final PaintTool tool = this.paint.getTool();
        switch (tool) {
            case LINE -> this.paint.drawLineShape(this.startX, this.startY, x, y, this.rightButton, this.shiftDown);
            case RECTANGLE ->
                    this.paint.drawRectangleShape(this.startX, this.startY, x, y, this.shiftDown, this.rightButton);
            case ELLIPSE ->
                    this.paint.drawEllipseShape(this.startX, this.startY, x, y, this.shiftDown, this.rightButton);
            case ROUNDED_RECTANGLE -> this.paint.drawRoundedRectangleShape(
                    this.startX, this.startY, x, y, this.shiftDown, this.rightButton
            );
            case SELECT -> {
                if (Math.abs(x - this.startX) > 1 || Math.abs(y - this.startY) > 1) {
                    this.paint.beginRectangularSelection(this.startX, this.startY, x, y);
                }
            }
            case FREE_FORM_SELECT -> {
                this.freeFormPoints.add(new Point(x, y));
                if (this.freeFormPoints.size() >= 3) {
                    final Polygon poly = new Polygon();
                    for (final Point p : this.freeFormPoints) {
                        poly.addPoint(p.x, p.y);
                    }
                    this.paint.beginFreeFormSelection(poly);
                }
                this.freeFormPoints.clear();
            }
            case CURVE -> {
                if (this.curvePhase == 0) {
                    this.curveX0 = this.startX;
                    this.curveY0 = this.startY;
                    this.curveX1 = x;
                    this.curveY1 = y;
                    this.curveC1x = (this.curveX0 + this.curveX1) / 2;
                    this.curveC1y = (this.curveY0 + this.curveY1) / 2;
                    this.curvePhase = 1;
                } else if (this.curvePhase == 1) {
                    this.curveC1x = x;
                    this.curveC1y = y;
                    this.curvePhase = 2;
                } else {
                    this.paint.drawCurveShape(
                            this.curveX0, this.curveY0, this.curveX1, this.curveY1,
                            this.curveC1x, this.curveC1y, x, y, this.rightButton
                    );
                    this.curvePhase = 0;
                }
            }
            case POLYGON -> {
                final Point first = this.polygonPoints.get(0);
                if (this.polygonPoints.size() >= 2
                        && Math.hypot(x - first.x, y - first.y) <= 5) {
                    finishPolygon();
                } else {
                    this.polygonPoints.add(new Point(x, y));
                }
            }
            case TEXT -> {
                if (this.placingText) {
                    final String text = JOptionPane.showInputDialog(
                            this,
                            "Text:",
                            "Text",
                            JOptionPane.PLAIN_MESSAGE
                    );
                    if (Objects.nonNull(text) && !text.isEmpty()) {
                        this.paint.drawTextAt(text, this.startX, this.startY + this.paint.getTextFont().getSize());
                    }
                    this.placingText = false;
                }
            }
            default -> {
            }
        }
        this.drawing = false;
        this.repaint();
        fireDocumentChanged();
    }

    private void onClick(final MouseEvent e) {
        if (e.getClickCount() >= 2 && this.paint.getTool() == PaintTool.POLYGON
                && this.polygonPoints.size() >= 2) {
            finishPolygon();
            this.repaint();
            fireDocumentChanged();
        }
    }

    private void onMove(final MouseEvent e) {
        final int x = imgX(e);
        final int y = imgY(e);
        this.currX = x;
        this.currY = y;
        this.shiftDown = e.isShiftDown();
        this.statusListener.accept(x, y);
        if (this.curvePhase > 0 || !this.polygonPoints.isEmpty()) {
            this.repaint();
        }
    }

    private void finishPolygon() {
        final int n = this.polygonPoints.size();
        if (n < 2) {
            this.polygonPoints.clear();
            return;
        }
        final int[] xs = new int[n];
        final int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = this.polygonPoints.get(i).x;
            ys[i] = this.polygonPoints.get(i).y;
        }
        this.paint.drawPolygonShape(xs, ys, n, this.polygonRightButton);
        this.polygonPoints.clear();
        this.drawing = false;
    }

    private boolean hitSelection(final int x, final int y) {
        final SelectionModel sel = this.paint.getSelection();
        return x >= sel.getX() && y >= sel.getY()
                && x < sel.getX() + sel.getWidth()
                && y < sel.getY() + sel.getHeight();
    }

    private static boolean isFreehandTool(final PaintTool tool) {
        return tool == PaintTool.PENCIL || tool == PaintTool.BRUSH
                || tool == PaintTool.ERASER || tool == PaintTool.AIRBRUSH;
    }

    private void fireDocumentChanged() {
        // listeners attached by frame via property or direct refresh
        final Object listener = getClientProperty("documentListener");
        if (listener instanceof Runnable r) {
            r.run();
        }
    }

    private void fireToolChanged() {
        final Object listener = getClientProperty("toolListener");
        if (listener instanceof Runnable r) {
            r.run();
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            final int z = Math.max(1, this.paint.getZoom());
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.scale(z, z);
            g2.drawImage(this.paint.getImage(), 0, 0, null);

            final SelectionModel sel = this.paint.getSelection();
            if (sel.isActive()) {
                g2.drawImage(sel.getFloating(), sel.getX(), sel.getY(), null);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(
                        1f / z, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{3f / z, 3f / z}, 0f
                ));
                g2.drawRect(sel.getX(), sel.getY(), sel.getWidth(), sel.getHeight());
            }

            // Rubber-band previews
            g2.setColor(this.paint.getForeground());
            g2.setStroke(new BasicStroke(1f / z));
            if (this.drawing || this.curvePhase > 0 || !this.polygonPoints.isEmpty()) {
                drawPreview(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawPreview(final Graphics2D g2) {
        final PaintTool tool = this.paint.getTool();
        int x1 = this.currX;
        int y1 = this.currY;
        if (this.shiftDown && (tool == PaintTool.LINE || tool == PaintTool.RECTANGLE
                || tool == PaintTool.ELLIPSE || tool == PaintTool.ROUNDED_RECTANGLE)) {
            if (tool == PaintTool.LINE) {
                final int[] c = BitmapOps.constrainLine(this.startX, this.startY, x1, y1);
                x1 = c[0];
                y1 = c[1];
            } else {
                final int[] c = BitmapOps.constrainSquare(this.startX, this.startY, x1, y1);
                x1 = c[0];
                y1 = c[1];
            }
        }
        switch (tool) {
            case LINE -> g2.drawLine(this.startX, this.startY, x1, y1);
            case RECTANGLE, SELECT -> {
                final int x = Math.min(this.startX, x1);
                final int y = Math.min(this.startY, y1);
                g2.drawRect(x, y, Math.abs(x1 - this.startX), Math.abs(y1 - this.startY));
            }
            case ELLIPSE -> {
                final int x = Math.min(this.startX, x1);
                final int y = Math.min(this.startY, y1);
                g2.drawOval(x, y, Math.abs(x1 - this.startX), Math.abs(y1 - this.startY));
            }
            case ROUNDED_RECTANGLE -> {
                final int x = Math.min(this.startX, x1);
                final int y = Math.min(this.startY, y1);
                final int w = Math.abs(x1 - this.startX);
                final int h = Math.abs(y1 - this.startY);
                g2.drawRoundRect(x, y, w, h, Math.min(w, h) / 3, Math.min(w, h) / 3);
            }
            case FREE_FORM_SELECT -> {
                if (this.freeFormPoints.size() >= 2) {
                    for (int i = 1; i < this.freeFormPoints.size(); i++) {
                        final Point a = this.freeFormPoints.get(i - 1);
                        final Point b = this.freeFormPoints.get(i);
                        g2.drawLine(a.x, a.y, b.x, b.y);
                    }
                }
            }
            case CURVE -> {
                if (this.curvePhase == 0 && this.drawing) {
                    g2.drawLine(this.startX, this.startY, this.currX, this.currY);
                } else if (this.curvePhase >= 1) {
                    final Path2D path = new Path2D.Double();
                    path.moveTo(this.curveX0, this.curveY0);
                    final int c2x = this.curvePhase == 2 ? this.currX : this.curveC1x;
                    final int c2y = this.curvePhase == 2 ? this.currY : this.curveC1y;
                    final int c1x = this.curvePhase == 1 ? this.currX : this.curveC1x;
                    final int c1y = this.curvePhase == 1 ? this.currY : this.curveC1y;
                    path.curveTo(c1x, c1y, c2x, c2y, this.curveX1, this.curveY1);
                    g2.draw(path);
                }
            }
            case POLYGON -> {
                if (!this.polygonPoints.isEmpty()) {
                    Point prev = this.polygonPoints.get(0);
                    for (int i = 1; i < this.polygonPoints.size(); i++) {
                        final Point p = this.polygonPoints.get(i);
                        g2.drawLine(prev.x, prev.y, p.x, p.y);
                        prev = p;
                    }
                    g2.drawLine(prev.x, prev.y, this.currX, this.currY);
                }
            }
            case TEXT -> {
                if (this.placingText) {
                    final int x = Math.min(this.startX, this.currX);
                    final int y = Math.min(this.startY, this.currY);
                    g2.drawRect(x, y, Math.abs(this.currX - this.startX), Math.abs(this.currY - this.startY));
                }
            }
            default -> {
            }
        }
    }
}
