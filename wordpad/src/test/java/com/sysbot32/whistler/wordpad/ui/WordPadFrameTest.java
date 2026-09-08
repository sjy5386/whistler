package com.sysbot32.whistler.wordpad.ui;

import com.sysbot32.whistler.config.PropertiesConfig;
import com.sysbot32.whistler.wordpad.model.DocumentKind;
import com.sysbot32.whistler.wordpad.model.ParagraphAlignment;
import com.sysbot32.whistler.wordpad.model.WordPad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordPadFrameTest {
    @Test
    void windowIdentifiesAsWordPadWithRichTextAndFormatControls(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        final AtomicReference<WordPadFrame> ref = new AtomicReference<>();
        runEdt(() -> {
            final WordPadFrame frame = newFrame(tempDir);
            ref.set(frame);
            assertTrue(frame.getTitle().contains("WordPad"), frame.getTitle());
            assertTrue(frame.getTitle().contains("Document"), frame.getTitle());
            assertEquals("Document", WordPadFrame.UNTITLED);
            final JTextPane editor = frame.getEditor();
            assertNotNull(editor);
            assertEquals("editor", editor.getName());
            assertInstanceOf(StyledDocument.class, editor.getDocument());
            assertFalse(editor.getDocument().getClass().getName().contains("PlainDocument"));
            assertTrue(frame.editorWrapsAtWindow());
            assertNotNull(frame.getFontFamilyCombo());
            assertEquals("fontFamily", frame.getFontFamilyCombo().getName());
            assertNotNull(frame.getFontSizeCombo());
            assertEquals("fontSize", frame.getFontSizeCombo().getName());
            assertEquals("bold", frame.formatControl("bold").getName());
            assertEquals("italic", frame.formatControl("italic").getName());
            assertEquals("underline", frame.formatControl("underline").getName());
            assertEquals("textColor", frame.formatControl("textColor").getName());
            assertEquals("alignLeft", frame.formatControl("alignLeft").getName());
            assertEquals("alignCenter", frame.formatControl("alignCenter").getName());
            assertEquals("alignRight", frame.formatControl("alignRight").getName());
            assertEquals("bullet", frame.formatControl("bullet").getName());
            assertEquals("standardToolbar", frame.getStandardToolbar().getName());
            assertToolbarUsesIconsNotText(frame.getStandardToolbar());
            assertNotNull(frame.formatControl("bold").getIcon());
            assertEquals("", frame.formatControl("bold").getText());
            assertNotNull(frame.formatControl("alignLeft").getIcon());
            assertEquals("", frame.formatControl("alignLeft").getText());
            assertEquals("ruler", frame.getRuler().getName());
            assertEquals("statusBar", frame.getStatusBar().getName());
            assertTrue(frame.getStatusBar().isVisible());
            frame.dispose();
        });
        assertNotNull(ref.get());
        assertFalse(ref.get().isDisplayable());
    }

    @Test
    void newOpenSaveRoundTripThroughFrame(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        runEdt(() -> {
            try {
                final WordPad model = new WordPad();
                final WordPadFrame frame = new WordPadFrame(model, new PropertiesConfig(tempDir.resolve("cfg.properties")));
                model.insert("Draft");
                model.setBold(0, 5, true);
                model.setAlignment(0, 5, ParagraphAlignment.CENTER);
                final Path file = tempDir.resolve("frame.rtf");
                assertTrue(frame.savePath(file));
                assertFalse(model.isEdited());
                assertEquals(file, model.getPath());

                frame.newDocument(DocumentKind.RICH_TEXT);
                assertEquals("", model.getText());
                assertNull(model.getPath());
                assertFalse(model.isEdited());
                assertEquals(ParagraphAlignment.LEFT, model.getAlignment(0));
                assertTrue(frame.getTitle().contains("WordPad"));
                model.insert("fresh");
                assertEquals(ParagraphAlignment.LEFT, model.getAlignment(0));
                assertEquals(WordPad.DEFAULT_FONT_FAMILY, model.getFontFamily(0));

                frame.openPath(file);
                assertEquals("Draft", trimTrailingBreak(model.getText()));
                assertTrue(model.isBold(0));
                assertEquals(ParagraphAlignment.CENTER, model.getAlignment(0));
                assertFalse(model.isEdited());
                assertTrue(frame.getTitle().contains("frame.rtf"));
                frame.dispose();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void toolbarBulletAppliesToParagraph(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        runEdt(() -> {
            final WordPad model = new WordPad();
            model.insert("item");
            final WordPadFrame frame = new WordPadFrame(model, new PropertiesConfig(tempDir.resolve("cfg.properties")));
            frame.getEditor().setCaretPosition(0);
            frame.formatControl("bullet").doClick();
            assertTrue(model.isBullet(0));
            frame.dispose();
        });
    }

    @Test
    void toolbarBoldAppliesToSelectedText(@TempDir final Path tempDir) throws Exception {
        assumeDisplayOrConstructs();
        runEdt(() -> {
            final WordPad model = new WordPad();
            model.insert("plainBOLD");
            final WordPadFrame frame = new WordPadFrame(model, new PropertiesConfig(tempDir.resolve("cfg.properties")));
            frame.getEditor().select(5, 9);
            frame.formatControl("bold").doClick();
            assertTrue(model.isBold(5));
            assertFalse(model.isBold(0));
            assertTrue(model.isEdited());
            frame.dispose();
        });
    }

    private static void assertToolbarUsesIconsNotText(final JToolBar toolBar) {
        int iconButtons = 0;
        for (int i = 0; i < toolBar.getComponentCount(); i++) {
            if (toolBar.getComponent(i) instanceof AbstractButton button && button.getIcon() != null) {
                assertEquals("", button.getText(), button.getName());
                assertEquals(ToolbarIcons.SIZE, button.getIcon().getIconWidth());
                iconButtons++;
            }
        }
        assertTrue(iconButtons >= 10, "expected standard toolbar glyphs, got " + iconButtons);
    }

    private static WordPadFrame newFrame(final Path tempDir) {
        return new WordPadFrame(new WordPad(), new PropertiesConfig(tempDir.resolve("wordpad.properties")));
    }

    private static void assumeDisplayOrConstructs() {
        // Headless JVMs can still construct a JFrame for structural checks; only skip if that path is impossible.
        if (GraphicsEnvironment.isHeadless()) {
            try {
                final JFrame probe = new JFrame("probe");
                probe.dispose();
            } catch (final Throwable t) {
                org.junit.jupiter.api.Assumptions.assumeFalse(true, "headless frame construction failed: " + t);
            }
        }
    }

    private static void runEdt(final Runnable action) throws InterruptedException, InvocationTargetException {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        SwingUtilities.invokeAndWait(action);
    }

    private static String trimTrailingBreak(final String text) {
        if (text.endsWith("\n")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }
}
