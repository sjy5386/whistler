package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks that B+C FM shell wiring exists in shipped sources.
 */
class FileManagerWiringTest {
    private static final Path ROOT = Path.of("src/main/java/com/sysbot32/whistler/file_manager");
    private static final Path FRAME = ROOT.resolve("FileManagerFrame.java");
    private static final Path APP = ROOT.resolve("FileManagerApplication.java");
    private static final Path PANEL = ROOT.resolve("ui/FilePanel.java");
    private static final Path ICONS = ROOT.resolve("ui/FileManagerIcons.java");
    private static final Path DIALOG = ROOT.resolve("ui/AddToArchiveDialog.java");
    private static final Path EXTRACT_DIALOG = ROOT.resolve("ui/ExtractDialog.java");
    private static final Path TABLE_MODEL = ROOT.resolve("ui/FileTableModel.java");
    private static final Path INSERT = ROOT.resolve("listing/InsertSelection.java");
    private static final Path ARCHIVE_OPS = ROOT.resolve("archive/ArchiveOperations.java");
    private static final Path ARCHIVE_OPTS = ROOT.resolve("archive/ArchiveWriteOptions.java");
    private static final Path EXTRACT_OPTS = ROOT.resolve("archive/ArchiveExtractOptions.java");

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
        assertTrue(Files.isRegularFile(ARCHIVE_OPS), "ArchiveOperations surface must exist");
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
        final String icons = Files.readString(ICONS);
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
        assertTrue(Files.isRegularFile(DIALOG));
        assertTrue(Files.isRegularFile(ARCHIVE_OPTS));
        final String dialog = Files.readString(DIALOG);
        for (final String token : new String[]{
                "Archive format:", "Compression level:", "Compression method:",
                "Update mode:", "Path mode:", "Delete files after", "Help", "syncMethodEnabled",
                "Relative pathnames", "No pathnames"
        }) {
            assertTrue(dialog.contains(token), "dialog layout missing: " + token);
        }
        assertTrue(!dialog.contains("setEnabled(false); // backend always uses relative"),
                "path mode should be enabled");
        assertTrue(!dialog.contains("Dictionary size:"), "7z dictionary control should be gone");
        assertTrue(!dialog.contains("Solid block"), "7z solid control should be gone");
        assertTrue(!dialog.contains("Create SFX"), "SFX control should be gone");
        assertTrue(!dialog.contains("Encryption"), "encryption stub panel should be gone");
        assertTrue(frame.contains("ExtractDialog"), "7zFM-style Extract dialog wired");
        assertTrue(frame.contains("actionExtractHere") || frame.contains("Extract Here"),
                "quick Extract Here present");
        assertTrue(frame.contains("actionExtractToNamedFolder") || frame.contains("Extract to \""),
                "quick Extract to \"name\\\" present");
        assertTrue(Files.isRegularFile(EXTRACT_DIALOG));
        assertTrue(Files.isRegularFile(EXTRACT_OPTS));
        final String extractDialog = Files.readString(EXTRACT_DIALOG);
        for (final String token : new String[]{
                "Extract to:", "Path mode:", "Overwrite mode:",
                "Eliminate duplication of root folder", "Help",
                "Ask before overwrite"
        }) {
            assertTrue(extractDialog.contains(token), "extract dialog layout missing: " + token);
        }
        assertTrue(!extractDialog.contains("Enter password"), "password field not productized yet");
        assertTrue(frame.contains("promptExtractCollision") || frame.contains("ExtractCollisionAsk"),
                "extract ask overwrite wired");
        assertTrue(frame.contains("handleFilesDropped") || frame.contains("setDropFilesHandler"),
                "DnD drop handler wired");
        assertTrue(panel.contains("installDragAndDrop") || panel.contains("ListingTransferHandler")
                        || panel.contains("javaFileListFlavor"),
                "panel drag-and-drop wired");
        assertTrue(frame.contains("AccessCoaching"), "TCC/permission coaching wired");
        assertTrue(Files.isRegularFile(ROOT.resolve("ops/AccessCoaching.java")));
        assertTrue(frame.contains("deleteEntries"), "zip delete wired");
        assertTrue(frame.contains("renameEntry"), "zip rename wired");
        assertTrue(frame.contains(".mkdir(") || frame.contains("mkdir(loc"), "zip mkdir wired");
        assertTrue(frame.contains("copyOrMoveToDisk"), "zip copy/move wired");
        assertTrue(frame.contains("Archives.createOrAdd") || frame.contains("createOrAdd"),
                "add-into-existing via createOrAdd");
        assertTrue(!frame.contains("not supported in MVP"), "MVP blocks for zip mutate must be gone");
        assertTrue(Files.isRegularFile(ROOT.resolve("ops/CollisionPolicy.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve("ops/TransferControl.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve("ui/ProgressTasks.java")));
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
        final String model = Files.readString(TABLE_MODEL);
        assertTrue(model.contains("loadOrder"), "Unsorted must restore load-order snapshot");
        assertTrue(Files.isRegularFile(INSERT));
    }

    @Test
    void packagesAreSplitByRole() {
        assertTrue(Files.isDirectory(ROOT.resolve("ui")));
        assertTrue(Files.isDirectory(ROOT.resolve("model")));
        assertTrue(Files.isDirectory(ROOT.resolve("listing")));
        assertTrue(Files.isDirectory(ROOT.resolve("ops")));
        assertTrue(Files.isDirectory(ROOT.resolve("archive")));
        assertTrue(Files.isRegularFile(ROOT.resolve("FileManagerFrame.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve("ui/FilePanel.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve("archive/ZipArchiveFs.java")));
        assertTrue(Files.isRegularFile(ROOT.resolve("ops/FileOperations.java")));
    }
}
