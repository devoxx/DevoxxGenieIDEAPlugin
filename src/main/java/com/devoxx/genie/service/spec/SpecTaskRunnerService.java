package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Project-scoped service that executes spec tasks sequentially in dependency order.
 * Monitors task completion via SpecService change listener.
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
    private final Set<String> completedTaskIds = new HashSet<>();
    private Set<String> selectedTaskIds = Collections.emptySet();

    public SpecTaskRunnerService(@NotNull Project project) {
        this.project = project;
    }

    public static SpecTaskRunnerService getInstance(@NotNull Project project) {
        return project.getService(SpecTaskRunnerService.class);
    }

    // ===== Public API =====

    /**
     * Sort and execute the given tasks sequentially.
     */
    public void runTasks(@NotNull List<TaskSpec> tasks) {
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

        try {
            orderedTasks = TaskDependencySorter.sort(tasks, allSpecs);
        } catch (CircularDependencyException e) {
            log.error("Circular dependency detected: {}", e.getMessage());
            state = RunnerState.ERROR;
            notifyRunFinished(RunnerState.ERROR);
            throw new RuntimeException(e.getMessage(), e);
        }

        // Log execution order so users can understand dependency-based ordering
        log.info("Task execution order (dependency-sorted): {}",
                orderedTasks.stream()
                        .map(t -> t.getId() != null ? t.getId() : "?")
                        .collect(Collectors.joining(" → ")));

        currentTaskIndex = -1;
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
        submitNextTask();
    }

    /**
     * Convenience: run all To Do tasks.
     */
    public void runAllTodoTasks() {
        List<TaskSpec> todoTasks = SpecService.getInstance(project).getSpecsByStatus("To Do");
        runTasks(todoTasks);
    }

    /**
     * Cancel the run after the current task finishes.
     */
    public void cancel() {
        if (state != RunnerState.RUNNING_TASK && state != RunnerState.WAITING_FOR_COMPLETION) {
            return;
        }
        log.info("Cancelling task runner");
        state = RunnerState.CANCELLED;
        finish(RunnerState.CANCELLED);
    }

    /**
     * Called when prompt execution completes (success or failure).
     * <p>
     * If the spec was already detected as Done while the prompt was executing
     * (via {@code onSpecsChanged}), advance immediately.  Otherwise start a
     * grace timer: if the spec isn't marked "Done" within 3 seconds, skip the
     * task and advance to the next one.
     */
    public void notifyPromptExecutionCompleted() {
        if (state != RunnerState.WAITING_FOR_COMPLETION) {
            return;
        }

        TaskSpec current = getCurrentTask();
        if (current == null) {
            return;
        }

        // If onSpecsChanged() already detected Done while the prompt was still
        // running, we deferred advancement until now.  Advance immediately.
        if (currentTaskDoneWhileExecuting) {
            currentTaskDoneWhileExecuting = false;
            log.info("Prompt execution completed for task {} (already marked Done), advancing", current.getId());
            cancelGraceTimer();
            completedCount++;
            if (current.getId() != null) {
                completedTaskIds.add(current.getId().toLowerCase());
            }
            notifyTaskCompleted(current);
            advanceToNextTask();
            return;
        }

        log.info("Prompt execution completed for task {}, starting grace timer", current.getId());
        cancelGraceTimer();

        graceTimer = new Timer(3000, e -> {
            // Check if the task was marked Done during the grace period
            TaskSpec fresh = SpecService.getInstance(project).getSpec(current.getId());
            if (fresh != null && "Done".equalsIgnoreCase(fresh.getStatus())) {
                // Already handled by onSpecsChanged(), nothing to do
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

    // ===== Internal Execution =====

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
            // Task file deleted
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

        // Start a fresh conversation for each task
        startFreshConversation();

        // Submit the task prompt
        state = RunnerState.WAITING_FOR_COMPLETION;
        notifyTaskStarted(fresh);

        String taskDetails = SpecContextBuilder.buildContext(fresh);
        String instruction = SpecContextBuilder.buildAgentInstruction(fresh);
        String prompt = instruction + "\n\n" + taskDetails +
                "\n\nPlease implement the task described above, satisfying all acceptance criteria.";

        project.getMessageBus()
                .syncPublisher(AppTopics.PROMPT_SUBMISSION_TOPIC)
                .onPromptSubmitted(project, prompt);

        // No timeout timer — tasks can take varying amounts of time,
        // especially during batch runs of 50+ tasks. The agent will
        // set the task to Done when finished, which triggers advancement.
    }

    private void onSpecsChanged() {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (state != RunnerState.WAITING_FOR_COMPLETION) {
                return;
            }

            TaskSpec current = getCurrentTask();
            if (current == null || current.getId() == null) {
                return;
            }

            // Re-read fresh status
            TaskSpec fresh = SpecService.getInstance(project).getSpec(current.getId());
            if (fresh == null) {
                // Task file not found — might be temporarily unavailable during a file move
                // (e.g., backlog_task_complete moves the file to completed/).
                // Don't skip immediately; let the next change event handle it.
                log.debug("Task {} not found in spec cache, waiting for next change event", current.getId());
                return;
            }

            if ("Done".equalsIgnoreCase(fresh.getStatus())) {
                // Don't advance immediately — the prompt may still be executing
                // (the LLM marks Done via a tool call mid-response).  Advancing
                // now would clear chat memory while the agent loop is running and
                // cause the next task's grace timer to start prematurely.
                //
                // Instead, set a flag so notifyPromptExecutionCompleted() can
                // advance as soon as the prompt finishes.
                log.info("Task {} detected as Done (deferring advance until prompt completes)", fresh.getId());
                cancelGraceTimer();
                currentTaskDoneWhileExecuting = true;
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

    private void finish(@NotNull RunnerState finalState) {
        state = finalState;
        cancelGraceTimer();
        if (specChangeListener != null) {
            SpecService.getInstance(project).removeChangeListener(specChangeListener);
            specChangeListener = null;
        }
        notifyRunFinished(finalState);
        log.info("Task runner finished: state={}, completed={}, skipped={}, total={}",
                finalState, completedCount, skippedCount, orderedTasks.size());
        // Reset to idle after notifying
        state = RunnerState.IDLE;
    }

    // ===== Conversation Management =====

    /**
     * Clear chat memory and file references to ensure each task starts
     * with a fresh LLM context. The output panels are NOT cleared so the
     * user can review all task responses from the run.
     */
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

    @Override
    public void dispose() {
        cancelGraceTimer();
        if (specChangeListener != null) {
            SpecService.getInstance(project).removeChangeListener(specChangeListener);
            specChangeListener = null;
        }
        listeners.clear();
        state = RunnerState.IDLE;
    }
}
