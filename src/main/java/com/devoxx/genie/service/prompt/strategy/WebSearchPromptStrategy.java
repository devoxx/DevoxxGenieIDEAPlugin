package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.WebSearchException;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.websearch.WebSearchPromptExecutionService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Strategy for executing web search based prompts.
 */
public class WebSearchPromptStrategy implements PromptExecutionStrategy {

    private static final Logger LOG = Logger.getInstance(WebSearchPromptStrategy.class);
    
    private final ChatMemoryManager chatMemoryManager;
    private volatile boolean isCancelled;

    public WebSearchPromptStrategy() {
        this.chatMemoryManager = ChatMemoryManager.getInstance();
    }

    /**
     * Execute a prompt using web search.
     */
    @Override
    public CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext,
                                          @NotNull PromptOutputPanel promptOutputPanel,
                                          @NotNull Runnable onComplete) {
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        promptOutputPanel.addUserPrompt(chatMessageContext);
        isCancelled = false;

        // Add user message to context and memory
        chatMemoryManager.prepareMemory(chatMessageContext);
        chatMemoryManager.addUserMessage(chatMessageContext);

        // Track execution time
        long startTime = System.currentTimeMillis();
        
        try {
            WebSearchPromptExecutionService.getInstance().searchWeb(chatMessageContext)
                .ifPresent(aiMessage -> {
                    if (!isCancelled) {
                        chatMessageContext.setAiMessage(aiMessage);
                        chatMessageContext.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                        
                        // Add the AI response to memory
                        chatMemoryManager.addAiResponse(chatMessageContext);
                        
                        // Display the response
                        promptOutputPanel.addChatResponse(chatMessageContext);
                        resultFuture.complete(null);
                    }
                });
        } catch (Exception e) {
            // Create a specific web search exception and handle it with our standardized handler
            WebSearchException webSearchError = new WebSearchException("Error in web search prompt execution", e);
            PromptErrorHandler.handleException(chatMessageContext.getProject(), webSearchError, chatMessageContext);
            resultFuture.completeExceptionally(webSearchError);
        } finally {
            onComplete.run();
        }
        
        return resultFuture;
    }

    /**
     * Cancel the current execution.
     */
    @Override
    public void cancel() {
        isCancelled = true;
    }
}
