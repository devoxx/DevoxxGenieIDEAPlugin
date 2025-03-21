package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.WebSearchException;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.service.prompt.websearch.WebSearchPromptExecutionService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for executing web search based prompts.
 */
@Slf4j
public class WebSearchPromptStrategy implements PromptExecutionStrategy {

    private final ChatMemoryManager chatMemoryManager;
    private final ThreadPoolManager threadPoolManager;
    private final Project project;

    public WebSearchPromptStrategy(@NotNull Project project) {
        this.project = project;
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
    }

    /**
     * Execute a prompt using web search.
     */
    @Override
    public PromptTask<PromptResult> execute(@NotNull ChatMessageContext context,
                                          @NotNull PromptOutputPanel panel) {

        log.debug("Web search with query: {}", context.getUserMessage().singleText());

        // Create a self-managed prompt task
        PromptTask<PromptResult> resultTask = new PromptTask<>(project);
        
        panel.addUserPrompt(context);

        // Add user message to context and memory
        chatMemoryManager.prepareMemory(context);
        chatMemoryManager.addUserMessage(context);

        // Track execution time
        long startTime = System.currentTimeMillis();
        
        // Execute web search using thread pool
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                var aiMessageOptional = WebSearchPromptExecutionService.getInstance().searchWeb(context);
                
                aiMessageOptional.ifPresentOrElse(aiMessage -> {
                    context.setAiMessage(aiMessage);
                    context.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    
                    // Add the AI response to memory
                    chatMemoryManager.addAiResponse(context);
                    
                    // Display the response
                    panel.addChatResponse(context);
                    resultTask.complete(PromptResult.success(context));
                }, () -> {
                    // No message found
                    WebSearchException webSearchError = new WebSearchException("No search results found");
                    PromptErrorHandler.handleException(project, webSearchError, context);
                    resultTask.complete(PromptResult.failure(context, webSearchError));
                });
            } catch (Exception e) {
                // Create a specific web search exception and handle it
                WebSearchException webSearchError = new WebSearchException(
                    "Error in web search prompt execution", e);
                PromptErrorHandler.handleException(project, webSearchError, context);
                resultTask.complete(PromptResult.failure(context, webSearchError));
            }
        });
        
        // Handle cancellation
        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                panel.removeLastUserPrompt(context);
            }
        });
        
        return resultTask;
    }

    /**
     * Cancel the current execution.
     */
    @Override
    public void cancel() {
        // No specific cancellation logic needed - the task is self-cancelling
    }
}
