package com.sysbot32.whistler.file_manager.listing;

import com.sysbot32.whistler.file_manager.model.FileEntry;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionHelpersTest {
    @Test
    void selectableSkipsParentLink() {
        final List<FileEntry> entries = List.of(
                FileEntry.upLink(),
                file("a.txt"),
                file("b.bin")
        );
        final Set<Integer> rows = SelectionHelpers.selectableRows(entries);
        assertFalse(rows.contains(0));
        assertTrue(rows.contains(1));
        assertTrue(rows.contains(2));
        assertEquals(2, rows.size());
    }

    @Test
    void invertTogglesSelectable() {
        final Set<Integer> selectable = Set.of(1, 2, 3);
        final Set<Integer> selected = Set.of(1, 3);
        final Set<Integer> inverted = SelectionHelpers.invert(selected, selectable);
        assertEquals(Set.of(2), inverted);
    }

    @Test
    void rowsMatchingTypeByExtension() {
        final List<FileEntry> entries = List.of(
                FileEntry.upLink(),
                file("a.txt"),
                file("b.TXT"),
                file("c.bin"),
                file("README")
        );
        final Set<Integer> txt = SelectionHelpers.rowsMatchingType(entries, "x.txt");
        assertEquals(Set.of(1, 2), txt);

        final Set<Integer> none = SelectionHelpers.rowsMatchingType(entries, "README");
        assertEquals(Set.of(4), none);
    }

    @Test
    void applyTypeFilterSelectAndDeselect() {
        final List<FileEntry> entries = List.of(FileEntry.upLink(), file("a.txt"), file("b.bin"), file("c.txt"));
        Set<Integer> sel = Set.of(2);
        sel = SelectionHelpers.applyTypeFilter(sel, entries, "z.txt", true);
        assertEquals(Set.of(1, 2, 3), sel);
        sel = SelectionHelpers.applyTypeFilter(sel, entries, "z.txt", false);
        assertEquals(Set.of(2), sel);
    }

    @Test
    void extensionOfFlatRelativePath() {
        assertEquals(".png", SelectionHelpers.extensionOf("pics/photo.PNG"));
        assertEquals("", SelectionHelpers.extensionOf("Makefile"));
    }

    private static FileEntry file(final String name) {
        return new FileEntry(name, false, false, 1L, null, Instant.EPOCH, null, null, "A");
    }
}
