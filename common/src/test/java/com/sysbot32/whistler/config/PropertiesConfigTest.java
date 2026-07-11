package com.sysbot32.whistler.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PropertiesConfigTest {
    @Test
    void saveAndReloadRestoresWordWrapAndFontPrefs(@TempDir final Path tempDir) {
        final Path file = tempDir.resolve("whistler.properties");

        final PropertiesConfig written = new PropertiesConfig(file);
        written.set("wordWrap", "true");
        written.set("fontName", "Dialog");
        written.set("fontStyle", "1");
        written.set("fontSize", "18");
        written.set("foreground", "#112233");
        written.set("background", "#fefefe");
        written.save();

        final PropertiesConfig loaded = new PropertiesConfig(file);
        assertEquals("true", loaded.get("wordWrap"));
        assertEquals("Dialog", loaded.get("fontName"));
        assertEquals("1", loaded.get("fontStyle"));
        assertEquals("18", loaded.get("fontSize"));
        assertEquals("#112233", loaded.get("foreground"));
        assertEquals("#fefefe", loaded.get("background"));
        assertEquals("fallback", loaded.get("missing", "fallback"));
        assertNull(loaded.get("missing"));
    }
}
