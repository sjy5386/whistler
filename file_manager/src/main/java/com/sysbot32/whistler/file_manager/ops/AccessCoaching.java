package com.sysbot32.whistler.file_manager.ops;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.nio.file.AccessDeniedException;
import java.util.Locale;

/**
 * Detect OS permission denials (macOS TCC / EPERM, Windows ACL, …) and show a short coaching dialog.
 */
@UtilityClass
public final class AccessCoaching {
    public static boolean isAccessDenied(final Throwable error) {
        Throwable t = error;
        while (t != null) {
            if (t instanceof AccessDeniedException) {
                return true;
            }
            final String msg = t.getMessage();
            if (msg != null) {
                final String m = msg.toLowerCase(Locale.ROOT);
                if (m.contains("operation not permitted")
                        || m.contains("permission denied")
                        || m.contains("access is denied")
                        || m.contains("eperm")
                        || m.contains("errno=1")
                        || m.contains("errno 1")) {
                    return true;
                }
            }
            // macOS often surfaces as IOException with "Operation not permitted"
            final String simple = t.getClass().getSimpleName();
            if ("FileSystemException".equals(simple) && msg != null
                    && msg.toLowerCase(Locale.ROOT).contains("not permitted")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * If {@code error} looks like a permission denial, show coaching and return {@code true}.
     * Otherwise return {@code false} (caller should show a generic error).
     */
    public static boolean showIfAccessDenied(final Component parent, final String title, final Throwable error) {
        if (!isAccessDenied(error)) {
            return false;
        }
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String guidance;
        if (os.contains("mac")) {
            guidance = """
                    This Mac blocked access to the file or folder (privacy / TCC).

                    Try one of the following:
                    · System Settings → Privacy & Security → Files and Folders
                      (or Full Disk Access) — allow this app / Java / Terminal
                    · Open the folder once in Finder, or move files out of
                      Desktop, Documents, or Downloads
                    · Run the app from a location that already has access

                    Technical detail:
                    """ + shortMessage(error);
        } else if (os.contains("win")) {
            guidance = """
                    Access to the file or folder was denied.

                    Check that you have permission to read/write the path,
                    and that the file is not locked by another program.

                    Technical detail:
                    """ + shortMessage(error);
        } else {
            guidance = """
                    Access to the file or folder was denied (permissions).

                    Check ownership and mode bits (chmod/chown), and that
                    the path is not a restricted mount.

                    Technical detail:
                    """ + shortMessage(error);
        }
        JOptionPane.showMessageDialog(
                parent,
                guidance,
                title == null || title.isBlank() ? "Permission denied" : title,
                JOptionPane.WARNING_MESSAGE
        );
        return true;
    }

    private static String shortMessage(final Throwable error) {
        if (error == null) {
            return "(none)";
        }
        final Throwable root = rootCause(error);
        final String m = root.getMessage();
        if (m != null && !m.isBlank()) {
            return root.getClass().getSimpleName() + ": " + m;
        }
        return root.getClass().getSimpleName();
    }

    private static Throwable rootCause(final Throwable error) {
        Throwable t = error;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
