package com.sysbot32.whistler.file_manager.ops;

import java.nio.file.Path;

/**
 * Called from a background extract when {@link com.sysbot32.whistler.file_manager.archive.ArchiveExtractOptions.OverwriteMode#ASK}
 * hits an existing target. Implementations must be safe to call off the EDT
 * (typically via {@link javax.swing.SwingUtilities#invokeAndWait}).
 */
@FunctionalInterface
public interface ExtractCollisionAsk {
    /**
     * @param target      resolved output path that already exists
     * @param entryName   archive entry name (for display)
     * @return user choice; {@link CollisionPolicy.UserChoice#CANCEL} aborts the whole extract
     */
    CollisionPolicy.UserChoice ask(Path target, String entryName);
}
