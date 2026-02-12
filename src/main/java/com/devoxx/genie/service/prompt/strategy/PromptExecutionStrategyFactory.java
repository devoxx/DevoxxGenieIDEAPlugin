package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating appropriate prompt execution strategies based on context.
 */
@Slf4j
public class PromptExecutionStrategyFactory {

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

        // Check if CLI Runners provider — bypasses Langchain4J entirely
        if (chatMessageContext.getLanguageModel() != null &&
                chatMessageContext.getLanguageModel().getProvider() == ModelProvider.CLIRunners) {
            log.debug("Creating CliPromptStrategy");
            return new CliPromptStrategy(project);
        }

        // Check if web search is requested
        if (chatMessageContext.isWebSearchRequested()) {
            log.debug("Creating WebSearchPromptStrategy");
            return new WebSearchPromptStrategy(project);
        }
        
        // Check if streaming mode is enabled in settings
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getStreamMode())) {
            log.debug("Creating StreamingPromptStrategy");
            return new StreamingPromptStrategy(project);
        }
        
        // Default to non-streaming
        log.debug("Creating NonStreamingPromptStrategy");
        return new NonStreamingPromptStrategy(project);
    }
}
