package com.sysbot32.whistler.file_manager;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * 7zFM-style row ordering:
 * <ol>
 *   <li>{@code ..} always first</li>
 *   <li>folders always before files (not inverted on descending)</li>
 *   <li>then the active column key, which does reverse on descending</li>
 * </ol>
 */
public final class EntryComparators {
    public static final int COL_NAME = 0;
    public static final int COL_SIZE = 1;
    public static final int COL_MODIFIED = 2;
    public static final int COL_CREATED = 3;
    public static final int COL_ACCESSED = 4;
    public static final int COL_ATTR = 5;
    public static final int COL_PACKED = 6;

    private EntryComparators() {
    }

    /**
     * Full comparator for a column. Parent-link and directory grouping are fixed;
     * only the column key respects {@code ascending}.
     */
    public static Comparator<FileEntry> forColumn(final int column, final boolean ascending) {
        Comparator<FileEntry> key = columnKey(column);
        if (!ascending) {
            key = key.reversed();
        }
        // Important: do not chain .reversed() after thenComparing — that reverses the whole
        // comparator so far (and would un-pin ".."). Reverse only the directory key itself.
        return Comparator
                .comparing(FileEntry::parentLink).reversed() // ".." pinned top
                .thenComparing(Comparator.comparing(FileEntry::directory).reversed()) // folders first
                .thenComparing(key);
    }

    /** Column value only — safe to reverse for descending. */
    private static Comparator<FileEntry> columnKey(final int column) {
        return switch (column) {
            case COL_NAME -> Comparator.comparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER);
            case COL_SIZE -> Comparator.comparing(EntryComparators::sizeKey, Comparator.nullsLast(Long::compareTo));
            case COL_MODIFIED -> Comparator.comparing(FileEntry::modified, Comparator.nullsLast(Instant::compareTo));
            case COL_CREATED -> Comparator.comparing(FileEntry::created, Comparator.nullsLast(Instant::compareTo));
            case COL_ACCESSED -> Comparator.comparing(FileEntry::accessed, Comparator.nullsLast(Instant::compareTo));
            case COL_ATTR -> Comparator.comparing(FileEntry::attributes, String.CASE_INSENSITIVE_ORDER);
            case COL_PACKED -> Comparator.comparing(EntryComparators::packedKey, Comparator.nullsLast(Long::compareTo));
            default -> Comparator.comparing(FileEntry::name, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private static Long sizeKey(final FileEntry e) {
        if (e.parentLink() || e.directory()) {
            return null;
        }
        return e.size();
    }

    private static Long packedKey(final FileEntry e) {
        if (e.parentLink() || e.directory()) {
            return null;
        }
        return e.packedSize();
    }

    public static void sortInPlace(final List<FileEntry> entries, final int column, final boolean ascending) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        entries.sort(forColumn(column, ascending));
    }
}
