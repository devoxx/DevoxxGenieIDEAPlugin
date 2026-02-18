package com.devoxx.genie.service.agent.tool;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BacklogTaskToolExecutorTest {

    @Mock
    private Project project;

    @Mock
    private SpecService specService;

    private BacklogTaskToolExecutor executor;
    private MockedStatic<SpecService> mockedSpecService;

    @BeforeEach
    void setUp() {
        mockedSpecService = Mockito.mockStatic(SpecService.class);
        mockedSpecService.when(() -> SpecService.getInstance(project)).thenReturn(specService);
        executor = new BacklogTaskToolExecutor(project);
    }

    @AfterEach
    void tearDown() {
        mockedSpecService.close();
    }

    @Test
    void execute_unknownToolName_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_unknown")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("Unknown task tool").contains("backlog_task_unknown");
    }

    @Test
    void createTask_withTitle_createsTaskSuccessfully() throws Exception {
        TaskSpec created = TaskSpec.builder()
                .id("TASK-1")
                .title("My Task")
                .filePath("/backlog/tasks/task-1.md")
                .build();
        when(specService.createTask(any(TaskSpec.class), anyBoolean())).thenReturn(created);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_create")
                .arguments("{\"title\": \"My Task\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Created task TASK-1").contains("My Task");
        verify(specService).createTask(any(TaskSpec.class), anyBoolean());
    }

    @Test
    void createTask_missingTitle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_create")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("title");
    }

    @Test
    void createTask_emptyTitle_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_create")
                .arguments("{\"title\": \"\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("title");
    }

    @Test
    void createTask_withAllFields_passesFieldsCorrectly() throws Exception {
        TaskSpec created = TaskSpec.builder()
                .id("TASK-2")
                .title("Full Task")
                .filePath("/backlog/tasks/task-2.md")
                .build();
        when(specService.createTask(any(TaskSpec.class), anyBoolean())).thenReturn(created);

        String args = """
                {
                    "title": "Full Task",
                    "description": "A detailed description",
                    "priority": "high",
                    "status": "In Progress",
                    "milestone": "v1.0",
                    "parentTaskId": "TASK-0",
                    "labels": ["bug", "urgent"],
                    "assignee": ["alice"],
                    "dependencies": ["TASK-0"],
                    "references": ["ref1"],
                    "documentation": ["doc1"],
                    "acceptanceCriteria": ["AC1", "AC2"]
                }
                """;

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_create")
                .arguments(args)
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Created task TASK-2");
        verify(specService).createTask(any(TaskSpec.class), anyBoolean());
    }

    @Test
    void listTasks_noResults_returnsNoTasksMessage() {
        when(specService.getSpecsByFilters(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_list")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("No tasks found");
    }

    @Test
    void listTasks_withResults_returnsFormattedList() {
        TaskSpec task1 = TaskSpec.builder()
                .id("TASK-1").title("Task One").status("To Do").priority("high").milestone("v1.0")
                .acceptanceCriteria(List.of(
                        AcceptanceCriterion.builder().index(0).text("AC1").checked(true).build(),
                        AcceptanceCriterion.builder().index(1).text("AC2").checked(false).build()
                ))
                .build();
        TaskSpec task2 = TaskSpec.builder()
                .id("TASK-2").title("Task Two").status("In Progress")
                .build();

        when(specService.getSpecsByFilters(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(task1, task2));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_list")
                .arguments("{\"status\": \"To Do\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Tasks (2 total)");
        assertThat(result).contains("TASK-1: Task One");
        assertThat(result).contains("[high]");
        assertThat(result).contains("(v1.0)");
        assertThat(result).contains("AC: 1/2");
    }

    @Test
    void searchTasks_missingQuery_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_search")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("query");
    }

    @Test
    void searchTasks_withResults_returnsFormattedList() {
        TaskSpec task = TaskSpec.builder()
                .id("TASK-1").title("Test Task").status("To Do").priority("high")
                .build();
        when(specService.searchSpecs(eq("test"), any(), any(), anyInt()))
                .thenReturn(List.of(task));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_search")
                .arguments("{\"query\": \"test\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Search results (1)");
        assertThat(result).contains("TASK-1: Test Task");
        assertThat(result).contains("[To Do]");
        assertThat(result).contains("[high]");
    }

    @Test
    void searchTasks_noResults_returnsNotFoundMessage() {
        when(specService.searchSpecs(eq("nothing"), any(), any(), anyInt()))
                .thenReturn(List.of());

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_search")
                .arguments("{\"query\": \"nothing\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("No tasks found matching query: nothing");
    }

    @Test
    void viewTask_missingId_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_view")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("id");
    }

    @Test
    void viewTask_notFound_returnsError() {
        when(specService.getSpec("TASK-99")).thenReturn(null);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_view")
                .arguments("{\"id\": \"TASK-99\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("TASK-99").contains("not found");
    }

    @Test
    void viewTask_found_returnsContext() {
        TaskSpec task = TaskSpec.builder().id("TASK-1").title("My Task").status("To Do").build();
        when(specService.getSpec("TASK-1")).thenReturn(task);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_view")
                .arguments("{\"id\": \"TASK-1\"}")
                .build();

        String result = executor.execute(request, null);
        // SpecContextBuilder wraps in <TaskSpec> tags
        assertThat(result).contains("TaskSpec");
        assertThat(result).contains("TASK-1");
    }

    @Test
    void editTask_missingId_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_edit")
                .arguments("{\"title\": \"New Title\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("id");
    }

    @Test
    void editTask_notFound_returnsError() {
        when(specService.getSpec("TASK-99")).thenReturn(null);

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_edit")
                .arguments("{\"id\": \"TASK-99\", \"title\": \"New\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("TASK-99").contains("not found");
    }

    @Test
    void editTask_updatesScalarFields() throws Exception {
        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Old").status("To Do").build();
        when(specService.getSpec("TASK-1")).thenReturn(task);

        String args = """
                {
                    "id": "TASK-1",
                    "title": "New Title",
                    "description": "New Desc",
                    "status": "In Progress",
                    "priority": "high",
                    "milestone": "v2.0",
                    "finalSummary": "Done summary"
                }
                """;

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_edit")
                .arguments(args)
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Updated task TASK-1 successfully");
        assertThat(task.getTitle()).isEqualTo("New Title");
        assertThat(task.getDescription()).isEqualTo("New Desc");
        assertThat(task.getStatus()).isEqualTo("In Progress");
        assertThat(task.getPriority()).isEqualTo("high");
        assertThat(task.getMilestone()).isEqualTo("v2.0");
        assertThat(task.getFinalSummary()).isEqualTo("Done summary");
        verify(specService).updateTask(task);
    }

    @Test
    void editTask_acceptanceCriteriaOperations() throws Exception {
        List<AcceptanceCriterion> ac = new ArrayList<>();
        ac.add(AcceptanceCriterion.builder().index(0).text("AC1").checked(false).build());
        ac.add(AcceptanceCriterion.builder().index(1).text("AC2").checked(false).build());
        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test")
                .acceptanceCriteria(ac).build();
        when(specService.getSpec("TASK-1")).thenReturn(task);

        // Add new criteria and check item 1 (1-based)
        String args = """
                {
                    "id": "TASK-1",
                    "acceptanceCriteriaAdd": ["AC3"],
                    "acceptanceCriteriaCheck": [1]
                }
                """;

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_edit")
                .arguments(args)
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Updated task TASK-1");
        assertThat(task.getAcceptanceCriteria()).hasSize(3);
        assertThat(task.getAcceptanceCriteria().get(0).isChecked()).isTrue();
        assertThat(task.getAcceptanceCriteria().get(2).getText()).isEqualTo("AC3");
    }

    @Test
    void editTask_planSetAndPlanAppend() throws Exception {
        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test").build();
        when(specService.getSpec("TASK-1")).thenReturn(task);

        String args = """
                {
                    "id": "TASK-1",
                    "planSet": "Step 1",
                    "planAppend": ["Step 2"]
                }
                """;

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_edit")
                .arguments(args)
                .build();

        executor.execute(request, null);
        assertThat(task.getImplementationPlan()).isEqualTo("Step 1\n\nStep 2");
    }

    @Test
    void editTask_planClear() throws Exception {
        TaskSpec task = TaskSpec.builder().id("TASK-1").title("Test")
                .implementationPlan("old plan").build();
        when(specService.getSpec("TASK-1")).thenReturn(task);

        String args = """
                {
                    "id": "TASK-1",
                    "planClear": true
                }
                """;

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_edit")
                .arguments(args)
                .build();

        executor.execute(request, null);
        assertThat(task.getImplementationPlan()).isNull();
    }

    @Test
    void completeTask_missingId_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_complete")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("id");
    }

    @Test
    void completeTask_success() throws Exception {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_complete")
                .arguments("{\"id\": \"TASK-1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Task TASK-1 marked as Done");
        verify(specService).completeTask("TASK-1");
    }

    @Test
    void archiveTask_missingId_returnsError() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_archive")
                .arguments("{}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("id");
    }

    @Test
    void archiveTask_success() throws Exception {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_archive")
                .arguments("{\"id\": \"TASK-1\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Task TASK-1 archived");
        verify(specService).archiveTask("TASK-1");
    }

    @Test
    void execute_exceptionInHandler_returnsErrorMessage() throws Exception {
        when(specService.createTask(any(TaskSpec.class), anyBoolean())).thenThrow(new RuntimeException("IO fail"));

        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("backlog_task_create")
                .arguments("{\"title\": \"Test\"}")
                .build();

        String result = executor.execute(request, null);
        assertThat(result).contains("Error").contains("IO fail");
    }
}
