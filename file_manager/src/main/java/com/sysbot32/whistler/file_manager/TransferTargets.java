package com.sysbot32.whistler.file_manager;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Pure dest-path rules for copy/move/extract/add (7zFM-style other-panel default).
 */
public final class TransferTargets {
    private TransferTargets() {
    }

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
     * Extract destination: other panel disk if dual; else empty (caller shows chooser).
     */
    public static Optional<Path> defaultExtractDir(final boolean dualPanel, final BrowseLocation inactive) {
        return otherPanelDiskPath(dualPanel, inactive);
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
