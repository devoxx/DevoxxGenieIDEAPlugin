package com.devoxx.genie.service.spec;

import com.devoxx.genie.model.spec.TaskSpec;

/**
 * Callback interface for monitoring spec task runner progress.
 * All methods are called on the EDT.
 */
public interface SpecTaskRunnerListener {

    /** Called when a batch run starts. */
    void onRunStarted(int totalTasks);

    /** Called when a specific task begins execution. */
    void onTaskStarted(TaskSpec task, int index, int total);

    /** Called when a task completes successfully (status changed to Done). */
    void onTaskCompleted(TaskSpec task, int index, int total);

    /** Called when a task is skipped (timeout, unsatisfied deps, deleted, etc.). */
    void onTaskSkipped(TaskSpec task, int index, int total, String reason);

    /** Called when the entire run finishes. */
    void onRunFinished(int completed, int skipped, int total, SpecTaskRunnerService.RunnerState finalState);
}
