package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks that B+C FM shell wiring exists in shipped sources.
 */
class FileManagerWiringTest {
    private static final Path FRAME = Path.of(
            "src/main/java/com/sysbot32/whistler/file_manager/FileManagerFrame.java"
    );
    private static final Path APP = Path.of(
            "src/main/java/com/sysbot32/whistler/file_manager/FileManagerApplication.java"
    );
    private static final Path PANEL = Path.of(
            "src/main/java/com/sysbot32/whistler/file_manager/FilePanel.java"
    );

    @Test
    void applicationMainAndFrameExist() throws Exception {
        assertTrue(Files.isRegularFile(APP), "FileManagerApplication missing");
        assertTrue(Files.isRegularFile(FRAME), "FileManagerFrame missing");
        final String app = Files.readString(APP);
        assertTrue(app.contains("FileManagerFrame"));
        assertTrue(app.contains("file_manager.properties"));
        assertTrue(app.contains("void main") && app.contains("@UtilityClass"),
                "main entry via @UtilityClass");
    }

    @Test
    void coreFmAndArchiveToolbarWired() throws Exception {
        final String src = Files.readString(FRAME);
        for (final String token : new String[]{
                "actionAdd", "actionExtract", "actionTest",
                "actionCopy", "actionMove", "actionDelete", "actionInfo",
                "VK_F2", "VK_F5", "VK_F6", "VK_F7", "VK_F9",
                "VK_DELETE", "VK_TAB", "VK_BACK_SPACE", "VK_ENTER", "VK_INSERT",
                "CTRL_DOWN_MASK", "WHEN_IN_FOCUSED_WINDOW",
                "TransferTargets", "planZipExtract", "defaultArchiveSaveDir",
                "BrowseLocation.zip", "Archives."
        }) {
            assertTrue(src.contains(token), "missing wiring: " + token);
        }
        assertTrue(src.contains("Archives.createOrAdd") || src.contains("Archives.zip()"),
                "FM must call archive ops through Archives facade");
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/ArchiveOperations.java")),
                "ArchiveOperations surface must exist");
    }

    @Test
    void tierCSelectionViewBookmarksAndFileExtrasWired() throws Exception {
        final String frame = Files.readString(FRAME);
        final String panel = Files.readString(PANEL);
        for (final String token : new String[]{
                "selectByType", "invertSelection", "deselectAll",
                "Flat View", "setFlatView", "ViewMode",
                "FolderBookmarks", "FolderHistory", "openBookmark", "setBookmark",
                "showHistory", "openRoot", "openOutside", "actionView", "actionEdit",
                "actionCreateFile", "openSameInOther", "openCurrentInOther",
                "VK_F3", "VK_F4", "SHIFT_DOWN_MASK", "ALT_DOWN_MASK",
                "rebuildFavoritesMenu", "Add folder to Favorites as"
        }) {
            assertTrue(frame.contains(token) || panel.contains(token), "missing C wiring: " + token);
        }
        assertTrue(panel.contains("FlatListing.list"), "Flat view must call FlatListing");
        assertTrue(panel.contains("SelectionHelpers"), "selection helpers must be used");
        assertTrue(panel.contains("forView") || panel.contains("largeIcon") || panel.contains("LARGE_ICONS"),
                "Large Icons must use larger glyphs");
        final String icons = Files.readString(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/FileManagerIcons.java"));
        assertTrue(icons.contains("LARGE_ICON_SIZE"), "large icon size constant required");
        assertTrue(icons.contains("largeIcon"), "largeIcon() factory required");
    }

    @Test
    void panelOpenAndUpControlWiredLike7zFm() throws Exception {
        final String src = Files.readString(PANEL);
        assertTrue(src.contains("fileManagerOpen"), "Enter must open, not select-next-row");
        assertTrue(src.contains("selectNextRowCell"), "default Enter action remapped");
        assertTrue(src.contains("openRowAtPoint"), "double-click opens row under cursor");
        assertTrue(src.contains("upButton"), "Up One Level button beside address bar");
        assertTrue(src.contains("Up One Level"), "tooltip / label for up control");
    }

    @Test
    void dualPanelDefaultsToSingle() throws Exception {
        final String src = Files.readString(FRAME);
        assertTrue(src.contains("KEY_DUAL, \"false\"") || src.contains("get(KEY_DUAL, \"false\")"),
                "dual panel should default false");
    }

    @Test
    void contextMenuAndProgressAndZipMutationsWired() throws Exception {
        final String frame = Files.readString(FRAME);
        final String panel = Files.readString(PANEL);
        assertTrue(panel.contains("isPopupTrigger"), "listing must handle popup-trigger");
        assertTrue(panel.contains("setContextMenuHandler") || panel.contains("contextMenuHandler"),
                "panel must accept context menu handler");
        assertTrue(panel.contains("handleListingPopup"), "popup handler on listing");
        assertTrue(frame.contains("showListingContextMenu"), "frame builds context menu");
        assertTrue(frame.contains("JPopupMenu"), "uses JPopupMenu");
        // 7zFM File-menu order markers in context menu
        assertTrue(frame.contains("Open Inside\\tCtrl+PgDn") || frame.contains("Open Inside\tCtrl+PgDn"),
                "Open Inside with accelerator like 7zFM");
        assertTrue(frame.contains("Copy To...\\tF5") || frame.contains("Copy To...\tF5"),
                "Copy To... label like 7zFM");
        assertTrue(frame.contains("Add to \"") || frame.contains("actionAddQuick"),
                "quick Add to \"xxx.zip\" present");
        assertTrue(frame.contains("actionAddCreateNew") || frame.contains("Add to archive"),
                "Add to archive dialog entry present");
        assertTrue(frame.contains("AddToArchiveDialog"),
                "7zFM-style Add to Archive dialog wired");
        assertTrue(frame.contains("current directory") || frame.contains("loc.path().getFileName()"),
                "multi-select zip name uses current directory");
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/AddToArchiveDialog.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/ArchiveWriteOptions.java")));
        final String dialog = Files.readString(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/AddToArchiveDialog.java"));
        for (final String token : new String[]{
                "Archive format:", "Compression level:", "Compression method:",
                "Update mode:", "Path mode:", "Delete files after", "Help", "syncMethodEnabled"
        }) {
            assertTrue(dialog.contains(token), "dialog layout missing: " + token);
        }
        // 7z-only placeholders removed for zip-only UI
        assertTrue(!dialog.contains("Dictionary size:"), "7z dictionary control should be gone");
        assertTrue(!dialog.contains("Solid block"), "7z solid control should be gone");
        assertTrue(!dialog.contains("Create SFX"), "SFX control should be gone");
        assertTrue(!dialog.contains("Encryption"), "encryption stub panel should be gone");
        assertTrue(frame.contains("new JMenu(\"Archive\")") || frame.contains("JMenu(\"Archive\")"),
                "archive ops under cascaded Archive submenu");
        assertTrue(frame.contains("updateWindowTitle"), "window title tracks current path");
        assertTrue(frame.contains("displayPath()"), "title uses location displayPath");
        assertTrue(frame.contains("actionChecksum") || frame.contains("Checksums."), "CRC submenu wired");
        assertTrue(frame.contains("actionSplit"), "Split wired");
        assertTrue(frame.contains("actionCombine"), "Combine wired");
        assertTrue(frame.contains("actionComment"), "Comment wired");
        assertTrue(frame.contains("actionDiff"), "Diff wired");
        assertTrue(frame.contains("actionLink"), "Link wired");
        assertTrue(frame.contains("actionAlternateStreams"), "Alternate streams wired");
        // File menu uses 7zFM order (no archive block in File menu; toolbar keeps them)
        final int openIdx = frame.indexOf("menuItem(\"Open\"");
        final int splitIdx = frame.indexOf("Split file...");
        final int exitIdx = frame.indexOf("menuItem(\"Exit\"");
        assertTrue(openIdx > 0 && splitIdx > openIdx && exitIdx > splitIdx,
                "File menu order Open → Split → Exit");
        assertTrue(frame.contains("ProgressTasks.run"), "cancellable progress path");
        assertTrue(frame.contains("CollisionPolicy") || frame.contains("promptCollision"),
                "name collision chooser wired");
        assertTrue(frame.contains("deleteEntries"), "zip delete wired");
        assertTrue(frame.contains("renameEntry"), "zip rename wired");
        assertTrue(frame.contains(".mkdir(") || frame.contains("mkdir(loc"), "zip mkdir wired");
        assertTrue(frame.contains("copyOrMoveToDisk"), "zip copy/move wired");
        assertTrue(frame.contains("Archives.createOrAdd") || frame.contains("createOrAdd"),
                "add-into-existing via createOrAdd");
        assertTrue(!frame.contains("not supported in MVP"), "MVP blocks for zip mutate must be gone");
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/ArchiveOperations.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/Archives.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/ZipArchiveOperations.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/CollisionPolicy.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/TransferControl.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/ProgressTasks.java")));
    }

    @Test
    void renameAndListInsertUseCorrectShippedPaths() throws Exception {
        final String frame = Files.readString(FRAME);
        final String panel = Files.readString(PANEL);
        assertTrue(frame.contains("resolveDiskEntry(entry)"),
                "F2 rename must use resolveDiskEntry for flat-view paths");
        assertTrue(panel.contains("InsertSelection.toggleAndAdvanceLead"),
                "Insert must use InsertSelection (moveLead, not setLeadSelectionIndex range-fill)");
        assertTrue(!panel.contains("setLeadSelectionIndex(next)"),
                "setLeadSelectionIndex(next) range-fills multi-select");
        assertTrue(!panel.contains("setSelectedIndex(next)"),
                "setSelectedIndex(next) clears multi-select on Insert");
        final String model = Files.readString(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/FileTableModel.java"));
        assertTrue(model.contains("loadOrder"), "Unsorted must restore load-order snapshot");
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/java/com/sysbot32/whistler/file_manager/InsertSelection.java")));
    }
}
