package com.sysbot32.whistler.file_manager.model;

import com.sysbot32.whistler.file_manager.listing.DirectoryListing;
import com.sysbot32.whistler.file_manager.ui.FilePanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers load-in-flight → navigate: stale complete must not apply, latest token wins.
 * Uses the shipped {@link ListingLoadGate} + real {@link DirectoryListing} (no UI oracle).
 */
class ListingLoadGateTest {
    @TempDir
    Path tempDir;

    @Test
    void nextTokenInvalidatesPrevious() {
        final ListingLoadGate gate = new ListingLoadGate();
        final long a = gate.nextToken();
        final long b = gate.nextToken();
        assertFalse(gate.isCurrent(a), "load A must be stale after navigate/refresh B");
        assertTrue(gate.isCurrent(b));
        assertEquals(b, gate.getGeneration());
    }

    @Test
    void loadInFlightNavigateDoesNotApplyStaleListing() throws Exception {
        final Path dirA = Files.createDirectory(this.tempDir.resolve("dirA"));
        final Path dirB = Files.createDirectory(this.tempDir.resolve("dirB"));
        Files.writeString(dirA.resolve("only-in-A.txt"), "A");
        Files.writeString(dirB.resolve("only-in-B.txt"), "B");

        final ListingLoadGate gate = new ListingLoadGate();
        final AtomicReference<List<FileEntry>> applied = new AtomicReference<>();
        final AtomicReference<BrowseLocation> appliedFor = new AtomicReference<>();

        // Start load A
        final BrowseLocation locA = BrowseLocation.disk(dirA);
        final long tokenA = gate.nextToken();
        final CountDownLatch aListed = new CountDownLatch(1);
        final CountDownLatch aMayFinish = new CountDownLatch(1);
        final CountDownLatch aDone = new CountDownLatch(1);

        final Thread loadA = new Thread(() -> {
            try {
                final List<FileEntry> listed = DirectoryListing.list(locA.path(), true);
                aListed.countDown();
                aMayFinish.await(5, TimeUnit.SECONDS);
                // Apply only if still current — same rule as FilePanel.refreshAsync done()
                if (gate.isCurrent(tokenA)) {
                    applied.set(listed);
                    appliedFor.set(locA);
                }
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                aDone.countDown();
            }
        }, "load-A");
        loadA.start();
        assertTrue(aListed.await(5, TimeUnit.SECONDS), "load A should list");

        // Navigate to B while A is still in flight (not yet applied)
        final BrowseLocation locB = BrowseLocation.disk(dirB);
        final long tokenB = gate.nextToken();
        assertFalse(gate.isCurrent(tokenA));
        assertTrue(gate.isCurrent(tokenB));

        // A finishes — must not apply
        aMayFinish.countDown();
        assertTrue(aDone.await(5, TimeUnit.SECONDS));
        assertTrue(applied.get() == null, "stale load A must not apply after navigate to B");

        // Load B and apply under token B
        final List<FileEntry> listedB = DirectoryListing.list(locB.path(), true);
        if (gate.isCurrent(tokenB)) {
            applied.set(listedB);
            appliedFor.set(locB);
        }

        assertEquals(locB, appliedFor.get());
        assertTrue(
                applied.get().stream().anyMatch(e -> "only-in-B.txt".equals(e.name())),
                "applied listing must be for B"
        );
        assertFalse(
                applied.get().stream().anyMatch(e -> "only-in-A.txt".equals(e.name())),
                "A's files must not appear after navigate to B"
        );
    }

    @Test
    void simulatePanelApplyPathWithGate() throws Exception {
        // Mirrors FilePanel: each refresh takes a token; only current token mutates model list
        final Path d1 = Files.createDirectory(this.tempDir.resolve("p1"));
        final Path d2 = Files.createDirectory(this.tempDir.resolve("p2"));
        Files.writeString(d1.resolve("x1"), "1");
        Files.writeString(d2.resolve("x2"), "2");

        final ListingLoadGate gate = new ListingLoadGate();
        final List<String> modelNames = new ArrayList<>();

        final long t1 = gate.nextToken();
        final List<FileEntry> list1 = DirectoryListing.list(d1, true);
        final long t2 = gate.nextToken(); // navigate before apply t1
        final List<FileEntry> list2 = DirectoryListing.list(d2, true);

        if (gate.isCurrent(t1)) {
            modelNames.clear();
            list1.forEach(e -> modelNames.add(e.name()));
        }
        if (gate.isCurrent(t2)) {
            modelNames.clear();
            list2.forEach(e -> modelNames.add(e.name()));
        }

        assertTrue(modelNames.contains("x2"));
        assertFalse(modelNames.contains("x1"));
    }
}
