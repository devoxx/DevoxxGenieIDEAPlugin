package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
<<<<<<< HEAD
import com.devoxx.genie.model.request.SemanticFile;
import com.devoxx.genie.service.rag.SearchResult;
import com.devoxx.genie.service.rag.SemanticSearchService;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.util.NotificationUtil;
=======
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
>>>>>>> master
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

<<<<<<< HEAD
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static com.devoxx.genie.model.Constant.FIND_COMMAND;
import static com.devoxx.genie.service.MessageCreationService.extractFileReferences;

=======
import java.util.concurrent.Future;

>>>>>>> master
public class NonStreamingPromptExecutor {

    private static final Logger LOG = Logger.getInstance(NonStreamingPromptExecutor.class);

    private final PromptExecutionService promptExecutionService;
    private volatile Future<?> currentTask;
    private volatile boolean isCancelled;

    public NonStreamingPromptExecutor() {
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

<<<<<<< HEAD
        if (FIND_COMMAND.equals(chatMessageContext.getCommandName())) {
            semanticSearch(chatMessageContext, promptOutputPanel, enableButtons);
            enableButtons.run();
            return;
        }

        prompt(chatMessageContext, promptOutputPanel, enableButtons);
    }

    /**
     * Execute the prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the enable buttons
     */
    private void prompt(ChatMessageContext chatMessageContext,
                        @NotNull PromptOutputPanel promptOutputPanel,
                        Runnable enableButtons) {
=======
>>>>>>> master
        currentTask = promptExecutionService.executeQuery(chatMessageContext)
            .thenAccept(response -> {
                if (!isCancelled && response != null) {
                    LOG.debug(">>>> Adding AI message to prompt output panel");
                    chatMessageContext.setAiMessage(response.content());

                    // Set token usage and cost
                    chatMessageContext.setTokenUsageAndCost(response.tokenUsage());

                    // Add the conversation to the chat service
                    ApplicationManager.getApplication().getMessageBus()
                        .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                        .onNewConversation(chatMessageContext);

                    promptOutputPanel.addChatResponse(chatMessageContext);
                } else if (isCancelled) {
                    LOG.debug(">>>> Prompt execution cancelled");
                    promptOutputPanel.removeLastUserPrompt(chatMessageContext);
                }
            })
            .exceptionally(throwable -> {
                ErrorHandler.handleError(chatMessageContext.getProject(), throwable);
                return null;
            })
            .whenComplete((result, throwable) -> enableButtons.run());
    }

    /**
<<<<<<< HEAD
     * Perform semantic search.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the enable buttons
     */
    private static void semanticSearch(ChatMessageContext chatMessageContext,
                                       @NotNull PromptOutputPanel promptOutputPanel,
                                       Runnable enableButtons) {
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
=======
>>>>>>> master
     * Stop prompt execution.
     */
    public void stopExecution() {
        if (currentTask != null && !currentTask.isDone()) {
            isCancelled = true;
            currentTask.cancel(true);
        }
    }
}
