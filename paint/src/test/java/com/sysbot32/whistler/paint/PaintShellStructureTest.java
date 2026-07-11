package com.sysbot32.whistler.paint;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks that the XP shell surface area is present in shipped sources.
 */
class PaintShellStructureTest {
    @Test
    void paintToolEnumListsAllClassicTools() {
        final String names = java.util.Arrays.stream(PaintTool.values())
                .map(PaintTool::displayName)
                .collect(Collectors.joining(","));
        assertTrue(names.contains("Free-Form Select"));
        assertTrue(names.contains("Select"));
        assertTrue(names.contains("Eraser/Color Eraser"));
        assertTrue(names.contains("Fill With Color"));
        assertTrue(names.contains("Pick Color"));
        assertTrue(names.contains("Magnifier"));
        assertTrue(names.contains("Pencil"));
        assertTrue(names.contains("Brush"));
        assertTrue(names.contains("Airbrush"));
        assertTrue(names.contains("Text"));
        assertTrue(names.contains("Line"));
        assertTrue(names.contains("Curve"));
        assertTrue(names.contains("Rectangle"));
        assertTrue(names.contains("Polygon"));
        assertTrue(names.contains("Ellipse"));
        assertTrue(names.contains("Rounded Rectangle"));
    }

    @Test
    void frameSourceDefinesClassicMenusAndColorBox() throws Exception {
        final Path frame = Path.of("src/main/java/com/sysbot32/whistler/paint/PaintFrame.java");
        final String source = Files.readString(frame);
        assertTrue(source.contains("createFileMenu"));
        assertTrue(source.contains("createEditMenu"));
        assertTrue(source.contains("createViewMenu"));
        assertTrue(source.contains("createImageMenu"));
        assertTrue(source.contains("createColorsMenu"));
        assertTrue(source.contains("createHelpMenu"));
        assertTrue(source.contains("ColorPalette.DEFAULT_COLORS"));
        assertTrue(source.contains("fgSwatch"));
        assertTrue(source.contains("bgSwatch"));
        assertTrue(source.contains("PaintTool.values()"));
    }

    @Test
    void applicationMainClassExists() throws Exception {
        final Method main = PaintApplication.class.getMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isStatic(main.getModifiers()));
    }
}
