package com.sysbot32.whistler.notepad.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineColumnTest {
    @Test
    void startOfDocumentIsLine1Col1() {
        assertEquals(new LineColumn(1, 1), LineColumn.fromOffset("hello", 0));
        assertEquals("Ln 1, Col 1", LineColumn.fromOffset("", 0).toStatusText());
    }

    @Test
    void columnAdvancesWithinLine() {
        assertEquals(new LineColumn(1, 4), LineColumn.fromOffset("abcdef", 3));
    }

    @Test
    void newlineAdvancesLineAndResetsColumn() {
        final String text = "ab\ncde";
        // caret after 'c' → line 2 col 2
        assertEquals(new LineColumn(2, 1), LineColumn.fromOffset(text, 3)); // at 'c'
        assertEquals(new LineColumn(2, 2), LineColumn.fromOffset(text, 4)); // after 'c'
        assertEquals(new LineColumn(2, 4), LineColumn.fromOffset(text, 6)); // end
    }

    @Test
    void crlfTreatedAsSingleLineBreak() {
        final String text = "ab\r\ncd";
        assertEquals(new LineColumn(2, 1), LineColumn.fromOffset(text, 4)); // at 'c'
        assertEquals(new LineColumn(2, 3), LineColumn.fromOffset(text, 6)); // end
    }

    @Test
    void caretPastEndIsClamped() {
        assertEquals(new LineColumn(1, 6), LineColumn.fromOffset("hello", 99));
    }
}
