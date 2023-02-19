package com.sysbot32.whistler.notepad;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class Notepad {
    private String content = "";
    private boolean edited = false;
    private Path path = null;

    public void createNew() {
        this.content = "";
        this.path = null;
    }

    public void setContent(final String content) {
        this.edited = !this.content.equals(content);
        this.content = content;
    }

    public String open(final Path path) throws IOException {
        this.content = Files.readString(path);
        this.path = path;
        return this.content;
    }

    public void save(final Path path) throws IOException {
        Files.writeString(path, this.content);
        this.path = path;
    }

    public int find(final String str, final int fromIndex) {
        return this.content.indexOf(str, fromIndex);
    }
}
