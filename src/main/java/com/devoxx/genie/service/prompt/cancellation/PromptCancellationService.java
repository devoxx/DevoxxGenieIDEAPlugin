package com.devoxx.genie.service.prompt.cancellation;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.strategy.PromptExecutionStrategy;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized service for handling prompt cancellation across all strategies.
 * Coordinates cleanup of resources when a prompt execution is cancelled.
 */
@Slf4j
public class PromptCancellationService {

    // Map to store active strategies by project hash and context id
    private final Map<String, Map<String, PromptExecutionStrategy>> activeStrategies = new ConcurrentHashMap<>();
    
    // Map to store active output panels by project hash and context id
    private final Map<String, Map<String, PromptOutputPanel>> activeOutputPanels = new ConcurrentHashMap<>();

    public static PromptCancellationService getInstance() {
        return ApplicationManager.getApplication().getService(PromptCancellationService.class);
    }

    /**
     * Register an active strategy and panel for potential cancellation
     */
    public void registerExecution(@NotNull Project project, 
                               @NotNull String contextId,
                               @NotNull PromptExecutionStrategy strategy,
                               @NotNull PromptOutputPanel panel) {
        String projectHash = project.getLocationHash();
        
        // Register the strategy
        activeStrategies.computeIfAbsent(projectHash, k -> new ConcurrentHashMap<>())
                      .put(contextId, strategy);
                      
        // Register the output panel
        activeOutputPanels.computeIfAbsent(projectHash, k -> new ConcurrentHashMap<>())
                        .put(contextId, panel);
                        
        log.debug("Registered execution for cancellation: project={}, contextId={}", 
                 projectHash, contextId);
    }

    /**
     * Unregister a completed execution
     */
    public void unregisterExecution(@NotNull Project project, @NotNull String contextId) {
        String projectHash = project.getLocationHash();
        
        Map<String, PromptExecutionStrategy> strategies = activeStrategies.get(projectHash);
        if (strategies != null) {
            strategies.remove(contextId);
            if (strategies.isEmpty()) {
                activeStrategies.remove(projectHash);
            }
        }
        
        Map<String, PromptOutputPanel> panels = activeOutputPanels.get(projectHash);
        if (panels != null) {
            panels.remove(contextId);
            if (panels.isEmpty()) {
                activeOutputPanels.remove(projectHash);
            }
        }
        
        log.debug("Unregistered execution: project={}, contextId={}", projectHash, contextId);
    }

    /**
     * Cancel a specific execution by context ID
     */
    public void cancelExecution(@NotNull Project project, @NotNull String contextId) {
        String projectHash = project.getLocationHash();
        
        // Get the strategy and panel for this specific context
        PromptExecutionStrategy strategy = getStrategy(projectHash, contextId);
        PromptOutputPanel panel = getPanel(projectHash, contextId);
        
        if (strategy != null) {
            log.debug("Cancelling specific execution: project={}, contextId={}", 
                    projectHash, contextId);
            strategy.cancel();
        }
        
        // Use PromptTaskTracker to cancel the matching task
        PromptTaskTracker taskTracker = PromptTaskTracker.getInstance();
        taskTracker.cancelTaskByContextId(project, contextId);
        
        // Clean up UI if needed
        cleanupCancelledExecution(project, contextId, panel);
        
        // Finally unregister this execution
        unregisterExecution(project, contextId);
    }

    /**
     * Cancel all executions for a project
     */
    public int cancelAllExecutions(@NotNull Project project) {
        String projectHash = project.getLocationHash();
        int count = 0;
        
        // Get all strategies for this project
        Map<String, PromptExecutionStrategy> strategies = activeStrategies.get(projectHash);
        if (strategies != null && !strategies.isEmpty()) {
            count = strategies.size();
            log.debug("Cancelling {} executions for project {}", count, projectHash);
            
            // Cancel each strategy
            for (Map.Entry<String, PromptExecutionStrategy> entry : strategies.entrySet()) {
                String contextId = entry.getKey();
                PromptExecutionStrategy strategy = entry.getValue();
                PromptOutputPanel panel = getPanel(projectHash, contextId);
                
                // Cancel the strategy
                strategy.cancel();
                
                // Clean up UI if needed
                cleanupCancelledExecution(project, contextId, panel);
            }
            
            // Clear the maps for this project
            activeStrategies.remove(projectHash);
            activeOutputPanels.remove(projectHash);
        }
        
        // Also use PromptTaskTracker for compatibility with existing code
        count += PromptTaskTracker.getInstance().cancelAllTasks(project);
        
        return count;
    }
    
    /**
     * Helper method to clean up after cancellation
     */
    private void cleanupCancelledExecution(
            @NotNull Project project, 
            @NotNull String contextId,
            @Nullable PromptOutputPanel panel) {
        
        // Get the corresponding task to access its context
        PromptTask<?> task = PromptTaskTracker.getInstance().getTaskByContextId(project, contextId);
        if (task != null) {
            Object userData = task.getUserData(PromptTask.CONTEXT_KEY);
            if (userData instanceof ChatMessageContext) {
                ChatMessageContext context = (ChatMessageContext) userData;
                
                // Remove last user message from memory
                ChatMemoryManager.getInstance().removeLastUserMessage(context);
                
                // Update UI if needed
                if (panel != null) {
                    ApplicationManager.getApplication().invokeLater(() -> 
                        panel.removeLastUserPrompt(context));
                }
                
                log.debug("Cleaned up cancelled execution: contextId={}", contextId);
            }
        }
    }
    
    /**
     * Helper method to get a strategy for a specific context
     */
    private @Nullable PromptExecutionStrategy getStrategy(String projectHash, String contextId) {
        Map<String, PromptExecutionStrategy> strategies = activeStrategies.get(projectHash);
        return strategies != null ? strategies.get(contextId) : null;
    }
    
    /**
     * Helper method to get a panel for a specific context
     */
    private @Nullable PromptOutputPanel getPanel(String projectHash, String contextId) {
        Map<String, PromptOutputPanel> panels = activeOutputPanels.get(projectHash);
        return panels != null ? panels.get(contextId) : null;
    }
}