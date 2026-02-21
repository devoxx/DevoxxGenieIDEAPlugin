package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.BacklogConfig;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for BacklogConfig model and BacklogConfigService.
 */
class BacklogConfigServiceTest {

    // ── BacklogConfig model tests ──────────────────────────────────────

    @Test
    void shouldCreateDefaultConfig() {
        BacklogConfig config = BacklogConfig.builder().build();

        assertThat(config.getDefaultStatus()).isEqualTo("To Do");
        assertThat(config.getTaskPrefix()).isEqualTo("task");
        assertThat(config.getDateFormat()).isEqualTo("yyyy-mm-dd");
        assertThat(config.getStatuses()).containsExactly("To Do", "In Progress", "Done");
        assertThat(config.getLabels()).isEmpty();
        assertThat(config.getMilestones()).isEmpty();
        assertThat(config.getMaxColumnWidth()).isEqualTo(20);
        assertThat(config.isAutoOpenBrowser()).isTrue();
        assertThat(config.getDefaultPort()).isEqualTo(6420);
        assertThat(config.isRemoteOperations()).isTrue();
        assertThat(config.isAutoCommit()).isFalse();
        assertThat(config.isBypassGitHooks()).isFalse();
        assertThat(config.isCheckActiveBranches()).isTrue();
        assertThat(config.getActiveBranchDays()).isEqualTo(30);
    }

    @Test
    void shouldBuildCustomConfig() {
        BacklogConfig config = BacklogConfig.builder()
                .projectName("TestProject")
                .taskPrefix("TEST")
                .defaultStatus("Draft")
                .milestones(List.of(
                        BacklogConfig.BacklogMilestone.builder()
                                .name("v1.0")
                                .description("First release")
                                .build()
                ))
                .build();

        assertThat(config.getProjectName()).isEqualTo("TestProject");
        assertThat(config.getTaskPrefix()).isEqualTo("TEST");
        assertThat(config.getDefaultStatus()).isEqualTo("Draft");
        assertThat(config.getMilestones()).hasSize(1);
        assertThat(config.getMilestones().get(0).getName()).isEqualTo("v1.0");
        assertThat(config.getMilestones().get(0).getDescription()).isEqualTo("First release");
    }

    // ── initBacklog tests ──────────────────────────────────────────────

