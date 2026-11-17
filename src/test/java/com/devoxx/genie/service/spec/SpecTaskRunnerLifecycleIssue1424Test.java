package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.activity.ActivityLoggingMessage;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.listener.PromptSubmissionListener;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Reproduces the three "Run All To Do" lifecycle problems reported in issue #1424
 * and pins the agreed behaviour:
 * <ol>
 *   <li>every batch task starts with an empty conversation in the tab that runs it;</li>
 *   <li>interrupted In Progress tasks are included only when the caller opts in, and run first;</li>
 *   <li>a task that ends unfinished is left In Progress with an explanatory note, and the
 *       batch continues with the next task.</li>
 * </ol>
 */
class SpecTaskRunnerLifecycleIssue1424Test {

    private static final String PROJECT_HASH = "projhash";
    private static final String ACTIVE_TAB_ID = "tab-1";
    /** The key the active chat tab really uses for its memory (ChatMessageContext.getMemoryKey()). */
    private static final String TAB_MEMORY_KEY = PROJECT_HASH + "-" + ACTIVE_TAB_ID;

    private MockedStatic<DevoxxGenieStateService> stateServiceMock;
    private MockedStatic<ChatMemoryService> chatMemoryMock;
    private MockedStatic<FileListManager> fileListMock;
    private MockedStatic<ApplicationManager> appManagerMock;

    private Application application;
    private SpecService specService;
    private FileListManager fileListManager;
    private PromptSubmissionListener promptSubmissionListener;
    private ChatMemoryService realMemory;
    private Project project;
    private final ArgumentCaptor<ActivityLoggingMessage> activityHandler =
            ArgumentCaptor.forClass(ActivityLoggingMessage.class);
    private MessageBusConnection appBusConnection;

    /** Task ids in the order the runner submitted them to the chat. */
    private final List<String> submittedTaskIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        stateServiceMock = Mockito.mockStatic(DevoxxGenieStateService.class);
        chatMemoryMock = Mockito.mockStatic(ChatMemoryService.class);
        fileListMock = Mockito.mockStatic(FileListManager.class);
        appManagerMock = Mockito.mockStatic(ApplicationManager.class);

        application = mock(Application.class);
        appManagerMock.when(ApplicationManager::getApplication).thenReturn(application);
        // Run invokeLater / pooled work inline so the test observes it synchronously.
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(application).invokeLater(any(Runnable.class));
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(application).executeOnPooledThread(any(Runnable.class));

        MessageBus appBus = mock(MessageBus.class);
        appBusConnection = mock(MessageBusConnection.class);
        when(application.getMessageBus()).thenReturn(appBus);
        when(appBus.connect()).thenReturn(appBusConnection);

        ConversationTabRegistry tabRegistry = mock(ConversationTabRegistry.class);
        when(application.getService(ConversationTabRegistry.class)).thenReturn(tabRegistry);

        DevoxxGenieStateService stateService = mock(DevoxxGenieStateService.class);
        stateServiceMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
        when(stateService.getSpecRunnerMode()).thenReturn("llm");

        // Real chat memory, so we observe what is actually left in the tab's conversation.
        realMemory = new ChatMemoryService();
        chatMemoryMock.when(ChatMemoryService::getInstance).thenReturn(realMemory);
        fileListManager = mock(FileListManager.class);
        fileListMock.when(FileListManager::getInstance).thenReturn(fileListManager);

        project = mock(Project.class);
        when(project.getName()).thenReturn("TestProject");
        when(project.getLocationHash()).thenReturn(PROJECT_HASH);
        when(tabRegistry.getActiveTabId(project)).thenReturn(ACTIVE_TAB_ID);

        specService = mock(SpecService.class);
        when(project.getService(SpecService.class)).thenReturn(specService);
        when(project.getService(BacklogConfigService.class)).thenReturn(mock(BacklogConfigService.class));

