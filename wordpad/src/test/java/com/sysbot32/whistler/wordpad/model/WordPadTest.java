package com.sysbot32.whistler.wordpad.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordPadTest {
    private static final String SAMPLE =
            "HelloBoldItalicUnderFontColor\nLEFT\nCENTER\nRIGHT";

    @Test
    void defaultConstructorIsEmptyUntitledAndClean() {
        final WordPad wordPad = new WordPad();
        assertEquals("", wordPad.getText());
        assertEquals(0, wordPad.getLength());
        assertNull(wordPad.getPath());
        assertFalse(wordPad.isEdited());
    }

    @Test
    void insertMarksDirtyAndKeepsCharacters() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("wrapping line of text");
        assertEquals("wrapping line of text", wordPad.getText());
        assertTrue(wordPad.isEdited());
    }

    @Test
    void characterAndParagraphFormattingStickToRanges() {
        final WordPad wordPad = formattedSample();

        assertFalse(wordPad.isBold(indexOf(SAMPLE, "Hello")));
        assertTrue(wordPad.isBold(indexOf(SAMPLE, "Bold")));
        assertTrue(wordPad.isItalic(indexOf(SAMPLE, "Italic")));
        assertTrue(wordPad.isUnderline(indexOf(SAMPLE, "Under")));
        assertEquals("SansSerif", wordPad.getFontFamily(indexOf(SAMPLE, "Font")));
        assertEquals(22, wordPad.getFontSize(indexOf(SAMPLE, "Font")));
        assertEquals(Color.BLUE.getRGB(), wordPad.getForeground(indexOf(SAMPLE, "Color")).getRGB());

        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(indexOf(SAMPLE, "LEFT")));
        assertEquals(ParagraphAlignment.CENTER, wordPad.getAlignment(indexOf(SAMPLE, "CENTER")));
        assertEquals(ParagraphAlignment.RIGHT, wordPad.getAlignment(indexOf(SAMPLE, "RIGHT")));
    }

    @Test
    void formattingSurvivesLaterEdits() {
        final WordPad wordPad = formattedSample();
        wordPad.insert(" and more");
        assertTrue(wordPad.getText().startsWith(SAMPLE));
        assertTrue(wordPad.isBold(indexOf(SAMPLE, "Bold")));
        assertTrue(wordPad.isItalic(indexOf(SAMPLE, "Italic")));
        assertEquals(ParagraphAlignment.CENTER, wordPad.getAlignment(indexOf(SAMPLE, "CENTER")));
        assertEquals("SansSerif", wordPad.getFontFamily(indexOf(SAMPLE, "Font")));
    }

    @Test
    void rtfRoundTripPreservesTextAndAttributes(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("letter.rtf");
        final WordPad writer = formattedSample();
        assertTrue(writer.isEdited());
        writer.save(file);
        assertFalse(writer.isEdited());
        assertEquals(file, writer.getPath());
        assertTrue(Files.size(file) > 0);

        final WordPad reader = new WordPad();
        reader.insert("stale");
        reader.setEdited(true);
        reader.open(file);

        assertEquals(file, reader.getPath());
        assertFalse(reader.isEdited());
        assertEquals(SAMPLE, trimTrailingBreak(reader.getText()));

        assertTrue(reader.isBold(indexOf(SAMPLE, "Bold")));
        assertFalse(reader.isBold(indexOf(SAMPLE, "Hello")));
        assertTrue(reader.isItalic(indexOf(SAMPLE, "Italic")));
        assertTrue(reader.isUnderline(indexOf(SAMPLE, "Under")));
        assertEquals("SansSerif", reader.getFontFamily(indexOf(SAMPLE, "Font")));
        assertEquals(22, reader.getFontSize(indexOf(SAMPLE, "Font")));
        assertEquals(Color.BLUE.getRGB(), reader.getForeground(indexOf(SAMPLE, "Color")).getRGB());
        assertEquals(ParagraphAlignment.LEFT, reader.getAlignment(indexOf(SAMPLE, "LEFT")));
        assertEquals(ParagraphAlignment.CENTER, reader.getAlignment(indexOf(SAMPLE, "CENTER")));
        assertEquals(ParagraphAlignment.RIGHT, reader.getAlignment(indexOf(SAMPLE, "RIGHT")));
    }

    @Test
    void txtRoundTripPreservesCharacters(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("notes.txt");
        final String body = "plain wrap text\n한글 메모";
        final WordPad writer = new WordPad();
        writer.insert(body);
        writer.setBold(0, 5, true);
        writer.save(file);
        assertFalse(writer.isEdited());
        assertEquals(body, Files.readString(file));

        final WordPad reader = new WordPad();
        reader.open(file);
        assertEquals(body, reader.getText());
        assertEquals(file, reader.getPath());
        assertFalse(reader.isEdited());
    }

    @Test
    void createNewClearsContentPathAndDirty(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("draft.rtf");
        final WordPad wordPad = formattedSample();
        wordPad.save(file);
        wordPad.insert(" extra");
        assertTrue(wordPad.isEdited());

        wordPad.createNew();
        assertEquals("", wordPad.getText());
        assertNull(wordPad.getPath());
        assertFalse(wordPad.isEdited());
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
        wordPad.insert("fresh");
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
        assertFalse(wordPad.isBold(0));
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
    }

    @Test
    void createNewResetsCenterAndRightAlignmentBeforeInsert() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("centered then right");
        wordPad.setAlignment(0, wordPad.getLength(), ParagraphAlignment.CENTER);
        assertEquals(ParagraphAlignment.CENTER, wordPad.getAlignment(0));
        wordPad.createNew();
        assertEquals("", wordPad.getText());
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));

        wordPad.insert("after new");
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));

        wordPad.setAlignment(0, wordPad.getLength(), ParagraphAlignment.RIGHT);
        wordPad.createNew();
        wordPad.insert("again");
        assertEquals("again", wordPad.getText());
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
    }

    @Test
    void openTxtDoesNotKeepPreviousParagraphAlignment(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("plain.txt");
        Files.writeString(file, "plain body");

        final WordPad wordPad = new WordPad();
        wordPad.insert("was center");
        wordPad.setAlignment(0, wordPad.getLength(), ParagraphAlignment.CENTER);
        wordPad.open(file);
        assertEquals("plain body", wordPad.getText());
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
        assertFalse(wordPad.isEdited());

        wordPad.insert("\nmore");
        wordPad.setAlignment(0, wordPad.getLength(), ParagraphAlignment.RIGHT);
        wordPad.open(file);
        assertEquals("plain body", wordPad.getText());
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
    }

    @Test
    void createNewThenInsertUsesDefaultSerifLikeFreshDocument() {
        final WordPad fresh = new WordPad();
        fresh.insert("fresh insert");
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, fresh.getFontFamily(0));

        final WordPad wordPad = new WordPad();
        wordPad.insert("SansSerif then new");
        wordPad.setFontFamily(0, wordPad.getLength(), "SansSerif");
        wordPad.setAlignment(0, wordPad.getLength(), ParagraphAlignment.CENTER);
        wordPad.createNew();
        wordPad.insert("after new");
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
        assertEquals(fresh.getFontFamily(0), wordPad.getFontFamily(0));
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
    }

    @Test
    void openTxtThenInsertUsesDefaultSerif(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("notes.txt");
        Files.writeString(file, "loaded");
        final WordPad wordPad = new WordPad();
        wordPad.insert("prior");
        wordPad.setFontFamily(0, wordPad.getLength(), "SansSerif");
        wordPad.setAlignment(0, wordPad.getLength(), ParagraphAlignment.RIGHT);
        wordPad.open(file);
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
        wordPad.insert(" more");
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(wordPad.getLength() - 1));
        assertEquals(ParagraphAlignment.LEFT, wordPad.getAlignment(0));
    }

    @Test
    void constructorOpenLoadsFileWithoutDirtyFlag(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("from-args.txt");
        Files.writeString(file, "from args");
        final WordPad wordPad = new WordPad(file);
        assertEquals("from args", wordPad.getText());
        assertEquals(file, wordPad.getPath());
        assertFalse(wordPad.isEdited());
    }

    @Test
    void defaultFontIsArialTen() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("Aa");
        assertEquals("Arial", WordPad.DEFAULT_FONT_FAMILY);
        assertEquals(10, WordPad.DEFAULT_FONT_SIZE);
        assertEquals(WordPad.DEFAULT_FONT_FAMILY, wordPad.getFontFamily(0));
        assertEquals(WordPad.DEFAULT_FONT_SIZE, wordPad.getFontSize(0));
        assertEquals(DocumentKind.RICH_TEXT, wordPad.getKind());
    }

    @Test
    void createNewTextKindIsPlainAndEmpty() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("gone");
        wordPad.createNew(DocumentKind.TEXT);
        assertEquals("", wordPad.getText());
        assertEquals(DocumentKind.TEXT, wordPad.getKind());
        assertNull(wordPad.getPath());
        assertFalse(wordPad.isEdited());
    }

    @Test
    void findAndReplaceAllDriveShippedDocument() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("one two one Two");
        assertEquals(0, wordPad.find("one", 0, true, false));
        assertEquals(8, wordPad.find("one", 1, true, false));
        assertEquals(-1, wordPad.find("ONE", 0, true, false));
        assertEquals(0, wordPad.find("ONE", 0, false, false));
        assertEquals(4, wordPad.find("two", 0, false, true));
        assertEquals(-1, wordPad.find("tw", 0, false, true));
        final int count = wordPad.replaceAll("one", "1", false, false);
        assertEquals(2, count);
        assertEquals("1 two 1 Two", wordPad.getText());
    }

    @Test
    void bulletTogglePrefixesParagraphAndSetsIndent() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("item");
        wordPad.setBullet(0, 4, true);
        assertTrue(wordPad.getText().startsWith("\u2022"));
        assertTrue(wordPad.isBullet(0));
        assertTrue(wordPad.getLeftIndent(0) > 0);
        wordPad.setBullet(0, wordPad.getLength(), false);
        assertEquals("item", wordPad.getText());
        assertFalse(wordPad.isBullet(0));
    }

    @Test
    void unicodeTextRoundTripUsesBom(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("uni.txt");
        final WordPad writer = new WordPad();
        writer.insert("한글 Unicode");
        writer.save(file, DocumentKind.UNICODE_TEXT);
        final byte[] bytes = Files.readAllBytes(file);
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals((byte) 0xFE, bytes[1]);
        final WordPad reader = new WordPad();
        reader.open(file);
        assertEquals("한글 Unicode", reader.getText());
        assertEquals(DocumentKind.UNICODE_TEXT, reader.getKind());
    }

    @Test
    void hasCharacterFormattingDetectsBold() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("plain");
        assertFalse(wordPad.hasCharacterFormatting());
        wordPad.setBold(0, 5, true);
        assertTrue(wordPad.hasCharacterFormatting());
    }

    @Test
    void dateTimeFormatsAreNonEmpty() {
        assertFalse(WordPad.dateTimeFormats(java.time.LocalDateTime.of(2001, 1, 2, 15, 4)).isEmpty());
    }

    @Test
    void paragraphIndentsAndTabsStick() {
        final WordPad wordPad = new WordPad();
        wordPad.insert("indented");
        wordPad.setParagraphIndents(0, 8, 36f, 12f, 18f);
        assertEquals(36f, wordPad.getLeftIndent(0), 0.01f);
        assertEquals(12f, wordPad.getRightIndent(0), 0.01f);
        assertEquals(18f, wordPad.getFirstLineIndent(0), 0.01f);
        wordPad.setTabStops(0, 8, 72f, 144f);
        assertEquals(2, wordPad.getTabStops(0).length);
        assertEquals(72f, wordPad.getTabStops(0)[0], 0.01f);
    }

    @Test
    void saveThenOpenOnFreshInstanceClearsDirty(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("memo.txt");
        final WordPad writer = new WordPad();
        writer.insert("memo body");
        writer.save(file);
        assertFalse(writer.isEdited());

        final WordPad reader = new WordPad();
        reader.open(file);
        assertEquals("memo body", reader.getText());
        assertFalse(reader.isEdited());
        assertEquals(file, reader.getPath());
    }

    private static WordPad formattedSample() {
        final WordPad wordPad = new WordPad();
        wordPad.insert(SAMPLE);
        applyRange(wordPad, "Bold", (doc, start, end) -> doc.setBold(start, end, true));
        applyRange(wordPad, "Italic", (doc, start, end) -> doc.setItalic(start, end, true));
        applyRange(wordPad, "Under", (doc, start, end) -> doc.setUnderline(start, end, true));
        applyRange(wordPad, "Font", (doc, start, end) -> {
            doc.setFontFamily(start, end, "SansSerif");
            doc.setFontSize(start, end, 22);
        });
        applyRange(wordPad, "Color", (doc, start, end) -> doc.setForeground(start, end, Color.BLUE));
        applyRange(wordPad, "LEFT", (doc, start, end) -> doc.setAlignment(start, end, ParagraphAlignment.LEFT));
        applyRange(wordPad, "CENTER", (doc, start, end) -> doc.setAlignment(start, end, ParagraphAlignment.CENTER));
        applyRange(wordPad, "RIGHT", (doc, start, end) -> doc.setAlignment(start, end, ParagraphAlignment.RIGHT));
        return wordPad;
    }

    private static int indexOf(final String haystack, final String needle) {
        final int index = haystack.indexOf(needle);
        assertTrue(index >= 0, "missing " + needle);
        return index;
    }

    private static void applyRange(final WordPad wordPad, final String token, final RangeOp op) {
        final int start = indexOf(SAMPLE, token);
        op.apply(wordPad, start, start + token.length());
    }

    private static String trimTrailingBreak(final String text) {
        if (text.endsWith("\n")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    @FunctionalInterface
    private interface RangeOp {
        void apply(WordPad wordPad, int start, int end);
    }
}
