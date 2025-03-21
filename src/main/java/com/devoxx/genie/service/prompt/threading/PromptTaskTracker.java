package com.devoxx.genie.service.prompt.threading;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and manages cancellable tasks across the application.
 * Provides a standardized way to register, track and cancel running tasks.
 */
@Slf4j
public class PromptTaskTracker {

    // Map of project hash -> task ID -> task
    private final Map<String, Map<String, CancellableTask>> projectTasks = new ConcurrentHashMap<>();
    
    public static PromptTaskTracker getInstance() {
        return ApplicationManager.getApplication().getService(PromptTaskTracker.class);
    }
    
    /**
     * Registers a new task for the specified project.
     * 
     * @param project The project this task belongs to
     * @param taskId A unique identifier for this task
     * @param task The cancellable task
     */
    public void registerTask(@NotNull Project project, @NotNull String taskId, @NotNull CancellableTask task) {
        String projectHash = project.getLocationHash();
        
        projectTasks.computeIfAbsent(projectHash, k -> new ConcurrentHashMap<>())
                   .put(taskId, task);
        
        log.debug("Registered task " + taskId + " for project " + projectHash);
    }
    
    /**
     * Cancels a specific task for the given project.
     *
     * @param project The project containing the task
     * @param taskId  The ID of the task to cancel
     */
    public void cancelTask(@NotNull Project project, @NotNull String taskId) {
        String projectHash = project.getLocationHash();
        Map<String, CancellableTask> tasks = projectTasks.get(projectHash);
        
        if (tasks == null) {
            log.debug("No tasks found for project " + projectHash);
            return;
        }
        
        CancellableTask task = tasks.remove(taskId);
        if (task != null) {
            log.debug("Cancelling task " + taskId + " for project " + projectHash);
            task.cancel();
            return;
        }

        log.debug("Task " + taskId + " not found for project " + projectHash);
    }
    
    /**
     * Cancels all tasks for the given project.
     * 
     * @param project The project whose tasks should be cancelled
     * @return Number of tasks cancelled
     */
    public int cancelAllTasks(@NotNull Project project) {
        String projectHash = project.getLocationHash();
        Map<String, CancellableTask> tasks = projectTasks.get(projectHash);
        
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        
        int count = tasks.size();
        log.debug("Cancelling " + count + " tasks for project " + projectHash);
        
        // Cancel all tasks
        tasks.values().forEach(CancellableTask::cancel);
        tasks.clear();
        
        return count;
    }
    
    /**
     * Marks a task as completed and removes it from tracking.
     * 
     * @param project The project containing the task
     * @param taskId The ID of the completed task
     */
    public void taskCompleted(@NotNull Project project, @NotNull String taskId) {
        String projectHash = project.getLocationHash();
        Map<String, CancellableTask> tasks = projectTasks.get(projectHash);
        
        if (tasks != null) {
            tasks.remove(taskId);
            log.debug("Task " + taskId + " completed for project " + projectHash);
            
            // Clean up the project map if empty
            if (tasks.isEmpty()) {
                projectTasks.remove(projectHash);
            }
        }
    }
    
    /**
     * Interface for tasks that can be cancelled.
     */
    public interface CancellableTask {
        /**
         * Cancels this task.
         */
        void cancel();
    }
}
