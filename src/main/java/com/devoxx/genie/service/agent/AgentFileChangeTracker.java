package com.devoxx.genie.service.agent;

import com.devoxx.genie.util.ReadAccess;
import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records the files an agent run modifies so the chat can offer a post-hoc "Agent changed N
 * files" review (issue #705). This complements the diff in the write-approval dialog: when
 * writes are auto-approved there is no dialog, and this is the only way to see what happened.
 *
 * <p>Flow: {@link #startRun()} at tool-provider creation (once per prompt), the write
 * executors call {@link #recordBeforeWrite} just before mutating a file, and the prompt
 * strategies {@link #drainInto(String)} the result onto the finished chat message.
 *
 * <p>Only the <em>first</em> snapshot of a file within a run is kept, so a file edited five
 * times shows one cumulative diff of the whole run rather than five separate ones.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class AgentFileChangeTracker {

    /** Files above this size are listed without a diff — snapshotting them is not worth the heap. */
    static final int MAX_SNAPSHOT_BYTES = 1024 * 1024;

    /** How many finished runs keep their snapshots, so older chat messages stay clickable. */
    private static final int RETAINED_RUNS = 20;

    /**
     * One file touched by an agent run.
     *
     * @param before the LF-normalized content before the run, or null when it was not captured
     *               (new file, or larger than {@link #MAX_SNAPSHOT_BYTES}) — a null before means
     *               the row is shown but cannot be diffed
     */
    public record FileChange(@NotNull String absolutePath,
                             @NotNull String displayPath,
                             @Nullable String before,
                             int linesAdded,
                             int linesRemoved) {

        public @NotNull String fileName() {
            return PathUtil.getFileName(displayPath);
        }

        public boolean diffable() {
            return before != null;
        }
    }

    private final Project project;

    /** Snapshots for the run currently in flight, keyed by absolute path. First write wins. */
    private final Map<String, PendingChange> currentRun = new ConcurrentHashMap<>();

    /** Finished runs, keyed by chat message id. Access is synchronized on the map itself. */
    private final Map<String, List<FileChange>> finishedRuns = new LinkedHashMap<>();

    public AgentFileChangeTracker(@NotNull Project project) {
        this.project = project;
    }

    public static @NotNull AgentFileChangeTracker getInstance(@NotNull Project project) {
        return project.getService(AgentFileChangeTracker.class);
    }

    /** Clears the in-flight buffer. Called once per prompt, before any tool can run. */
    public void startRun() {
        currentRun.clear();
    }

    /**
     * Captures a file's content before an agent tool overwrites it. Safe to call repeatedly for
     * the same file — only the first call in a run is kept, so the diff spans the whole run.
     *
     * @param file    the file about to be written, or null when it is being created
     * @param rawBefore the file's current raw content, or null when the file does not exist yet
     */
    public void recordBeforeWrite(@NotNull String displayPath,
                                  @Nullable VirtualFile file,
                                  @Nullable String rawBefore) {
        String absolutePath = file != null ? file.getPath() : displayPath;
        String before = rawBefore == null ? "" : normalize(rawBefore);
        if (rawBefore != null && rawBefore.length() > MAX_SNAPSHOT_BYTES) {
            before = null;
        }
        currentRun.putIfAbsent(absolutePath, new PendingChange(absolutePath, displayPath, before));
    }

    /**
     * Moves the in-flight run onto a finished chat message and returns its changes. Line counts
     * are computed here, against the files as they stand once the run is over.
     *
     * @return the files this run changed, empty when it changed none
     */
    public @NotNull List<FileChange> drainInto(@NotNull String messageId) {
        List<PendingChange> pending = new ArrayList<>(currentRun.values());
        currentRun.clear();

        if (pending.isEmpty()) {
            return List.of();
        }

        List<FileChange> changes = new ArrayList<>(pending.size());
        for (PendingChange p : pending) {
            String after = readCurrentContent(p.absolutePath());
            // The file may have been deleted or moved after the write; without an "after" there
            // is nothing meaningful to count, so report it as touched with no line stats.
            int[] counts = (p.before() == null || after == null)
                    ? new int[]{0, 0}
                    : countLineChanges(p.before(), after);
            changes.add(new FileChange(p.absolutePath(), p.displayPath(), p.before(), counts[0], counts[1]));
        }

        synchronized (finishedRuns) {
            finishedRuns.put(messageId, List.copyOf(changes));
            // Bounded so a long session cannot pin every snapshot it ever took in memory.
            while (finishedRuns.size() > RETAINED_RUNS) {
                var oldest = finishedRuns.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
        }

        return List.copyOf(changes);
    }

    /** Looks up a retained change, absent once its run has aged out of the retention window. */
    public @NotNull Optional<FileChange> findChange(@NotNull String messageId, @NotNull String absolutePath) {
        synchronized (finishedRuns) {
            return finishedRuns.getOrDefault(messageId, List.of()).stream()
                    .filter(c -> c.absolutePath().equals(absolutePath))
                    .findFirst();
        }
    }

    /**
     * Opens the IDE diff for one changed file: the snapshot taken before the run against the
     * file as it is now. The right side is the live virtual file, so the diff keeps up if the
     * user edits it while the window is open. Must be called on the EDT.
     */
    public void showDiff(@NotNull FileChange change) {
        String before = change.before();
        if (before == null) {
            log.debug("No snapshot retained for {}, cannot show diff", change.displayPath());
            return;
        }

        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(change.absolutePath());
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(change.fileName());

        SimpleDiffRequest request = new SimpleDiffRequest(
                "Agent changes — " + change.displayPath(),
                contentFactory.create(project, before, fileType),
                file != null ? contentFactory.create(project, file)
                             : contentFactory.create(project, "", fileType),
                "Before agent run",
                "Current");

        DiffManager.getInstance().showDiff(project, request);
    }

    private @Nullable String readCurrentContent(@NotNull String absolutePath) {
        try {
            return ReadAccess.compute(() -> {
                VirtualFile file = LocalFileSystem.getInstance().findFileByPath(absolutePath);
                if (file == null || !file.exists() || file.isDirectory()) {
                    return null;
                }
                try {
                    return normalize(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.debug("Could not re-read {} for change stats", absolutePath, e);
                    return null;
                }
            });
        } catch (Exception e) {
            log.debug("Could not read {} for change stats", absolutePath, e);
            return null;
        }
    }

    /** @return {linesAdded, linesRemoved} */
    static int[] countLineChanges(@NotNull String before, @NotNull String after) {
        try {
            Diff.Change change = Diff.buildChanges(before.split("\n", -1), after.split("\n", -1));
            int added = 0;
            int removed = 0;
            for (Diff.Change c = change; c != null; c = c.link) {
                added += c.inserted;
                removed += c.deleted;
            }
            return new int[]{added, removed};
        } catch (FilesTooBigForDiffException e) {
            log.debug("File too big for line change stats", e);
            return new int[]{0, 0};
        }
    }

    private static @NotNull String normalize(@NotNull String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private record PendingChange(@NotNull String absolutePath,
                                 @NotNull String displayPath,
                                 @Nullable String before) {
    }
}
