package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

/**
 * 7zFM Insert: toggle selection at the lead row, then advance the lead without
 * range-filling (DefaultListSelectionModel.setLeadSelectionIndex would).
 */
@UtilityClass
public final class InsertSelection {
    /**
     * Toggle whether {@code lead} is selected, then move the lead to {@code lead+1}
     * without adding a contiguous selection range.
     *
     * @return the new lead index after advance (or the same lead if at end / invalid)
     */
    public static int toggleAndAdvanceLead(final ListSelectionModel sm, final int rowCount) {
        if (sm == null || rowCount <= 0) {
            return -1;
        }
        int lead = sm.getLeadSelectionIndex();
        if (lead < 0 || lead >= rowCount) {
            lead = sm.getMinSelectionIndex();
        }
        if (lead < 0 || lead >= rowCount) {
            // Nothing focused yet — start at row 0 without selecting via setLeadSelectionIndex
            lead = 0;
            moveLeadOnly(sm, 0);
        }
        if (sm.isSelectedIndex(lead)) {
            sm.removeSelectionInterval(lead, lead);
        } else {
            sm.addSelectionInterval(lead, lead);
        }
        final int next = Math.min(lead + 1, rowCount - 1);
        if (next != lead) {
            moveLeadOnly(sm, next);
        }
        return next;
    }

    /**
     * Move lead (and anchor) without changing which indices are selected.
     * Prefer {@link DefaultListSelectionModel#moveLeadSelectionIndex(int)} —
     * never {@link ListSelectionModel#setLeadSelectionIndex(int)} (range-fills).
     */
    public static void moveLeadOnly(final ListSelectionModel sm, final int index) {
        if (sm instanceof DefaultListSelectionModel dsm) {
            dsm.moveLeadSelectionIndex(index);
            dsm.setAnchorSelectionIndex(index);
            return;
        }
        // Fallback: select then unselect if needed so lead moves without net select
        final boolean wasSelected = sm.isSelectedIndex(index);
        sm.addSelectionInterval(index, index);
        if (!wasSelected) {
            sm.removeSelectionInterval(index, index);
        }
    }
}
