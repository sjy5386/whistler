package com.sysbot32.whistler.wordpad;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WordPadShellStructureTest {
    @Test
    void applicationMainClassExists() throws Exception {
        final Method main = WordPadApplication.class.getMethod("main", String[].class);
        assertTrue(Modifier.isStatic(main.getModifiers()));
    }

    @Test
    void frameSourceWiresFileActionsToDocumentOperations() throws Exception {
        final Path frame = Path.of("src/main/java/com/sysbot32/whistler/wordpad/ui/WordPadFrame.java");
        final String source = Files.readString(frame);
        assertTrue(source.contains("createFileMenu"));
        assertTrue(source.contains("menuItem(\"New...\""));
        assertTrue(source.contains("menuItem(\"Open...\""));
        assertTrue(source.contains("menuItem(\"Save\""));
        assertTrue(source.contains("menuItem(\"Save As...\""));
        assertTrue(source.contains("menuItem(\"Print...\""));
        assertTrue(source.contains("newMenuItem.addActionListener(e -> this.newDocument())"));
        assertTrue(source.contains("openMenuItem.addActionListener(e -> this.openDocument())"));
        assertTrue(source.contains("saveMenuItem.addActionListener(e -> this.saveDocument(false))"));
        assertTrue(source.contains("saveAsMenuItem.addActionListener(e -> this.saveDocument(true))"));
        assertTrue(source.contains("this.wordPad.createNew(kind)"));
        assertTrue(source.contains("this.wordPad.open(path)"));
        assertTrue(source.contains("this.wordPad.save(path"));
        assertTrue(source.contains("createViewMenu"));
        assertTrue(source.contains("createInsertMenu"));
        assertTrue(source.contains("Find..."));
        assertTrue(source.contains("Date and Time..."));
        assertTrue(source.contains("Bullet Style"));
        assertTrue(source.contains("Paragraph..."));
        assertTrue(!source.contains("throw new UnsupportedOperationException"));
    }

    @Test
    void frameSourceExposesRichTextSurfaceAndFormatControls() throws Exception {
        final Path frame = Path.of("src/main/java/com/sysbot32/whistler/wordpad/ui/WordPadFrame.java");
        final String source = Files.readString(frame);
        assertTrue(source.contains("new JTextPane(wordPad.getDocument())"));
        assertTrue(source.contains("getScrollableTracksViewportWidth"));
        assertTrue(!source.contains("new JTextArea"));
        assertTrue(source.contains("setName(\"editor\")"));
        assertTrue(source.contains("setName(\"fontFamily\")"));
        assertTrue(source.contains("setName(\"fontSize\")"));
        assertTrue(source.contains("ToolbarIcons.Glyph.BOLD"));
        assertTrue(source.contains("ToolbarIcons.Glyph.ITALIC"));
        assertTrue(source.contains("ToolbarIcons.Glyph.UNDERLINE"));
        assertTrue(source.contains("setName(\"textColor\")"));
        assertTrue(source.contains("ToolbarIcons.Glyph.ALIGN_LEFT"));
        assertTrue(source.contains("ToolbarIcons.Glyph.ALIGN_CENTER"));
        assertTrue(source.contains("ToolbarIcons.Glyph.ALIGN_RIGHT"));
        assertTrue(source.contains("static final String TITLE = \"WordPad\""));
        assertTrue(source.contains("static final String UNTITLED = \"Document\""));
        assertTrue(source.contains("ToolbarIcons.Glyph.BULLETS"));
        assertTrue(source.contains("ToolbarIcons.icon("));
        assertTrue(source.contains("setName(\"standardToolbar\")"));
        assertTrue(source.contains("For Help, press F1"));
        final String ruler = Files.readString(Path.of("src/main/java/com/sysbot32/whistler/wordpad/ui/RulerBar.java"));
        assertTrue(ruler.contains("setName(\"ruler\")"));
    }
}
