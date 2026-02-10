package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Project-scoped service that discovers, parses, and caches Backlog.md task spec files.
 * Watches for file system changes in the spec directory (default: "backlog/").
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SpecService implements Disposable {

    private final Project project;
    private final Map<String, TaskSpec> specCache = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new ArrayList<>();
    private MessageBusConnection messageBusConnection;

    public SpecService(@NotNull Project project) {
        this.project = project;
        initFileWatcher();
        refresh();
    }

    public static SpecService getInstance(@NotNull Project project) {
        return project.getService(SpecService.class);
    }

    /**
     * Returns all cached task specs.
     */
    public @NotNull List<TaskSpec> getAllSpecs() {
        return new ArrayList<>(specCache.values());
    }

    /**
     * Returns specs filtered by status.
     */
    public @NotNull List<TaskSpec> getSpecsByStatus(@NotNull String status) {
        return specCache.values().stream()
                .filter(spec -> status.equalsIgnoreCase(spec.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a spec by its ID.
     */
    public @Nullable TaskSpec getSpec(@NotNull String id) {
        return specCache.values().stream()
                .filter(spec -> id.equalsIgnoreCase(spec.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all distinct statuses present in the cached specs.
     */
    public @NotNull List<String> getStatuses() {
        return specCache.values().stream()
                .map(TaskSpec::getStatus)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the spec directory exists in the project.
     */
    public boolean hasSpecDirectory() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null && Files.isDirectory(specDir);
    }

    /**
     * Force a full refresh of the spec cache by re-scanning the spec directory.
     */
    public void refresh() {
        specCache.clear();

        Path specDir = getSpecDirectoryPath();
        if (specDir == null || !Files.isDirectory(specDir)) {
            log.debug("Spec directory not found for project: {}", project.getName());
            return;
        }

        try {
            discoverAndParseSpecs(specDir);
            log.info("Loaded {} task specs from {}", specCache.size(), specDir);
        } catch (IOException e) {
            log.warn("Failed to scan spec directory: {}", e.getMessage());
        }

        notifyListeners();
    }

    /**
     * Add a listener that will be called when specs change.
     */
    public void addChangeListener(@NotNull Runnable listener) {
        changeListeners.add(listener);
    }

    private void discoverAndParseSpecs(@NotNull Path specDir) throws IOException {
        // Scan all .md files recursively in the backlog directory
        try (Stream<Path> files = Files.walk(specDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .forEach(this::parseAndCacheSpec);
        }
    }

    private void parseAndCacheSpec(@NotNull Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            TaskSpec spec = SpecFrontmatterParser.parse(content, file.toAbsolutePath().toString());
            if (spec != null && spec.getId() != null) {
                spec.setLastModified(Files.getLastModifiedTime(file).toMillis());
                specCache.put(spec.getFilePath(), spec);
            }
        } catch (IOException e) {
            log.warn("Failed to read spec file: {}", file, e);
        }
    }

    private @Nullable Path getSpecDirectoryPath() {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return null;
        }
        String specDirName = DevoxxGenieStateService.getInstance().getSpecDirectory();
        if (specDirName == null || specDirName.isEmpty()) {
            specDirName = "backlog";
        }
        return Paths.get(basePath, specDirName);
    }

    private void initFileWatcher() {
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
                boolean specChanged = false;
                for (VFileEvent event : events) {
                    if (isSpecFileEvent(event)) {
                        specChanged = true;
                        break;
                    }
                }
                if (specChanged) {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> refresh());
                }
            }
        });
    }

    private boolean isSpecFileEvent(@NotNull VFileEvent event) {
        Path specDir = getSpecDirectoryPath();
        if (specDir == null) {
            return false;
        }

        String path = null;
        if (event instanceof VFileCreateEvent || event instanceof VFileDeleteEvent || event instanceof VFileContentChangeEvent) {
            VirtualFile file = event.getFile();
            if (file != null) {
                path = file.getPath();
            } else {
                path = event.getPath();
            }
        }

        if (path == null) {
            return false;
        }

        return path.startsWith(specDir.toString()) && path.endsWith(".md");
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    @Override
    public void dispose() {
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
        }
        specCache.clear();
        changeListeners.clear();
    }
}
