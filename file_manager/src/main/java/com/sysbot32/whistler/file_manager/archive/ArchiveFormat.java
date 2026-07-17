package com.sysbot32.whistler.file_manager.archive;


/**
 * Supported archive container formats. Only {@link #ZIP} is implemented for now;
 * additional formats register as new {@link ArchiveOperations} backends.
 */
public enum ArchiveFormat {
    ZIP
}
