package com.devoxx.genie.service.agent;

import com.devoxx.genie.service.agent.AgentFileChangeTracker.FileChange;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentFileChangeTrackerTest {

    @Mock
    private Project project;

    private AgentFileChangeTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new AgentFileChangeTracker(project);
        tracker.startRun();
    }

    // --- recording ---

    @Test
    void recordBeforeWrite_capturesSnapshotForDrain() {
        tracker.recordBeforeWrite("src/Foo.java", fileAt("/p/src/Foo.java"), "old\n");

        List<FileChange> changes = tracker.drainInto("msg-1");

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).displayPath()).isEqualTo("src/Foo.java");
        assertThat(changes.get(0).absolutePath()).isEqualTo("/p/src/Foo.java");
        assertThat(changes.get(0).before()).isEqualTo("old\n");
        assertThat(changes.get(0).diffable()).isTrue();
    }

    @Test
    void recordBeforeWrite_firstSnapshotWinsSoARunShowsOneCumulativeDiff() {
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "version one");
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "version two");
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "version three");

        List<FileChange> changes = tracker.drainInto("msg-1");

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).before()).isEqualTo("version one");
    }

    @Test
    void recordBeforeWrite_normalizesLineEndings() {
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "one\r\ntwo\r\n");

        assertThat(tracker.drainInto("msg-1").get(0).before()).isEqualTo("one\ntwo\n");
    }

    @Test
    void recordBeforeWrite_newFileHasEmptyBeforeAndIsStillDiffable() {
        tracker.recordBeforeWrite("New.java", fileAt("/p/New.java"), null);

        FileChange change = tracker.drainInto("msg-1").get(0);
        assertThat(change.before()).isEmpty();
        assertThat(change.diffable()).isTrue();
    }

    @Test
    void recordBeforeWrite_oversizedFileIsListedButNotDiffable() {
        String huge = "x".repeat(AgentFileChangeTracker.MAX_SNAPSHOT_BYTES + 1);

        tracker.recordBeforeWrite("Big.java", fileAt("/p/Big.java"), huge);

        FileChange change = tracker.drainInto("msg-1").get(0);
        assertThat(change.before()).isNull();
        assertThat(change.diffable()).isFalse();
    }

    @Test
    void recordBeforeWrite_multipleFilesAreAllReported() {
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "a");
        tracker.recordBeforeWrite("B.java", fileAt("/p/B.java"), "b");

        assertThat(tracker.drainInto("msg-1"))
                .extracting(FileChange::displayPath)
                .containsExactlyInAnyOrder("A.java", "B.java");
    }

    // --- run boundaries ---

    @Test
    void startRun_discardsChangesFromAPreviousUnfinishedRun() {
        tracker.recordBeforeWrite("Stale.java", fileAt("/p/Stale.java"), "stale");

        tracker.startRun();
        tracker.recordBeforeWrite("Fresh.java", fileAt("/p/Fresh.java"), "fresh");

        assertThat(tracker.drainInto("msg-1"))
                .extracting(FileChange::displayPath)
                .containsExactly("Fresh.java");
    }

    @Test
    void drainInto_emptiesTheBufferSoTheNextRunStartsClean() {
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "a");

        assertThat(tracker.drainInto("msg-1")).hasSize(1);
        assertThat(tracker.drainInto("msg-2")).isEmpty();
    }

    @Test
    void drainInto_noChanges_returnsEmpty() {
        assertThat(tracker.drainInto("msg-1")).isEmpty();
    }

    // --- retention / lookup ---

    @Test
    void findChange_returnsTheRetainedSnapshotForItsMessage() {
        tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "a");
        tracker.drainInto("msg-1");

        assertThat(tracker.findChange("msg-1", "/p/A.java")).isPresent();
        assertThat(tracker.findChange("msg-1", "/p/Other.java")).isEmpty();
        assertThat(tracker.findChange("unknown-msg", "/p/A.java")).isEmpty();
    }

    @Test
    void findChange_olderRunsAgeOutOfTheRetentionWindow() {
        // 21 runs with a window of 20: the first must have been evicted, the last retained.
        for (int i = 0; i < 21; i++) {
            tracker.startRun();
            tracker.recordBeforeWrite("A.java", fileAt("/p/A.java"), "run " + i);
            tracker.drainInto("msg-" + i);
        }

        assertThat(tracker.findChange("msg-0", "/p/A.java")).isEmpty();
        assertThat(tracker.findChange("msg-20", "/p/A.java")).isPresent();
    }

    // --- line counts ---

    @Test
    void countLineChanges_countsAddedAndRemovedLines() {
        int[] counts = AgentFileChangeTracker.countLineChanges(
                "one\ntwo\nthree\n",
                "one\nTWO\nthree\nfour\n");

        // "two" -> "TWO" is one removed + one added, "four" is a second addition.
        assertThat(counts[0]).isEqualTo(2);
        assertThat(counts[1]).isEqualTo(1);
    }

    @Test
    void countLineChanges_pureAddition() {
        int[] counts = AgentFileChangeTracker.countLineChanges("one\n", "one\ntwo\nthree\n");

        assertThat(counts[0]).isEqualTo(2);
        assertThat(counts[1]).isZero();
    }

    @Test
    void countLineChanges_pureDeletion() {
        int[] counts = AgentFileChangeTracker.countLineChanges("one\ntwo\nthree\n", "one\n");

        assertThat(counts[0]).isZero();
        assertThat(counts[1]).isEqualTo(2);
    }

    @Test
    void countLineChanges_identicalContentHasNoChanges() {
        int[] counts = AgentFileChangeTracker.countLineChanges("same\ncontent\n", "same\ncontent\n");

        assertThat(counts[0]).isZero();
        assertThat(counts[1]).isZero();
    }

    @Test
    void fileName_isDerivedFromTheDisplayPath() {
        FileChange change = new FileChange("/p/src/main/Foo.java", "src/main/Foo.java", "", 0, 0);

        assertThat(change.fileName()).isEqualTo("Foo.java");
    }

    private VirtualFile fileAt(String path) {
        VirtualFile file = mock(VirtualFile.class);
        when(file.getPath()).thenReturn(path);
        return file;
    }
}
