package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.BacklogConfig;
import com.devoxx.genie.model.spec.BacklogDocument;
import com.devoxx.genie.model.spec.DefinitionOfDoneItem;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Project-scoped service that discovers, parses, and caches Backlog.md task spec files and documents.
 * Watches for file system changes in the spec directory (default: "backlog/").
 * Provides write operations for creating, updating, completing, and archiving tasks and documents.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SpecService implements Disposable {

    private final Project project;
    private final Map<String, TaskSpec> specCache = new ConcurrentHashMap<>();
    private final Map<String, BacklogDocument> documentCache = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private MessageBusConnection messageBusConnection;

    public SpecService(@NotNull Project project) {
        this.project = project;
        initFileWatcher();
        refresh();
    }

    public static SpecService getInstance(@NotNull Project project) {
        return project.getService(SpecService.class);
    }

    // ===== Task Read Operations =====

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
     * Returns specs matching the given filters.
     * When a search term is provided, results are fuzzy-matched and ranked by relevance.
     */
    public @NotNull List<TaskSpec> getSpecsByFilters(@Nullable String status,
                                                      @Nullable String assignee,
                                                      @Nullable List<String> labels,
                                                      @Nullable String search,
                                                      int limit) {
        Stream<TaskSpec> stream = specCache.values().stream();

        if (status != null && !status.isEmpty()) {
            stream = stream.filter(s -> status.equalsIgnoreCase(s.getStatus()));
        }
        if (assignee != null && !assignee.isEmpty()) {
            stream = stream.filter(s -> s.getAssignees() != null &&
                    s.getAssignees().stream().anyMatch(a -> a.equalsIgnoreCase(assignee)));
        }
        if (labels != null && !labels.isEmpty()) {
            stream = stream.filter(s -> s.getLabels() != null &&
                    labels.stream().allMatch(l -> s.getLabels().stream().anyMatch(sl -> sl.equalsIgnoreCase(l))));
        }

        if (search != null && !search.isEmpty()) {
            // Score, filter, and sort by relevance
            List<Map.Entry<TaskSpec, Double>> scored = stream
                    .map(s -> Map.entry(s, FuzzySearchHelper.scoreMultiField(search, s.getTitle(), s.getDescription(), s.getId())))
                    .filter(e -> e.getValue() >= 0.3)
                    .sorted(Map.Entry.<TaskSpec, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            Stream<TaskSpec> resultStream = scored.stream().map(Map.Entry::getKey);
            if (limit > 0) {
                resultStream = resultStream.limit(limit);
            }
            return resultStream.collect(Collectors.toList());
        }

        if (limit > 0) {
            stream = stream.limit(limit);
        }

        return stream.collect(Collectors.toList());
    }

    /**
     * Search specs by query string with fuzzy matching on title and description.
     * Results are ranked by relevance score (best matches first).
     */
    public @NotNull List<TaskSpec> searchSpecs(@NotNull String query,
                                                @Nullable String status,
                                                @Nullable String priority,
                                                int limit) {
        Stream<TaskSpec> stream = specCache.values().stream();

        if (status != null && !status.isEmpty()) {
            stream = stream.filter(s -> status.equalsIgnoreCase(s.getStatus()));
        }
        if (priority != null && !priority.isEmpty()) {
            stream = stream.filter(s -> priority.equalsIgnoreCase(s.getPriority()));
        }

        // Score each spec and filter out non-matches, then sort by relevance
        List<Map.Entry<TaskSpec, Double>> scored = stream
                .map(s -> Map.entry(s, FuzzySearchHelper.scoreMultiField(query, s.getTitle(), s.getDescription())))
                .filter(e -> e.getValue() >= 0.3)
                .sorted(Map.Entry.<TaskSpec, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        Stream<TaskSpec> resultStream = scored.stream().map(Map.Entry::getKey);
        if (limit > 0) {
            resultStream = resultStream.limit(limit);
        }

        return resultStream.collect(Collectors.toList());
    }

    // ===== Task Write Operations =====

    /**
     * Creates a new task file and returns the created spec.
     * If the task has no Definition of Done items, project-wide DoD defaults from config.yml
     * are automatically applied (unless skipDodDefaults is true).
     */
    public @NotNull TaskSpec createTask(@NotNull TaskSpec spec) throws IOException {
        return createTask(spec, false);
    }

    /**
     * Creates a new task file and returns the created spec.
     *
     * @param spec             the task specification to create
     * @param skipDodDefaults  if true, project-wide Definition of Done defaults are NOT applied
     */
    public @NotNull TaskSpec createTask(@NotNull TaskSpec spec, boolean skipDodDefaults) throws IOException {
        writeLock.lock();
        try {
            BacklogConfigService configService = BacklogConfigService.getInstance(project);
            configService.ensureInitialized();
            if (spec.getId() == null || spec.getId().isEmpty()) {
                spec.setId(configService.getNextTaskId());
            }
            if (spec.getCreatedAt() == null) {
                spec.setCreatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            // Apply Definition of Done defaults from config if the task has none
            if (!skipDodDefaults && (spec.getDefinitionOfDone() == null || spec.getDefinitionOfDone().isEmpty())) {
                BacklogConfig config = configService.getConfig();
                List<String> dodDefaults = config.getDefinitionOfDone();
                if (dodDefaults != null && !dodDefaults.isEmpty()) {
                    List<DefinitionOfDoneItem> dodItems = new ArrayList<>();
                    for (int i = 0; i < dodDefaults.size(); i++) {
                        dodItems.add(DefinitionOfDoneItem.builder()
                                .index(i)
                                .text(dodDefaults.get(i))
                                .checked(false)
                                .build());
                    }
                    spec.setDefinitionOfDone(dodItems);
                }
            }

            Path tasksDir = configService.getTasksDir();
            if (tasksDir == null) {
                throw new IOException("Cannot determine tasks directory");
            }
            Files.createDirectories(tasksDir);

            String fileName = buildTaskFileName(spec.getId(), spec.getTitle());
            Path filePath = tasksDir.resolve(fileName);
            spec.setFilePath(filePath.toAbsolutePath().toString());

            String content = SpecFrontmatterGenerator.generate(spec);
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            refreshVfs();
            refresh();
            return spec;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates an existing task file on disk.
     */
    public void updateTask(@NotNull TaskSpec spec) throws IOException {
        writeLock.lock();
        try {
            if (spec.getFilePath() == null) {
                throw new IOException("Task has no file path");
            }
            spec.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            String content = SpecFrontmatterGenerator.generate(spec);
            Files.writeString(Paths.get(spec.getFilePath()), content, StandardCharsets.UTF_8);
            refreshVfs();
            refresh();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Completes a task by setting status to "Done". The file stays in place.
     * Use archiveTask() to move it to the archive directory.
     */
    public void completeTask(@NotNull String id) throws IOException {
        writeLock.lock();
        try {
            TaskSpec spec = getSpec(id);
            if (spec == null) {
                throw new IOException("Task not found: " + id);
            }

            spec.setStatus("Done");
            spec.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            String content = SpecFrontmatterGenerator.generate(spec);
            Files.writeString(Paths.get(spec.getFilePath()), content, StandardCharsets.UTF_8);

            refreshVfs();
            refresh();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Archives a task by moving it to archive/tasks/ directory.
     */
    public void archiveTask(@NotNull String id) throws IOException {
        writeLock.lock();
        try {
            TaskSpec spec = getSpec(id);
            if (spec == null) {
                throw new IOException("Task not found: " + id);
            }

            BacklogConfigService configService = BacklogConfigService.getInstance(project);
            Path archiveDir = configService.getArchiveTasksDir();
            if (archiveDir == null) {
                throw new IOException("Cannot determine archive directory");
            }
            Files.createDirectories(archiveDir);

            Path source = Paths.get(spec.getFilePath());
            Path target = archiveDir.resolve(source.getFileName());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            refreshVfs();
            refresh();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Archives all tasks with status "Done". Returns the number of tasks archived.
     */
    public int archiveDoneTasks() throws IOException {
        writeLock.lock();
        try {
            List<TaskSpec> doneTasks = specCache.values().stream()
                    .filter(s -> "Done".equalsIgnoreCase(s.getStatus()))
                    .collect(Collectors.toList());

            if (doneTasks.isEmpty()) {
                return 0;
            }

            BacklogConfigService configService = BacklogConfigService.getInstance(project);
            Path archiveDir = configService.getArchiveTasksDir();
            if (archiveDir == null) {
                throw new IOException("Cannot determine archive directory");
            }
            Files.createDirectories(archiveDir);

            int count = 0;
            for (TaskSpec spec : doneTasks) {
                if (spec.getFilePath() == null) continue;
                Path source = Paths.get(spec.getFilePath());
                if (!Files.exists(source)) continue;
                Path target = archiveDir.resolve(source.getFileName());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                count++;
            }

            refreshVfs();
            refresh();
            return count;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Returns all archived tasks by scanning the archive/tasks/ directory.
     * These are NOT part of the normal specCache.
     */
    public @NotNull List<TaskSpec> getArchivedTasks() {
        BacklogConfigService configService = BacklogConfigService.getInstance(project);
        Path archiveDir = configService.getArchiveTasksDir();
        if (archiveDir == null || !Files.isDirectory(archiveDir)) {
            return List.of();
        }

        List<TaskSpec> archived = new ArrayList<>();
        try (Stream<Path> files = Files.walk(archiveDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            TaskSpec spec = SpecFrontmatterParser.parse(content, file.toAbsolutePath().toString());
                            if (spec != null && spec.getId() != null) {
                                spec.setLastModified(Files.getLastModifiedTime(file).toMillis());
                                archived.add(spec);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to read archived task file: {}", file, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan archive directory: {}", archiveDir, e);
        }
        return archived;
    }

    /**
     * Unarchives a task by moving it from archive/tasks/ back to tasks/.
     */
    public void unarchiveTask(@NotNull String id) throws IOException {
        writeLock.lock();
        try {
            // Find the task in the archive directory
            List<TaskSpec> archivedTasks = getArchivedTasks();
            TaskSpec spec = archivedTasks.stream()
                    .filter(s -> id.equalsIgnoreCase(s.getId()))
                    .findFirst()
                    .orElse(null);

            if (spec == null) {
                throw new IOException("Archived task not found: " + id);
            }

            BacklogConfigService configService = BacklogConfigService.getInstance(project);
            Path tasksDir = configService.getTasksDir();
            if (tasksDir == null) {
                throw new IOException("Cannot determine tasks directory");
            }
            Files.createDirectories(tasksDir);

            Path source = Paths.get(spec.getFilePath());
            Path target = tasksDir.resolve(source.getFileName());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            refreshVfs();
            refresh();
        } finally {
            writeLock.unlock();
        }
    }

    // ===== Document Operations =====

    /**
     * Returns all cached documents.
     */
    public @NotNull List<BacklogDocument> getAllDocuments() {
        return new ArrayList<>(documentCache.values());
    }

    /**
     * Returns a document by its ID.
     */
    public @Nullable BacklogDocument getDocument(@NotNull String id) {
        return documentCache.values().stream()
                .filter(doc -> id.equalsIgnoreCase(doc.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates a new document file.
     */
    public @NotNull BacklogDocument createDocument(@NotNull String title, @NotNull String content) throws IOException {
        writeLock.lock();
        try {
            BacklogConfigService configService = BacklogConfigService.getInstance(project);
            configService.ensureInitialized();
            String id = configService.getNextDocumentId();

            Path docsDir = configService.getDocsDir();
            if (docsDir == null) {
                throw new IOException("Cannot determine docs directory");
            }
            Files.createDirectories(docsDir);

            BacklogDocument doc = BacklogDocument.builder()
                    .id(id)
                    .title(title)
                    .content(content)
                    .build();

            String fileName = id.replace(' ', '-') + ".md";
            Path filePath = docsDir.resolve(fileName);
            doc.setFilePath(filePath.toAbsolutePath().toString());

            String fileContent = SpecFrontmatterGenerator.generateDocument(doc);
            Files.writeString(filePath, fileContent, StandardCharsets.UTF_8);
            refreshVfs();
            refresh();
            return doc;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates an existing document.
     */
    public void updateDocument(@NotNull String id, @NotNull String content, @Nullable String title) throws IOException {
        writeLock.lock();
        try {
            BacklogDocument doc = getDocument(id);
            if (doc == null) {
                throw new IOException("Document not found: " + id);
            }
            doc.setContent(content);
            if (title != null && !title.isEmpty()) {
                doc.setTitle(title);
            }

            String fileContent = SpecFrontmatterGenerator.generateDocument(doc);
            Files.writeString(Paths.get(doc.getFilePath()), fileContent, StandardCharsets.UTF_8);
            refreshVfs();
            refresh();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Search documents by query string with fuzzy matching on title and content.
     * Results are ranked by relevance score (best matches first).
     */
    public @NotNull List<BacklogDocument> searchDocuments(@NotNull String query, int limit) {
        List<Map.Entry<BacklogDocument, Double>> scored = documentCache.values().stream()
                .map(d -> Map.entry(d, FuzzySearchHelper.scoreMultiField(query, d.getTitle(), d.getContent())))
                .filter(e -> e.getValue() >= 0.3)
                .sorted(Map.Entry.<BacklogDocument, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        Stream<BacklogDocument> resultStream = scored.stream().map(Map.Entry::getKey);
        if (limit > 0) {
            resultStream = resultStream.limit(limit);
        }
        return resultStream.collect(Collectors.toList());
    }

    /**
     * List documents with optional search filter.
     */
    public @NotNull List<BacklogDocument> listDocuments(@Nullable String search) {
        if (search == null || search.isEmpty()) {
            return getAllDocuments();
        }
        return searchDocuments(search, 0);
    }

    // ===== Refresh & Listeners =====

    /**
     * Force a full refresh of all caches by re-scanning the spec directory.
     * Uses writeLock to prevent race conditions between VFS watcher and explicit refreshes.
     */
    public void refresh() {
        writeLock.lock();
        try {
            specCache.clear();
            documentCache.clear();

            Path specDir = getSpecDirectoryPath();
            if (specDir == null || !Files.isDirectory(specDir)) {
                log.debug("Spec directory not found for project: {}", project.getName());
                return;
            }

            try {
                discoverAndParseSpecs(specDir);
                discoverAndParseDocs(specDir);
                log.info("Loaded {} task specs and {} documents from {}", specCache.size(), documentCache.size(), specDir);
            } catch (IOException e) {
                log.warn("Failed to scan spec directory: {}", e.getMessage());
            }
        } finally {
            writeLock.unlock();
        }

        notifyListeners();
    }

    /**
     * Add a listener that will be called when specs change.
     */
    public void addChangeListener(@NotNull Runnable listener) {
        changeListeners.add(listener);
    }

    /**
     * Remove a previously registered change listener.
     */
    public void removeChangeListener(@NotNull Runnable listener) {
        changeListeners.remove(listener);
    }

    private void discoverAndParseSpecs(@NotNull Path specDir) throws IOException {
        // Scan .md files in tasks/, completed/, and root of spec dir (but not docs/)
        Path tasksDir = specDir.resolve("tasks");
        Path completedDir = specDir.resolve("completed");

        scanTasksIn(tasksDir);
        scanTasksIn(completedDir);

        // Also scan root-level .md files (backward compatible with flat layout)
        try (Stream<Path> files = Files.list(specDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .forEach(this::parseAndCacheSpec);
        }
    }

    private void scanTasksIn(@NotNull Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .forEach(this::parseAndCacheSpec);
        } catch (IOException e) {
            log.warn("Failed to scan tasks in: {}", dir);
        }
    }

    private void discoverAndParseDocs(@NotNull Path specDir) throws IOException {
        Path docsDir = specDir.resolve("docs");
        if (!Files.isDirectory(docsDir)) {
            return;
        }
        try (Stream<Path> files = Files.walk(docsDir)) {
            files.filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .forEach(this::parseAndCacheDocument);
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

    private void parseAndCacheDocument(@NotNull Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            BacklogDocument doc = parseDocument(content, file.toAbsolutePath().toString());
            if (doc != null && doc.getId() != null) {
                doc.setLastModified(Files.getLastModifiedTime(file).toMillis());
                documentCache.put(doc.getFilePath(), doc);
            }
        } catch (IOException e) {
            log.warn("Failed to read document file: {}", file, e);
        }
    }

    private @Nullable BacklogDocument parseDocument(@NotNull String content, @NotNull String filePath) {
        // Reuse frontmatter regex pattern
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                "\\A---\\s*\\n(.*?)\\n---\\s*\\n?(.*)", java.util.regex.Pattern.DOTALL
        ).matcher(content);

        if (!matcher.matches()) {
            return null;
        }

        String frontmatter = matcher.group(1);
        String body = matcher.group(2).trim();

        BacklogDocument.BacklogDocumentBuilder builder = BacklogDocument.builder();
        builder.filePath(filePath);
        builder.content(body);

        for (String line : frontmatter.split("\\n")) {
            String trimmed = line.trim();
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                String key = trimmed.substring(0, colonIndex).trim();
                String value = SpecFrontmatterParser.stripQuotes(trimmed.substring(colonIndex + 1).trim());
                switch (key.toLowerCase()) {
                    case "id" -> builder.id(value);
                    case "title" -> builder.title(value);
                }
            }
        }

        return builder.build();
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

    /**
     * Builds a task filename like "task-3 - My-Task-Title.md" (lowercase filename, ID stays uppercase in frontmatter).
     */
    private static @NotNull String buildTaskFileName(@NotNull String id, @Nullable String title) {
        String idPart = id.toLowerCase().replace(' ', '-');
        if (title == null || title.isBlank()) {
            return idPart + ".md";
        }
        String titlePart = title.trim()
                .replaceAll("[^a-zA-Z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
        return idPart + " - " + titlePart + ".md";
    }

    /**
     * Triggers a VFS refresh for the spec directory so IntelliJ's VFS picks up changes
     * made via java.nio.file operations.
     */
    private void refreshVfs() {
        Path specDir = getSpecDirectoryPath();
        if (specDir == null) return;
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(specDir.toAbsolutePath().toString());
        if (vf != null) {
            vf.refresh(false, true);
        }
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
        documentCache.clear();
        changeListeners.clear();
    }
}
