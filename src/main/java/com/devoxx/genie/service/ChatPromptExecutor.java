package com.devoxx.genie.service;

import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

public class ChatPromptExecutor {

    private final PromptExecutionService promptExecutionService = PromptExecutionService.getInstance();
    private final SettingsStateService settingsState = SettingsStateService.getInstance();

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
                if (chatMessageContext.getContext().toLowerCase().contains("search")) {
                    webSearchPrompt(chatMessageContext, promptOutputPanel, enableButtons);
                } else {
                    if (SettingsStateService.getInstance().getStreamMode()) {
                        setupStreaming(chatMessageContext, promptOutputPanel, enableButtons);
                    } else {
                        runPrompt(chatMessageContext, promptOutputPanel, enableButtons);
                    }
                }
            }
        }.queue();
    }

    /**
     * Web search prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the Enable buttons
     */
    private void webSearchPrompt(@NotNull ChatMessageContext chatMessageContext,
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
    public void updatePromptWithCommandIfPresent(@NotNull ChatMessageContext chatMessageContext,
                                                 PromptOutputPanel promptOutputPanel) {
        Optional<String> commandFromPrompt = getCommandFromPrompt(chatMessageContext.getUserPrompt(), promptOutputPanel);
        chatMessageContext.setUserPrompt(commandFromPrompt.orElse(chatMessageContext.getUserPrompt()));
    }

    /**
     * Setup streaming.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel  the prompt output panel
     * @param enableButtons      the Enable buttons
     */
    private void setupStreaming(@NotNull ChatMessageContext chatMessageContext,
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
            chatMemoryService.add(messageCreationService.createSystemMessage(chatMessageContext));
        }

        UserMessage userMessage = messageCreationService.createUserMessage(chatMessageContext);
        chatMemoryService.add(userMessage);

        promptOutputPanel.addUserPrompt(chatMessageContext);

        streamingChatLanguageModel.generate(
            chatMemoryService.messages(),
            new StreamingResponseHandler(chatMessageContext, promptOutputPanel, enableButtons));
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
                prompt = settingsState.getTestPrompt();
            } else if (prompt.equalsIgnoreCase("/review")) {
                prompt = settingsState.getReviewPrompt();
            } else if (prompt.equalsIgnoreCase("/explain")) {
                prompt = settingsState.getExplainPrompt();
            } else if (prompt.equalsIgnoreCase("/custom")) {
                prompt = settingsState.getCustomPrompt();
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
    private void runPrompt(@NotNull ChatMessageContext chatMessageContext,
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
                    promptOutputPanel.addWarningText(chatMessageContext, "Timeout occurred. Please try again.");
                    return null;
                }
                promptOutputPanel.addWarningText(chatMessageContext, e.getMessage());
                return null;
            });

        if (promptExecutionService.isRunning()) {
            promptOutputPanel.addUserPrompt(chatMessageContext);
        } else {
            enableButtons.run();
        }
    }
}
