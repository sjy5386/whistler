package com.sysbot32.whistler.file_manager;

import com.sysbot32.whistler.config.Config;
import lombok.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Recent folder paths for 7zFM Folders History (Alt+F12).
 * Stored as {@code folderHistory} pipe-separated absolute paths (most recent first).
 */
public final class FolderHistory {
    private static final String KEY = "folderHistory";
    public static final int MAX_ENTRIES = 24;

    @NonNull
    private final Config config;
    private final List<String> paths = new ArrayList<>();

    /** Explicit ctor: must assign config before {@link #load()}. */
    public FolderHistory(@NonNull final Config config) {
        this.config = config;
        load();
    }

    public void record(final Path directory) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        final String abs = directory.toAbsolutePath().normalize().toString();
        this.paths.remove(abs);
        this.paths.addFirst(abs);
        while (this.paths.size() > MAX_ENTRIES) {
            this.paths.removeLast();
        }
        save();
    }

    public List<Path> entries() {
        final List<Path> result = new ArrayList<>();
        for (final String p : this.paths) {
            final Path path = Path.of(p);
            if (Files.isDirectory(path)) {
                result.add(path);
            }
        }
        return List.copyOf(result);
    }

    public List<String> displayEntries() {
        return List.copyOf(this.paths);
    }

    private void load() {
        this.paths.clear();
        final String raw = this.config.get(KEY, "");
        if (raw == null || raw.isBlank()) {
            return;
        }
        final LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (final String part : raw.split("\\|")) {
            final String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                seen.add(trimmed);
            }
        }
        this.paths.addAll(seen);
        while (this.paths.size() > MAX_ENTRIES) {
            this.paths.removeLast();
        }
    }

    private void save() {
        this.config.set(KEY, String.join("|", this.paths));
        this.config.save();
    }
}
