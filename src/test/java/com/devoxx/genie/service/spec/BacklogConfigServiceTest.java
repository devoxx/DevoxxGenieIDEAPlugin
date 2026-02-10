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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for BacklogConfig model and BacklogConfigService init logic.
 */
class BacklogConfigServiceTest {

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
    void initBacklog_idempotent(@TempDir Path tempDir) throws IOException {
        try (MockedStatic<DevoxxGenieStateService> stateMock = Mockito.mockStatic(DevoxxGenieStateService.class)) {
            BacklogConfigService service = setupService(tempDir, stateMock);

            service.initBacklog("MyProject");
            // Write some content to a task file to verify it's not overwritten
            Path tasksDir = tempDir.resolve("backlog/tasks");
            Files.writeString(tasksDir.resolve("TASK-1.md"), "existing task");

            // Second call should not throw and should not overwrite config
            assertThatNoException().isThrownBy(() -> service.initBacklog("MyProject"));

            // Existing files should still be present
            assertThat(tasksDir.resolve("TASK-1.md")).hasContent("existing task");
            // Config should still contain original project name
            String configContent = Files.readString(
                    tempDir.resolve("backlog/config.yml"), StandardCharsets.UTF_8);
            assertThat(configContent).contains("project_name: \"MyProject\"");
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
