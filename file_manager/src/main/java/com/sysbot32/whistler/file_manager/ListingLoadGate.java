package com.sysbot32.whistler.file_manager;

import lombok.Getter;

/**
 * Coordinates async listing loads so only the latest navigation/refresh wins.
 * Each {@link #nextToken()} invalidates all previous in-flight loads.
 */
public final class ListingLoadGate {
    @Getter
    private long generation;

    /**
     * Begin a new load for the current location. Returns a token that must be
     * checked with {@link #isCurrent(long)} before applying results.
     */
    public long nextToken() {
        return ++this.generation;
    }

    /** {@code true} if {@code token} is still the latest load. */
    public boolean isCurrent(final long token) {
        return token == this.generation;
    }
}
