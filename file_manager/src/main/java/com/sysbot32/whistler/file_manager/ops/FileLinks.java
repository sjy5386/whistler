package com.sysbot32.whistler.file_manager.ops;


import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Create hard / symbolic links (7zFM Link dialog subset; no Windows junctions).
 */
@UtilityClass
public final class FileLinks {
    public enum Kind {
        HARD,
        SYMBOLIC
    }
    /**
     * @param linkPath path of the new link to create
     * @param target   existing path the link points to (or hard-link target file)
     */
    public static void create(final Kind kind, final Path linkPath, final Path target) throws IOException {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(linkPath, "linkPath");
        Objects.requireNonNull(target, "target");
        if (Files.exists(linkPath)) {
            throw new FileAlreadyExistsException(linkPath.toString());
        }
        final Path parent = linkPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        switch (kind) {
            case HARD -> {
                if (!Files.isRegularFile(target)) {
                    throw new IOException("Hard link target must be an existing regular file");
                }
                Files.createLink(linkPath, target);
            }
            case SYMBOLIC -> {
                if (!Files.exists(target)) {
                    throw new IOException("Symbolic link target does not exist: " + target);
                }
                Files.createSymbolicLink(linkPath, target);
            }
            default -> throw new IOException("Unsupported link kind: " + kind);
        }
    }
}
