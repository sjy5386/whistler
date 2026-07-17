package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves Insert accumulates multi-select without range-fill from setLeadSelectionIndex.
 * Drives the shipped {@link InsertSelection} algorithm on a real ListSelectionModel.
 */
class InsertSelectionTest {
    @Test
    void fourInsertsFromRowZeroAccumulateAllFour() {
        final DefaultListSelectionModel sm = new DefaultListSelectionModel();
        sm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Must use moveLead — setLeadSelectionIndex on empty model does not establish lead
        InsertSelection.moveLeadOnly(sm, 0);

        // 4× Insert starting at row 0 → selected {0,1,2,3}, lead at 3
        for (int i = 0; i < 4; i++) {
            InsertSelection.toggleAndAdvanceLead(sm, 10);
        }

        assertTrue(sm.isSelectedIndex(0), "row 0");
        assertTrue(sm.isSelectedIndex(1), "row 1");
        assertTrue(sm.isSelectedIndex(2), "row 2");
        assertTrue(sm.isSelectedIndex(3), "row 3");
        assertFalse(sm.isSelectedIndex(4), "row 4 not selected — only lead advanced");
        // After toggling 0..3, lead advances to 4 (next row to toggle)
        assertEquals(4, sm.getLeadSelectionIndex());
    }

    @Test
    void setLeadSelectionIndexWouldRangeFillButMoveLeadDoesNot() {
        final DefaultListSelectionModel sm = new DefaultListSelectionModel();
        sm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sm.addSelectionInterval(0, 0);
        sm.setAnchorSelectionIndex(0);
        sm.setLeadSelectionIndex(0);

        // Broken pattern (what we must not do): setLeadSelectionIndex(3) range-fills 0..3
        final DefaultListSelectionModel broken = new DefaultListSelectionModel();
        broken.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        broken.addSelectionInterval(0, 0);
        broken.setAnchorSelectionIndex(0);
        broken.setLeadSelectionIndex(0);
        broken.setLeadSelectionIndex(3);
        // After setLeadSelectionIndex(3), contiguous selection is typical
        assertTrue(broken.isSelectedIndex(0));
        assertTrue(broken.isSelectedIndex(3));

        // Correct: moveLead only — selection stays {0}
        InsertSelection.moveLeadOnly(sm, 3);
        assertTrue(sm.isSelectedIndex(0));
        assertFalse(sm.isSelectedIndex(1));
        assertFalse(sm.isSelectedIndex(2));
        assertFalse(sm.isSelectedIndex(3));
        assertEquals(3, sm.getLeadSelectionIndex());
    }

    @Test
    void toggleDeselectsThenAdvances() {
        final DefaultListSelectionModel sm = new DefaultListSelectionModel();
        sm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        sm.addSelectionInterval(0, 0);
        sm.addSelectionInterval(1, 1);
        sm.setLeadSelectionIndex(0);
        sm.setAnchorSelectionIndex(0);

        InsertSelection.toggleAndAdvanceLead(sm, 5);
        // row 0 deselected, lead moved to 1; row 1 still selected
        assertFalse(sm.isSelectedIndex(0));
        assertTrue(sm.isSelectedIndex(1));
        assertEquals(1, sm.getLeadSelectionIndex());
    }
}
