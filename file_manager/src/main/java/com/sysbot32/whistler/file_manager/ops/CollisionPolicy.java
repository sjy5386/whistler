package com.sysbot32.whistler.file_manager.ops;


import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

/**
 * Pure name-collision decisions for disk copy/move.
 * UI supplies a {@link UserChoice} when a target already exists; session state
 * can remember Overwrite All / Skip All.
 */
@UtilityClass
public final class CollisionPolicy {
    public enum UserChoice {
        OVERWRITE,
        OVERWRITE_ALL,
        SKIP,
        SKIP_ALL,
        CANCEL
    }

    public enum Action {
        /** Copy/move this item (overwrite if present). */
        PROCEED,
        /** Leave existing target; skip this source. */
        SKIP,
        /** Abort the whole transfer. */
        ABORT
    }

    public enum Remember {
        NONE,
        OVERWRITE_ALL,
        SKIP_ALL
    }

    public record Decision(Action action, Remember remember) {
    }

    /**
     * Resolve what to do for one source→target pair.
     *
     * @param targetExists whether {@code destDir/sourceName} already exists
     * @param remembered   session memory from a prior All choice
     * @param choice       user answer when {@code targetExists} and no All memory;
     *                     ignored when target does not exist or memory applies.
     *                     May be {@code null} only when no prompt is required.
     */
    public static Decision resolve(
            final boolean targetExists,
            final Remember remembered,
            final UserChoice choice
    ) {
        final Remember mem = remembered == null ? Remember.NONE : remembered;
        if (!targetExists) {
            return new Decision(Action.PROCEED, mem);
        }
        if (mem == Remember.OVERWRITE_ALL) {
            return new Decision(Action.PROCEED, mem);
        }
        if (mem == Remember.SKIP_ALL) {
            return new Decision(Action.SKIP, mem);
        }
        if (choice == null) {
            throw new IllegalArgumentException("User choice required when target exists");
        }
        return switch (choice) {
            case OVERWRITE -> new Decision(Action.PROCEED, Remember.NONE);
            case OVERWRITE_ALL -> new Decision(Action.PROCEED, Remember.OVERWRITE_ALL);
            case SKIP -> new Decision(Action.SKIP, Remember.NONE);
            case SKIP_ALL -> new Decision(Action.SKIP, Remember.SKIP_ALL);
            case CANCEL -> new Decision(Action.ABORT, Remember.NONE);
        };
    }

    /** Mutable session wrapper around {@link #resolve}. */
    @Getter
    @Accessors(fluent = true)
    public static final class Session {
        private Remember remember = Remember.NONE;

        public Decision decide(final boolean targetExists, final UserChoice choice) {
            final Decision d = resolve(targetExists, this.remember, choice);
            this.remember = d.remember();
            return d;
        }
    }
}
