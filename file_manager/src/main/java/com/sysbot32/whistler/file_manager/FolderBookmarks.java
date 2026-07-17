package com.sysbot32.whistler.file_manager;

import com.sysbot32.whistler.config.Config;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * 7zFM-style folder bookmarks 0–9 persisted in config ({@code bookmark.0} … {@code bookmark.9}).
 */
@RequiredArgsConstructor
public final class FolderBookmarks {
    private static final String KEY_PREFIX = "bookmark.";
    public static final int SLOT_COUNT = 10;

    @NonNull
    private final Config config;

    public void set(final int slot, final Path directory) {
        requireSlot(slot);
        Objects.requireNonNull(directory, "directory");
        this.config.set(KEY_PREFIX + slot, directory.toAbsolutePath().normalize().toString());
        this.config.save();
    }

    public void clear(final int slot) {
        requireSlot(slot);
        this.config.set(KEY_PREFIX + slot, null);
        this.config.save();
    }

    public Optional<Path> get(final int slot) {
        requireSlot(slot);
        final String raw = this.config.get(KEY_PREFIX + slot);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final Path path = Path.of(raw);
        if (!Files.isDirectory(path)) {
            return Optional.empty();
        }
        return Optional.of(path.toAbsolutePath().normalize());
    }

    private static void requireSlot(final int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IllegalArgumentException("Bookmark slot must be 0–9: " + slot);
        }
    }
}