    @Test
    void initBacklog_createsAllDirectoriesAndConfig(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog("MyProject");

            Path backlogDir = tempDir.resolve("backlog");
            assertThat(backlogDir.resolve("tasks")).isDirectory();
            assertThat(backlogDir.resolve("drafts")).isDirectory();
            assertThat(backlogDir.resolve("completed")).isDirectory();
            assertThat(backlogDir.resolve("archive/tasks")).isDirectory();
            assertThat(backlogDir.resolve("archive/drafts")).isDirectory();
            assertThat(backlogDir.resolve("archive/milestones")).isDirectory();
            assertThat(backlogDir.resolve("docs")).isDirectory();
            assertThat(backlogDir.resolve("decisions")).isDirectory();
            assertThat(backlogDir.resolve("milestones")).isDirectory();
            assertThat(backlogDir.resolve("config.yml")).isRegularFile();
        }
    }

    @Test
    void initBacklog_setsProjectNameAndTaskPrefix(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog("DevoxxGenie");

            String configContent = Files.readString(
                    tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(configContent).contains("project_name: \"DevoxxGenie\"");
            assertThat(configContent).contains("task_prefix: \"task\"");
            assertThat(configContent).contains("default_status: \"To Do\"");
            assertThat(configContent).contains("date_format: yyyy-mm-dd");
            assertThat(configContent).contains("statuses:");
            assertThat(configContent).contains("labels:");
            assertThat(configContent).contains("milestones:");
        }
    }

    @Test
    void initBacklog_usesDefaultProjectName_whenNull(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog(null);

            String configContent = Files.readString(
                    tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(configContent).contains("project_name: \"My Project\"");
        }
    }

    @Test
    void initBacklog_throwsIOException_whenBasePathIsNull() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("backlog");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(null);

            BacklogConfigService service = new BacklogConfigService(project);

            assertThatThrownBy(() -> service.initBacklog("Test"))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("project base path is null");
        }
    }

    @Test
    void initBacklog_idempotent(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog("MyProject");
            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("TASK-1.md"), "existing task");

            assertThatNoException().isThrownBy(() -> service.initBacklog("MyProject"));

            assertThat(tasksDir.resolve("TASK-1.md")).hasContent("existing task");
            String configContent = Files.readString(
                    tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(configContent).contains("project_name: \"MyProject\"");
        }
    }

    // ── isBacklogInitialized tests ─────────────────────────────────────

    @Test
    void isBacklogInitialized_returnsFalseWhenNoConfig(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            assertThat(service.isBacklogInitialized()).isFalse();
        }
    }

    @Test
    void isBacklogInitialized_returnsTrueWhenConfigExists(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog("TestProject");

            assertThat(service.isBacklogInitialized()).isTrue();
        }
    }

    @Test
    void isBacklogInitialized_returnsFalse_whenBasePathIsNull() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("backlog");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(null);

            BacklogConfigService service = new BacklogConfigService(project);

            assertThat(service.isBacklogInitialized()).isFalse();
        }
    }

    // ── saveConfig / loadConfig round-trip ──────────────────────────────

    @Test
    void saveAndLoadConfig_roundTrip_allScalarFields(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig original = BacklogConfig.builder()
                    .projectName("RoundTrip Project")
                    .defaultStatus("In Progress")
                    .taskPrefix("RT")
                    .dateFormat("dd/MM/yyyy")
                    .maxColumnWidth(40)
                    .autoOpenBrowser(false)
                    .defaultPort(8080)
                    .remoteOperations(false)
                    .autoCommit(true)
                    .bypassGitHooks(true)
                    .checkActiveBranches(false)
                    .activeBranchDays(7)
                    .build();

            service.saveConfig(original);
            service.invalidateCache();
            BacklogConfig loaded = service.getConfig();

            assertThat(loaded.getProjectName()).isEqualTo("RoundTrip Project");
            assertThat(loaded.getDefaultStatus()).isEqualTo("In Progress");
            assertThat(loaded.getTaskPrefix()).isEqualTo("RT");
            assertThat(loaded.getDateFormat()).isEqualTo("dd/MM/yyyy");
            assertThat(loaded.getMaxColumnWidth()).isEqualTo(40);
            assertThat(loaded.isAutoOpenBrowser()).isFalse();
            assertThat(loaded.getDefaultPort()).isEqualTo(8080);
            assertThat(loaded.isRemoteOperations()).isFalse();
            assertThat(loaded.isAutoCommit()).isTrue();
            assertThat(loaded.isBypassGitHooks()).isTrue();
            assertThat(loaded.isCheckActiveBranches()).isFalse();
            assertThat(loaded.getActiveBranchDays()).isEqualTo(7);
        }
    }

    @Test
    void saveAndLoadConfig_roundTrip_withStatuses(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig original = BacklogConfig.builder()
                    .statuses(List.of("Draft", "To Do", "In Progress", "Review", "Done"))
                    .labels(List.of("bug", "feature", "docs"))
                    .build();

            service.saveConfig(original);
            service.invalidateCache();
            BacklogConfig loaded = service.getConfig();

            assertThat(loaded.getStatuses()).containsExactly("Draft", "To Do", "In Progress", "Review", "Done");
            assertThat(loaded.getLabels()).containsExactly("bug", "feature", "docs");
        }
    }

    @Test
    void saveAndLoadConfig_roundTrip_withMilestones(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig original = BacklogConfig.builder()
                    .milestones(List.of(
                            BacklogConfig.BacklogMilestone.builder().name("v1.0").build(),
                            BacklogConfig.BacklogMilestone.builder().name("v2.0").build()
                    ))
                    .build();

            service.saveConfig(original);
            service.invalidateCache();
            BacklogConfig loaded = service.getConfig();

            // Milestones are serialized as inline list of names
            assertThat(loaded.getMilestones()).isNotNull();
        }
    }

    @Test
    void saveAndLoadConfig_roundTrip_emptyLists(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig original = BacklogConfig.builder()
                    .statuses(List.of())
                    .labels(List.of())
                    .milestones(List.of())
                    .build();

            service.saveConfig(original);
            service.invalidateCache();
            BacklogConfig loaded = service.getConfig();

            assertThat(loaded.getStatuses()).isEmpty();
            assertThat(loaded.getLabels()).isEmpty();
        }
    }

    @Test
    void saveConfig_throwsIOException_whenBasePathIsNull() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("backlog");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(null);

            BacklogConfigService service = new BacklogConfigService(project);

            assertThatThrownBy(() -> service.saveConfig(BacklogConfig.builder().build()))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("project base path is null");
        }
    }

    // ── parseConfig via getConfig (file-based) ─────────────────────────

    @Test
    void getConfig_parsesScalarFieldsFromFile(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    project_name: "My Custom Project"
                    default_status: "Draft"
                    task_prefix: "PROJ"
                    date_format: dd-MM-yyyy
                    max_column_width: 35
                    auto_open_browser: false
                    default_port: 9999
                    remote_operations: false
                    auto_commit: true
                    bypass_git_hooks: true
                    check_active_branches: false
                    active_branch_days: 14
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getProjectName()).isEqualTo("My Custom Project");
            assertThat(config.getDefaultStatus()).isEqualTo("Draft");
            assertThat(config.getTaskPrefix()).isEqualTo("PROJ");
            assertThat(config.getDateFormat()).isEqualTo("dd-MM-yyyy");
            assertThat(config.getMaxColumnWidth()).isEqualTo(35);
            assertThat(config.isAutoOpenBrowser()).isFalse();
            assertThat(config.getDefaultPort()).isEqualTo(9999);
            assertThat(config.isRemoteOperations()).isFalse();
            assertThat(config.isAutoCommit()).isTrue();
            assertThat(config.isBypassGitHooks()).isTrue();
            assertThat(config.isCheckActiveBranches()).isFalse();
            assertThat(config.getActiveBranchDays()).isEqualTo(14);
        }
    }

    @Test
    void getConfig_parsesInlineArrays(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    statuses: ["Draft", "To Do", "In Progress", "Done"]
                    labels: ["bug", "feature"]
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getStatuses()).containsExactly("Draft", "To Do", "In Progress", "Done");
            assertThat(config.getLabels()).containsExactly("bug", "feature");
        }
    }

    @Test
    void getConfig_parsesEmptyInlineArrays(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    statuses: []
                    labels: []
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getStatuses()).isEmpty();
            assertThat(config.getLabels()).isEmpty();
        }
    }

    @Test
    void getConfig_parsesMultilineListItems(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    statuses:
                      - Draft
                      - To Do
                      - In Progress
                      - Done
                    labels:
                      - bug
                      - enhancement
                      - docs
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getStatuses()).containsExactly("Draft", "To Do", "In Progress", "Done");
            assertThat(config.getLabels()).containsExactly("bug", "enhancement", "docs");
        }
    }

    @Test
    void getConfig_parsesMilestoneListItems(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            // The parser expects description as a separate list item (- description:)
            String yaml = """
                    milestones:
                      - name: v1.0
                      - description: First release
                      - name: v2.0
                      - description: Second release
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getMilestones()).hasSize(2);
            assertThat(config.getMilestones().get(0).getName()).isEqualTo("v1.0");
            assertThat(config.getMilestones().get(0).getDescription()).isEqualTo("First release");
            assertThat(config.getMilestones().get(1).getName()).isEqualTo("v2.0");
            assertThat(config.getMilestones().get(1).getDescription()).isEqualTo("Second release");
        }
    }

    @Test
    void getConfig_parsesSimpleMilestoneNames(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    milestones:
                      - Alpha
                      - Beta
                      - GA
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getMilestones()).hasSize(3);
            assertThat(config.getMilestones().get(0).getName()).isEqualTo("Alpha");
            assertThat(config.getMilestones().get(1).getName()).isEqualTo("Beta");
            assertThat(config.getMilestones().get(2).getName()).isEqualTo("GA");
        }
    }

    @Test
    void getConfig_skipsCommentsAndBlankLines(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    # This is a comment
                    project_name: "CommentTest"

                    # Another comment
                    task_prefix: "CT"
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getProjectName()).isEqualTo("CommentTest");
            assertThat(config.getTaskPrefix()).isEqualTo("CT");
        }
    }

    @Test
    void getConfig_ignoresUnknownFields(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    project_name: "UnknownFieldTest"
                    unknown_field: some_value
                    another_unknown:
                      - item1
                      - item2
                    task_prefix: "UFT"
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getProjectName()).isEqualTo("UnknownFieldTest");
            assertThat(config.getTaskPrefix()).isEqualTo("UFT");
        }
    }

    @Test
    void getConfig_returnsDefault_whenConfigFileDoesNotExist(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            BacklogConfig config = service.getConfig();

            assertThat(config.getDefaultStatus()).isEqualTo("To Do");
            assertThat(config.getTaskPrefix()).isEqualTo("task");
        }
    }

    @Test
    void getConfig_parsesAlternateCamelCaseKeys(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            // The parser supports both snake_case and camelCase variants
            String yaml = """
                    projectName: "CamelProject"
                    defaultStatus: "Draft"
                    taskPrefix: "CP"
                    dateFormat: "MM/dd/yyyy"
                    maxColumnWidth: 50
                    autoOpenBrowser: false
                    defaultPort: 7777
                    remoteOperations: false
                    autoCommit: true
                    bypassGitHooks: true
                    checkActiveBranches: false
                    activeBranchDays: 60
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getProjectName()).isEqualTo("CamelProject");
            assertThat(config.getDefaultStatus()).isEqualTo("Draft");
            assertThat(config.getTaskPrefix()).isEqualTo("CP");
            assertThat(config.getDateFormat()).isEqualTo("MM/dd/yyyy");
            assertThat(config.getMaxColumnWidth()).isEqualTo(50);
            assertThat(config.isAutoOpenBrowser()).isFalse();
            assertThat(config.getDefaultPort()).isEqualTo(7777);
            assertThat(config.isRemoteOperations()).isFalse();
            assertThat(config.isAutoCommit()).isTrue();
            assertThat(config.isBypassGitHooks()).isTrue();
            assertThat(config.isCheckActiveBranches()).isFalse();
            assertThat(config.getActiveBranchDays()).isEqualTo(60);
        }
    }

    // ── getConfig caching and invalidateCache ──────────────────────────

    @Test
    void getConfig_cachesBetweenCalls(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            Files.writeString(configPath, "project_name: \"First\"\n", StandardCharsets.UTF_8);
            BacklogConfig first = service.getConfig();
            assertThat(first.getProjectName()).isEqualTo("First");

            // Modify file on disk — should still return cached value
            Files.writeString(configPath, "project_name: \"Second\"\n", StandardCharsets.UTF_8);
            BacklogConfig cached = service.getConfig();
            assertThat(cached.getProjectName()).isEqualTo("First");
        }
    }

    @Test
    void invalidateCache_forcesReload(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            Files.writeString(configPath, "project_name: \"First\"\n", StandardCharsets.UTF_8);
            service.getConfig();

            Files.writeString(configPath, "project_name: \"Second\"\n", StandardCharsets.UTF_8);
            service.invalidateCache();
            BacklogConfig reloaded = service.getConfig();

            assertThat(reloaded.getProjectName()).isEqualTo("Second");
        }
    }

    @Test
    void saveConfig_updatesCacheImmediately(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .projectName("Cached")
                    .build();
            service.saveConfig(config);

            // getConfig should return the saved config without needing to read disk
            assertThat(service.getConfig().getProjectName()).isEqualTo("Cached");
        }
    }

    // ── ensureInitialized ──────────────────────────────────────────────

    @Test
    void ensureInitialized_initializesWhenNotYetInitialized(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            assertThat(service.isBacklogInitialized()).isFalse();

            service.ensureInitialized();

            assertThat(service.isBacklogInitialized()).isTrue();
            assertThat(tempDir.resolve("backlog/tasks")).isDirectory();
        }
    }

    @Test
    void ensureInitialized_noop_whenAlreadyInitialized(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog("First");
            String original = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);

            service.ensureInitialized();

            String afterEnsure = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(afterEnsure).isEqualTo(original);
        }
    }

    // ── Directory path accessors ───────────────────────────────────────

    @Test
    void directoryPathAccessors_returnCorrectPaths(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            Path backlog = tempDir.resolve("backlog");
            assertThat(service.getTasksDir()).isEqualTo(backlog.resolve("tasks"));
            assertThat(service.getDocsDir()).isEqualTo(backlog.resolve("docs"));
            assertThat(service.getCompletedDir()).isEqualTo(backlog.resolve("completed"));
            assertThat(service.getArchiveTasksDir()).isEqualTo(backlog.resolve("archive/tasks"));
            assertThat(service.getArchiveMilestonesDir()).isEqualTo(backlog.resolve("archive/milestones"));
            assertThat(service.getSpecDirectoryPath()).isEqualTo(backlog);
        }
    }

    @Test
    void directoryPathAccessors_returnNull_whenBasePathIsNull() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("backlog");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(null);

            BacklogConfigService service = new BacklogConfigService(project);

            assertThat(service.getTasksDir()).isNull();
            assertThat(service.getDocsDir()).isNull();
            assertThat(service.getCompletedDir()).isNull();
            assertThat(service.getArchiveTasksDir()).isNull();
            assertThat(service.getArchiveMilestonesDir()).isNull();
            assertThat(service.getSpecDirectoryPath()).isNull();
        }
    }

    @Test
    void getSpecDirectoryPath_usesDefaultWhenSpecDirectoryIsNull(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn(null);
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(tempDir.toString());

            BacklogConfigService service = new BacklogConfigService(project);

            assertThat(service.getSpecDirectoryPath()).isEqualTo(tempDir.resolve("backlog"));
        }
    }

    @Test
    void getSpecDirectoryPath_usesDefaultWhenSpecDirectoryIsEmpty(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(tempDir.toString());

            BacklogConfigService service = new BacklogConfigService(project);

            assertThat(service.getSpecDirectoryPath()).isEqualTo(tempDir.resolve("backlog"));
        }
    }

    @Test
    void getSpecDirectoryPath_usesCustomSpecDirectory(@TempDir Path tempDir) {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
            when(stateService.getSpecDirectory()).thenReturn("my-specs");
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

            Project project = mock(Project.class);
            when(project.getBasePath()).thenReturn(tempDir.toString());

            BacklogConfigService service = new BacklogConfigService(project);

            assertThat(service.getSpecDirectoryPath()).isEqualTo(tempDir.resolve("my-specs"));
        }
    }

    // ── getNextTaskId / getNextDocumentId ───────────────────────────────

    @Test
    void getNextTaskId_returnsOne_whenNoTasksExist(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            assertThat(service.getNextTaskId()).isEqualTo("task-1");
        }
    }

    @Test
    void getNextTaskId_scansTasksDirectory(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("task-1.md"), "---\nid: TASK-1\n---\n", StandardCharsets.UTF_8);
            Files.writeString(tasksDir.resolve("task-3.md"), "---\nid: TASK-3\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-4");
        }
    }

    @Test
    void getNextTaskId_scansCompletedDirectory(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path completedDir = tempDir.resolve("backlog/completed");
            Files.writeString(completedDir.resolve("task-5.md"), "---\nid: TASK-5\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-6");
        }
    }

    @Test
    void getNextTaskId_scansArchiveDirectory(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path archiveDir = tempDir.resolve("backlog/archive/tasks");
            Files.writeString(archiveDir.resolve("task-10.md"), "---\nid: TASK-10\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-11");
        }
    }

    @Test
    void getNextTaskId_takesMaxAcrossAllDirectories(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Files.writeString(tempDir.resolve("backlog/tasks/t1.md"), "---\nid: TASK-2\n---\n", StandardCharsets.UTF_8);
            Files.writeString(tempDir.resolve("backlog/completed/t2.md"), "---\nid: TASK-7\n---\n", StandardCharsets.UTF_8);
            Files.writeString(tempDir.resolve("backlog/archive/tasks/t3.md"), "---\nid: TASK-5\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-8");
        }
    }

    @Test
    void getNextTaskId_usesCustomPrefix(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .taskPrefix("PROJ")
                    .build();
            service.saveConfig(config);

            assertThat(service.getNextTaskId()).isEqualTo("PROJ-1");
        }
    }

    @Test
    void getNextTaskId_usesDefaultPrefix_whenPrefixIsEmpty(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .taskPrefix("")
                    .build();
            service.saveConfig(config);

            assertThat(service.getNextTaskId()).isEqualTo("task-1");
        }
    }

    @Test
    void getNextDocumentId_returnsOne_whenNoDocsExist(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            assertThat(service.getNextDocumentId()).isEqualTo("DOC-1");
        }
    }

    @Test
    void getNextDocumentId_scansDocsDirectory(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path docsDir = tempDir.resolve("backlog/docs");
            Files.writeString(docsDir.resolve("doc-1.md"), "---\nid: DOC-1\n---\n", StandardCharsets.UTF_8);
            Files.writeString(docsDir.resolve("doc-4.md"), "---\nid: DOC-4\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextDocumentId()).isEqualTo("DOC-5");
        }
    }

    // ── scanMaxId edge cases ───────────────────────────────────────────

    @Test
    void scanMaxId_ignoresNonMdFiles(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("task-99.txt"), "---\nid: TASK-99\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-1");
        }
    }

    @Test
    void scanMaxId_handlesNonNumericSuffix(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("task-abc.md"), "---\nid: TASK-abc\n---\n", StandardCharsets.UTF_8);
            Files.writeString(tasksDir.resolve("task-2.md"), "---\nid: TASK-2\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-3");
        }
    }

    @Test
    void scanMaxId_handlesIdWithoutDash(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("nodash.md"), "---\nid: NODASH\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-1");
        }
    }

    @Test
    void scanMaxId_handlesQuotedId(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("task-5.md"), "---\nid: \"TASK-5\"\n---\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-6");
        }
    }

    @Test
    void scanMaxId_handlesFileWithNoIdFrontmatter(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            service.initBacklog("Test");

            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("noid.md"), "# Just a markdown file\nNo frontmatter here.\n", StandardCharsets.UTF_8);

            assertThat(service.getNextTaskId()).isEqualTo("task-1");
        }
    }

    // ── Serialization format verification ──────────────────────────────

    @Test
    void serializeConfig_includesAllFields(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .projectName("SerTest")
                    .defaultStatus("Draft")
                    .taskPrefix("ST")
                    .dateFormat("yyyy/MM/dd")
                    .maxColumnWidth(30)
                    .autoOpenBrowser(false)
                    .defaultPort(5000)
                    .remoteOperations(false)
                    .autoCommit(true)
                    .bypassGitHooks(true)
                    .checkActiveBranches(false)
                    .activeBranchDays(15)
                    .statuses(List.of("A", "B"))
                    .labels(List.of("x", "y"))
                    .milestones(List.of(
                            BacklogConfig.BacklogMilestone.builder().name("M1").build()))
                    .build();

            service.saveConfig(config);

            String content = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);

            assertThat(content).contains("project_name: \"SerTest\"");
            assertThat(content).contains("default_status: \"Draft\"");
            assertThat(content).contains("task_prefix: \"ST\"");
            assertThat(content).contains("date_format: yyyy/MM/dd");
            assertThat(content).contains("max_column_width: 30");
            assertThat(content).contains("auto_open_browser: false");
            assertThat(content).contains("default_port: 5000");
            assertThat(content).contains("remote_operations: false");
            assertThat(content).contains("auto_commit: true");
            assertThat(content).contains("bypass_git_hooks: true");
            assertThat(content).contains("check_active_branches: false");
            assertThat(content).contains("active_branch_days: 15");
            assertThat(content).contains("statuses: [\"A\", \"B\"]");
            assertThat(content).contains("labels: [\"x\", \"y\"]");
            assertThat(content).contains("milestones: [\"M1\"]");
        }
    }

    @Test
    void serializeConfig_handlesNullProjectName(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .projectName(null)
                    .build();

            service.saveConfig(config);

            String content = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);

            assertThat(content).doesNotContain("project_name:");
        }
    }

    @Test
    void serializeConfig_handlesNullMilestones(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .milestones(null)
                    .build();

            service.saveConfig(config);

            String content = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);

            assertThat(content).contains("milestones: []");
        }
    }

    @Test
    void serializeInlineList_handlesEmptyList(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .labels(List.of())
                    .build();

            service.saveConfig(config);

            String content = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);

            assertThat(content).contains("labels: []");
        }
    }

    // ── Milestone parsing edge case: name without description flushes ──

    @Test
    void parseConfig_milestoneNameWithoutDescription_flushedOnNextKey(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            // Milestone with name: but no following description:, then another key
            String yaml = """
                    milestones:
                      - name: OrphanMilestone
                    task_prefix: "OM"
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getMilestones()).hasSize(1);
            assertThat(config.getMilestones().get(0).getName()).isEqualTo("OrphanMilestone");
            assertThat(config.getMilestones().get(0).getDescription()).isNull();
            assertThat(config.getTaskPrefix()).isEqualTo("OM");
        }
    }

    @Test
    void parseConfig_milestoneNameAtEndOfFile_flushed(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            // Milestone with name: at end of file (no more keys)
            String yaml = """
                    milestones:
                      - name: LastMilestone
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getMilestones()).hasSize(1);
            assertThat(config.getMilestones().get(0).getName()).isEqualTo("LastMilestone");
        }
    }

    // ── Definition of Done defaults ──────────────────────────────────────

    @Test
    void shouldCreateDefaultConfigWithEmptyDod() {
        BacklogConfig config = BacklogConfig.builder().build();
        assertThat(config.getDefinitionOfDone()).isEmpty();
    }

    @Test
    void shouldBuildConfigWithDodDefaults() {
        BacklogConfig config = BacklogConfig.builder()
                .definitionOfDone(List.of("Tests pass", "Documentation updated", "No regressions"))
                .build();

        assertThat(config.getDefinitionOfDone()).containsExactly(
                "Tests pass", "Documentation updated", "No regressions");
    }

    @Test
    void saveAndLoadConfig_roundTrip_withDodDefaults(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig original = BacklogConfig.builder()
                    .definitionOfDone(List.of("Tests pass", "Code reviewed", "No regressions introduced"))
                    .build();

            service.saveConfig(original);
            service.invalidateCache();
            BacklogConfig loaded = service.getConfig();

            assertThat(loaded.getDefinitionOfDone()).containsExactly(
                    "Tests pass", "Code reviewed", "No regressions introduced");
        }
    }

    @Test
    void saveAndLoadConfig_roundTrip_emptyDod(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig original = BacklogConfig.builder()
                    .definitionOfDone(List.of())
                    .build();

            service.saveConfig(original);
            service.invalidateCache();
            BacklogConfig loaded = service.getConfig();

            assertThat(loaded.getDefinitionOfDone()).isEmpty();
        }
    }

    @Test
    void serializeConfig_dodDefaultsUsesMultilineFormat(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder()
                    .definitionOfDone(List.of("Tests pass", "Docs updated"))
                    .build();

            service.saveConfig(config);

            String content = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(content).contains("definition_of_done:");
            assertThat(content).contains("  - \"Tests pass\"");
            assertThat(content).contains("  - \"Docs updated\"");
        }
    }

    @Test
    void serializeConfig_noDodSection_whenEmpty(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Files.createDirectories(tempDir.resolve("backlog"));

            BacklogConfig config = BacklogConfig.builder().build();
            service.saveConfig(config);

            String content = Files.readString(tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(content).doesNotContain("definition_of_done");
        }
    }

    @Test
    void getConfig_parsesDodFromMultilineList(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    project_name: "DodTest"
                    definition_of_done:
                      - Tests pass
                      - Documentation updated
                      - No regressions introduced
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getDefinitionOfDone()).containsExactly(
                    "Tests pass", "Documentation updated", "No regressions introduced");
        }
    }

    @Test
    void getConfig_parsesDodFromInlineArray(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);
            Path configPath = tempDir.resolve("backlog/config.yml");
            Files.createDirectories(configPath.getParent());

            String yaml = """
                    definition_of_done: ["Tests pass", "Code reviewed"]
                    """;
            Files.writeString(configPath, yaml, StandardCharsets.UTF_8);

            BacklogConfig config = service.getConfig();

            assertThat(config.getDefinitionOfDone()).containsExactly("Tests pass", "Code reviewed");
        }
    }

    /**
     * Sets up a BacklogConfigService backed by a temp directory with mocked
     * DevoxxGenieStateService returning "backlog" as the spec directory.
     */
    private BacklogConfigService setupService(Path tempDir, MockedStatic<DevoxxGenieStateService> stateMock) {
        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
        when(stateService.getSpecDirectory()).thenReturn("backlog");
        stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(tempDir.toString());

        return new BacklogConfigService(project);
    }
}
