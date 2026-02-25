package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.WebSearchException;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.websearch.WebSearchPromptExecutionService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for executing web search based prompts.
 */
@Slf4j
public class WebSearchPromptStrategy extends AbstractPromptExecutionStrategy {

    public WebSearchPromptStrategy(@NotNull Project project) {
        super(project);
    }

    @Override
    protected String getStrategyName() {
        return "web search prompt";
    }

    @Override
    protected void executeStrategySpecific(
            @NotNull ChatMessageContext context, 
            @NotNull PromptOutputPanel panel,
            @NotNull PromptTask<PromptResult> resultTask) {
        
        log.debug("Web search with query: {}", context.getUserPrompt());

        // Track execution time
        long startTime = System.currentTimeMillis();
        
        // Execute web search using thread pool
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                var aiMessageOptional = WebSearchPromptExecutionService.getInstance().searchWeb(context);
                
                aiMessageOptional.ifPresentOrElse(aiMessage -> {
                    context.setAiMessage(aiMessage);
                    context.setExecutionTimeMs(System.currentTimeMillis() - startTime);

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
    }

    /**
     * Cancel the current execution.
     * This strategy doesn't need specific cancellation logic - the task is self-cancelling.
     */
    @Override
    public void cancel() {
        // No specific cancellation logic needed
    }
}
