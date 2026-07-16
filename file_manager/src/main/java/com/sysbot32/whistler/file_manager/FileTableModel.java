package com.sysbot32.whistler.file_manager;

import lombok.Getter;

import javax.swing.table.AbstractTableModel;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Details columns close to classic 7-Zip File Manager:
 * Name · Size · Modified · Created · Accessed · Attributes · Packed Size
 * <p>
 * Sorting is model-side (not TableRowSorter) so {@code ..} stays pinned and reverse
 * sort does not break multi-key ordering.
 */
public final class FileTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {
            "Name", "Size", "Modified", "Created", "Accessed", "Attributes", "Packed Size"
    };
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final List<FileEntry> entries = new ArrayList<>();
    @Getter
    private int sortColumn = EntryComparators.COL_NAME;
    @Getter
    private boolean sortAscending = true;

    public void setEntries(final List<FileEntry> next) {
        this.entries.clear();
        if (next != null) {
            this.entries.addAll(next);
        }
        reapplySort();
        fireTableDataChanged();
    }

    /**
     * Toggle or switch sort column (header click). Re-sorts current rows.
     */
    public void toggleSort(final int column) {
        if (column < 0 || column >= COLUMNS.length) {
            return;
        }
        if (this.sortColumn == column) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.sortColumn = column;
            this.sortAscending = true;
        }
        reapplySort();
        fireTableDataChanged();
    }

    private void reapplySort() {
        EntryComparators.sortInPlace(this.entries, this.sortColumn, this.sortAscending);
    }

    public FileEntry getEntry(final int row) {
        if (row < 0 || row >= this.entries.size()) {
            return null;
        }
        return this.entries.get(row);
    }

    @Override
    public int getRowCount() {
        return this.entries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(final int column) {
        if (column < 0 || column >= COLUMNS.length) {
            return "";
        }
        final String base = COLUMNS[column];
        if (column != this.sortColumn) {
            return base;
        }
        return base + (this.sortAscending ? " ▲" : " ▼");
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return switch (columnIndex) {
            case 0 -> FileEntry.class;
            case 1, 6 -> Long.class;
            case 2, 3, 4 -> Instant.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final FileEntry entry = this.entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> entry;
            case 1 -> entry.parentLink() || entry.directory() ? null : entry.size();
            case 2 -> entry.modified();
            case 3 -> entry.created();
            case 4 -> entry.accessed();
            case 5 -> entry.parentLink() ? "" : entry.attributes();
            case 6 -> packedValue(entry);
            default -> null;
        };
    }

    private static Long packedValue(final FileEntry entry) {
        if (entry.parentLink() || entry.directory() || entry.packedSize() == null) {
            return null;
        }
        return entry.packedSize();
    }

    public static String formatSize(final long size) {
        return String.format("%,d", size);
    }

    public static String formatDate(final Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FMT.format(instant);
    }
}
