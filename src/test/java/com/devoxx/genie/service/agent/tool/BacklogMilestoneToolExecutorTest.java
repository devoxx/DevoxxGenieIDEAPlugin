package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.BacklogConfig;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.BacklogConfigService;
import com.devoxx.genie.service.spec.SpecService;
import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BacklogMilestoneToolExecutorTest {

    @Mock
    private Project project;

    @Mock
    private SpecService specService;

    @Mock
    private BacklogConfigService configService;

    private BacklogMilestoneToolExecutor executor;
    private MockedStatic<SpecService> mockedSpecService;
    private MockedStatic<BacklogConfigService> mockedConfigService;

    @BeforeEach
    void setUp() {
        mockedSpecService = Mockito.mockStatic(SpecService.class);
        mockedSpecService.when(() -> SpecService.getInstance(project)).thenReturn(specService);

        mockedConfigService = Mockito.mockStatic(BacklogConfigService.class);
        mockedConfigService.when(() -> BacklogConfigService.getInstance(project)).thenReturn(configService);

        executor = new BacklogMilestoneToolExecutor(project);
    }

    @AfterEach
    void tearDown() {
        mockedSpecService.close();
        mockedConfigService.close();
    }

    @Test
    void execute_unknownToolName_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_unknown")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Unknown milestone tool");
    }

    @Test
    void listMilestones_noMilestones_returnsEmptyMessage() {
        BacklogConfig config = BacklogConfig.builder().milestones(new ArrayList<>()).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("No milestones configured");
    }

    @Test
    void listMilestones_nullMilestones_returnsEmptyMessage() {
        BacklogConfig config = BacklogConfig.builder().build();
        config.setMilestones(null);
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("No milestones configured");
    }

    @Test
    void listMilestones_withMilestones_returnsFormattedList() {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").description("First release").build());
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v2.0").description(null).build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);
        when(specService.getAllSpecs()).thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Milestones (2)");
        assertThat(result).contains("v1.0: First release");
        assertThat(result).contains("- v2.0");
    }

    @Test
    void listMilestones_showsExtraMilestonesFromTasks() {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test").milestone("v3.0").build();
        when(specService.getAllSpecs()).thenReturn(List.of(task));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Milestones found on tasks (not in config)");
        assertThat(result).contains("v3.0");
    }

    @Test
    void addMilestone_missingName_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_add")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("name");
    }

    @Test
    void addMilestone_duplicate_returnsError() {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_add")
                .arguments("{\"name\": \"v1.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("already exists");
    }

    @Test
    void addMilestone_success() throws Exception {
        BacklogConfig config = BacklogConfig.builder().milestones(new ArrayList<>()).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_add")
                .arguments("{\"name\": \"v2.0\", \"description\": \"Next release\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Added milestone: v2.0");
        verify(configService).saveConfig(any(BacklogConfig.class));
    }

    @Test
    void addMilestone_nullMilestonesList_createsNewList() throws Exception {
        BacklogConfig config = BacklogConfig.builder().build();
        config.setMilestones(null);
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_add")
                .arguments("{\"name\": \"v1.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Added milestone: v1.0");
        verify(configService).saveConfig(any(BacklogConfig.class));
    }

    @Test
    void renameMilestone_missingFrom_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_rename")
                .arguments("{\"to\": \"v2.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("from");
    }

    @Test
    void renameMilestone_missingTo_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_rename")
                .arguments("{\"from\": \"v1.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("to");
    }

    @Test
    void renameMilestone_notFound_returnsError() {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_rename")
                .arguments("{\"from\": \"v3.0\", \"to\": \"v4.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("v3.0").contains("not found");
    }

    @Test
    void renameMilestone_success_updatesTasksByDefault() throws Exception {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test").milestone("v1.0").build();
        when(specService.getAllSpecs()).thenReturn(List.of(task));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_rename")
                .arguments("{\"from\": \"v1.0\", \"to\": \"v2.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Renamed milestone 'v1.0' to 'v2.0'");
        assertThat(result).contains("Updated 1 task(s)");
        verify(specService).updateTask(task);
    }

    @Test
    void renameMilestone_nullMilestonesList_returnsError() {
        BacklogConfig config = BacklogConfig.builder().build();
        config.setMilestones(null);
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_rename")
                .arguments("{\"from\": \"v1.0\", \"to\": \"v2.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("No milestones configured");
    }

    @Test
    void removeMilestone_missingName_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_remove")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("name");
    }

    @Test
    void removeMilestone_notFound_returnsError() {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_remove")
                .arguments("{\"name\": \"nonexistent\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("nonexistent").contains("not found");
    }

    @Test
    void removeMilestone_clearHandling_clearsMilestoneOnTasks() throws Exception {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test").milestone("v1.0").build();
        when(specService.getAllSpecs()).thenReturn(List.of(task));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_remove")
                .arguments("{\"name\": \"v1.0\", \"taskHandling\": \"clear\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Removed milestone 'v1.0'");
        assertThat(result).contains("clear handling applied to 1 task(s)");
        assertThat(task.getMilestone()).isNull();
    }

    @Test
    void removeMilestone_reassignHandling() throws Exception {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test").milestone("v1.0").build();
        when(specService.getAllSpecs()).thenReturn(List.of(task));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_remove")
                .arguments("{\"name\": \"v1.0\", \"taskHandling\": \"reassign\", \"reassignTo\": \"v2.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Removed milestone 'v1.0'");
        assertThat(task.getMilestone()).isEqualTo("v2.0");
    }

    @Test
    void removeMilestone_keepHandling_doesNotModifyTasks() throws Exception {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        when(specService.getAllSpecs()).thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_remove")
                .arguments("{\"name\": \"v1.0\", \"taskHandling\": \"keep\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Removed milestone 'v1.0'");
        assertThat(result).contains("keep handling applied to 0 task(s)");
    }

    @Test
    void archiveMilestone_missingName_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_archive")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("name");
    }

    @Test
    void archiveMilestone_notFound_returnsError() {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_archive")
                .arguments("{\"name\": \"nonexistent\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("nonexistent").contains("not found");
    }

    @Test
    void archiveMilestone_success() throws Exception {
        List<BacklogConfig.BacklogMilestone> milestones = new ArrayList<>();
        milestones.add(BacklogConfig.BacklogMilestone.builder().name("v1.0").build());
        BacklogConfig config = BacklogConfig.builder().milestones(milestones).build();
        when(configService.getConfig()).thenReturn(config);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_archive")
                .arguments("{\"name\": \"v1.0\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Archived milestone: v1.0");
        verify(configService).saveConfig(any(BacklogConfig.class));
    }

    @Test
    void execute_exceptionInHandler_returnsErrorMessage() {
        when(configService.getConfig()).thenThrow(new RuntimeException("config read failed"));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_milestone_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("config read failed");
    }
}
