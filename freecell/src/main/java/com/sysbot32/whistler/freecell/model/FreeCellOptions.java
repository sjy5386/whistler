package com.sysbot32.whistler.freecell.model;

import com.sysbot32.whistler.config.Config;
import lombok.Getter;

import java.util.Objects;

/**
 * User options persisted via {@link Config} (classic FreeCell Options dialog).
 */
@Getter
public final class FreeCellOptions {
    private static final String KEY_MESSAGES = "options.messagesOnIllegalMoves";
    private static final String KEY_QUICK = "options.quickPlay";
    private static final String KEY_DOUBLE_CLICK = "options.doubleClickToFreeCell";

    private final Config config;

    private boolean messagesOnIllegalMoves;
    private boolean quickPlay;
    private boolean doubleClickToFreeCell;

    public FreeCellOptions(final Config config) {
        this.config = Objects.requireNonNull(config, "config");
        this.messagesOnIllegalMoves = parseBool(config.get(KEY_MESSAGES), true);
        this.quickPlay = parseBool(config.get(KEY_QUICK), true);
        this.doubleClickToFreeCell = parseBool(config.get(KEY_DOUBLE_CLICK), true);
    }

    public void setMessagesOnIllegalMoves(final boolean value) {
        this.messagesOnIllegalMoves = value;
        this.config.set(KEY_MESSAGES, Boolean.toString(value));
        this.config.save();
    }

    public void setQuickPlay(final boolean value) {
        this.quickPlay = value;
        this.config.set(KEY_QUICK, Boolean.toString(value));
        this.config.save();
    }

    public void setDoubleClickToFreeCell(final boolean value) {
        this.doubleClickToFreeCell = value;
        this.config.set(KEY_DOUBLE_CLICK, Boolean.toString(value));
        this.config.save();
    }

    private static boolean parseBool(final String raw, final boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw);
    }
}
