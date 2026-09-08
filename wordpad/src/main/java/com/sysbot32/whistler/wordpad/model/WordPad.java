package com.sysbot32.whistler.wordpad.model;

import lombok.Getter;
import lombok.Setter;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Styled rich-text document with RTF / plain-text persistence and dirty tracking.
 */
@Getter
public class WordPad {
    public static final String DEFAULT_FONT_FAMILY = "Arial";
    public static final int DEFAULT_FONT_SIZE = 10;
    public static final Color DEFAULT_FOREGROUND = Color.BLACK;
    public static final String BULLET_PREFIX = "\u2022\t";
    public static final float BULLET_LEFT_INDENT = 36f;
    public static final float BULLET_FIRST_LINE_INDENT = -18f;

    private final DefaultStyledDocument document = new DefaultStyledDocument();
    @Setter
    private boolean edited = false;
    private Path path = null;
    private boolean loading = false;
    @Setter
    private DocumentKind kind = DocumentKind.RICH_TEXT;

    public WordPad() {
        this.applyDefaultStyle();
        this.document.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(final DocumentEvent e) {
                WordPad.this.markEdited();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                WordPad.this.markEdited();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                WordPad.this.markEdited();
            }
        });
    }

    public WordPad(final Path path) {
        this();
        try {
            this.open(path);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to open document: " + path, e);
        }
    }

    public void createNew() {
        this.createNew(DocumentKind.RICH_TEXT);
    }

    public void createNew(final DocumentKind kind) {
        this.loading = true;
        try {
            this.clearContent();
            this.resetRunAttributes();
            this.applyDefaultStyle();
        } finally {
            this.loading = false;
        }
        this.kind = Objects.requireNonNull(kind, "kind");
        this.path = null;
        this.edited = false;
    }

    public void open(final Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        this.loading = true;
        try {
            this.clearContent();
            this.resetRunAttributes();
            if (shouldReadAsPlainText(path)) {
                this.insertSilently(0, readPlainText(path));
            } else {
                final RTFEditorKit kit = new RTFEditorKit();
                try (InputStream in = Files.newInputStream(path)) {
                    kit.read(in, this.document, 0);
                }
            }
            this.kind = detectKind(path, shouldReadAsPlainText(path));
            this.path = path;
            this.edited = false;
        } catch (final BadLocationException e) {
            throw new IOException("Failed to open document: " + path, e);
        } finally {
            this.loading = false;
        }
    }

    public void save(final Path path) throws IOException {
        this.save(path, this.kindForPath(path));
    }

    public void save(final Path path, final DocumentKind kind) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(kind, "kind");
        if (kind.isPlainText()) {
            final Charset charset = kind == DocumentKind.UNICODE_TEXT
                    ? StandardCharsets.UTF_16LE
                    : StandardCharsets.UTF_8;
            byte[] bytes = this.getText().getBytes(charset);
            if (kind == DocumentKind.UNICODE_TEXT) {
                final byte[] bom = {(byte) 0xFF, (byte) 0xFE};
                final byte[] withBom = new byte[bom.length + bytes.length];
                System.arraycopy(bom, 0, withBom, 0, bom.length);
                System.arraycopy(bytes, 0, withBom, bom.length, bytes.length);
                bytes = withBom;
            }
            Files.write(path, bytes);
        } else {
            final RTFEditorKit kit = new RTFEditorKit();
            try (OutputStream out = Files.newOutputStream(path)) {
                kit.write(out, this.document, 0, this.document.getLength());
            } catch (final BadLocationException e) {
                throw new IOException("Failed to write RTF: " + path, e);
            }
        }
        this.kind = kind;
        this.path = path;
        this.edited = false;
    }

    public String getText() {
        try {
            return this.document.getText(0, this.document.getLength());
        } catch (final BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    public int getLength() {
        return this.document.getLength();
    }

    public void insert(final String text) {
        this.insert(this.document.getLength(), text);
    }

    public void insert(final int offset, final String text) {
        Objects.requireNonNull(text, "text");
        try {
            this.document.insertString(offset, text, null);
        } catch (final BadLocationException e) {
            throw new IllegalArgumentException("Cannot insert at offset " + offset, e);
        }
    }

    public void setBold(final int start, final int end, final boolean bold) {
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBold(attrs, bold);
        this.applyCharacterAttributes(start, end, attrs);
    }

    public void setItalic(final int start, final int end, final boolean italic) {
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setItalic(attrs, italic);
        this.applyCharacterAttributes(start, end, attrs);
    }

    public void setUnderline(final int start, final int end, final boolean underline) {
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setUnderline(attrs, underline);
        this.applyCharacterAttributes(start, end, attrs);
    }

    public void setFontFamily(final int start, final int end, final String fontFamily) {
        Objects.requireNonNull(fontFamily, "fontFamily");
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, fontFamily);
        this.applyCharacterAttributes(start, end, attrs);
    }

    public void setFontSize(final int start, final int end, final int fontSize) {
        if (fontSize < 1) {
            throw new IllegalArgumentException("fontSize must be positive: " + fontSize);
        }
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontSize(attrs, fontSize);
        this.applyCharacterAttributes(start, end, attrs);
    }

    public void setForeground(final int start, final int end, final Color color) {
        Objects.requireNonNull(color, "color");
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        this.applyCharacterAttributes(start, end, attrs);
    }

    public void setAlignment(final int start, final int end, final ParagraphAlignment alignment) {
        Objects.requireNonNull(alignment, "alignment");
        this.requireRange(start, end);
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setAlignment(attrs, alignment.getSwingConstant());
        this.document.setParagraphAttributes(start, Math.max(end - start, 0), attrs, false);
        this.markEdited();
    }

    public boolean isBold(final int offset) {
        return StyleConstants.isBold(this.characterAttributes(offset));
    }

    public boolean isItalic(final int offset) {
        return StyleConstants.isItalic(this.characterAttributes(offset));
    }

    public boolean isUnderline(final int offset) {
        return StyleConstants.isUnderline(this.characterAttributes(offset));
    }

    public String getFontFamily(final int offset) {
        return StyleConstants.getFontFamily(this.characterAttributes(offset));
    }

    public int getFontSize(final int offset) {
        return StyleConstants.getFontSize(this.characterAttributes(offset));
    }

    public Color getForeground(final int offset) {
        return StyleConstants.getForeground(this.characterAttributes(offset));
    }

    public ParagraphAlignment getAlignment(final int offset) {
        return ParagraphAlignment.fromSwing(
                StyleConstants.getAlignment(this.document.getParagraphElement(this.clampOffset(offset)).getAttributes())
        );
    }

    public void setParagraphIndents(
            final int start,
            final int end,
            final float left,
            final float right,
            final float firstLine
    ) {
        this.requireRange(start, end);
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(attrs, left);
        StyleConstants.setRightIndent(attrs, right);
        StyleConstants.setFirstLineIndent(attrs, firstLine);
        this.document.setParagraphAttributes(start, Math.max(end - start, 0), attrs, false);
        this.markEdited();
    }

    public float getLeftIndent(final int offset) {
        return StyleConstants.getLeftIndent(this.paragraphAttributes(offset));
    }

    public float getRightIndent(final int offset) {
        return StyleConstants.getRightIndent(this.paragraphAttributes(offset));
    }

    public float getFirstLineIndent(final int offset) {
        return StyleConstants.getFirstLineIndent(this.paragraphAttributes(offset));
    }

    public void setTabStops(final int start, final int end, final float... positions) {
        this.requireRange(start, end);
        final TabStop[] stops = new TabStop[positions.length];
        for (int i = 0; i < positions.length; i++) {
            stops[i] = new TabStop(positions[i]);
        }
        final SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setTabSet(attrs, new TabSet(stops));
        this.document.setParagraphAttributes(start, Math.max(end - start, 0), attrs, false);
        this.markEdited();
    }

    public float[] getTabStops(final int offset) {
        final TabSet tabs = StyleConstants.getTabSet(this.paragraphAttributes(offset));
        if (Objects.isNull(tabs) || tabs.getTabCount() == 0) {
            return new float[0];
        }
        final float[] positions = new float[tabs.getTabCount()];
        for (int i = 0; i < tabs.getTabCount(); i++) {
            positions[i] = tabs.getTab(i).getPosition();
        }
        return positions;
    }

    public void setBullet(final int start, final int end, final boolean bullet) {
        this.requireRange(start, end);
        final int from = start;
        final int to = Math.max(end, start);
        int pos = from;
        while (pos <= to && pos <= this.document.getLength()) {
            final Element paragraph = this.document.getParagraphElement(Math.min(pos, Math.max(this.document.getLength() - 1, 0)));
            final int paraStart = paragraph.getStartOffset();
            final int paraEnd = Math.min(paragraph.getEndOffset(), this.document.getLength());
            final boolean already = this.paragraphHasBullet(paraStart, paraEnd);
            this.loading = true;
            try {
                if (bullet && !already) {
                    this.document.insertString(paraStart, BULLET_PREFIX, null);
                    this.setParagraphIndents(paraStart, paraStart, BULLET_LEFT_INDENT, 0f, BULLET_FIRST_LINE_INDENT);
                } else if (!bullet && already) {
                    this.document.remove(paraStart, BULLET_PREFIX.length());
                    this.setParagraphIndents(paraStart, paraStart, 0f, 0f, 0f);
                }
            } catch (final BadLocationException e) {
                throw new IllegalStateException(e);
            } finally {
                this.loading = false;
            }
            pos = this.document.getParagraphElement(paraStart).getEndOffset();
            if (pos <= paraStart) {
                break;
            }
        }
        this.markEdited();
    }

    public boolean isBullet(final int offset) {
        final Element paragraph = this.document.getParagraphElement(this.clampOffset(offset));
        return this.paragraphHasBullet(paragraph.getStartOffset(), Math.min(paragraph.getEndOffset(), this.document.getLength()));
    }

    public int find(final String needle, final int fromIndex, final boolean matchCase, final boolean wholeWord) {
        if (Objects.isNull(needle) || needle.isEmpty()) {
            return -1;
        }
        final String haystack = this.getText();
        final String hay = matchCase ? haystack : haystack.toLowerCase(Locale.ROOT);
        final String needleNorm = matchCase ? needle : needle.toLowerCase(Locale.ROOT);
        int from = Math.max(fromIndex, 0);
        while (from <= hay.length() - needleNorm.length()) {
            final int index = hay.indexOf(needleNorm, from);
            if (index < 0) {
                return -1;
            }
            if (!wholeWord || isWholeWord(haystack, index, needle.length())) {
                return index;
            }
            from = index + 1;
        }
        return -1;
    }

    public int replaceAll(final String find, final String replace, final boolean matchCase, final boolean wholeWord) {
        if (Objects.isNull(find) || find.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (true) {
            final int index = this.find(find, from, matchCase, wholeWord);
            if (index < 0) {
                break;
            }
            try {
                this.document.remove(index, find.length());
                this.document.insertString(index, replace, null);
            } catch (final BadLocationException e) {
                throw new IllegalStateException(e);
            }
            from = index + replace.length();
            count++;
        }
        return count;
    }

    public void deleteRange(final int start, final int end) {
        this.requireRange(start, end);
        if (end > start) {
            try {
                this.document.remove(start, end - start);
            } catch (final BadLocationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public boolean hasCharacterFormatting() {
        if (this.document.getLength() == 0) {
            return false;
        }
        int offset = 0;
        while (offset < this.document.getLength()) {
            final Element run = this.document.getCharacterElement(offset);
            if (isNonDefaultRun(run.getAttributes()) || this.isBullet(offset)) {
                return true;
            }
            offset = Math.max(run.getEndOffset(), offset + 1);
        }
        return false;
    }

    public static List<String> dateTimeFormats(final LocalDateTime when) {
        final LocalDateTime stamp = Objects.requireNonNullElse(when, LocalDateTime.now());
        return List.of(
                stamp.format(DateTimeFormatter.ofPattern("h:mm a")),
                stamp.format(DateTimeFormatter.ofPattern("h:mm:ss a")),
                stamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                stamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                stamp.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                stamp.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                stamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a"))
        );
    }

    public static boolean isPlainTextPath(final Path path) {
        return hasExtension(path, "txt");
    }

    public static boolean isRtfPath(final Path path) {
        return hasExtension(path, "rtf");
    }

    private void applyCharacterAttributes(final int start, final int end, final SimpleAttributeSet attrs) {
        this.requireRange(start, end);
        final int length = end - start;
        if (length > 0) {
            this.document.setCharacterAttributes(start, length, attrs, false);
        }
        this.markEdited();
    }

    private javax.swing.text.AttributeSet characterAttributes(final int offset) {
        return this.document.getCharacterElement(this.clampOffset(offset)).getAttributes();
    }

    private javax.swing.text.AttributeSet paragraphAttributes(final int offset) {
        return this.document.getParagraphElement(this.clampOffset(offset)).getAttributes();
    }

    private boolean paragraphHasBullet(final int paraStart, final int paraEnd) {
        if (paraEnd <= paraStart) {
            return false;
        }
        try {
            return this.document.getText(paraStart, 1).startsWith("\u2022");
        } catch (final BadLocationException e) {
            return false;
        }
    }

    private static boolean isWholeWord(final String haystack, final int index, final int length) {
        final boolean startOk = index == 0 || !Character.isLetterOrDigit(haystack.charAt(index - 1));
        final int end = index + length;
        final boolean endOk = end >= haystack.length() || !Character.isLetterOrDigit(haystack.charAt(end));
        return startOk && endOk;
    }

    private static boolean isNonDefaultRun(final javax.swing.text.AttributeSet attrs) {
        if (StyleConstants.isBold(attrs) || StyleConstants.isItalic(attrs) || StyleConstants.isUnderline(attrs)) {
            return true;
        }
        if (!DEFAULT_FONT_FAMILY.equals(StyleConstants.getFontFamily(attrs))) {
            return true;
        }
        if (StyleConstants.getFontSize(attrs) != DEFAULT_FONT_SIZE) {
            return true;
        }
        final Color fg = StyleConstants.getForeground(attrs);
        return Objects.nonNull(fg) && fg.getRGB() != DEFAULT_FOREGROUND.getRGB();
    }

    private static String readPlainText(final Path path) throws IOException {
        final byte[] bytes = Files.readAllBytes(path);
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static DocumentKind detectKind(final Path path, final boolean plainText) throws IOException {
        if (!plainText) {
            return DocumentKind.RICH_TEXT;
        }
        final byte[] bytes = Files.readAllBytes(path);
        if (bytes.length >= 2 && ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE
                || (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF)) {
            return DocumentKind.UNICODE_TEXT;
        }
        return DocumentKind.TEXT;
    }

    private DocumentKind kindForPath(final Path path) {
        if (isRtfPath(path)) {
            return DocumentKind.RICH_TEXT;
        }
        if (this.kind == DocumentKind.UNICODE_TEXT) {
            return DocumentKind.UNICODE_TEXT;
        }
        if (isPlainTextPath(path)) {
            return DocumentKind.TEXT;
        }
        return DocumentKind.RICH_TEXT;
    }

    private int clampOffset(final int offset) {
        final int length = this.document.getLength();
        if (length <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(offset, length - 1));
    }

    private void requireRange(final int start, final int end) {
        if (start < 0 || end < start || end > this.document.getLength()) {
            throw new IllegalArgumentException(
                    "range [" + start + ", " + end + ") is invalid for length " + this.document.getLength()
            );
        }
    }

    private void applyDefaultStyle() {
        final Style def = this.document.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, DEFAULT_FONT_FAMILY);
        StyleConstants.setFontSize(def, DEFAULT_FONT_SIZE);
        StyleConstants.setBold(def, false);
        StyleConstants.setItalic(def, false);
        StyleConstants.setUnderline(def, false);
        StyleConstants.setForeground(def, DEFAULT_FOREGROUND);
        StyleConstants.setAlignment(def, StyleConstants.ALIGN_LEFT);
    }

    /**
     * Merge LEFT onto the implied paragraph. replace=true would strip DEFAULT_STYLE
     * so insertString(null) would resolve FontFamily to Monospaced instead of Arial.
     */
    private void resetRunAttributes() {
        final SimpleAttributeSet paragraph = new SimpleAttributeSet();
        StyleConstants.setAlignment(paragraph, StyleConstants.ALIGN_LEFT);
        this.document.setParagraphAttributes(0, this.document.getLength(), paragraph, false);
    }

    private void clearContent() {
        try {
            if (this.document.getLength() > 0) {
                this.document.remove(0, this.document.getLength());
            }
        } catch (final BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void insertSilently(final int offset, final String text) throws BadLocationException {
        if (Objects.nonNull(text) && !text.isEmpty()) {
            this.document.insertString(offset, text, null);
        }
    }

    private void markEdited() {
        if (!this.loading) {
            this.edited = true;
        }
    }

    private static boolean shouldReadAsPlainText(final Path path) throws IOException {
        if (isRtfPath(path)) {
            return false;
        }
        if (isPlainTextPath(path)) {
            return true;
        }
        return !looksLikeRtf(path);
    }

    private static boolean looksLikeRtf(final Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            final byte[] buf = in.readNBytes(5);
            if (buf.length < 5) {
                return false;
            }
            return "{\\rtf".equals(new String(buf, StandardCharsets.US_ASCII));
        }
    }

    private static boolean hasExtension(final Path path, final String extension) {
        if (Objects.isNull(path) || Objects.isNull(path.getFileName())) {
            return false;
        }
        final String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith("." + extension);
    }
}
