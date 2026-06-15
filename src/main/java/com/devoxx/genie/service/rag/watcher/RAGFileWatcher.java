package com.devoxx.genie.service.rag.watcher;

import com.devoxx.genie.service.rag.ProjectIndexerService;
import com.devoxx.genie.service.rag.manifest.IndexManifest;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Listens to VFS changes for one project and re-embeds any tracked file the user saves.
 * Two design choices to keep the user out of harm's way:
 *
 * <ul>
 *   <li><b>Debounced</b> ({@link #DEBOUNCE_MILLIS}ms): rapid edits coalesce into one
 *       reindex pass so Ctrl+S-on-every-line doesn't hammer Ollama.</li>
 *   <li><b>Tracked-only</b>: a file is only auto-reindexed if it appears in the manifest.
 *       Auto-indexing brand-new files would mean a stray "save" in a generated/binary tree
 *       could silently bootstrap an enormous unintended index. The user must run the
 *       initial "Index Files" action; the watcher then keeps that set fresh.</li>
 * </ul>
 *
 * <p>Project-level service: created lazily on first {@link #getInstance} call and disposed
 * with the project. The {@code postStartupActivity} extension wires it up at project open.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class RAGFileWatcher implements Disposable {

    /** Quiet period after the last change before reindexing fires. */
    static final long DEBOUNCE_MILLIS = 2_000;

    private final Project project;
    /** Cached at construction so the scheduler thread doesn't need to hit a static lookup. */
    private final ProjectIndexerService indexer;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DevoxxGenie-RAG-Watcher");
                t.setDaemon(true);
                return t;
            });
    private final Set<Path> pendingChanges = new HashSet<>();
    private final Set<Path> pendingDeletes = new HashSet<>();
    private final Object pendingLock = new Object();

    private MessageBusConnection connection;
    private ScheduledFuture<?> pendingFlush;

    public RAGFileWatcher(@NotNull Project project) {
        this(project, ProjectIndexerService.getInstance());
    }

    /** Visible for tests. */
    RAGFileWatcher(@NotNull Project project, @NotNull ProjectIndexerService indexer) {
        this.project = project;
        this.indexer = indexer;
        start();
    }

    public static RAGFileWatcher getInstance(@NotNull Project project) {
        return project.getService(RAGFileWatcher.class);
    }

    private void start() {
        connection = project.getMessageBus().connect(this);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
                if (!isRagOn()) return;
                handleEvents(events);
            }
        });
        log.debug("RAG file watcher started for {}", project.getName());
    }

    private boolean isRagOn() {
        DevoxxGenieStateService s = DevoxxGenieStateService.getInstance();
        return Boolean.TRUE.equals(s.getRagEnabled());
    }

    /** Visible for tests. */
    void handleEvents(@NotNull List<? extends VFileEvent> events) {
        String basePath = project.getBasePath();
        if (basePath == null) return;

        IndexManifest manifest = IndexManifestService.getInstance().forProject(project);

        List<Path> changes = new ArrayList<>();
        List<Path> deletes = new ArrayList<>();
        for (VFileEvent event : events) {
            Path path = pathOf(event);
            if (path == null) continue;
            if (!path.toString().startsWith(basePath)) continue;
            if (!manifest.isTracked(path)) continue;

            if (event instanceof VFileDeleteEvent || event instanceof VFileMoveEvent) {
                deletes.add(path);
            } else if (event instanceof VFileContentChangeEvent || event instanceof VFileCreateEvent) {
                changes.add(path);
            }
        }

        if (changes.isEmpty() && deletes.isEmpty()) return;

        synchronized (pendingLock) {
            pendingChanges.addAll(changes);
            pendingDeletes.addAll(deletes);
            // A delete supersedes any pending change for the same path.
            pendingChanges.removeAll(deletes);

            if (pendingFlush != null) pendingFlush.cancel(false);
            pendingFlush = scheduler.schedule(this::flush, DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    /** Visible for tests. Runs the indexer on the scheduler thread directly — that thread is
     *  already a background daemon, so there's no point hopping to the IntelliJ pool. */
    void flush() {
        Set<Path> toReindex;
        Set<Path> toDelete;
        synchronized (pendingLock) {
            toReindex = new HashSet<>(pendingChanges);
            toDelete = new HashSet<>(pendingDeletes);
            pendingChanges.clear();
            pendingDeletes.clear();
        }
        if (toReindex.isEmpty() && toDelete.isEmpty()) return;

        try {
            if (!toDelete.isEmpty()) {
                log.info("RAG watcher removing {} deleted file(s) from index", toDelete.size());
                indexer.removeFiles(project, toDelete);
            }
            if (!toReindex.isEmpty()) {
                log.info("RAG watcher re-indexing {} changed file(s)", toReindex.size());
                indexer.reindexFiles(project, toReindex);
            }
        } catch (Exception e) {
            log.warn("RAG watcher flush failed: {}", e.getMessage());
        }
    }

    private static Path pathOf(@NotNull VFileEvent event) {
        VirtualFile file = event.getFile();
        String s = file != null ? file.getPath() : event.getPath();
        if (s == null) return null;
        try {
            return Paths.get(s);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void dispose() {
        synchronized (pendingLock) {
            if (pendingFlush != null) {
                pendingFlush.cancel(false);
                pendingFlush = null;
            }
            pendingChanges.clear();
            pendingDeletes.clear();
        }
        scheduler.shutdownNow();
        if (connection != null) {
            Disposer.dispose(connection);
        }
    }
}
