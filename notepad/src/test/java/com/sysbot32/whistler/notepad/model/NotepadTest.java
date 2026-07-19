package com.sysbot32.whistler.notepad.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotepadTest {
    @Test
    void findDownIsCaseSensitiveWhenRequested() {
        final Notepad notepad = new Notepad();
        notepad.setContent("Hello hello HELLO");
        assertEquals(0, notepad.find("Hello", 0, true, SearchDirection.DOWN));
        assertEquals(6, notepad.find("hello", 0, true, SearchDirection.DOWN));
        assertEquals(12, notepad.find("HELLO", 0, true, SearchDirection.DOWN));
        assertEquals(-1, notepad.find("HeLLo", 0, true, SearchDirection.DOWN));
    }

    @Test
    void findDownIgnoresCaseWhenRequested() {
        final Notepad notepad = new Notepad();
        notepad.setContent("Hello hello HELLO");
        assertEquals(0, notepad.find("hello", 0, false, SearchDirection.DOWN));
        assertEquals(6, notepad.find("HELLO", 1, false, SearchDirection.DOWN));
    }

    @Test
    void findUpSearchesBackward() {
        final Notepad notepad = new Notepad();
        notepad.setContent("abc abc abc");
        // caret after last "abc" → first hit going up is at 8
        assertEquals(8, notepad.find("abc", 11, true, SearchDirection.UP));
        assertEquals(4, notepad.find("abc", 8, true, SearchDirection.UP));
        assertEquals(0, notepad.find("abc", 4, true, SearchDirection.UP));
        assertEquals(-1, notepad.find("abc", 0, true, SearchDirection.UP));
    }

    @Test
    void findUpRespectsMatchCase() {
        final Notepad notepad = new Notepad();
        notepad.setContent("Foo foo FOO");
        assertEquals(8, notepad.find("FOO", 11, true, SearchDirection.UP));
        assertEquals(4, notepad.find("foo", 8, true, SearchDirection.UP));
        assertEquals(-1, notepad.find("foo", 4, true, SearchDirection.UP));
        assertEquals(0, notepad.find("foo", 4, false, SearchDirection.UP));
    }

    @Test
    void findAdvancesFromIndexForFindNextDown() {
        final Notepad notepad = new Notepad();
        notepad.setContent("abc abc abc");
        final int first = notepad.find("abc", 0, true, SearchDirection.DOWN);
        final int second = notepad.find("abc", first + "abc".length(), true, SearchDirection.DOWN);
        final int third = notepad.find("abc", second + "abc".length(), true, SearchDirection.DOWN);
        assertEquals(0, first);
        assertEquals(4, second);
        assertEquals(8, third);
        assertEquals(-1, notepad.find("abc", third + "abc".length(), true, SearchDirection.DOWN));
    }

    @Test
    void replaceAllReplacesEveryMatch() {
        final Notepad notepad = new Notepad();
        notepad.setContent("one two one");
        final int count = notepad.replaceAll("one", "1", true);
        assertEquals(2, count);
        assertEquals("1 two 1", notepad.getContent());
        assertTrue(notepad.isEdited());
    }

    @Test
    void replaceAllCanIgnoreCase() {
        final Notepad notepad = new Notepad();
        notepad.setContent("Foo foo FOO");
        final int count = notepad.replaceAll("foo", "bar", false);
        assertEquals(3, count);
        assertEquals("bar bar bar", notepad.getContent());
    }

    @Test
    void replaceAllWithEmptyNeedleDoesNothing() {
        final Notepad notepad = new Notepad();
        notepad.setContent("keep");
        notepad.setEdited(false);
        assertEquals(0, notepad.replaceAll("", "x", true));
        assertEquals("keep", notepad.getContent());
        assertFalse(notepad.isEdited());
    }

    @Test
    void createNewResetsDirtyContentAndPath(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("draft.txt");
        Files.writeString(file, "saved");
        final Notepad notepad = new Notepad(file);
        assertEquals("saved", notepad.getContent());
        assertEquals(file, notepad.getPath());
        assertFalse(notepad.isEdited());

        notepad.setContent("dirty draft");
        notepad.setEdited(true);
        notepad.createNew();

        assertEquals("", notepad.getContent());
        assertNull(notepad.getPath());
        assertFalse(notepad.isEdited());
    }

    @Test
    void openSaveRoundTripPreservesBytesAndClearsDirty(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("note.txt");
        final String body = "line1\nline2\n한글 메모";

        final Notepad writer = new Notepad();
        writer.setContent(body);
        writer.setEdited(true);
        writer.save(file);

        assertFalse(writer.isEdited());
        assertEquals(file, writer.getPath());
        assertEquals(body, Files.readString(file));

        final Notepad reader = new Notepad();
        reader.setContent("stale");
        reader.setEdited(true);
        final String opened = reader.open(file);

        assertEquals(body, opened);
        assertEquals(body, reader.getContent());
        assertEquals(file, reader.getPath());
        assertFalse(reader.isEdited());
    }

    @Test
    void openLogDocumentAppendsTimeStampAndMarksDirty(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("diary.txt");
        Files.writeString(file, ".LOG\nfirst entry\n");

        final Notepad notepad = new Notepad();
        final String opened = notepad.open(file);

        assertTrue(Notepad.isLogDocument(Files.readString(file)));
        assertTrue(opened.startsWith(".LOG\nfirst entry\n"));
        assertTrue(opened.length() > ".LOG\nfirst entry\n".length());
        assertTrue(notepad.isEdited());
        assertEquals(file, notepad.getPath());
        // stamp should be on its own trailing line
        assertTrue(opened.trim().contains("first entry"));
    }

    @Test
    void isLogDocumentRequiresExactFirstLine() {
        assertTrue(Notepad.isLogDocument(".LOG\nmore"));
        assertTrue(Notepad.isLogDocument(".LOG\r\nmore"));
        assertTrue(Notepad.isLogDocument(".LOG"));
        assertFalse(Notepad.isLogDocument(" .LOG\n"));
        assertFalse(Notepad.isLogDocument(".log\n"));
        assertFalse(Notepad.isLogDocument("not a log"));
    }

    @Test
    void appendLogStampAddsStampLine() {
        final String stamped = Notepad.appendLogStamp(".LOG\n", "AM 1:00 2001-01-01");
        assertEquals(".LOG\nAM 1:00 2001-01-01" + System.lineSeparator(), stamped);
        final String stampedNoNl = Notepad.appendLogStamp(".LOG", "AM 2:00 2001-01-02");
        assertEquals(".LOG" + System.lineSeparator() + "AM 2:00 2001-01-02" + System.lineSeparator(), stampedNoNl);
    }

    @Test
    void constructorOpenLoadsFileWithoutDirtyFlag(@TempDir final Path tempDir) throws IOException {
        final Path file = tempDir.resolve("from-args.txt");
        Files.writeString(file, "from args");
        final Notepad notepad = new Notepad(file);
        assertEquals("from args", notepad.getContent());
        assertEquals(file, notepad.getPath());
        assertFalse(notepad.isEdited());
    }

    @Test
    void getTimeDateReturnsNonEmptyStamp() {
        final String stamp = Notepad.getTimeDate();
        assertFalse(stamp.isBlank());
        assertTrue(stamp.matches(".*\\d{4}-\\d{2}-\\d{2}$"));
    }
}
