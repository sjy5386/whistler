package com.sysbot32.whistler.file_manager;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Disk file operations used by the file manager.
 */
@UtilityClass
public final class FileOperations {
    /**
     * Asks for a collision choice when {@code target} already exists.
     * Return value is fed into {@link CollisionPolicy}.
     */
    @FunctionalInterface
    public interface CollisionAsk {
        CollisionPolicy.UserChoice ask(Path source, Path target);
    }

    public static void copy(final List<Path> sources, final Path destDir) throws IOException {
        copy(sources, destDir, null, null);
    }

    public static void copy(
            final List<Path> sources,
            final Path destDir,
            final CollisionAsk collisionAsk,
            final TransferControl control
    ) throws IOException {
        transfer(sources, destDir, false, collisionAsk, control);
    }

    public static void move(final List<Path> sources, final Path destDir) throws IOException {
        move(sources, destDir, null, null);
    }

    public static void move(
            final List<Path> sources,
            final Path destDir,
            final CollisionAsk collisionAsk,
            final TransferControl control
    ) throws IOException {
        transfer(sources, destDir, true, collisionAsk, control);
    }

    private static void transfer(
            final List<Path> sources,
            final Path destDir,
            final boolean move,
            final CollisionAsk collisionAsk,
            final TransferControl control
    ) throws IOException {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(destDir, "destDir");
        Files.createDirectories(destDir);
        if (control != null) {
            control.begin(sources.size());
        }
        final CollisionPolicy.Session session = new CollisionPolicy.Session();
        for (final Path source : sources) {
            if (control != null) {
                control.throwIfCancelled();
            }
            final Path target = destDir.resolve(source.getFileName().toString());
            final boolean exists = Files.exists(target);
            CollisionPolicy.UserChoice choice = null;
            if (exists && session.remember() == CollisionPolicy.Remember.NONE) {
                if (collisionAsk == null) {
                    choice = CollisionPolicy.UserChoice.OVERWRITE;
                } else {
                    choice = collisionAsk.ask(source, target);
                }
            }
            final CollisionPolicy.Decision decision = session.decide(exists, choice);
            if (decision.action() == CollisionPolicy.Action.ABORT) {
                throw new CancellationException("Cancelled");
            }
            if (decision.action() == CollisionPolicy.Action.SKIP) {
                if (control != null) {
                    control.advance("Skipped " + source.getFileName());
                }
                continue;
            }
            if (move) {
                moveOne(source, target, decision.action() == CollisionPolicy.Action.PROCEED && exists);
            } else {
                copyOne(source, target, true);
            }
            if (control != null) {
                control.advance((move ? "Moved " : "Copied ") + source.getFileName());
            }
        }
    }

    public static void delete(final List<Path> paths) throws IOException {
        delete(paths, null);
    }

    public static void delete(final List<Path> paths, final TransferControl control) throws IOException {
        if (control != null) {
            control.begin(paths.size());
        }
        for (final Path path : paths) {
            if (control != null) {
                control.throwIfCancelled();
            }
            deleteRecursive(path, control);
            if (control != null) {
                control.advance("Deleted " + (path.getFileName() != null ? path.getFileName() : path));
            }
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

    /** Create an empty file (7zFM Create File / Shift+F4). */
    public static Path createFile(final Path parent, final String name) throws IOException {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || name.contains("/") || name.contains("\\") || "..".equals(name)) {
            throw new IOException("Invalid file name: " + name);
        }
        final Path file = parent.resolve(name);
        Files.createFile(file);
        return file;
    }

    private static void moveOne(final Path source, final Path target, final boolean replace) throws IOException {
        try {
            if (replace) {
                if (Files.isDirectory(target) && Files.isDirectory(source)) {
                    // fall through to copy+delete for directory merge/replace
                    throw new IOException("use copy");
                }
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        } catch (final IOException ex) {
            copyOne(source, target, replace);
            deleteRecursive(source, null);
        }
    }

    private static void copyOne(final Path source, final Path target, final boolean replace) throws IOException {
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
                    if (replace) {
                        Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.copy(file, dest);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            final Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (replace) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, target);
            }
        }
    }

    private static void deleteRecursive(final Path path, final TransferControl control) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                        throws IOException {
                    if (control != null) {
                        control.throwIfCancelled();
                    }
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                        throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    if (control != null) {
                        control.throwIfCancelled();
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
