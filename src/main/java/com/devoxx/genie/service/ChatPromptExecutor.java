package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.exception.ProviderUnavailableException;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

public class ChatPromptExecutor {

    private final PromptExecutionService promptExecutionService = PromptExecutionService.getInstance();
    private StreamingResponseHandler currentStreamingHandler;

    public ChatPromptExecutor() {
    }

    /**
     * Execute the prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the Enable buttons
     */
    public void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                              PromptOutputPanel promptOutputPanel,
                              Runnable enableButtons) {

        new Task.Backgroundable(chatMessageContext.getProject(), "Working...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                if (isWebSearch(chatMessageContext)) {
                    executeWebSearch(chatMessageContext, promptOutputPanel, enableButtons);
                } else if (DevoxxGenieStateService.getInstance().getStreamMode()) {
                    executeStreamingPrompt(chatMessageContext, promptOutputPanel, enableButtons);
                } else {
                    executeNonStreamingPrompt(chatMessageContext, promptOutputPanel, enableButtons);
                }
            }
        }.queue();
    }

    /**
     * Is web search.
     * @param chatMessageContext the chat message context
     * @return the boolean
     */
    private boolean isWebSearch(@NotNull ChatMessageContext chatMessageContext) {
        return chatMessageContext.getContext() != null &&
            chatMessageContext.getContext().toLowerCase().contains("search");
    }

    /**
     * Web search prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the Enable buttons
     */
    private void executeWebSearch(@NotNull ChatMessageContext chatMessageContext,
                                 @NotNull PromptOutputPanel promptOutputPanel,
                                 Runnable enableButtons) {
        promptOutputPanel.addUserPrompt(chatMessageContext);
        WebSearchService.getInstance().searchWeb(chatMessageContext)
            .ifPresent(aiMessage -> {
                chatMessageContext.setAiMessage(aiMessage);
                promptOutputPanel.addChatResponse(chatMessageContext);
                enableButtons.run();
            });
    }

    /**
     * Process possible command prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     */
    public Optional<String> updatePromptWithCommandIfPresent(@NotNull ChatMessageContext chatMessageContext,
                                                             PromptOutputPanel promptOutputPanel) {
        Optional<String> commandFromPrompt = getCommandFromPrompt(chatMessageContext.getUserPrompt(), promptOutputPanel);
        chatMessageContext.setUserPrompt(commandFromPrompt.orElse(chatMessageContext.getUserPrompt()));
        return commandFromPrompt;
    }


    /**
     * Execute streaming response.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the Enable buttons
     */
    private void executeStreamingPrompt(@NotNull ChatMessageContext chatMessageContext,
                                        @NotNull PromptOutputPanel promptOutputPanel,
                                        Runnable enableButtons) {
        StreamingChatLanguageModel streamingChatLanguageModel = chatMessageContext.getStreamingChatLanguageModel();
        if (streamingChatLanguageModel == null) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(), "Streaming model not available, please select another provider.");
            enableButtons.run();
            return;
        }

        ChatMemoryService chatMemoryService = ChatMemoryService.getInstance();
        MessageCreationService messageCreationService = MessageCreationService.getInstance();

        if (chatMemoryService.isEmpty()) {
            chatMemoryService.add(new SystemMessage(DevoxxGenieStateService.getInstance().getSystemPrompt()));
        }

        UserMessage userMessage = messageCreationService.createUserMessage(chatMessageContext);
        chatMemoryService.add(userMessage);

        promptOutputPanel.addUserPrompt(chatMessageContext);

        currentStreamingHandler = new StreamingResponseHandler(chatMessageContext, promptOutputPanel, enableButtons);
        streamingChatLanguageModel.generate(chatMemoryService.messages(), currentStreamingHandler);
    }

    /**
     * Stop streaming.
     */
    public void stopStreaming() {
        if (currentStreamingHandler != null) {
            currentStreamingHandler.stop();
            currentStreamingHandler = null;
        }
    }

    /**
     * Get the command from the prompt.
     *
     * @param prompt            the prompt
     * @param promptOutputPanel the prompt output panel
     * @return the command
     */
    private Optional<String> getCommandFromPrompt(@NotNull String prompt,
                                                  PromptOutputPanel promptOutputPanel) {
        if (prompt.startsWith("/")) {

            if (prompt.equalsIgnoreCase("/test")) {
                prompt = DevoxxGenieStateService.getInstance().getTestPrompt();
            } else if (prompt.equalsIgnoreCase("/review")) {
                prompt = DevoxxGenieStateService.getInstance().getReviewPrompt();
            } else if (prompt.equalsIgnoreCase("/explain")) {
                prompt = DevoxxGenieStateService.getInstance().getExplainPrompt();
            } else if (prompt.equalsIgnoreCase("/custom")) {
                prompt = DevoxxGenieStateService.getInstance().getCustomPrompt();
            } else {
                promptOutputPanel.showHelpText();
                return Optional.empty();
            }
        }
        return Optional.of(prompt);
    }

    /**
     * Run the prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the Enable buttons
     */
    private void executeNonStreamingPrompt(@NotNull ChatMessageContext chatMessageContext,
                                           PromptOutputPanel promptOutputPanel,
                                           Runnable enableButtons) {

        promptExecutionService.executeQuery(chatMessageContext)
            .thenAccept(aiMessageOptional -> {
                enableButtons.run();
                if (aiMessageOptional.isPresent()) {
                    chatMessageContext.setAiMessage(aiMessageOptional.get());
                    promptOutputPanel.addChatResponse(chatMessageContext);
                }
            }).exceptionally(e -> {
                enableButtons.run();
                if (e.getCause() instanceof CancellationException) {
                    // This means the user has cancelled the prompt, so no warning required
                    return null;
                }
                if (e.getCause() instanceof TimeoutException) {
                    NotificationUtil.sendNotification(chatMessageContext.getProject(),
                        "Timeout occurred. Please increase the timeout setting.");
                    return null;
                } else if (e.getCause() instanceof ProviderUnavailableException) {
                    NotificationUtil.sendNotification(chatMessageContext.getProject(),
                        "LLM provider not available. Please select another provider or make sure it's running.");
                    return null;
                }
                String message = e.getMessage() + ". Maybe create an issue on GitHub?";
                NotificationUtil.sendNotification(chatMessageContext.getProject(), "Error occurred: " + message);
                return null;
            });

        if (promptExecutionService.isRunning()) {
            promptOutputPanel.addUserPrompt(chatMessageContext);
        } else {
            enableButtons.run();
        }
    }
}
