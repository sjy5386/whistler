package com.sysbot32.whistler.notepad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindQueryTest {
    @Test
    void whenFindDialogVisibleLiveControlsOverrideCache() {
        final FindQuery cached = new FindQuery("old", false, SearchDirection.DOWN);
        final FindQuery resolved = FindQuery.resolve(
                cached,
                true,
                "needle",
                true,
                SearchDirection.UP
        );
        assertEquals("needle", resolved.getText());
        assertTrue(resolved.isMatchCase());
        assertEquals(SearchDirection.UP, resolved.getDirection());
    }

    @Test
    void whenFindDialogHiddenCacheIsKept() {
        final FindQuery cached = new FindQuery("cached", true, SearchDirection.UP);
        final FindQuery resolved = FindQuery.resolve(
                cached,
                false,
                "ignored",
                false,
                SearchDirection.DOWN
        );
        assertEquals("cached", resolved.getText());
        assertTrue(resolved.isMatchCase());
        assertEquals(SearchDirection.UP, resolved.getDirection());
    }

    @Test
    void liveEmptyTextIsHonoredSoFindNextCanReopenDialog() {
        final FindQuery cached = new FindQuery("still-there", true, SearchDirection.DOWN);
        final FindQuery resolved = FindQuery.resolve(
                cached,
                true,
                "",
                false,
                SearchDirection.DOWN
        );
        assertEquals("", resolved.getText());
        assertFalse(resolved.isMatchCase());
    }

    @Test
    void findUsesResolvedDirectionAgainstShippedNotepad() {
        final Notepad notepad = new Notepad();
        notepad.setContent("aaa bbb aaa");
        final FindQuery down = FindQuery.resolve(
                new FindQuery("aaa", true, SearchDirection.DOWN),
                true,
                "aaa",
                true,
                SearchDirection.DOWN
        );
        final FindQuery up = FindQuery.resolve(
                down,
                true,
                "aaa",
                true,
                SearchDirection.UP
        );
        assertEquals(0, notepad.find(down.getText(), 0, down.isMatchCase(), down.getDirection()));
        assertEquals(8, notepad.find(up.getText(), 11, up.isMatchCase(), up.getDirection()));
        assertEquals(0, notepad.find(up.getText(), 8, up.isMatchCase(), up.getDirection()));
    }

    @Test
    void findDialogApplySessionRestoresMatchCaseAndDirection() {
        final FindDialog dialog = new FindDialog(null);
        // Default Match case is false; session had match-case + Up.
        assertFalse(dialog.isMatchCase());
        dialog.applySession("Needle", true, SearchDirection.UP);
        assertEquals("Needle", dialog.getFindText());
        assertTrue(dialog.isMatchCase());
        assertEquals(SearchDirection.UP, dialog.getDirection());

        // Live resolve after re-seed must keep match-case true (the skeptic bug).
        final FindQuery cached = new FindQuery("Needle", true, SearchDirection.UP);
        final FindQuery resolved = FindQuery.resolve(
                cached,
                true,
                dialog.getFindText(),
                dialog.isMatchCase(),
                dialog.getDirection()
        );
        assertTrue(resolved.isMatchCase());
        assertEquals(SearchDirection.UP, resolved.getDirection());
        assertEquals("Needle", resolved.getText());
    }
}

