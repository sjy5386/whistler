package com.sysbot32.whistler.file_manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileExtrasTest {
    @TempDir
    Path tempDir;

    @Test
    void commentsRoundTrip() throws Exception {
        final Path f = Files.writeString(this.tempDir.resolve("note.txt"), "body");
        FileComments.set(f, "hello comment");
        assertEquals("hello comment", FileComments.get(f).orElseThrow());
        FileComments.clear(f);
        assertTrue(FileComments.get(f).isEmpty());
    }

    @Test
    void symbolicLink() throws Exception {
        final Path target = Files.writeString(this.tempDir.resolve("t.txt"), "x");
        final Path link = this.tempDir.resolve("t.link");
        FileLinks.create(FileLinks.Kind.SYMBOLIC, link, target);
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("x", Files.readString(link));
    }

    @Test
    void hardLink() throws Exception {
        final Path target = Files.writeString(this.tempDir.resolve("h.txt"), "hard");
        final Path link = this.tempDir.resolve("h2.txt");
        FileLinks.create(FileLinks.Kind.HARD, link, target);
        assertTrue(Files.isRegularFile(link));
        assertEquals("hard", Files.readString(link));
        Files.writeString(link, "changed");
        assertEquals("changed", Files.readString(target));
    }

    @Test
    void diffDetectsDifference() throws Exception {
        final Path a = Files.writeString(this.tempDir.resolve("a.txt"), "line1\nline2\n");
        final Path b = Files.writeString(this.tempDir.resolve("b.txt"), "line1\nlineX\n");
        final FileDiff.Result same = FileDiff.compare(a, a);
        assertTrue(same.identical());
        final FileDiff.Result diff = FileDiff.compare(a, b);
        assertFalse(diff.identical());
        assertTrue(diff.report().contains("line2") || diff.report().contains("lineX"));
    }

    @Test
    void alternateStreamsIfSupported() throws Exception {
        final Path f = Files.writeString(this.tempDir.resolve("s.txt"), "data");
        if (!AlternateStreams.isSupported(f)) {
            return;
        }
        AlternateStreams.write(f, "my.stream", "value123");
        final List<AlternateStreams.StreamInfo> list = AlternateStreams.list(f);
        assertTrue(list.stream().anyMatch(s -> "my.stream".equals(s.name())));
        AlternateStreams.delete(f, "my.stream");
        assertTrue(AlternateStreams.list(f).stream().noneMatch(s -> "my.stream".equals(s.name())));
    }

    @Test
    void zipCommentRoundTrip() throws Exception {
        final Path a = Files.writeString(this.tempDir.resolve("in.txt"), "z");
        final Path zip = this.tempDir.resolve("c.zip");
        ZipArchiveFs.createZip(zip, List.of(a));
        ZipArchiveFs.setComment(zip, "archive note");
        assertEquals("archive note", ZipArchiveFs.getComment(zip));
        final Path out = Files.createDirectory(this.tempDir.resolve("zout"));
        ZipArchiveFs.extract(zip, List.of(), out, true);
        assertEquals("z", Files.readString(out.resolve("in.txt")));
    }

    @Test
    void parseVolumeSize() {
        assertEquals(1024L, FileManagerFrame.parseVolumeSize("1K"));
        assertEquals(2L * 1024 * 1024, FileManagerFrame.parseVolumeSize("2M"));
        assertEquals(100L, FileManagerFrame.parseVolumeSize("100"));
    }
}
