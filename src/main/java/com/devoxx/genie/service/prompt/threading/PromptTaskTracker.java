package com.devoxx.genie.service.prompt.threading;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks and manages cancellable tasks across the application.
 * Provides a standardized way to register, track and cancel running tasks.
 */
@Slf4j
public class PromptTaskTracker {

    // Map of project hash -> set of tasks
    private final Map<String, Set<PromptTask<?>>> projectTasks = new ConcurrentHashMap<>();
    
    // Backward compatibility: Map of project hash -> task ID -> task
    private final Map<String, Map<String, CancellableTask>> legacyTasks = new ConcurrentHashMap<>();
    
    public static PromptTaskTracker getInstance() {
        return ApplicationManager.getApplication().getService(PromptTaskTracker.class);
    }
    
    /**
     * Registers a new task for tracking.
     * 
     * @param task The PromptTask to register
     */
    public <T> void registerTask(@NotNull PromptTask<T> task) {
        String projectHash = task.getProject().getLocationHash();
        projectTasks.computeIfAbsent(projectHash, k -> ConcurrentHashMap.newKeySet())
                   .add(task);
        log.debug("Registered task {} for project {}", task.getId(), projectHash);
    }
    
    /**
     * Marks a task as completed and removes it from tracking.
     * 
     * @param task The completed PromptTask
     */
    public <T> void taskCompleted(@NotNull PromptTask<T> task) {
        String projectHash = task.getProject().getLocationHash();
        Set<PromptTask<?>> tasks = projectTasks.get(projectHash);
        
        if (tasks != null) {
            tasks.remove(task);
            log.debug("Task {} completed for project {}", task.getId(), projectHash);
            
            // Clean up the project map if empty
            if (tasks.isEmpty()) {
                projectTasks.remove(projectHash);
            }
        }
    }
    
    /**
     * Cancels all tasks for the given project.
     * 
     * @param project The project whose tasks should be cancelled
     * @return Number of tasks cancelled
     */
    public int cancelAllTasks(@NotNull Project project) {
        String projectHash = project.getLocationHash();
        int count = 0;
        
        // Handle new-style tasks
        Set<PromptTask<?>> tasks = projectTasks.get(projectHash);
        if (tasks != null && !tasks.isEmpty()) {
            count += tasks.size();
            log.debug("Cancelling {} new-style tasks for project {}", tasks.size(), projectHash);
            
            // Clone to avoid ConcurrentModificationException
            new HashSet<>(tasks).forEach(task -> task.cancel(true));
        }
        
        // Handle legacy tasks
        Map<String, CancellableTask> oldTasks = legacyTasks.get(projectHash);
        if (oldTasks != null && !oldTasks.isEmpty()) {
            count += oldTasks.size();
            log.debug("Cancelling {} legacy tasks for project {}", oldTasks.size(), projectHash);
            
            // Cancel all tasks
            oldTasks.values().forEach(CancellableTask::cancel);
            oldTasks.clear();
        }
        
        return count;
    }
    
    // === LEGACY SUPPORT METHODS ===
    
    /**
     * Registers a new task for the specified project.
     * @deprecated Use {@link #registerTask(PromptTask)} instead
     */
    @Deprecated
    public void registerTask(@NotNull Project project, @NotNull String taskId, @NotNull CancellableTask task) {
        String projectHash = project.getLocationHash();
        
        legacyTasks.computeIfAbsent(projectHash, k -> new ConcurrentHashMap<>())
                   .put(taskId, task);
        
        log.debug("Registered legacy task {} for project {}", taskId, projectHash);
    }
    
    /**
     * Cancels a specific task for the given project.
     * @deprecated Use {@link PromptTask#cancel(boolean)} instead
     */
    @Deprecated
    public void cancelTask(@NotNull Project project, @NotNull String taskId) {
        String projectHash = project.getLocationHash();
        Map<String, CancellableTask> tasks = legacyTasks.get(projectHash);
        
        if (tasks == null) {
            log.debug("No legacy tasks found for project {}", projectHash);
            return;
        }
        
        CancellableTask task = tasks.remove(taskId);
        if (task != null) {
            log.debug("Cancelling legacy task {} for project {}", taskId, projectHash);
            task.cancel();
            return;
        }

        log.debug("Legacy task {} not found for project {}", taskId, projectHash);
    }
    
    /**
     * Marks a task as completed and removes it from tracking.
     * @deprecated Use {@link #taskCompleted(PromptTask)} instead
     */
    @Deprecated
    public void taskCompleted(@NotNull Project project, @NotNull String taskId) {
        String projectHash = project.getLocationHash();
        Map<String, CancellableTask> tasks = legacyTasks.get(projectHash);
        
        if (tasks != null) {
            tasks.remove(taskId);
            log.debug("Legacy task {} completed for project {}", taskId, projectHash);
            
            // Clean up the project map if empty
            if (tasks.isEmpty()) {
                legacyTasks.remove(projectHash);
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
