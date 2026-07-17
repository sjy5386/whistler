package com.sysbot32.whistler.file_manager.model;

import com.sysbot32.whistler.file_manager.ui.FilePanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowseLocationTest {
    @TempDir
    Path tempDir;

    @Test
    void diskParentAndEnter() {
        final Path child = this.tempDir.resolve("a").resolve("b");
        final BrowseLocation loc = BrowseLocation.disk(child);
        assertTrue(loc.isDisk());
        assertEquals(BrowseLocation.disk(child.getParent()), loc.parent());
        assertEquals(BrowseLocation.disk(child.resolve("c")), loc.enterDirectory("c"));
    }

    @Test
    void zipNavigation() {
        final Path zip = this.tempDir.resolve("x.zip");
        final BrowseLocation root = BrowseLocation.zip(zip, "");
        assertTrue(root.displayPath().contains("!"));
        final BrowseLocation folder = root.enterDirectory("docs");
        assertEquals("docs/", folder.zipInternalPath());
        assertEquals("", folder.parent().zipInternalPath());

        final BrowseLocation nested = folder.enterDirectory("img");
        assertEquals("docs/img/", nested.zipInternalPath());
        assertEquals("docs/", nested.parent().zipInternalPath());

        // leave archive
        assertTrue(root.parent().isDisk());
    }

    @Test
    void parseDisplayPathRoundTripStyle() {
        final Path zip = this.tempDir.resolve("a.zip").toAbsolutePath();
        final BrowseLocation loc = FilePanel.parseDisplayPath(zip + "!/folder/sub/");
        assertTrue(loc.isZip());
        assertEquals(zip.normalize(), loc.path());
        assertEquals("folder/sub/", loc.zipInternalPath());
    }
}
