package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.cli.CliTaskExecutorService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.spec.SpecTaskRunnerService.RunnerState;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SpecTaskRunnerServiceTest {

    // ── Initial state ──────────────────────────────────────────────────

    @Test
    void initialState_isIdle() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
        assertThat(service.isRunning()).isFalse();
        assertThat(service.getCurrentTask()).isNull();
        assertThat(service.getTotalTasks()).isZero();
        assertThat(service.getCurrentTaskIndex()).isEqualTo(-1);
        assertThat(service.getCompletedCount()).isZero();
        assertThat(service.getSkippedCount()).isZero();
        assertThat(service.isCliMode()).isFalse();
    }

    // ── runTasks with empty list ────────────────────────────────────────

    @Test
    void runTasks_emptyList_doesNothing() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        service.runTasks(List.of());

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
        assertThat(service.isRunning()).isFalse();
    }

    // ── Listener management ────────────────────────────────────────────

    @Test
    void addAndRemoveListener() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
        service.addListener(listener);
        service.removeListener(listener);

        // Just verify no exceptions; actual notification tested in lifecycle tests
    }

    // ── cancel when idle ───────────────────────────────────────────────

    @Test
    void cancel_whenIdle_doesNothing() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        service.cancel(); // should be a no-op

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
    }

    // ── dispose ────────────────────────────────────────────────────────

    @Test
    void dispose_resetsState() {
        try (var ctx = new RunnerMockContext()) {
            SpecTaskRunnerService service = ctx.createService();

            service.addListener(mock(SpecTaskRunnerListener.class));
            service.dispose();

            assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
            assertThat(service.isCliMode()).isFalse();
        }
    }

    // ── Full LLM lifecycle: single task already Done ───────────────────

    @Test
    void runTasks_singleDoneTask_completesImmediately() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("Already done")
                    .status("Done")
                    .build();

            // The fresh read returns Done status
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));

            // Task was already Done so it should count as completed
            assertThat(service.getCompletedCount()).isEqualTo(1);
            assertThat(service.getState()).isEqualTo(RunnerState.IDLE);

            verify(listener).onRunStarted(1);
            verify(listener).onTaskCompleted(eq(task), anyInt(), eq(1));
            verify(listener).onRunFinished(eq(1), eq(0), eq(1), eq(RunnerState.ALL_COMPLETED));
        }
    }

    // ── Single task not found → skipped ────────────────────────────────

    @Test
    void runTasks_taskNotFound_skipped() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("Ghost task")
                    .status("To Do")
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(null); // not found on re-read

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));

            assertThat(service.getSkippedCount()).isEqualTo(1);
            verify(listener).onTaskSkipped(eq(task), anyInt(), eq(1), contains("not found"));
            verify(listener).onRunFinished(eq(0), eq(1), eq(1), eq(RunnerState.ALL_COMPLETED));
        }
    }

    // ── Single task submitted via LLM ──────────────────────────────────

    @Test
    void runTasks_llmMode_submitsPrompt() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("LLM task")
                    .status("To Do")
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));

            // Should be waiting for completion
            assertThat(service.isRunning()).isTrue();
            assertThat(service.getState()).isEqualTo(RunnerState.WAITING_FOR_COMPLETION);
            assertThat(service.getCurrentTask()).isEqualTo(task);
            assertThat(service.getTotalTasks()).isEqualTo(1);
            assertThat(service.getCurrentTaskIndex()).isEqualTo(0);
            assertThat(service.isCliMode()).isFalse();

            // Verify prompt was submitted
            verify(ctx.promptSubmissionListener).onPromptSubmitted(eq(ctx.project), anyString());
            verify(ctx.chatMemoryService).clearMemory(ctx.project);
            verify(ctx.fileListManager).clear(ctx.project);

            verify(listener).onRunStarted(1);
            verify(listener).onTaskStarted(eq(task), eq(0), eq(1));
        }
    }

    // ── Single task submitted via CLI ──────────────────────────────────

    @Test
    void runTasks_cliMode_executesCliTool() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("CLI task")
                    .status("To Do")
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("claude");

            CliToolConfig cliTool = CliToolConfig.builder()
                    .name("claude")
                    .type(CliToolConfig.CliType.CLAUDE)
                    .enabled(true)
                    .build();
            when(ctx.stateService.getCliTools()).thenReturn(List.of(cliTool));

            SpecTaskRunnerService service = ctx.createService();

            service.runTasks(List.of(task));

            assertThat(service.isRunning()).isTrue();
            assertThat(service.isCliMode()).isTrue();
            verify(ctx.cliTaskExecutorService).execute(eq(cliTool), anyString(), eq("TASK-1"), eq("CLI task"));
        }
    }

    // ── CLI tool not found → skip ──────────────────────────────────────

    @Test
    void runTasks_cliMode_toolNotFound_skips() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("CLI task")
                    .status("To Do")
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("nonexistent");
            when(ctx.stateService.getCliTools()).thenReturn(List.of());

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));

            assertThat(service.getSkippedCount()).isEqualTo(1);
            verify(listener).onTaskSkipped(eq(task), anyInt(), eq(1), contains("not found"));
        }
    }

    @Test
    void runTasks_cliMode_toolNameNull_skips() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("CLI task")
                    .status("To Do")
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn(null);

            SpecTaskRunnerService service = ctx.createService();

            service.runTasks(List.of(task));

            assertThat(service.getSkippedCount()).isEqualTo(1);
        }
    }

    @Test
    void runTasks_cliMode_disabledTool_skips() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-1")
                    .title("CLI task")
                    .status("To Do")
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("claude");

            CliToolConfig disabledTool = CliToolConfig.builder()
                    .name("claude")
                    .type(CliToolConfig.CliType.CLAUDE)
                    .enabled(false)
                    .build();
            when(ctx.stateService.getCliTools()).thenReturn(List.of(disabledTool));

            SpecTaskRunnerService service = ctx.createService();

            service.runTasks(List.of(task));

            assertThat(service.getSkippedCount()).isEqualTo(1);
        }
    }

    // ── runTasks when already running → ignored ────────────────────────

    @Test
    void runTasks_whenAlreadyRunning_ignored() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task1 = TaskSpec.builder().id("TASK-1").title("T1").status("To Do").build();
            TaskSpec task2 = TaskSpec.builder().id("TASK-2").title("T2").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task1, task2));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task1);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            service.runTasks(List.of(task1));

            // Now running, second call should be ignored
            service.runTasks(List.of(task2));

            // Still on task1
            assertThat(service.getCurrentTask()).isEqualTo(task1);
        }
    }

    // ── notifyPromptExecutionCompleted ────────────────────────────────

    @Test
    void notifyPromptExecutionCompleted_whenNotWaiting_doesNothing() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        // State is IDLE, calling notifyPromptExecutionCompleted should be a no-op
        service.notifyPromptExecutionCompleted();

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
    }

    // ── notifyCliTaskFailed ────────────────────────────────────────────

    @Test
    void notifyCliTaskFailed_skipsTaskAndStopsRun() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("CLI task").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("claude");

            CliToolConfig cliTool = CliToolConfig.builder()
                    .name("claude").type(CliToolConfig.CliType.CLAUDE).enabled(true).build();
            when(ctx.stateService.getCliTools()).thenReturn(List.of(cliTool));

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));
            assertThat(service.getState()).isEqualTo(RunnerState.WAITING_FOR_COMPLETION);

            service.notifyCliTaskFailed(1, "Authentication failed\nPlease check credentials");

            assertThat(service.getSkippedCount()).isEqualTo(1);
            verify(listener).onTaskSkipped(eq(task), anyInt(), eq(1), contains("exit code 1"));
            verify(listener).onRunFinished(eq(0), eq(1), eq(1), eq(RunnerState.ERROR));
        }
    }

    @Test
    void notifyCliTaskFailed_whenNotWaiting_doesNothing() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        service.notifyCliTaskFailed(1, "some error");

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
        assertThat(service.getSkippedCount()).isZero();
    }

    @Test
    void notifyCliTaskFailed_blankErrorOutput_usesUnknownError() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("CLI task").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("claude");

            CliToolConfig cliTool = CliToolConfig.builder()
                    .name("claude").type(CliToolConfig.CliType.CLAUDE).enabled(true).build();
            when(ctx.stateService.getCliTools()).thenReturn(List.of(cliTool));

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));
            service.notifyCliTaskFailed(127, "  \n  \n  ");

            verify(listener).onTaskSkipped(eq(task), anyInt(), eq(1), contains("Unknown error"));
        }
    }

    // ── cancel while running ───────────────────────────────────────────

    @Test
    void cancel_whileRunning_stopsExecution() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("T1").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(task));
            assertThat(service.isRunning()).isTrue();

            service.cancel();

            assertThat(service.isRunning()).isFalse();
            verify(listener).onRunFinished(anyInt(), anyInt(), eq(1), eq(RunnerState.CANCELLED));
        }
    }

    @Test
    void cancel_cliMode_cancelsCliProcess() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("T1").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("claude");

            CliToolConfig cliTool = CliToolConfig.builder()
                    .name("claude").type(CliToolConfig.CliType.CLAUDE).enabled(true).build();
            when(ctx.stateService.getCliTools()).thenReturn(List.of(cliTool));

            SpecTaskRunnerService service = ctx.createService();
            service.runTasks(List.of(task));

            service.cancel();

            verify(ctx.cliTaskExecutorService).cancelAllProcesses();
        }
    }

    // ── Multiple tasks: one Done, one To Do ────────────────────────────

    @Test
    void runTasks_multipleTasks_skipsCompletedAndSubmitsNext() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec done = TaskSpec.builder().id("TASK-1").title("Done").status("Done").build();
            TaskSpec todo = TaskSpec.builder().id("TASK-2").title("Todo").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(done, todo));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(done);
            when(ctx.specService.getSpec("TASK-2")).thenReturn(todo);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(done, todo));

            // TASK-1 was already Done, TASK-2 should now be executing
            assertThat(service.getCompletedCount()).isEqualTo(1);
            assertThat(service.getCurrentTask()).isEqualTo(todo);
            assertThat(service.getState()).isEqualTo(RunnerState.WAITING_FOR_COMPLETION);

            verify(listener).onTaskCompleted(eq(done), anyInt(), eq(2));
            verify(listener).onTaskStarted(eq(todo), anyInt(), eq(2));
        }
    }

    // ── Unsatisfied dependencies → skip ────────────────────────────────

    @Test
    void runTasks_unsatisfiedDependencies_skipsTask() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec dep = TaskSpec.builder().id("TASK-1").title("Dependency").status("To Do").build();
            TaskSpec task = TaskSpec.builder()
                    .id("TASK-2").title("Dependent").status("To Do")
                    .dependencies(List.of("TASK-99"))
                    .build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(dep, task));
            // task is sorted first (dependency not in selected set)
            when(ctx.specService.getSpec("TASK-2")).thenReturn(task);
            when(ctx.specService.getSpec("TASK-1")).thenReturn(dep);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            // Run just task (with unsatisfied dep on TASK-99, which doesn't exist)
            service.runTasks(List.of(task));

            assertThat(service.getSkippedCount()).isEqualTo(1);
            verify(listener).onTaskSkipped(eq(task), anyInt(), eq(1), contains("Unsatisfied dependencies"));
        }
    }

    // ── Listener notification coverage ─────────────────────────────────

    @Test
    void listeners_receiveAllNotifications_inOrder() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("T1").status("Done").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();

            List<String> events = new ArrayList<>();
            service.addListener(new SpecTaskRunnerListener() {
                @Override
                public void onRunStarted(int totalTasks) {
                    events.add("runStarted:" + totalTasks);
                }
                @Override
                public void onTaskStarted(TaskSpec t, int index, int total) {
                    events.add("taskStarted:" + t.getId());
                }
                @Override
                public void onTaskCompleted(TaskSpec t, int index, int total) {
                    events.add("taskCompleted:" + t.getId());
                }
                @Override
                public void onTaskSkipped(TaskSpec t, int index, int total, String reason) {
                    events.add("taskSkipped:" + t.getId());
                }
                @Override
                public void onRunFinished(int completed, int skipped, int total, RunnerState state) {
                    events.add("runFinished:" + state);
                }
            });

            service.runTasks(List.of(task));

            assertThat(events).containsExactly(
                    "runStarted:1",
                    "taskCompleted:TASK-1",
                    "runFinished:ALL_COMPLETED"
            );
        }
    }

    @Test
    void multipleListeners_allNotified() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("T1").status("Done").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener1 = mock(SpecTaskRunnerListener.class);
            SpecTaskRunnerListener listener2 = mock(SpecTaskRunnerListener.class);
            service.addListener(listener1);
            service.addListener(listener2);

            service.runTasks(List.of(task));

            verify(listener1).onRunStarted(1);
            verify(listener2).onRunStarted(1);
            verify(listener1).onRunFinished(eq(1), eq(0), eq(1), eq(RunnerState.ALL_COMPLETED));
            verify(listener2).onRunFinished(eq(1), eq(0), eq(1), eq(RunnerState.ALL_COMPLETED));
        }
    }

    // ── dispose while running ──────────────────────────────────────────

    @Test
    void dispose_whileRunning_cleansUp() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("T1").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");

            SpecTaskRunnerService service = ctx.createService();
            service.runTasks(List.of(task));
            assertThat(service.isRunning()).isTrue();

            service.dispose();

            assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
            assertThat(service.isCliMode()).isFalse();
            verify(ctx.specService).removeChangeListener(any());
        }
    }

    // ── RunnerState enum coverage ──────────────────────────────────────

    @Test
    void runnerState_allValuesExist() {
        assertThat(RunnerState.values()).containsExactly(
                RunnerState.IDLE,
                RunnerState.RUNNING_TASK,
                RunnerState.WAITING_FOR_COMPLETION,
                RunnerState.ALL_COMPLETED,
                RunnerState.CANCELLED,
                RunnerState.ERROR
        );
    }

    // ── CLI tool null tools list ──────────────────────────────────────

    @Test
    void runTasks_cliMode_nullToolsList_skips() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec task = TaskSpec.builder().id("TASK-1").title("CLI task").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(task));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(task);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("cli");
            when(ctx.stateService.getSpecSelectedCliTool()).thenReturn("claude");
            when(ctx.stateService.getCliTools()).thenReturn(null);

            SpecTaskRunnerService service = ctx.createService();

            service.runTasks(List.of(task));

            assertThat(service.getSkippedCount()).isEqualTo(1);
        }
    }

    // ── Parallel mode: all Done tasks ─────────────────────────────────

    @Test
    void runTasks_parallelMode_allDoneTasks_completesImmediately() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec t1 = TaskSpec.builder().id("TASK-1").title("T1").status("Done").build();
            TaskSpec t2 = TaskSpec.builder().id("TASK-2").title("T2").status("Done").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(t1, t2));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(t1);
            when(ctx.specService.getSpec("TASK-2")).thenReturn(t2);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");
            when(ctx.stateService.getSpecExecutionMode()).thenReturn("PARALLEL");
            when(ctx.stateService.getSpecMaxConcurrency()).thenReturn(4);

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(t1, t2));

            assertThat(service.getCompletedCount()).isEqualTo(2);
            assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
            verify(listener).onRunFinished(eq(2), eq(0), eq(2), eq(RunnerState.ALL_COMPLETED));
        }
    }

    // ── Parallel mode: task not found ──────────────────────────────────

    @Test
    void runTasks_parallelMode_taskNotFound_skipped() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec t1 = TaskSpec.builder().id("TASK-1").title("T1").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(t1));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(null); // not found
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");
            when(ctx.stateService.getSpecExecutionMode()).thenReturn("PARALLEL");
            when(ctx.stateService.getSpecMaxConcurrency()).thenReturn(4);

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(t1));

            assertThat(service.getSkippedCount()).isEqualTo(1);
            verify(listener).onTaskSkipped(eq(t1), anyInt(), eq(1), contains("not found"));
        }
    }

    // ── Parallel mode: execution mode getter ──────────────────────────

    @Test
    void runTasks_parallelMode_setsExecutionMode() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec t1 = TaskSpec.builder().id("TASK-1").title("T1").status("Done").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(t1));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(t1);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");
            when(ctx.stateService.getSpecExecutionMode()).thenReturn("PARALLEL");
            when(ctx.stateService.getSpecMaxConcurrency()).thenReturn(4);

            SpecTaskRunnerService service = ctx.createService();
            service.runTasks(List.of(t1));

            // After completion, executionMode resets to SEQUENTIAL
            assertThat(service.getExecutionMode()).isEqualTo(com.devoxx.genie.model.enumarations.ExecutionMode.SEQUENTIAL);
        }
    }

    // ── Parallel mode: cancel ──────────────────────────────────────────

    @Test
    void cancel_parallelMode_stopsExecution() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec t1 = TaskSpec.builder().id("TASK-1").title("T1").status("To Do").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(t1));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(t1);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");
            when(ctx.stateService.getSpecExecutionMode()).thenReturn("PARALLEL");
            when(ctx.stateService.getSpecMaxConcurrency()).thenReturn(4);

            SpecTaskRunnerService service = ctx.createService();
            SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
            service.addListener(listener);

            service.runTasks(List.of(t1));
            assertThat(service.isRunning()).isTrue();

            service.cancel();

            assertThat(service.isRunning()).isFalse();
            verify(listener).onRunFinished(anyInt(), anyInt(), eq(1), eq(RunnerState.CANCELLED));
        }
    }

    // ── Parallel mode: circular dependency ─────────────────────────────

    @Test
    void runTasks_parallelMode_circularDependency_throwsError() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec t1 = TaskSpec.builder().id("TASK-1").title("T1").status("To Do")
                    .dependencies(List.of("TASK-2")).build();
            TaskSpec t2 = TaskSpec.builder().id("TASK-2").title("T2").status("To Do")
                    .dependencies(List.of("TASK-1")).build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(t1, t2));
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");
            when(ctx.stateService.getSpecExecutionMode()).thenReturn("PARALLEL");
            when(ctx.stateService.getSpecMaxConcurrency()).thenReturn(4);

            SpecTaskRunnerService service = ctx.createService();

            assertThatThrownBy(() -> service.runTasks(List.of(t1, t2)))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(CircularDependencyException.class);
        }
    }

    // ── Parallel mode: invalid execution mode string defaults to sequential ─

    @Test
    void runTasks_invalidExecutionMode_fallsBackToSequential() {
        try (var ctx = new RunnerMockContext()) {
            TaskSpec t1 = TaskSpec.builder().id("TASK-1").title("T1").status("Done").build();

            when(ctx.specService.getAllSpecs()).thenReturn(List.of(t1));
            when(ctx.specService.getSpec("TASK-1")).thenReturn(t1);
            when(ctx.stateService.getSpecRunnerMode()).thenReturn("llm");
            when(ctx.stateService.getSpecExecutionMode()).thenReturn("INVALID_MODE");
            when(ctx.stateService.getSpecMaxConcurrency()).thenReturn(4);

            SpecTaskRunnerService service = ctx.createService();
            service.runTasks(List.of(t1));

            assertThat(service.getCompletedCount()).isEqualTo(1);
        }
    }

    // ── notifyPromptExecutionCompleted with taskId (parallel) ──────────

    @Test
    void notifyPromptExecutionCompleted_withTaskId_whenNotWaiting_doesNothing() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        service.notifyPromptExecutionCompleted("TASK-1");

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
    }

    // ── notifyCliTaskFailed with taskId (parallel) ─────────────────────

    @Test
    void notifyCliTaskFailed_withTaskId_whenNotWaiting_doesNothing() {
        Project project = mock(Project.class);
        SpecTaskRunnerService service = new SpecTaskRunnerService(project);

        service.notifyCliTaskFailed(1, "some error", "TASK-1");

        assertThat(service.getState()).isEqualTo(RunnerState.IDLE);
        assertThat(service.getSkippedCount()).isZero();
    }

    /**
     * Manages all MockedStatic instances and mock objects needed
     * to test SpecTaskRunnerService.
     */
    private static class RunnerMockContext implements AutoCloseable {
        final MockedStatic<DevoxxGenieStateService> stateServiceMock;
        final MockedStatic<ChatMemoryService> chatMemoryMock;
        final MockedStatic<FileListManager> fileListMock;
        final MockedStatic<ApplicationManager> appManagerMock;

        final DevoxxGenieStateService stateService;
        final SpecService specService;
        final BacklogConfigService configService;
        final CliTaskExecutorService cliTaskExecutorService;
        final ChatMemoryService chatMemoryService;
        final FileListManager fileListManager;
        final PromptSubmissionListener promptSubmissionListener;
        final Project project;

        RunnerMockContext() {
            // Static mocks
            stateServiceMock = Mockito.mockStatic(DevoxxGenieStateService.class);
            chatMemoryMock = Mockito.mockStatic(ChatMemoryService.class);
            fileListMock = Mockito.mockStatic(FileListManager.class);
            appManagerMock = Mockito.mockStatic(ApplicationManager.class);

            Application application = mock(Application.class);
            appManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
            // Parallel mode dispatches tasks via executeOnPooledThread with a blocking
            // layerLatch.await(). We make this a no-op because: (1) running inline
            // would deadlock the test thread, and (2) MockedStatic is thread-scoped
            // so a real background thread can't see the mocked ApplicationManager.

            stateService = mock(DevoxxGenieStateService.class);
            stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getSpecRunnerMode()).thenReturn("llm");

            chatMemoryService = mock(ChatMemoryService.class);
            chatMemoryMock.when(ChatMemoryService::getInstance).thenReturn(chatMemoryService);

            fileListManager = mock(FileListManager.class);
            fileListMock.when(FileListManager::getInstance).thenReturn(fileListManager);

            // Project with services
            project = mock(Project.class);
            when(project.getName()).thenReturn("TestProject");

            specService = mock(SpecService.class);
            when(project.getService(SpecService.class)).thenReturn(specService);

            configService = mock(BacklogConfigService.class);
            when(project.getService(BacklogConfigService.class)).thenReturn(configService);

            cliTaskExecutorService = mock(CliTaskExecutorService.class);
            when(project.getService(CliTaskExecutorService.class)).thenReturn(cliTaskExecutorService);

            // MessageBus for prompt submission
            MessageBus messageBus = mock(MessageBus.class);
            when(project.getMessageBus()).thenReturn(messageBus);

            promptSubmissionListener = mock(PromptSubmissionListener.class);
            when(messageBus.syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC))
                    .thenReturn(promptSubmissionListener);
        }

        SpecTaskRunnerService createService() {
            return new SpecTaskRunnerService(project);
        }

        @Override
        public void close() {
            fileListMock.close();
            chatMemoryMock.close();
            stateServiceMock.close();
            appManagerMock.close();
        }
    }
}
