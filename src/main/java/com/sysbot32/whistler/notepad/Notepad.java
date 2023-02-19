package com.sysbot32.whistler.notepad;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
public class Notepad {
    private String content = "";
    private boolean edited = false;
    private Path path = null;

    public Notepad(final Path path) {
        try {
            this.open(path);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

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
        this.edited = false;
        this.path = path;
        return this.content;
    }

    public void save(final Path path) throws IOException {
        Files.writeString(path, this.content);
        this.edited = false;
        this.path = path;
    }

    public String insert(final int offset, final String str) {
        this.content = new StringBuilder(this.content).insert(offset, str).toString();
        this.edited = true;
        return this.content;
    }

    public String replace(final int start, final int end, final String str) {
        this.content = new StringBuilder(this.content).replace(start, end, str).toString();
        this.edited = true;
        return this.content;
    }

    public int find(final String str, final int fromIndex) {
        return this.content.indexOf(str, fromIndex);
    }

    public static String getTimeDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("a h:mm yyyy-MM-dd"));
    }
}
