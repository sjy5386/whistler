package com.sysbot32.whistler.paint;

import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ToolIconsTest {
    @Test
    void everyToolHasCachedIconOfExpectedSize() {
        for (final PaintTool tool : PaintTool.values()) {
            final ImageIcon icon = ToolIcons.icon(tool);
            assertNotNull(icon);
            assertEquals(ToolIcons.SIZE, icon.getIconWidth());
            assertEquals(ToolIcons.SIZE, icon.getIconHeight());
            assertSame(icon, ToolIcons.icon(tool));
        }
    }
}
