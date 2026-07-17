package com.sysbot32.whistler.file_manager;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared cancel + progress counters for long-running copy/move/delete/add/extract.
 * Checked between items (or by callers between chunks).
 */
public final class TransferControl {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private volatile int completed;
    private volatile int total;
    private volatile String detail = "";

    public void cancel() {
        this.cancelled.set(true);
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }

    public void throwIfCancelled() {
        if (this.cancelled.get()) {
            throw new CancellationException("Cancelled");
        }
    }

    public void begin(final int totalItems) {
        this.total = Math.max(0, totalItems);
        this.completed = 0;
        this.detail = "";
    }

    public void setDetail(final String detail) {
        this.detail = detail == null ? "" : detail;
    }

    /** Mark one unit complete after work; throws if cancelled before increment. */
    public void advance(final String detail) {
        throwIfCancelled();
        this.completed++;
        setDetail(detail);
    }

    public int completed() {
        return this.completed;
    }

    public int total() {
        return this.total;
    }

    public String detail() {
        return this.detail;
    }

    /** 0–100, or -1 when total is unknown/zero. */
    public int percent() {
        if (this.total <= 0) {
            return -1;
        }
        return Math.min(100, (int) Math.round(100.0 * this.completed / this.total));
    }
}
