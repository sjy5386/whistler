package com.sysbot32.whistler.notepad.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderFooterTest {
    private static final LocalDateTime FIXED = LocalDateTime.of(2001, 8, 24, 15, 5);

    @Test
    void expandsFilePageDateTimeCodes() {
        final String expanded = HeaderFooter.expand("&f - Page &p - &d &t", "notes.txt", 3, FIXED);
        assertTrue(expanded.startsWith("notes.txt - Page 3 - "));
        assertTrue(expanded.contains("8/24/2001"));
        assertTrue(expanded.toLowerCase().contains("3:05"));
    }

    @Test
    void dropsAlignmentMarkers() {
        assertEquals("Title", HeaderFooter.expand("&lTitle", "x", 1, FIXED));
        assertEquals("Title", HeaderFooter.expand("&cTitle", "x", 1, FIXED));
        assertEquals("Title", HeaderFooter.expand("&rTitle", "x", 1, FIXED));
    }

    @Test
    void doublesAmpersandPrintsLiteral() {
        assertEquals("&", HeaderFooter.expand("&&", "x", 1, FIXED));
    }

    @Test
    void emptyTemplateIsEmpty() {
        assertEquals("", HeaderFooter.expand("", "x", 1, FIXED));
        assertEquals("", HeaderFooter.expand(null, "x", 1, FIXED));
    }

    @Test
    void toPrintMessageFormatUsesPagePlaceholder() {
        final String pattern = HeaderFooter.toPrintMessageFormat("Page &p of &f", "doc.txt", FIXED);
        assertEquals("Page {0} of doc.txt", pattern);
        assertFalse(pattern.contains("&p"));
    }
}
