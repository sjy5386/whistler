package com.sysbot32.whistler.file_manager.listing;

import com.sysbot32.whistler.file_manager.model.FileEntry;
import lombok.experimental.UtilityClass;

import java.util.*;

/**
 * Pure selection math for 7zFM-style Edit commands (select all / invert / by type).
 * Works on table model row indices; skips parent-link rows when selecting "all".
 */
@UtilityClass
public final class SelectionHelpers {
    /** Rows that may be selected (everything except {@code ..}). */
    public static Set<Integer> selectableRows(final List<FileEntry> entries) {
        final Set<Integer> rows = new HashSet<>();
        if (entries == null) {
            return rows;
        }
        for (int i = 0; i < entries.size(); i++) {
            final FileEntry e = entries.get(i);
            if (e != null && !e.parentLink()) {
                rows.add(i);
            }
        }
        return rows;
    }

    public static Set<Integer> invert(final Set<Integer> current, final Set<Integer> selectable) {
        final Set<Integer> next = new HashSet<>(Objects.requireNonNullElse(selectable, Set.of()));
        if (current != null) {
            for (final Integer row : current) {
                if (next.contains(row)) {
                    next.remove(row);
                } else if (selectable != null && selectable.contains(row)) {
                    next.add(row);
                }
            }
        }
        return next;
    }

    /**
     * Rows matching the extension of {@code typeName} (e.g. {@code foo.txt} → {@code .txt}).
     * Names without a dot match other extension-less names.
     */
    public static Set<Integer> rowsMatchingType(final List<FileEntry> entries, final String typeName) {
        final Set<Integer> rows = new HashSet<>();
        if (entries == null || typeName == null) {
            return rows;
        }
        final String ext = extensionOf(typeName);
        for (int i = 0; i < entries.size(); i++) {
            final FileEntry e = entries.get(i);
            if (e == null || e.parentLink()) {
                continue;
            }
            if (ext.equals(extensionOf(e.name()))) {
                rows.add(i);
            }
        }
        return rows;
    }

    /**
     * Union (select) or difference (deselect) of {@code current} with type matches.
     */
    public static Set<Integer> applyTypeFilter(
            final Set<Integer> current,
            final List<FileEntry> entries,
            final String typeName,
            final boolean select
    ) {
        final Set<Integer> typed = rowsMatchingType(entries, typeName);
        final Set<Integer> next = new HashSet<>(Objects.requireNonNullElse(current, Set.of()));
        if (select) {
            next.addAll(typed);
        } else {
            next.removeAll(typed);
        }
        return next;
    }

    public static String extensionOf(final String name) {
        if (name == null || name.isBlank() || "..".equals(name)) {
            return "";
        }
        // Use last path segment for flat-view names like "dir/file.txt"
        String base = name.replace('\\', '/');
        final int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        final int dot = base.lastIndexOf('.');
        if (dot <= 0 || dot == base.length() - 1) {
            return "";
        }
        return base.substring(dot).toLowerCase(Locale.ROOT);
    }

    public static List<Integer> sortedRows(final Set<Integer> rows) {
        final List<Integer> list = new ArrayList<>(Objects.requireNonNullElse(rows, Set.of()));
        list.sort(Integer::compareTo);
        return list;
    }
}
