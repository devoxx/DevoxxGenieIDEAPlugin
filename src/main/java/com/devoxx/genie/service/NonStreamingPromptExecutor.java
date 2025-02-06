package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;
import static com.devoxx.genie.service.MessageCreationService.extractFileReferences;

public class NonStreamingPromptExecutor {

    private static final Logger LOG = Logger.getInstance(NonStreamingPromptExecutor.class);

    private final Project project;
    private final PromptExecutionService promptExecutionService;
    private volatile Future<?> currentTask;
    private volatile boolean isCancelled;

    public NonStreamingPromptExecutor(Project project) {
        this.project = project;
        this.promptExecutionService = PromptExecutionService.getInstance();
    }

    /**
     * Execute the prompt.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the enable buttons
     */
    public void execute(ChatMessageContext chatMessageContext,
                        @NotNull PromptOutputPanel promptOutputPanel,
                        Runnable enableButtons) {
        promptOutputPanel.addUserPrompt(chatMessageContext);
        isCancelled = false;

        if (FIND_COMMAND.equals(chatMessageContext.getCommandName())) {
            semanticSearch(chatMessageContext, promptOutputPanel);
            enableButtons.run();
            return;
        }

        prompt(chatMessageContext, promptOutputPanel, enableButtons);
    }

    /**
     * Execute the prompt.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the enable buttons
     */
    private void prompt(ChatMessageContext chatMessageContext,
                        @NotNull PromptOutputPanel promptOutputPanel,
                        Runnable enableButtons) {
        currentTask = promptExecutionService.executeQuery(chatMessageContext)
                .thenAccept(response -> {
                    if (!isCancelled && response != null) {
                        LOG.debug(">>>> Adding AI message to prompt output panel");
                        chatMessageContext.setAiMessage(response.aiMessage());

                        // Set token usage and cost
                        chatMessageContext.setTokenUsageAndCost(response.tokenUsage());

                        // Add the conversation to the chat service
                        project.getMessageBus()
                                .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                                .onNewConversation(chatMessageContext);

                        promptOutputPanel.addChatResponse(chatMessageContext);
                    } else if (isCancelled) {
                        LOG.debug(">>>> Prompt execution cancelled");
                        promptOutputPanel.removeLastUserPrompt(chatMessageContext);
                    }
                })
                .exceptionally(throwable -> {
                    if (!(throwable.getCause() instanceof CancellationException)) {
                        LOG.error("Error occurred while processing chat message", throwable);
                        ErrorHandler.handleError(chatMessageContext.getProject(), throwable);
                    }
                    return null;
                })
                .whenComplete((result, throwable) -> enableButtons.run());
    }

    /**
     * Perform semantic search.
     *
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     */
    private static void semanticSearch(ChatMessageContext chatMessageContext,
                                       @NotNull PromptOutputPanel promptOutputPanel) {
        try {
            SemanticSearchService semanticSearchService = SemanticSearchService.getInstance();
            Map<String, SearchResult> searchResults = semanticSearchService.search(
                    chatMessageContext.getProject(),
                    chatMessageContext.getUserPrompt()
            );

            if (!searchResults.isEmpty()) {
                List<SemanticFile> fileReferences = extractFileReferences(searchResults);
                chatMessageContext.setSemanticReferences(fileReferences);
                promptOutputPanel.addChatResponse(chatMessageContext);
            } else {
                NotificationUtil.sendNotification(chatMessageContext.getProject(),
                        "No relevant files found for your search query.");
            }
        } catch (Exception e) {
            LOG.error("Error performing semantic search", e);
            ErrorHandler.handleError(chatMessageContext.getProject(), e);

        }
    }

    /**
     * Stop prompt execution.
     */
    public void stopExecution() {
        if (currentTask != null && !currentTask.isDone()) {
            isCancelled = true;
            currentTask.cancel(true);
        }
    }
}
