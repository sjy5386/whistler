package com.sysbot32.whistler.notepad;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class Notepad {
    @Setter
    private String content = "";
    private Path path = null;

    public void createNew() {
        this.content = "";
        this.path = null;
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
}
