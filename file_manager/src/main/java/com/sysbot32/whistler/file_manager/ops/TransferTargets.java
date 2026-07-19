package com.sysbot32.whistler.file_manager.ops;

import com.sysbot32.whistler.file_manager.model.BrowseLocation;

import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Pure dest-path rules for copy/move/extract/add (7zFM-style other-panel default).
 */
@UtilityClass
public final class TransferTargets {
    /**
     * When dual panels are on and the inactive side is a disk folder, that path is the default target.
     */
    public static Optional<Path> otherPanelDiskPath(final boolean dualPanel, final BrowseLocation inactive) {
        if (!dualPanel || inactive == null || !inactive.isDisk()) {
            return Optional.empty();
        }
        return Optional.of(inactive.path());
    }

    /**
     * Prefer other-panel disk path for new zip save directory; else active disk path.
     */
    public static Path defaultArchiveSaveDir(
            final boolean dualPanel,
            final BrowseLocation active,
            final BrowseLocation inactive
    ) {
        return otherPanelDiskPath(dualPanel, inactive).orElseGet(() -> {
            if (active != null && active.isDisk()) {
                return active.path();
            }
            if (active != null && active.isZip() && active.path().getParent() != null) {
                return active.path().getParent();
            }
            return Path.of(System.getProperty("user.home"));
        });
    }

    /**
     * Extract destination: other panel disk if dual; else empty (caller shows chooser / dialog).
     */
    public static Optional<Path> defaultExtractDir(final boolean dualPanel, final BrowseLocation inactive) {
        return otherPanelDiskPath(dualPanel, inactive);
    }

    /**
     * Default “Extract to” folder for the dialog (7zFM-style).
     * Prefer the other panel’s disk path when dual; otherwise {@code <zip parent>/<stem>}.
     */
    public static Path defaultExtractDestination(
            final Path zipFile,
            final boolean dualPanel,
            final BrowseLocation inactive
    ) {
        final Optional<Path> other = defaultExtractDir(dualPanel, inactive);
        if (other.isPresent()) {
            return other.get();
        }
        return extractToNamedFolder(zipFile);
    }

    /** Parent of the archive (or {@code user.home} if the zip has no parent). */
    public static Path extractHereDir(final Path zipFile) {
        if (zipFile == null) {
            return Path.of(System.getProperty("user.home"));
        }
        final Path parent = zipFile.getParent();
        return parent != null ? parent : Path.of(System.getProperty("user.home"));
    }

    /** {@code archive.zip} → {@code <parent>/archive} (no extension). */
    public static Path extractToNamedFolder(final Path zipFile) {
        return extractHereDir(zipFile).resolve(archiveStemName(zipFile));
    }

    /** File name without a trailing archive extension ({@code .zip}/{@code .jar}/{@code .war}). */
    public static String archiveStemName(final Path zipFile) {
        if (zipFile == null || zipFile.getFileName() == null) {
            return "extracted";
        }
        final String name = zipFile.getFileName().toString();
        final String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (final String ext : new String[]{".zip", ".jar", ".war"}) {
            if (lower.endsWith(ext) && name.length() > ext.length()) {
                return name.substring(0, name.length() - ext.length());
            }
        }
        return name.isBlank() ? "extracted" : name;
    }

    /**
     * Paths to extract when browsing inside a zip with optional selection.
     * Empty selection at zip root → whole archive ({@code extractAll}).
     * Empty selection in a subfolder → that folder prefix.
     * Non-empty selection → those paths only.
     */
    public static ExtractPlan planZipExtract(final BrowseLocation zipLoc, final java.util.List<String> selectedInternal) {
        if (zipLoc == null || !zipLoc.isZip()) {
            throw new IllegalArgumentException("Not a zip location");
        }
        if (selectedInternal != null && !selectedInternal.isEmpty()) {
            return new ExtractPlan(false, selectedInternal);
        }
        final String prefix = zipLoc.zipInternalPath();
        if (prefix == null || prefix.isEmpty()) {
            return new ExtractPlan(true, java.util.List.of());
        }
        return new ExtractPlan(false, java.util.List.of(prefix));
    }

    public record ExtractPlan(boolean extractAll, java.util.List<String> internalPaths) {
    }
}
