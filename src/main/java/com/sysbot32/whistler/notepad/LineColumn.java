package com.sysbot32.whistler.notepad;

import java.util.Objects;

/**
 * 1-based line/column as shown on classic Notepad status bar ("Ln n, Col n").
 */
public final class LineColumn {
    private final int line;
    private final int column;

    public LineColumn(final int line, final int column) {
        this.line = line;
        this.column = column;
    }

    public String toStatusText() {
        return "Ln " + this.line + ", Col " + this.column;
    }

    /**
     * @param content full document text
     * @param caretOffset caret index in {@code content} (0-based, clamped)
     */
    public static LineColumn fromOffset(final String content, final int caretOffset) {
        final String text = Objects.requireNonNullElse(content, "");
        final int offset = Math.max(0, Math.min(caretOffset, text.length()));
        int line = 1;
        int column = 1;
        for (int i = 0; i < offset; i++) {
            final char ch = text.charAt(i);
            if (ch == '\n') {
                line++;
                column = 1;
            } else if (ch == '\r') {
                // treat CR as newline for classic CRLF docs; skip LF pair
                if (i + 1 < offset && text.charAt(i + 1) == '\n') {
                    continue;
                }
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new LineColumn(line, column);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LineColumn that)) {
            return false;
        }
        return this.line == that.line && this.column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.line, this.column);
    }

    @Override
    public String toString() {
        return this.toStatusText();
    }
}
