package com.devoxx.genie.service.prompt.threading;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A self-managed task for prompt execution that handles its own registration
 * and lifecycle with the PromptTaskTracker.
 * 
 * @param <T> The type of result this task will produce
 */
@Slf4j
public class PromptTask<T> extends CompletableFuture<T> {
    // Key for storing context in user data
    public static final String CONTEXT_KEY = "CHAT_MESSAGE_CONTEXT";
    
    /**
     * -- GETTER --
     *  Get the unique identifier for this task
     */
    @Getter
    private final String id;
    private final Project project;
    /**
     * -- GETTER --
     *  Check if this task has been cancelled
     */
    @Getter
    private volatile boolean cancelled = false;
    
    // User data for additional context
    private Object userData;
    private String userDataKey;
    
    /**
     * Create a new prompt task for the specified project
     */
    public PromptTask(@NotNull Project project) {
        this.project = project;
        this.id = project.getLocationHash() + "-" + System.nanoTime();
        
        // Self-register with task tracker
        PromptTaskTracker.getInstance().registerTask(this);
        log.debug("Created task {} for project {}", id, project.getName());
    }
    
    /**
     * Store additional data with this task
     * 
     * @param key The key to store the data under
     * @param data The data to store
     */
    public void putUserData(@NotNull String key, @Nullable Object data) {
        this.userDataKey = key;
        this.userData = data;
    }
    
    /**
     * Retrieve stored user data
     * 
     * @param key The key the data was stored under
     * @return The stored data, or null if not found
     */
    @Nullable
    public Object getUserData(@NotNull String key) {
        return key.equals(userDataKey) ? userData : null;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        
        // If this is a prompt task with a context, clean up memory
        if (getUserData(CONTEXT_KEY) != null) {
            ChatMessageContext context = (ChatMessageContext) getUserData(CONTEXT_KEY);
            ChatMemoryManager.getInstance().removeLastUserMessage(context);
            log.debug("Cleaned up memory for cancelled task {}", id);
        }
        
        PromptTaskTracker.getInstance().taskCompleted(this);
        log.debug("Task {} cancelled", id);
        return super.cancel(mayInterruptIfRunning);
    }
    
    @Override
    public boolean complete(T value) {
        PromptTaskTracker.getInstance().taskCompleted(this);
        log.debug("Task {} completed successfully", id);
        return super.complete(value);
    }
    
    @Override
    public boolean completeExceptionally(@NotNull Throwable ex) {
        PromptTaskTracker.getInstance().taskCompleted(this);
        log.debug("Task {} completed exceptionally: {}", id, ex.getMessage());
        return super.completeExceptionally(ex);
    }

    /**
     * Get the project associated with this task
     */
    public @NotNull Project getProject() {
        return project;
    }

}
