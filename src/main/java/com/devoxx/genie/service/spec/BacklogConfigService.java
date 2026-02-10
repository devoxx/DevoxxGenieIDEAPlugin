package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.BacklogConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Project-scoped service for managing the backlog/config.yml configuration file.
 * Provides config reading/writing and sequential ID generation for tasks and documents.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class BacklogConfigService {

    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^id:\\s*(.+)$", Pattern.MULTILINE);

    /**
     * All subdirectories that Backlog.md creates during initialization.
     */
    private static final List<String> BACKLOG_SUBDIRECTORIES = List.of(
            "tasks", "drafts", "completed",
            "archive/tasks", "archive/drafts", "archive/milestones",
            "docs", "decisions", "milestones"
    );

    private final Project project;
    private BacklogConfig cachedConfig;

    public BacklogConfigService(@NotNull Project project) {
        this.project = project;
    }

    public static BacklogConfigService getInstance(@NotNull Project project) {
        return project.getService(BacklogConfigService.class);
    }

    /**
     * Returns the parsed config, reading from disk if not cached.
     */
    public @NotNull BacklogConfig getConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfig();
        }
        return cachedConfig;
    }

    /**
     * Saves the config to disk and updates the cache.
     */
    public void saveConfig(@NotNull BacklogConfig config) throws IOException {
        Path configPath = getConfigPath();
        if (configPath == null) {
            throw new IOException("Cannot determine config path — project base path is null");
        }
        Files.createDirectories(configPath.getParent());
        String content = serializeConfig(config);
        Files.writeString(configPath, content, StandardCharsets.UTF_8);
        this.cachedConfig = config;
    }

    /**
     * Generates the next task ID by scanning existing task files in tasks/, completed/, and archive/.
     */
    public @NotNull String getNextTaskId() {
        String prefix = getConfig().getTaskPrefix();
        if (prefix == null || prefix.isEmpty()) {
            prefix = "TASK";
        }
        prefix = prefix.toUpperCase();
        int maxNum = Math.max(scanMaxId(getTasksDir()),
                Math.max(scanMaxId(getCompletedDir()), scanMaxId(getArchiveTasksDir())));
        return prefix + "-" + (maxNum + 1);
    }

    /**
     * Generates the next document ID by scanning existing document files.
     */
    public @NotNull String getNextDocumentId() {
        int maxNum = scanMaxId(getDocsDir());
        return "DOC-" + (maxNum + 1);
    }

    /**
     * Returns the tasks directory path, creating it if needed.
     */
    public @Nullable Path getTasksDir() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null ? specDir.resolve("tasks") : null;
    }

    /**
     * Returns the docs directory path.
     */
    public @Nullable Path getDocsDir() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null ? specDir.resolve("docs") : null;
    }

    /**
     * Returns the completed tasks directory path.
     */
    public @Nullable Path getCompletedDir() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null ? specDir.resolve("completed") : null;
    }

    /**
     * Returns the archive/tasks directory path.
     */
    public @Nullable Path getArchiveTasksDir() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null ? specDir.resolve("archive").resolve("tasks") : null;
    }

    /**
     * Returns the archive/milestones directory path.
     */
    public @Nullable Path getArchiveMilestonesDir() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null ? specDir.resolve("archive").resolve("milestones") : null;
    }

    /**
     * Returns the base spec directory path (e.g., project/backlog).
     */
    public @Nullable Path getSpecDirectoryPath() {
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

    /**
     * Initializes the backlog directory structure with all standard subdirectories
     * and a default config.yml. Safe to call multiple times (idempotent).
     *
     * @param projectName optional project name to set in config.yml
     */
    public void initBacklog(@Nullable String projectName) throws IOException {
        Path specDir = getSpecDirectoryPath();
        if (specDir == null) {
            throw new IOException("Cannot determine spec directory — project base path is null");
        }

        // Create all subdirectories
        for (String subdir : BACKLOG_SUBDIRECTORIES) {
            Files.createDirectories(specDir.resolve(subdir));
        }

        // Write default config.yml if it does not exist yet
        Path configPath = specDir.resolve("config.yml");
        if (!Files.isRegularFile(configPath)) {
            BacklogConfig config = BacklogConfig.builder()
                    .projectName(projectName != null ? projectName : "My Project")
                    .taskPrefix("task")
                    .build();
            saveConfig(config);
        }
    }

    /**
     * Returns true if the backlog has been initialized (config.yml exists).
     */
    public boolean isBacklogInitialized() {
        Path configPath = getConfigPath();
        return configPath != null && Files.isRegularFile(configPath);
    }

    /**
     * Invalidate the cached config so it will be re-read from disk.
     */
    public void invalidateCache() {
        cachedConfig = null;
    }

    private @NotNull BacklogConfig loadConfig() {
        Path configPath = getConfigPath();
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return BacklogConfig.builder().build();
        }

        try {
            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            return parseConfig(content);
        } catch (IOException e) {
            log.warn("Failed to read config.yml: {}", e.getMessage());
            return BacklogConfig.builder().build();
        }
    }

    private @Nullable Path getConfigPath() {
        Path specDir = getSpecDirectoryPath();
        return specDir != null ? specDir.resolve("config.yml") : null;
    }

    /**
     * Simple YAML parser for config.yml. Handles scalar fields and simple lists.
     */
    private @NotNull BacklogConfig parseConfig(@NotNull String content) {
        BacklogConfig.BacklogConfigBuilder builder = BacklogConfig.builder();
        String currentKey = null;
        List<String> currentList = null;
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        boolean inMilestones = false;
        String milestoneName = null;

        for (String line : content.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // Detect indented list items
            if (trimmed.startsWith("- ") && currentKey != null) {
                String value = SpecFrontmatterParser.stripQuotes(trimmed.substring(2).trim());
                if (inMilestones) {
                    // Milestone list items might be "- name: value" or just "- value"
                    if (value.startsWith("name:")) {
                        milestoneName = SpecFrontmatterParser.stripQuotes(value.substring(5).trim());
                    } else if (value.startsWith("description:") && milestoneName != null) {
                        String desc = SpecFrontmatterParser.stripQuotes(value.substring(12).trim());
                        milestones.add(BacklogConfig.BacklogMilestone.builder()
                                .name(milestoneName).description(desc).build());
                        milestoneName = null;
                    } else {
                        // Simple milestone name without description
                        milestones.add(BacklogConfig.BacklogMilestone.builder()
                                .name(value).build());
                    }
                } else if (currentList != null) {
                    currentList.add(value);
                }
                continue;
            }

            // Key-value pair
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex > 0) {
                // Flush previous list
                if (currentKey != null && currentList != null && !inMilestones) {
                    applyConfigList(currentKey, currentList, builder);
                }
                if (inMilestones && milestoneName != null) {
                    milestones.add(BacklogConfig.BacklogMilestone.builder()
                            .name(milestoneName).build());
                    milestoneName = null;
                }

                currentKey = trimmed.substring(0, colonIndex).trim();
                String value = trimmed.substring(colonIndex + 1).trim();
                inMilestones = "milestones".equalsIgnoreCase(currentKey);

                if (value.isEmpty()) {
                    currentList = new ArrayList<>();
                } else if (value.startsWith("[")) {
                    // Inline array: ["To Do", "In Progress", "Done"] or []
                    currentList = null;
                    List<String> inlineItems = parseInlineArray(value);
                    applyConfigList(currentKey, inlineItems, builder);
                } else {
                    currentList = null;
                    value = SpecFrontmatterParser.stripQuotes(value);
                    applyConfigScalar(currentKey, value, builder);
                }
            }
        }

        // Flush
        if (currentKey != null && currentList != null && !inMilestones) {
            applyConfigList(currentKey, currentList, builder);
        }
        if (milestoneName != null) {
            milestones.add(BacklogConfig.BacklogMilestone.builder().name(milestoneName).build());
        }
        if (!milestones.isEmpty()) {
            builder.milestones(milestones);
        }

        return builder.build();
    }

    private void applyConfigScalar(@NotNull String key, @NotNull String value, BacklogConfig.BacklogConfigBuilder builder) {
        switch (key.toLowerCase()) {
            case "project_name", "projectname" -> builder.projectName(value);
            case "default_status", "defaultstatus" -> builder.defaultStatus(value);
            case "task_prefix", "taskprefix" -> builder.taskPrefix(value);
            case "date_format", "dateformat" -> builder.dateFormat(value);
            case "max_column_width", "maxcolumnwidth" -> builder.maxColumnWidth(parseIntOrDefault(value, 20));
            case "auto_open_browser", "autoopenbrowser" -> builder.autoOpenBrowser(Boolean.parseBoolean(value));
            case "default_port", "defaultport" -> builder.defaultPort(parseIntOrDefault(value, 6420));
            case "remote_operations", "remoteoperations" -> builder.remoteOperations(Boolean.parseBoolean(value));
            case "auto_commit", "autocommit" -> builder.autoCommit(Boolean.parseBoolean(value));
            case "bypass_git_hooks", "bypassgithooks" -> builder.bypassGitHooks(Boolean.parseBoolean(value));
            case "check_active_branches", "checkactivebranches" -> builder.checkActiveBranches(Boolean.parseBoolean(value));
            case "active_branch_days", "activebranchdays" -> builder.activeBranchDays(parseIntOrDefault(value, 30));
            default -> log.trace("Ignoring unknown config field: {}", key);
        }
    }

    /**
     * Parses an inline YAML array like ["To Do", "In Progress", "Done"] or [].
     */
    private @NotNull List<String> parseInlineArray(@NotNull String value) {
        String inner = value.strip();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        inner = inner.trim();
        if (inner.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> items = new ArrayList<>();
        for (String part : inner.split(",")) {
            items.add(SpecFrontmatterParser.stripQuotes(part.trim()));
        }
        return items;
    }

    private int parseIntOrDefault(@NotNull String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void applyConfigList(@NotNull String key, @NotNull List<String> values, BacklogConfig.BacklogConfigBuilder builder) {
        switch (key.toLowerCase()) {
            case "statuses" -> builder.statuses(new ArrayList<>(values));
            case "labels" -> builder.labels(new ArrayList<>(values));
            default -> log.trace("Ignoring unknown config list field: {}", key);
        }
    }

    private @NotNull String serializeConfig(@NotNull BacklogConfig config) {
        StringBuilder sb = new StringBuilder();
        if (config.getProjectName() != null) {
            sb.append("project_name: \"").append(config.getProjectName()).append("\"\n");
        }
        sb.append("default_status: \"").append(config.getDefaultStatus()).append("\"\n");
        sb.append("statuses: ").append(serializeInlineList(config.getStatuses())).append("\n");
        sb.append("labels: ").append(serializeInlineList(config.getLabels())).append("\n");
        sb.append("milestones: ").append(serializeInlineList(
                config.getMilestones() != null
                        ? config.getMilestones().stream().map(BacklogConfig.BacklogMilestone::getName).toList()
                        : List.of())).append("\n");
        sb.append("date_format: ").append(config.getDateFormat()).append("\n");
        sb.append("max_column_width: ").append(config.getMaxColumnWidth()).append("\n");
        sb.append("auto_open_browser: ").append(config.isAutoOpenBrowser()).append("\n");
        sb.append("default_port: ").append(config.getDefaultPort()).append("\n");
        sb.append("remote_operations: ").append(config.isRemoteOperations()).append("\n");
        sb.append("auto_commit: ").append(config.isAutoCommit()).append("\n");
        sb.append("bypass_git_hooks: ").append(config.isBypassGitHooks()).append("\n");
        sb.append("check_active_branches: ").append(config.isCheckActiveBranches()).append("\n");
        sb.append("active_branch_days: ").append(config.getActiveBranchDays()).append("\n");
        sb.append("task_prefix: \"").append(config.getTaskPrefix()).append("\"\n");
        return sb.toString();
    }

    private @NotNull String serializeInlineList(@Nullable List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(items.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Scan all .md files in the given directory for "id: PREFIX-N" frontmatter
     * and return the maximum N found.
     */
    private int scanMaxId(@Nullable Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return 0;
        }
        int max = 0;
        try (Stream<Path> files = Files.walk(dir)) {
            List<Path> mdFiles = files
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(Files::isRegularFile)
                    .toList();

            for (Path file : mdFiles) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    Matcher m = TASK_ID_PATTERN.matcher(content);
                    if (m.find()) {
                        String id = SpecFrontmatterParser.stripQuotes(m.group(1).trim());
                        int dashIdx = id.lastIndexOf('-');
                        if (dashIdx >= 0) {
                            try {
                                int num = Integer.parseInt(id.substring(dashIdx + 1));
                                if (num > max) {
                                    max = num;
                                }
                            } catch (NumberFormatException ignored) {
                                // Not a numeric suffix
                            }
                        }
                    }
                } catch (IOException e) {
                    log.debug("Failed to read file for ID scan: {}", file);
                }
            }
        } catch (IOException e) {
            log.debug("Failed to scan directory for IDs: {}", dir);
        }
        return max;
    }
}