        MessageBus messageBus = mock(MessageBus.class);
        when(project.getMessageBus()).thenReturn(messageBus);
        promptSubmissionListener = mock(PromptSubmissionListener.class);
        when(messageBus.syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)).thenReturn(promptSubmissionListener);
    }

    @AfterEach
    void tearDown() {
        appManagerMock.close();
        fileListMock.close();
        chatMemoryMock.close();
        stateServiceMock.close();
    }

    // ── Issue 1: chat memory is not cleared between batch tasks ─────────────

    @Test
    void issue1_eachBatchTaskStartsWithEmptyMemoryInTheTabThatRunsIt() {
        TaskSpec t1 = task("TASK-1", "To Do");
        TaskSpec t2 = task("TASK-2", "To Do");
        AtomicReference<TaskSpec> t1State = new AtomicReference<>(t1);
        when(specService.getAllSpecs()).thenReturn(List.of(t1, t2));
        when(specService.getSpec("TASK-1")).thenAnswer(inv -> t1State.get());
        when(specService.getSpec("TASK-2")).thenReturn(t2);

        // The active tab already has a conversation (the user chatted before starting the batch).
        realMemory.initializeByKey(TAB_MEMORY_KEY, 50);
        realMemory.addMessageByKey(TAB_MEMORY_KEY, UserMessage.from("earlier chat question"));
        realMemory.addMessageByKey(TAB_MEMORY_KEY, AiMessage.from("earlier chat answer"));

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);

        // Simulate an idle tab executing each submitted prompt the way ActionButtonsPanel
        // does: tell the runner the prompt is starting, record how much history the
        // prompt starts with, then append the task's own turn to the tab's memory.
        List<Integer> memorySizeAtSubmit = new ArrayList<>();
        doAnswer(inv -> {
            String prompt = inv.getArgument(1);
            assertThat(runner.onTaskPromptStarting(prompt, ACTIVE_TAB_ID)).isTrue();
            memorySizeAtSubmit.add(realMemory.getMessagesByKey(TAB_MEMORY_KEY).size());
            realMemory.addMessageByKey(TAB_MEMORY_KEY, UserMessage.from(prompt));
            realMemory.addMessageByKey(TAB_MEMORY_KEY, AiMessage.from("worked on task"));
            return null;
        }).when(promptSubmissionListener).onPromptSubmitted(eq(project), anyString(), any());

        ArgumentCaptor<Runnable> specListener = ArgumentCaptor.forClass(Runnable.class);

        runner.runTasks(List.of(t1, t2));
        verify(specService).addChangeListener(specListener.capture());

        // Agent marks TASK-1 Done while executing, then the prompt completes.
        t1State.set(task("TASK-1", "Done"));
        specListener.getValue().run();
        runner.notifyPromptExecutionCompleted();

        assertThat(memorySizeAtSubmit)
                .as("history in tab memory (%s) when TASK-1 and TASK-2 were submitted", TAB_MEMORY_KEY)
                .containsExactly(0, 0);
        // The prompt must go to the tab whose memory was cleared, and that tab's files are reset.
        verify(promptSubmissionListener, times(2)).onPromptSubmitted(eq(project), anyString(), eq(ACTIVE_TAB_ID));
        verify(fileListManager, times(2)).clear(project, ACTIVE_TAB_ID);
    }

    @Test
    void issue1_busyTab_keepsLiveMemoryUntilQueuedTaskPromptActuallyStarts() {
        TaskSpec t1 = task("TASK-1", "To Do");
        when(specService.getAllSpecs()).thenReturn(List.of(t1));
        when(specService.getSpec("TASK-1")).thenReturn(t1);
        List<String> published = new ArrayList<>();
        // The tab is busy: it queues the prompt instead of running it (ActionButtonsPanel
        // keeps it in pendingSpecPrompt until the running execution ends).
        doAnswer(inv -> published.add(inv.getArgument(1)))
                .when(promptSubmissionListener).onPromptSubmitted(eq(project), anyString(), any());

        // A conversation is mid tool-loop in that tab.
        realMemory.initializeByKey(TAB_MEMORY_KEY, 50);
        realMemory.addMessageByKey(TAB_MEMORY_KEY, UserMessage.from("running question"));
        realMemory.addMessageByKey(TAB_MEMORY_KEY, AiMessage.from("tool call in flight"));

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        runner.runTasks(List.of(t1));

        assertThat(published).hasSize(1);
        assertThat(realMemory.getMessagesByKey(TAB_MEMORY_KEY))
                .as("the running conversation keeps its context while the task prompt is queued")
                .hasSize(2);
        verify(fileListManager, never()).clear(eq(project), anyString());

        // The running execution finishes (its late reply lands), then the queued prompt starts.
        realMemory.addMessageByKey(TAB_MEMORY_KEY, AiMessage.from("late final answer"));
        assertThat(runner.onTaskPromptStarting("an ordinary user prompt", ACTIVE_TAB_ID))
                .as("prompts that are not the runner's do not reset the conversation").isFalse();
        assertThat(realMemory.getMessagesByKey(TAB_MEMORY_KEY)).hasSize(3);

        assertThat(runner.onTaskPromptStarting(published.get(0), ACTIVE_TAB_ID)).isTrue();
        assertThat(realMemory.getMessagesByKey(TAB_MEMORY_KEY))
                .as("fresh conversation once the task prompt really starts").isEmpty();
        verify(fileListManager).clear(project, ACTIVE_TAB_ID);
        assertThat(runner.onTaskPromptStarting(published.get(0), ACTIVE_TAB_ID))
                .as("a task prompt resets the conversation only once").isFalse();
    }

    @Test
    void issue1_pendingTaskPromptIsForgottenWhenRunEnds() {
        TaskSpec t1 = task("TASK-1", "To Do");
        when(specService.getAllSpecs()).thenReturn(List.of(t1));
        when(specService.getSpec("TASK-1")).thenReturn(t1);
        List<String> published = new ArrayList<>();
        doAnswer(inv -> published.add(inv.getArgument(1)))
                .when(promptSubmissionListener).onPromptSubmitted(eq(project), anyString(), any());

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        runner.runTasks(List.of(t1));
        runner.cancel();

        realMemory.initializeByKey(TAB_MEMORY_KEY, 50);
        realMemory.addMessageByKey(TAB_MEMORY_KEY, UserMessage.from("user chat after cancel"));
        assertThat(runner.onTaskPromptStarting(published.get(0), ACTIVE_TAB_ID)).isFalse();
        assertThat(realMemory.getMessagesByKey(TAB_MEMORY_KEY)).hasSize(1);
    }

    // ── Issue 2: In Progress tasks are ignored by "Run All To Do" ──────────

    @Test
    void issue2_runAllTodo_withInProgressOptIn_runsInterruptedTaskFirst() {
        TaskSpec todo = task("TASK-1", "To Do");
        TaskSpec interrupted = task("TASK-2", "In Progress");
        stubRunAll(todo, interrupted);

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        runner.runAllTodoTasks(true);

        assertThat(runner.getTotalTasks())
                .as("tasks in the batch (interrupted In Progress task included)")
                .isEqualTo(2);
        assertThat(submittedTaskIds)
                .as("the interrupted task is resumed before To Do tasks")
                .containsExactly("TASK-2");
    }

    @Test
    void issue2_runAllTodo_withoutOptIn_leavesInProgressTasksAlone() {
        TaskSpec todo = task("TASK-1", "To Do");
        TaskSpec interrupted = task("TASK-2", "In Progress");
        stubRunAll(todo, interrupted);

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        runner.runAllTodoTasks(false);

        assertThat(runner.getTotalTasks()).isEqualTo(1);
        assertThat(submittedTaskIds).containsExactly("TASK-1");
    }

    // ── Issue 3: hitting max agent actions ─────────────────────────────────

    @Test
    void issue3_maxActionsReached_marksTaskInProgressWithNote_andContinues() throws Exception {
        TaskSpec t1 = task("TASK-1", "To Do");
        TaskSpec t2 = task("TASK-2", "To Do");
        when(specService.getAllSpecs()).thenReturn(List.of(t1, t2));
        when(specService.getSpec("TASK-1")).thenReturn(t1);
        when(specService.getSpec("TASK-2")).thenReturn(t2);
        recordSubmittedTaskIds();

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        SpecTaskRunnerListener listener = mock(SpecTaskRunnerListener.class);
        runner.addListener(listener);
        runner.runTasks(List.of(t1, t2));
        assertThat(submittedTaskIds).containsExactly("TASK-1");

        // AgentLoopTracker hits the limit for the main agent of this project, then the
        // LLM wraps up and prompt execution ends with the task still unfinished.
        verify(appBusConnection).subscribe(eq(AppTopics.ACTIVITY_LOG_MSG), activityHandler.capture());
        activityHandler.getValue().onActivityMessage(loopLimit(PROJECT_HASH, null, 25));
        runner.notifyPromptExecutionCompleted();
        fireGraceTimer(runner);

        ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);
        verify(specService).setStatusAndAppendNote(eq("TASK-1"), eq("In Progress"), note.capture());
        assertThat(note.getValue()).contains("agent action limit reached (25)");
        // A full rewrite would drop fields TaskSpec does not model (type, modified_files, …).
        verify(specService, never()).updateTask(any());
        verify(listener).onTaskSkipped(any(TaskSpec.class), anyInt(), eq(2),
                contains("agent action limit reached (25)"));

        assertThat(submittedTaskIds)
                .as("batch continues with the next task")
                .containsExactly("TASK-1", "TASK-2");
    }

    @Test
    void issue3_limitEventsFromSubAgentsOrOtherProjects_areIgnored() throws Exception {
        TaskSpec t1 = task("TASK-1", "To Do");
        when(specService.getAllSpecs()).thenReturn(List.of(t1));
        when(specService.getSpec("TASK-1")).thenReturn(t1);

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        runner.runTasks(List.of(t1));

        verify(appBusConnection).subscribe(eq(AppTopics.ACTIVITY_LOG_MSG), activityHandler.capture());
        activityHandler.getValue().onActivityMessage(loopLimit("other-project", null, 25));
        activityHandler.getValue().onActivityMessage(loopLimit(PROJECT_HASH, "sub-agent-1", 10));
        runner.notifyPromptExecutionCompleted();
        fireGraceTimer(runner);

        ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);
        verify(specService).setStatusAndAppendNote(eq("TASK-1"), eq("In Progress"), note.capture());
        assertThat(note.getValue())
                .doesNotContain("action limit")
                .contains("not marked Done");
    }

    @Test
    void issue3_previousTaskLimit_doesNotLeakIntoNextTask() throws Exception {
        TaskSpec t1 = task("TASK-1", "To Do");
        TaskSpec t2 = task("TASK-2", "To Do");
        when(specService.getAllSpecs()).thenReturn(List.of(t1, t2));
        when(specService.getSpec("TASK-1")).thenReturn(t1);
        when(specService.getSpec("TASK-2")).thenReturn(t2);

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        runner.runTasks(List.of(t1, t2));
        verify(appBusConnection).subscribe(eq(AppTopics.ACTIVITY_LOG_MSG), activityHandler.capture());

        activityHandler.getValue().onActivityMessage(loopLimit(PROJECT_HASH, null, 25));
        runner.notifyPromptExecutionCompleted();
        fireGraceTimer(runner);     // TASK-1 unfinished (limit) → TASK-2 starts
        runner.notifyPromptExecutionCompleted();
        fireGraceTimer(runner);     // TASK-2 unfinished, no limit hit

        ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);
        verify(specService).setStatusAndAppendNote(eq("TASK-2"), eq("In Progress"), note.capture());
        assertThat(note.getValue()).doesNotContain("action limit");
        verify(appBusConnection).disconnect();
    }

    // ── Hang: task marked Done during the grace period after the prompt ended ──

    @Test
    void taskMarkedDoneDuringGracePeriod_advancesToNextTask() {
        TaskSpec t1 = task("TASK-1", "To Do");
        TaskSpec t2 = task("TASK-2", "To Do");
        AtomicReference<TaskSpec> t1State = new AtomicReference<>(t1);
        when(specService.getAllSpecs()).thenReturn(List.of(t1, t2));
        when(specService.getSpec("TASK-1")).thenAnswer(inv -> t1State.get());
        when(specService.getSpec("TASK-2")).thenReturn(t2);
        recordSubmittedTaskIds();

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        ArgumentCaptor<Runnable> specListener = ArgumentCaptor.forClass(Runnable.class);
        runner.runTasks(List.of(t1, t2));
        verify(specService).addChangeListener(specListener.capture());

        // Prompt finishes first (grace timer starts), then the "Done" file change lands.
        runner.notifyPromptExecutionCompleted();
        t1State.set(task("TASK-1", "Done"));
        specListener.getValue().run();

        assertThat(runner.getCompletedCount()).isEqualTo(1);
        assertThat(submittedTaskIds)
                .as("runner advances once the late Done arrives")
                .containsExactly("TASK-1", "TASK-2");
    }

    @Test
    void parallel_taskMarkedDoneDuringGracePeriod_countsDownLayer() throws Exception {
        TaskSpec t1 = task("TASK-1", "To Do");
        AtomicReference<TaskSpec> t1State = new AtomicReference<>(t1);
        when(specService.getAllSpecs()).thenReturn(List.of(t1));
        when(specService.getSpec("TASK-1")).thenAnswer(inv -> t1State.get());

        SpecTaskRunnerService runner = new SpecTaskRunnerService(project);
        ArgumentCaptor<Runnable> specListener = ArgumentCaptor.forClass(Runnable.class);
        // Keep the layer-wait from running inline (it would block on the latch).
        doReturn(null).when(application).executeOnPooledThread(any(Runnable.class));
        runner.runTasks(List.of(t1), com.devoxx.genie.model.enumarations.ExecutionMode.PARALLEL);
        verify(specService).addChangeListener(specListener.capture());

        runner.notifyPromptExecutionCompleted("TASK-1");
        t1State.set(task("TASK-1", "Done"));
        specListener.getValue().run();

        Field f = SpecTaskRunnerService.class.getDeclaredField("layerLatch");
        f.setAccessible(true);
        assertThat(((java.util.concurrent.CountDownLatch) f.get(runner)).getCount()).isZero();
        assertThat(runner.getCompletedCount()).isEqualTo(1);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static TaskSpec task(String id, String status) {
        return TaskSpec.builder().id(id).title(id + " title").status(status)
                .filePath("/backlog/tasks/" + id + ".md").build();
    }

    private static ActivityMessage loopLimit(String projectHash, String subAgentId, int maxCalls) {
        return ActivityMessage.builder()
                .agentType(AgentType.LOOP_LIMIT)
                .projectLocationHash(projectHash)
                .subAgentId(subAgentId)
                .maxCalls(maxCalls)
                .callNumber(maxCalls + 1)
                .build();
    }

    private void stubRunAll(TaskSpec todo, TaskSpec inProgress) {
        when(specService.getAllSpecs()).thenReturn(List.of(todo, inProgress));
        when(specService.getSpecsByStatus("To Do")).thenReturn(List.of(todo));
        when(specService.getSpecsByStatus("In Progress")).thenReturn(List.of(inProgress));
        when(specService.getSpec(todo.getId())).thenReturn(todo);
        when(specService.getSpec(inProgress.getId())).thenReturn(inProgress);
        recordSubmittedTaskIds();
    }

    private void recordSubmittedTaskIds() {
        doAnswer(inv -> {
            String prompt = inv.getArgument(1);
            submittedTaskIds.add(prompt.contains("TASK-1") ? "TASK-1"
                    : prompt.contains("TASK-2") ? "TASK-2" : "?");
            return null;
        }).when(promptSubmissionListener).onPromptSubmitted(eq(project), anyString(), any());
    }

    /** Fires the 3s Swing grace timer deterministically on the test thread. */
    private static void fireGraceTimer(SpecTaskRunnerService runner) throws Exception {
        Field f = SpecTaskRunnerService.class.getDeclaredField("graceTimer");
        f.setAccessible(true);
        Timer timer = (Timer) f.get(runner);
        assertThat(timer).as("grace timer should be pending").isNotNull();
        timer.stop();
        for (ActionListener l : timer.getActionListeners()) {
            l.actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, null));
        }
    }
}
