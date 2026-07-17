package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryComparatorsTest {
    @Test
    void parentLinkAlwaysFirstEvenWhenDescending() {
        final List<FileEntry> rows = new ArrayList<>(List.of(
                file("z.txt", 10),
                FileEntry.upLink(),
                file("a.txt", 20)
        ));
        EntryComparators.sortInPlace(rows, EntryComparators.COL_NAME, false);
        assertTrue(rows.getFirst().parentLink());
        assertEquals("z.txt", rows.get(1).name());
        assertEquals("a.txt", rows.get(2).name());
    }

    @Test
    void nameSortFoldersBeforeFilesAscending() {
        final List<FileEntry> rows = new ArrayList<>(List.of(
                file("b.txt", 1),
                dir("a-dir"),
                FileEntry.upLink(),
                file("a.txt", 1),
                dir("z-dir")
        ));
        EntryComparators.sortInPlace(rows, EntryComparators.COL_NAME, true);
        assertTrue(rows.get(0).parentLink());
        assertEquals("a-dir", rows.get(1).name());
        assertEquals("z-dir", rows.get(2).name());
        assertEquals("a.txt", rows.get(3).name());
        assertEquals("b.txt", rows.get(4).name());
    }

    @Test
    void nameSortFoldersStayBeforeFilesWhenDescending() {
        // Descending reverses names only — not the folder/file grouping (7zFM-like)
        final List<FileEntry> rows = new ArrayList<>(List.of(
                file("b.txt", 1),
                dir("a-dir"),
                FileEntry.upLink(),
                file("a.txt", 1),
                dir("z-dir")
        ));
        EntryComparators.sortInPlace(rows, EntryComparators.COL_NAME, false);
        assertTrue(rows.get(0).parentLink());
        assertTrue(rows.get(1).directory());
        assertTrue(rows.get(2).directory());
        assertEquals("z-dir", rows.get(1).name());
        assertEquals("a-dir", rows.get(2).name());
        assertEquals("b.txt", rows.get(3).name());
        assertEquals("a.txt", rows.get(4).name());
    }

    @Test
    void sizeSortAscendingNumericWithinFiles() {
        final List<FileEntry> rows = new ArrayList<>(List.of(
                FileEntry.upLink(),
                file("big", 1000),
                file("small", 10),
                dir("folder")
        ));
        EntryComparators.sortInPlace(rows, EntryComparators.COL_SIZE, true);
        assertTrue(rows.getFirst().parentLink());
        assertEquals("folder", rows.get(1).name()); // dirs still before files
        assertEquals("small", rows.get(2).name());
        assertEquals("big", rows.get(3).name());
    }

    @Test
    void sizeSortDescendingKeepsFoldersFirst() {
        final List<FileEntry> rows = new ArrayList<>(List.of(
                FileEntry.upLink(),
                file("big", 1000),
                file("small", 10),
                dir("folder")
        ));
        EntryComparators.sortInPlace(rows, EntryComparators.COL_SIZE, false);
        assertTrue(rows.getFirst().parentLink());
        assertEquals("folder", rows.get(1).name());
        assertEquals("big", rows.get(2).name());
        assertEquals("small", rows.get(3).name());
    }

    @Test
    void modifiedSortUsesInstant() {
        final Instant t1 = Instant.parse("2020-01-01T00:00:00Z");
        final Instant t2 = Instant.parse("2024-01-01T00:00:00Z");
        final List<FileEntry> rows = new ArrayList<>(List.of(
                FileEntry.upLink(),
                entry("old", false, 1, t1),
                entry("new", false, 1, t2)
        ));
        EntryComparators.sortInPlace(rows, EntryComparators.COL_MODIFIED, true);
        assertEquals("old", rows.get(1).name());
        assertEquals("new", rows.get(2).name());

        EntryComparators.sortInPlace(rows, EntryComparators.COL_MODIFIED, false);
        assertTrue(rows.getFirst().parentLink());
        assertEquals("new", rows.get(1).name());
        assertEquals("old", rows.get(2).name());
    }

    @Test
    void modelToggleSortCyclesDirection() {
        final FileTableModel model = new FileTableModel();
        model.setEntries(List.of(
                FileEntry.upLink(),
                file("b", 2),
                file("a", 1)
        ));
        assertEquals("a", model.getEntry(1).name());

        model.toggleSort(EntryComparators.COL_NAME); // same column → descending
        assertTrue(model.getEntry(0).parentLink());
        assertEquals("b", model.getEntry(1).name());

        model.toggleSort(EntryComparators.COL_SIZE);
        assertTrue(model.isSortAscending());
        assertEquals(EntryComparators.COL_SIZE, model.getSortColumn());
        assertEquals("a", model.getEntry(1).name()); // smaller first
    }

    @Test
    void unsortedRestoresLoadOrderAfterColumnSort() {
        final FileTableModel model = new FileTableModel();
        // Deliberate load order: b then a (not name-sorted)
        model.setEntries(List.of(
                FileEntry.upLink(),
                file("b", 2),
                file("a", 1)
        ));
        // Default sorted=true applies name sort → a before b
        assertEquals("a", model.getEntry(1).name());
        assertEquals("b", model.getEntry(2).name());

        model.sortBy(EntryComparators.COL_SIZE, false); // size desc: b then a
        assertEquals("b", model.getEntry(1).name());

        model.setUnsorted(); // must restore original load order b then a
        assertFalse(model.isSorted());
        assertEquals("b", model.getEntry(1).name());
        assertEquals("a", model.getEntry(2).name());
    }

    private static FileEntry file(final String name, final long size) {
        return entry(name, false, size, Instant.EPOCH);
    }

    private static FileEntry dir(final String name) {
        return entry(name, true, 0, Instant.EPOCH);
    }

    private static FileEntry entry(
            final String name,
            final boolean directory,
            final long size,
            final Instant modified
    ) {
        return new FileEntry(name, directory, false, size, null, modified, null, null, directory ? "D" : "A");
    }
}
