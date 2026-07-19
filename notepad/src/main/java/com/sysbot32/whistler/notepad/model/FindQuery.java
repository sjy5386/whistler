package com.sysbot32.whistler.notepad.model;

import lombok.Getter;

import java.util.Objects;

/**
 * Active find parameters. When the Find dialog is open, live controls win over cached values
 * so F3 / Edit → Find Next honor Up/Down and Match case without requiring another dialog click.
 */
@Getter
public final class FindQuery {
    private final String text;
    private final boolean matchCase;
    private final SearchDirection direction;

    public FindQuery(final String text, final boolean matchCase, final SearchDirection direction) {
        this.text = Objects.requireNonNullElse(text, "");
        this.matchCase = matchCase;
        this.direction = Objects.requireNonNullElse(direction, SearchDirection.DOWN);
    }

    /**
     * Prefer live Find dialog values when that dialog is visible; otherwise keep the cache.
     */
    public static FindQuery resolve(
            final FindQuery cached,
            final boolean findDialogVisible,
            final String liveText,
            final boolean liveMatchCase,
            final SearchDirection liveDirection
    ) {
        final FindQuery base = Objects.requireNonNullElseGet(
                cached,
                () -> new FindQuery("", false, SearchDirection.DOWN)
        );
        if (!findDialogVisible) {
            return base;
        }
        return new FindQuery(liveText, liveMatchCase, liveDirection);
    }
}
