package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.enumarations.ExecutionMode;
import com.devoxx.genie.model.spec.CliToolConfig;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.cli.CliConsoleManager;
import com.devoxx.genie.service.cli.CliTaskExecutorService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Project-scoped service that executes spec tasks in dependency order.
 * Supports both sequential execution (one task at a time) and parallel
 * execution (independent tasks within a dependency layer run concurrently).
 * The execution mode is controlled by the specExecutionMode setting.
 *
 * <p>In parallel mode, tasks are grouped into topological layers using
 * {@link TaskDependencySorter#sortByLayers}. All tasks in a layer execute
 * concurrently, and the runner proceeds to the next layer only after all
 * tasks in the current layer complete.</p>
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SpecTaskRunnerService implements Disposable {

    public enum RunnerState {
        IDLE,
        RUNNING_TASK,
        WAITING_FOR_COMPLETION,
        ALL_COMPLETED,
        CANCELLED,
        ERROR
    }

    private final Project project;
    private final List<SpecTaskRunnerListener> listeners = new CopyOnWriteArrayList<>();

    private volatile RunnerState state = RunnerState.IDLE;
    private List<TaskSpec> orderedTasks = Collections.emptyList();
    private int currentTaskIndex = -1;
    @Getter
    private int completedCount;
    @Getter
    private int skippedCount;
    private Runnable specChangeListener;

    // Grace timer: after prompt execution completes, wait briefly for the spec
    // file to be updated to "Done" before skipping the task and advancing.
    private Timer graceTimer;

    // Set by onSpecsChanged() when the current task is detected as Done while
    // the prompt is still executing.  notifyPromptExecutionCompleted() checks
    // this flag so it can advance immediately instead of starting a grace timer.
    private volatile boolean currentTaskDoneWhileExecuting = false;

    // Track which tasks in this run have completed (for dependency checking)
    private final Set<String> completedTaskIds = Collections.synchronizedSet(new HashSet<>());
    private Set<String> selectedTaskIds = Collections.emptySet();

    // Execution mode flag, determined at start of runTasks()
    @Getter
    private boolean cliMode = false;

    // --- Parallel execution state ---
    @Getter
    private ExecutionMode executionMode = ExecutionMode.SEQUENTIAL;
    private int maxConcurrency = 4;
    private List<List<TaskSpec>> layers = Collections.emptyList();
    private int currentLayerIndex = -1;

    /** Per-task grace timers for parallel mode (task ID -> Timer). */
    private final ConcurrentHashMap<String, Timer> parallelGraceTimers = new ConcurrentHashMap<>();

    /** Per-task "done while executing" flags for parallel mode. */
    private final ConcurrentHashMap<String, Boolean> parallelTaskDoneFlags = new ConcurrentHashMap<>();

    /** Latch for the current layer — counts down as each task completes/skips. */
    private volatile CountDownLatch layerLatch;

    /** Tasks currently executing in the active layer. */
    private final Set<String> activeLayerTaskIds = ConcurrentHashMap.newKeySet();

    /** Track per-task results within a layer for deterministic reporting. */
    private final ConcurrentHashMap<String, LayerTaskResult> layerTaskResults = new ConcurrentHashMap<>();

    /** Result of a single task within a parallel layer. */
    static final class LayerTaskResult {
        final TaskSpec task;
        final boolean completed;
        final boolean skipped;
        final String skipReason;
        final long elapsedMs;

        LayerTaskResult(TaskSpec task, boolean completed, boolean skipped, String skipReason, long elapsedMs) {
            this.task = task;
            this.completed = completed;
            this.skipped = skipped;
            this.skipReason = skipReason;
            this.elapsedMs = elapsedMs;
        }
    }

    public SpecTaskRunnerService(@NotNull Project project) {
        this.project = project;
    }

    public static SpecTaskRunnerService getInstance(@NotNull Project project) {
        return project.getService(SpecTaskRunnerService.class);
    }

    // ===== Public API =====

    /**
     * Sort and execute the given tasks, using the configured execution mode.
     */
    public void runTasks(@NotNull List<TaskSpec> tasks) {
        runTasks(tasks, null);
    }

    /**
     * Sort and execute the given tasks, optionally overriding the execution mode.
     *
     * @param tasks         tasks to execute
     * @param modeOverride  if non-null, overrides the configured execution mode
     */
    public void runTasks(@NotNull List<TaskSpec> tasks, @Nullable ExecutionMode modeOverride) {
        if (state == RunnerState.RUNNING_TASK || state == RunnerState.WAITING_FOR_COMPLETION) {
            log.warn("Runner is already active, ignoring runTasks call");
            return;
        }

        if (tasks.isEmpty()) {
            log.info("No tasks to run");
            return;
        }

        // Ensure backlog directory structure exists before running
        BacklogConfigService.getInstance(project).ensureInitialized();

        SpecService specService = SpecService.getInstance(project);
        List<TaskSpec> allSpecs = specService.getAllSpecs();

        // Determine execution mode (override takes precedence over settings)
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        if (modeOverride != null) {
            executionMode = modeOverride;
        } else {
            String modeSetting = stateService.getSpecExecutionMode();
            executionMode = parseExecutionMode(modeSetting);
        }
        maxConcurrency = stateService.getSpecMaxConcurrency() != null
                ? Math.max(1, Math.min(8, stateService.getSpecMaxConcurrency()))
                : 4;

        // Determine CLI mode
        String runnerMode = stateService.getSpecRunnerMode();
        cliMode = "cli".equalsIgnoreCase(runnerMode);

        if (executionMode == ExecutionMode.PARALLEL) {
            try {
                layers = TaskDependencySorter.sortByLayers(tasks, allSpecs);
                orderedTasks = layers.stream().flatMap(List::stream).collect(Collectors.toList());
            } catch (CircularDependencyException e) {
                log.error("Circular dependency detected: {}", e.getMessage());
                state = RunnerState.ERROR;
                notifyRunFinished(RunnerState.ERROR);
                throw new RuntimeException(e.getMessage(), e);
            }

            log.info("Parallel execution: {} layers, maxConcurrency={}", layers.size(), maxConcurrency);
            for (int i = 0; i < layers.size(); i++) {
                log.info("  Layer {}: {}", i,
                        layers.get(i).stream()
                                .map(t -> t.getId() != null ? t.getId() : "?")
                                .collect(Collectors.joining(", ")));
            }
        } else {
            try {
                orderedTasks = TaskDependencySorter.sort(tasks, allSpecs);
            } catch (CircularDependencyException e) {
                log.error("Circular dependency detected: {}", e.getMessage());
                state = RunnerState.ERROR;
                notifyRunFinished(RunnerState.ERROR);
                throw new RuntimeException(e.getMessage(), e);
            }

            log.info("Sequential execution order: {}",
                    orderedTasks.stream()
                            .map(t -> t.getId() != null ? t.getId() : "?")
                            .collect(Collectors.joining(" -> ")));
        }

        currentTaskIndex = -1;
        currentLayerIndex = -1;
        completedCount = 0;
        skippedCount = 0;
        completedTaskIds.clear();
        selectedTaskIds = tasks.stream()
                .map(TaskSpec::getId)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Register spec change listener
        specChangeListener = this::onSpecsChanged;
        specService.addChangeListener(specChangeListener);

        state = RunnerState.RUNNING_TASK;
        notifyRunStarted();

        if (executionMode == ExecutionMode.PARALLEL) {
            executeNextLayer();
        } else {
            submitNextTask();
        }
    }

    /**
     * Convenience: run all To Do tasks.
     */
    public void runAllTodoTasks() {
        List<TaskSpec> todoTasks = SpecService.getInstance(project).getSpecsByStatus("To Do");
        runTasks(todoTasks);
    }

    /**
     * Convenience: run all To Do tasks in parallel mode.
     */
    public void runAllTodoTasksParallel() {
        List<TaskSpec> todoTasks = SpecService.getInstance(project).getSpecsByStatus("To Do");
        runTasks(todoTasks, ExecutionMode.PARALLEL);
    }

    /**
     * Cancel the run after the current task finishes.
     */
    public void cancel() {
        if (state != RunnerState.RUNNING_TASK && state != RunnerState.WAITING_FOR_COMPLETION) {
            return;
        }
        log.info("Cancelling task runner");
        if (cliMode) {
            CliTaskExecutorService.getInstance(project).cancelAllProcesses();
        }
        state = RunnerState.CANCELLED;

        // Release any pending layer latch so the parallel executor thread can proceed
        CountDownLatch latch = layerLatch;
        if (latch != null) {
            while (latch.getCount() > 0) {
                latch.countDown();
            }
        }

        // Cancel all parallel grace timers
        cancelAllParallelGraceTimers();

        finish(RunnerState.CANCELLED);
    }

    /**
     * Called when a CLI tool exits with a non-zero exit code (legacy single-task API).
     */
    public void notifyCliTaskFailed(int exitCode, @NotNull String errorOutput) {
        notifyCliTaskFailed(exitCode, errorOutput, null);
    }

    /**
     * Called when a CLI tool exits with a non-zero exit code.
     * In parallel mode, only the specific task fails; the layer continues.
     */
    public void notifyCliTaskFailed(int exitCode, @NotNull String errorOutput, @Nullable String taskId) {
        if (state != RunnerState.WAITING_FOR_COMPLETION) {
            return;
        }

        if (executionMode == ExecutionMode.PARALLEL && taskId != null) {
            handleParallelTaskFailure(taskId, exitCode, errorOutput);
            return;
        }

        // Sequential mode
        TaskSpec current = getCurrentTask();
        if (current == null) {
            return;
        }

        cancelGraceTimer();

        String firstLine = errorOutput.lines()
                .filter(l -> !l.isBlank())
                .findFirst()
                .orElse("Unknown error");
        String reason = "CLI tool failed (exit code " + exitCode + "): " + firstLine;

        log.error("CLI task {} failed: {}", current.getId(), reason);
        skippedCount++;
        notifyTaskSkipped(current, reason);

        log.info("Stopping task runner due to CLI failure");
        finish(RunnerState.ERROR);
    }

    /**
     * Called when prompt execution completes (legacy single-task API).
     */
    public void notifyPromptExecutionCompleted() {
        notifyPromptExecutionCompleted(null);
    }

    /**
     * Called when prompt execution completes (success or failure).
     * In parallel mode, the taskId identifies which task completed.
     */
    public void notifyPromptExecutionCompleted(@Nullable String taskId) {
        if (state != RunnerState.WAITING_FOR_COMPLETION) {
            return;
        }

        if (executionMode == ExecutionMode.PARALLEL && taskId != null) {
            handleParallelTaskCompletion(taskId);
            return;
        }

        // Sequential mode
        TaskSpec current = getCurrentTask();
        if (current == null) {
            return;
        }

        if (currentTaskDoneWhileExecuting) {
            currentTaskDoneWhileExecuting = false;
            log.info("Prompt execution completed for task {} (already marked Done), advancing", current.getId());
            cancelGraceTimer();
            markTaskCompleted(current);
            advanceToNextTask();
            return;
        }

        log.info("Prompt execution completed for task {}, starting grace timer", current.getId());
        cancelGraceTimer();

        graceTimer = new Timer(3000, e -> {
            TaskSpec fresh = SpecService.getInstance(project).getSpec(current.getId());
            if (fresh != null && "Done".equalsIgnoreCase(fresh.getStatus())) {
                return;
            }
            if (state != RunnerState.WAITING_FOR_COMPLETION) {
                return;
            }
            log.warn("Task {} not marked Done after grace period, skipping", current.getId());
            skipTask(current, "Prompt execution completed but task was not marked Done");
        });
        graceTimer.setRepeats(false);
        graceTimer.start();
    }

    private void markTaskCompleted(@NotNull TaskSpec task) {
        completedCount++;
        if (task.getId() != null) {
            completedTaskIds.add(task.getId().toLowerCase());
        }
        notifyTaskCompleted(task);
    }

    private void cancelGraceTimer() {
        if (graceTimer != null) {
            graceTimer.stop();
            graceTimer = null;
        }
    }

    public boolean isRunning() {
        return state == RunnerState.RUNNING_TASK || state == RunnerState.WAITING_FOR_COMPLETION;
    }

    public @NotNull RunnerState getState() {
        return state;
    }

    public @Nullable TaskSpec getCurrentTask() {
        if (currentTaskIndex >= 0 && currentTaskIndex < orderedTasks.size()) {
            return orderedTasks.get(currentTaskIndex);
        }
        return null;
    }

    public int getTotalTasks() {
        return orderedTasks.size();
    }

    public int getCurrentTaskIndex() {
        return currentTaskIndex;
    }

    public void addListener(@NotNull SpecTaskRunnerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull SpecTaskRunnerListener listener) {
        listeners.remove(listener);
    }

    // ===== Sequential Execution =====

    private void submitNextTask() {
        currentTaskIndex++;

        if (currentTaskIndex >= orderedTasks.size()) {
            finish(RunnerState.ALL_COMPLETED);
            return;
        }

        if (state == RunnerState.CANCELLED) {
            finish(RunnerState.CANCELLED);
            return;
        }

        TaskSpec task = orderedTasks.get(currentTaskIndex);
        log.info("Processing task {}/{}: {}", currentTaskIndex + 1, orderedTasks.size(), task.getId());

        // Re-read task from SpecService to get fresh status
        TaskSpec fresh = SpecService.getInstance(project).getSpec(task.getId());
        if (fresh == null) {
            skipTask(task, "Task file not found");
            return;
        }

        // If already done, count as completed and skip
        if ("Done".equalsIgnoreCase(fresh.getStatus())) {
            completedCount++;
            if (fresh.getId() != null) {
                completedTaskIds.add(fresh.getId().toLowerCase());
            }
            notifyTaskCompleted(fresh);
            advanceToNextTask();
            return;
        }

        // Check unsatisfied dependencies
        List<TaskSpec> allSpecs = SpecService.getInstance(project).getAllSpecs();
        List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                fresh, completedTaskIds, selectedTaskIds, allSpecs);
        if (!unsatisfied.isEmpty()) {
            skipTask(fresh, "Unsatisfied dependencies: " + String.join(", ", unsatisfied));
            return;
        }

        state = RunnerState.WAITING_FOR_COMPLETION;
        notifyTaskStarted(fresh);

        if (cliMode) {
            submitTaskViaCli(fresh);
        } else {
            submitTaskViaLlm(fresh);
        }
    }

    private void submitTaskViaLlm(@NotNull TaskSpec task) {
        startFreshConversation();

        String taskDetails = SpecContextBuilder.buildContext(task);
        String instruction = SpecContextBuilder.buildAgentInstruction(task);
        String prompt = instruction + "\n\n" + taskDetails +
                "\n\nPlease implement the task described above, satisfying all acceptance criteria.";

        project.getMessageBus()
                .syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)
                .onPromptSubmitted(project, prompt);
    }

    private void submitTaskViaCli(@NotNull TaskSpec task) {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        String toolName = stateService.getSpecSelectedCliTool();
        log.info("CLI mode: specRunnerMode={}, specSelectedCliTool='{}'",
                stateService.getSpecRunnerMode(), toolName);

        CliToolConfig cliTool = findCliTool(toolName);
        if (cliTool == null) {
            log.error("CLI tool '{}' not found or not enabled. Available tools: {}", toolName,
                    stateService.getCliTools());
            skipTask(task, "CLI tool '" + toolName + "' not found or not enabled");
            return;
        }

        log.info("CLI tool resolved: name={}, type={}, path={}, extraArgs={}, enabled={}",
                cliTool.getName(), cliTool.getType(),
                cliTool.getExecutablePath(), cliTool.getExtraArgs(), cliTool.isEnabled());

        String taskId = task.getId() != null ? task.getId() : "?";
        String instruction = SpecContextBuilder.buildCliInstruction(task);
        String prompt = instruction +
                "\n\nStart by using backlog task_view with id '" + taskId +
                "' to read the full task details and acceptance criteria." +
                "\n\nPlease implement the task, satisfying all acceptance criteria.";

        log.info("CLI prompt for task {}: {} chars", taskId, prompt.length());

        CliTaskExecutorService.getInstance(project).execute(
                cliTool, prompt,
                task.getId() != null ? task.getId() : "?",
                task.getTitle() != null ? task.getTitle() : "Untitled"
        );
    }

    private @Nullable CliToolConfig findCliTool(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        List<CliToolConfig> tools = stateService.getCliTools();
        if (tools == null) return null;
        return tools.stream()
                .filter(t -> t.isEnabled() && name.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElse(null);
    }

    private void onSpecsChanged() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (state != RunnerState.WAITING_FOR_COMPLETION) {
                return;
            }

            if (executionMode == ExecutionMode.PARALLEL) {
                onSpecsChangedParallel();
                return;
            }

            // Sequential mode
            TaskSpec current = getCurrentTask();
            if (current == null || current.getId() == null) {
                return;
            }

            TaskSpec fresh = SpecService.getInstance(project).getSpec(current.getId());
            if (fresh == null) {
                log.debug("Task {} not found in spec cache, waiting for next change event", current.getId());
                return;
            }

            if ("Done".equalsIgnoreCase(fresh.getStatus())) {
                log.info("Task {} detected as Done (deferring advance until prompt completes)", fresh.getId());
                cancelGraceTimer();
                currentTaskDoneWhileExecuting = true;

                if (cliMode) {
                    CliTaskExecutorService.getInstance(project).notifyTaskDone();
                }
            }
        });
    }

    private void skipTask(@NotNull TaskSpec task, @NotNull String reason) {
        skippedCount++;
        log.info("Skipping task {}: {}", task.getId(), reason);
        notifyTaskSkipped(task, reason);
        advanceToNextTask();
    }

    private void advanceToNextTask() {
        if (state == RunnerState.CANCELLED) {
            finish(RunnerState.CANCELLED);
            return;
        }
        state = RunnerState.RUNNING_TASK;
        submitNextTask();
    }

    // ===== Parallel Layer Execution =====

    /**
     * Execute the next layer of independent tasks in parallel.
     * Called from the EDT after the previous layer completes.
     */
    private void executeNextLayer() {
        currentLayerIndex++;

        if (currentLayerIndex >= layers.size()) {
            finish(RunnerState.ALL_COMPLETED);
            return;
        }

        if (state == RunnerState.CANCELLED) {
            finish(RunnerState.CANCELLED);
            return;
        }

        List<TaskSpec> layer = layers.get(currentLayerIndex);
        log.info("=== Starting layer {}/{} ({} tasks) ===",
                currentLayerIndex + 1, layers.size(), layer.size());

        // Pre-filter: check for already-done or missing tasks
        List<TaskSpec> tasksToExecute = new ArrayList<>();
        for (TaskSpec task : layer) {
            TaskSpec fresh = SpecService.getInstance(project).getSpec(task.getId());
            if (fresh == null) {
                skippedCount++;
                notifyTaskSkipped(task, "Task file not found");
                continue;
            }
            if ("Done".equalsIgnoreCase(fresh.getStatus())) {
                completedCount++;
                if (fresh.getId() != null) {
                    completedTaskIds.add(fresh.getId().toLowerCase());
                }
                notifyTaskCompleted(fresh);
                continue;
            }
            // Check unsatisfied dependencies
            List<TaskSpec> allSpecs = SpecService.getInstance(project).getAllSpecs();
            List<String> unsatisfied = TaskDependencySorter.getUnsatisfiedDependencies(
                    fresh, completedTaskIds, selectedTaskIds, allSpecs);
            if (!unsatisfied.isEmpty()) {
                skippedCount++;
                notifyTaskSkipped(fresh, "Unsatisfied dependencies: " + String.join(", ", unsatisfied));
                continue;
            }
            tasksToExecute.add(fresh);
        }

        if (tasksToExecute.isEmpty()) {
            log.info("Layer {}: all tasks already done or skipped, proceeding to next layer",
                    currentLayerIndex + 1);
            executeNextLayer();
            return;
        }

        log.info("Layer {}: executing {} tasks (concurrency capped at {})",
                currentLayerIndex + 1, tasksToExecute.size(), maxConcurrency);

        state = RunnerState.WAITING_FOR_COMPLETION;
        layerLatch = new CountDownLatch(tasksToExecute.size());
        activeLayerTaskIds.clear();
        layerTaskResults.clear();
        parallelTaskDoneFlags.clear();

        long layerStartTime = System.currentTimeMillis();

        // Submit all tasks in this layer
        for (TaskSpec task : tasksToExecute) {
            String tid = task.getId() != null ? task.getId() : "?";
            activeLayerTaskIds.add(tid);

            // Update the flat index for listener reporting
            currentTaskIndex = orderedTasks.indexOf(task);
            notifyTaskStarted(task);

            if (cliMode) {
                submitTaskViaCli(task);
            } else {
                // In parallel LLM mode, each task needs its own fresh conversation.
                // The message bus / prompt submission is inherently single-threaded.
                // Parallel mode provides real concurrency only for CLI mode.
                submitTaskViaLlm(task);
            }
        }

        // Wait for all tasks in the layer on a background thread, then advance
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                layerLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Layer latch interrupted");
            }

            long layerElapsed = System.currentTimeMillis() - layerStartTime;

            // Report layer summary (deterministic order)
            reportLayerSummary(tasksToExecute, layerElapsed);

            // Advance to next layer on EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                if (state == RunnerState.CANCELLED) {
                    finish(RunnerState.CANCELLED);
                    return;
                }
                state = RunnerState.RUNNING_TASK;
                executeNextLayer();
            });
        });
    }

    /**
     * Handle specs-changed events in parallel mode.
     * Check all active layer tasks for completion.
     */
    private void onSpecsChangedParallel() {
        for (String taskId : activeLayerTaskIds) {
            TaskSpec fresh = SpecService.getInstance(project).getSpec(taskId);
            if (fresh == null) {
                log.debug("Parallel task {} not found in spec cache, waiting", taskId);
                continue;
            }
            if ("Done".equalsIgnoreCase(fresh.getStatus())) {
                log.info("Parallel task {} detected as Done (deferring advance until prompt completes)", taskId);
                cancelParallelGraceTimer(taskId);
                parallelTaskDoneFlags.put(taskId, true);

                if (cliMode) {
                    CliTaskExecutorService.getInstance(project).notifyTaskDone(taskId);
                }
            }
        }
    }

    /**
     * Handle completion of a specific task in parallel mode.
     */
    private void handleParallelTaskCompletion(@NotNull String taskId) {
        if (!activeLayerTaskIds.contains(taskId)) {
            log.debug("Parallel task {} not in active layer, ignoring completion", taskId);
            return;
        }

        if (Boolean.TRUE.equals(parallelTaskDoneFlags.remove(taskId))) {
            // Task was already marked Done via onSpecsChanged
            log.info("Parallel task {} prompt completed (already marked Done), counting as completed", taskId);
            cancelParallelGraceTimer(taskId);
            TaskSpec fresh = SpecService.getInstance(project).getSpec(taskId);
            if (fresh != null) {
                markTaskCompleted(fresh);
                layerTaskResults.put(taskId, new LayerTaskResult(fresh, true, false, null, 0));
            }
            activeLayerTaskIds.remove(taskId);
            layerLatch.countDown();
            return;
        }

        // Start grace timer for this task
        log.info("Parallel task {} prompt completed, starting grace timer", taskId);
        cancelParallelGraceTimer(taskId);

        Timer timer = new Timer(3000, e -> {
            TaskSpec fresh = SpecService.getInstance(project).getSpec(taskId);
            if (fresh != null && "Done".equalsIgnoreCase(fresh.getStatus())) {
                return; // Already handled
            }
            if (state != RunnerState.WAITING_FOR_COMPLETION) {
                return;
            }
            log.warn("Parallel task {} not marked Done after grace period, skipping", taskId);
            skippedCount++;
            if (fresh != null) {
                notifyTaskSkipped(fresh, "Prompt execution completed but task was not marked Done");
                layerTaskResults.put(taskId, new LayerTaskResult(fresh, false, true,
                        "Prompt execution completed but task was not marked Done", 0));
            }
            activeLayerTaskIds.remove(taskId);
            layerLatch.countDown();
        });
        timer.setRepeats(false);
        parallelGraceTimers.put(taskId, timer);
        timer.start();
    }

    /**
     * Handle failure of a specific task in parallel mode.
     */
    private void handleParallelTaskFailure(@NotNull String taskId, int exitCode, @NotNull String errorOutput) {
        if (!activeLayerTaskIds.contains(taskId)) {
            return;
        }

        cancelParallelGraceTimer(taskId);

        String firstLine = errorOutput.lines()
                .filter(l -> !l.isBlank())
                .findFirst()
                .orElse("Unknown error");
        String reason = "CLI tool failed (exit code " + exitCode + "): " + firstLine;

        log.error("Parallel CLI task {} failed: {}", taskId, reason);
        skippedCount++;

        TaskSpec fresh = SpecService.getInstance(project).getSpec(taskId);
        if (fresh != null) {
            notifyTaskSkipped(fresh, reason);
            layerTaskResults.put(taskId, new LayerTaskResult(fresh, false, true, reason, 0));
        }

        activeLayerTaskIds.remove(taskId);
        layerLatch.countDown();
    }

    private void cancelParallelGraceTimer(@NotNull String taskId) {
        Timer timer = parallelGraceTimers.remove(taskId);
        if (timer != null) {
            timer.stop();
        }
    }

    private void cancelAllParallelGraceTimers() {
        for (Timer timer : parallelGraceTimers.values()) {
            timer.stop();
        }
        parallelGraceTimers.clear();
    }

    /**
     * Report a deterministic layer summary after all tasks complete.
     * Results are sorted by the original task order within the layer.
     */
    private void reportLayerSummary(@NotNull List<TaskSpec> layerTasks, long elapsedMs) {
        int layerCompleted = 0;
        int layerSkipped = 0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Layer %d completed (%dms) ===%n",
                currentLayerIndex + 1, elapsedMs));

        for (TaskSpec task : layerTasks) {
            String tid = task.getId() != null ? task.getId() : "?";
            LayerTaskResult result = layerTaskResults.get(tid);
            if (result != null) {
                if (result.completed) {
                    layerCompleted++;
                    sb.append(String.format("  [OK]   %s: %s%n", tid,
                            task.getTitle() != null ? task.getTitle() : ""));
                } else if (result.skipped) {
                    layerSkipped++;
                    sb.append(String.format("  [SKIP] %s: %s (%s)%n", tid,
                            task.getTitle() != null ? task.getTitle() : "",
                            result.skipReason != null ? result.skipReason : "unknown"));
                }
            } else {
                sb.append(String.format("  [???]  %s%n", tid));
            }
        }

        sb.append(String.format("  Total: %d completed, %d skipped out of %d%n",
                layerCompleted, layerSkipped, layerTasks.size()));

        log.info(sb.toString());

        // Print to CLI console if in CLI mode
        if (cliMode) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    CliConsoleManager.getInstance(project).printSystem(sb.toString());
                } catch (Exception ignored) {
                    // Console might not be available
                }
            });
        }
    }

    private void finish(@NotNull RunnerState finalState) {
        state = finalState;
        cancelGraceTimer();
        cancelAllParallelGraceTimers();
        if (specChangeListener != null) {
            SpecService.getInstance(project).removeChangeListener(specChangeListener);
            specChangeListener = null;
        }
        notifyRunFinished(finalState);
        log.info("Task runner finished: state={}, completed={}, skipped={}, total={}",
                finalState, completedCount, skippedCount, orderedTasks.size());
        // Reset to idle after notifying
        state = RunnerState.IDLE;
        // Reset mode flags
        cliMode = false;
        executionMode = ExecutionMode.SEQUENTIAL;
        layers = Collections.emptyList();
        activeLayerTaskIds.clear();
        layerTaskResults.clear();
        parallelTaskDoneFlags.clear();
    }

    // ===== Conversation Management =====

    private void startFreshConversation() {
        ChatMemoryService.getInstance().clearMemory(project);
        FileListManager.getInstance().clear(project);
        log.debug("Started fresh conversation for next task");
    }

    // ===== Listener Notifications =====

    private void notifyRunStarted() {
        int total = orderedTasks.size();
        for (SpecTaskRunnerListener l : listeners) {
            l.onRunStarted(total);
        }
    }

    private void notifyTaskStarted(@NotNull TaskSpec task) {
        for (SpecTaskRunnerListener l : listeners) {
            l.onTaskStarted(task, currentTaskIndex, orderedTasks.size());
        }
    }

    private void notifyTaskCompleted(@NotNull TaskSpec task) {
        for (SpecTaskRunnerListener l : listeners) {
            l.onTaskCompleted(task, currentTaskIndex, orderedTasks.size());
        }
    }

    private void notifyTaskSkipped(@NotNull TaskSpec task, @NotNull String reason) {
        for (SpecTaskRunnerListener l : listeners) {
            l.onTaskSkipped(task, currentTaskIndex, orderedTasks.size(), reason);
        }
    }

    private void notifyRunFinished(@NotNull RunnerState finalState) {
        int total = orderedTasks.size();
        for (SpecTaskRunnerListener l : listeners) {
            l.onRunFinished(completedCount, skippedCount, total, finalState);
        }
    }

    private static ExecutionMode parseExecutionMode(@Nullable String mode) {
        if (mode != null) {
            try {
                return ExecutionMode.valueOf(mode);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return ExecutionMode.SEQUENTIAL;
    }

    @Override
    public void dispose() {
        cancelGraceTimer();
        cancelAllParallelGraceTimers();
        if (specChangeListener != null) {
            SpecService.getInstance(project).removeChangeListener(specChangeListener);
            specChangeListener = null;
        }
        listeners.clear();
        state = RunnerState.IDLE;
        cliMode = false;
        executionMode = ExecutionMode.SEQUENTIAL;
    }
}
