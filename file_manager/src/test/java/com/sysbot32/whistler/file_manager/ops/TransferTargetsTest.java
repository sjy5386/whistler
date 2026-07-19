package com.sysbot32.whistler.file_manager.ops;

import com.sysbot32.whistler.file_manager.model.BrowseLocation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTargetsTest {
    @TempDir
    Path tempDir;

    @Test
    void otherPanelDiskPathOnlyWhenDualAndDisk() {
        final BrowseLocation disk = BrowseLocation.disk(this.tempDir);
        final BrowseLocation zip = BrowseLocation.zip(this.tempDir.resolve("a.zip"), "");

        assertTrue(TransferTargets.otherPanelDiskPath(true, disk).isPresent());
        assertEquals(this.tempDir.toAbsolutePath().normalize(),
                TransferTargets.otherPanelDiskPath(true, disk).orElseThrow());
        assertFalse(TransferTargets.otherPanelDiskPath(false, disk).isPresent());
        assertFalse(TransferTargets.otherPanelDiskPath(true, zip).isPresent());
    }

    @Test
    void defaultArchiveSaveDirPrefersOtherPanel() {
        final Path other = this.tempDir.resolve("other");
        final BrowseLocation active = BrowseLocation.disk(this.tempDir);
        final BrowseLocation inactive = BrowseLocation.disk(other);

        assertEquals(
                other.toAbsolutePath().normalize(),
                TransferTargets.defaultArchiveSaveDir(true, active, inactive)
        );
        assertEquals(
                this.tempDir.toAbsolutePath().normalize(),
                TransferTargets.defaultArchiveSaveDir(false, active, inactive)
        );
    }

    @Test
    void planZipExtractWholeAtRootSelectedNone() {
        final BrowseLocation root = BrowseLocation.zip(this.tempDir.resolve("x.zip"), "");
        final TransferTargets.ExtractPlan plan = TransferTargets.planZipExtract(root, List.of());
        assertTrue(plan.extractAll());
        assertTrue(plan.internalPaths().isEmpty());
    }

    @Test
    void planZipExtractCurrentFolderWhenNestedNoSelection() {
        final BrowseLocation nested = BrowseLocation.zip(this.tempDir.resolve("x.zip"), "docs/");
        final TransferTargets.ExtractPlan plan = TransferTargets.planZipExtract(nested, List.of());
        assertFalse(plan.extractAll());
        assertEquals(List.of("docs/"), plan.internalPaths());
    }

    @Test
    void planZipExtractUsesSelection() {
        final BrowseLocation root = BrowseLocation.zip(this.tempDir.resolve("x.zip"), "docs/");
        final TransferTargets.ExtractPlan plan =
                TransferTargets.planZipExtract(root, List.of("docs/a.txt", "docs/b/"));
        assertFalse(plan.extractAll());
        assertEquals(2, plan.internalPaths().size());
    }

    @Test
    void archiveStemAndExtractDestinations() {
        final Path zip = this.tempDir.resolve("MyPack.zip");
        assertEquals("MyPack", TransferTargets.archiveStemName(zip));
        assertEquals(this.tempDir, TransferTargets.extractHereDir(zip));
        assertEquals(this.tempDir.resolve("MyPack"), TransferTargets.extractToNamedFolder(zip));

        final Path other = this.tempDir.resolve("other");
        final BrowseLocation inactive = BrowseLocation.disk(other);
        assertEquals(
                other.toAbsolutePath().normalize(),
                TransferTargets.defaultExtractDestination(zip, true, inactive)
        );
        assertEquals(
                this.tempDir.resolve("MyPack"),
                TransferTargets.defaultExtractDestination(zip, false, inactive)
        );
    }
}
