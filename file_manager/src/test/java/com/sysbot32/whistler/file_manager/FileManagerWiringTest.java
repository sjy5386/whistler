package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks that B shell wiring exists in shipped sources
 * (toolbar commands, core keys, zip open-inside / extract / add).
 */
class FileManagerWiringTest {
    private static final Path FRAME = Path.of(
            "src/main/java/com/sysbot32/whistler/file_manager/FileManagerFrame.java"
    );
    private static final Path APP = Path.of(
            "src/main/java/com/sysbot32/whistler/file_manager/FileManagerApplication.java"
    );

    @Test
    void applicationMainAndFrameExist() throws Exception {
        assertTrue(Files.isRegularFile(APP), "FileManagerApplication missing");
        assertTrue(Files.isRegularFile(FRAME), "FileManagerFrame missing");
        final String app = Files.readString(APP);
        assertTrue(app.contains("FileManagerFrame"));
        assertTrue(app.contains("file_manager.properties"));
        assertTrue(app.contains("public static void main"));
    }

    @Test
    void toolbarAndCoreKeyBindingsWired() throws Exception {
        final String src = Files.readString(FRAME);
        for (final String token : new String[]{
                "actionAdd", "actionExtract", "actionTest",
                "actionCopy", "actionMove", "actionDelete", "actionInfo",
                "VK_F2", "VK_F5", "VK_F6", "VK_F7", "VK_F9",
                "VK_DELETE", "VK_TAB", "VK_BACK_SPACE", "VK_ENTER", "VK_INSERT",
                "CTRL_DOWN_MASK", "WHEN_IN_FOCUSED_WINDOW",
                "TransferTargets", "planZipExtract", "defaultArchiveSaveDir",
                "BrowseLocation.zip", "ZipArchiveFs.createZip", "ZipArchiveFs.extract"
        }) {
            assertTrue(src.contains(token), "missing wiring: " + token);
        }
    }

    @Test
    void panelOpenAndUpControlWiredLike7zFm() throws Exception {
        final Path panel = Path.of("src/main/java/com/sysbot32/whistler/file_manager/FilePanel.java");
        final String src = Files.readString(panel);
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
}
