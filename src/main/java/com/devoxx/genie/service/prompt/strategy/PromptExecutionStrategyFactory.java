package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating appropriate prompt execution strategies based on context.
 */
public class PromptExecutionStrategyFactory {

    private static final Logger LOG = Logger.getInstance(PromptExecutionStrategyFactory.class);

    public static PromptExecutionStrategyFactory getInstance() {
        return ApplicationManager.getApplication().getService(PromptExecutionStrategyFactory.class);
    }

    /**
     * Create an appropriate strategy based on the chat message context.
     *
     * @param chatMessageContext The context for the prompt execution
     * @return The appropriate strategy implementation
     */
    public PromptExecutionStrategy createStrategy(@NotNull ChatMessageContext chatMessageContext) {
        Project project = chatMessageContext.getProject();
        
        // Check if web search is requested
        if (chatMessageContext.isWebSearchRequested()) {
            LOG.debug("Creating WebSearchPromptStrategy");
            return new WebSearchPromptStrategy();
        }
        
        // Check if streaming mode is enabled in settings
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode())) {
            LOG.debug("Creating StreamingPromptStrategy");
            return new StreamingPromptStrategy();
        }
        
        // Default to non-streaming
        LOG.debug("Creating NonStreamingPromptStrategy");
        return new NonStreamingPromptStrategy(project);
    }
}
