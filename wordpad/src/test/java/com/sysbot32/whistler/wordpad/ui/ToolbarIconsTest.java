package com.sysbot32.whistler.wordpad.ui;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ToolbarIconsTest {
    @Test
    void everyGlyphHasCachedIconOfExpectedSize() {
        for (final ToolbarIcons.Glyph glyph : ToolbarIcons.Glyph.values()) {
            final ImageIcon icon = ToolbarIcons.icon(glyph);
            assertNotNull(icon);
            assertEquals(ToolbarIcons.SIZE, icon.getIconWidth());
            assertEquals(ToolbarIcons.SIZE, icon.getIconHeight());
            assertSame(icon, ToolbarIcons.icon(glyph));
        }
    }
}
