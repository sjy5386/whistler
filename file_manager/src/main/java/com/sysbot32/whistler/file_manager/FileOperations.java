package com.sysbot32.whistler.file_manager;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;

/**
 * Disk file operations used by the file manager.
 */
public final class FileOperations {
    private FileOperations() {
    }

    public static void copy(final List<Path> sources, final Path destDir) throws IOException {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(destDir, "destDir");
        Files.createDirectories(destDir);
        for (final Path source : sources) {
            copyOne(source, destDir.resolve(source.getFileName().toString()));
        }
    }

    public static void move(final List<Path> sources, final Path destDir) throws IOException {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(destDir, "destDir");
        Files.createDirectories(destDir);
        for (final Path source : sources) {
            final Path target = destDir.resolve(source.getFileName().toString());
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException ex) {
                copyOne(source, target);
                delete(List.of(source));
            }
        }
    }

    public static void delete(final List<Path> paths) throws IOException {
        for (final Path path : paths) {
            deleteRecursive(path);
        }
    }

    public static void rename(final Path path, final String newName) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(newName, "newName");
        if (newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
            throw new IOException("Invalid name: " + newName);
        }
        final Path target = path.resolveSibling(newName);
        Files.move(path, target);
    }

    public static Path mkdir(final Path parent, final String name) throws IOException {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.contains("/") || name.contains("\\") || "..".equals(name)) {
            throw new IOException("Invalid folder name: " + name);
        }
        final Path dir = parent.resolve(name);
        Files.createDirectory(dir);
        return dir;
    }

    private static void copyOne(final Path source, final Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                        throws IOException {
                    final Path relative = source.relativize(dir);
                    final Path dest = relative.toString().isEmpty() ? target : target.resolve(relative);
                    Files.createDirectories(dest);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                        throws IOException {
                    final Path dest = target.resolve(source.relativize(file));
                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            final Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteRecursive(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                        throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Files.delete(path);
        }
    }
}
