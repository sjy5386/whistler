package com.sysbot32.whistler.wordpad.model;

public enum DocumentKind {
    RICH_TEXT("Rich Text Document", "rtf"),
    TEXT("Text Document", "txt"),
    UNICODE_TEXT("Unicode Text Document", "txt");

    private final String displayName;
    private final String extension;

    DocumentKind(final String displayName, final String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getExtension() {
        return this.extension;
    }

    public boolean isPlainText() {
        return this != RICH_TEXT;
    }
}
