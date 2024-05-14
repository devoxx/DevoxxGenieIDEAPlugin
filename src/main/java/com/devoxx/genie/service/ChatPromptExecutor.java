package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.SettingsState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ChatPromptExecutor {

    private final PromptExecutionService promptExecutionService = PromptExecutionService.getInstance();
    private final SettingsState settingsState = SettingsState.getInstance();

    public ChatPromptExecutor() {
    }

    /**
     * Execute the prompt.
     * @param chatMessageContext the chat message context
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the Enable buttons
     */
    public void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                              PromptOutputPanel promptOutputPanel,
                              Runnable enableButtons) {

        Optional<String> commandFromPrompt = getCommandFromPrompt(chatMessageContext.getUserPrompt(), promptOutputPanel);
        chatMessageContext.setUserPrompt(commandFromPrompt.orElse(chatMessageContext.getUserPrompt()));

        Task.Backgroundable task = new Task.Backgroundable(chatMessageContext.getProject(), "Working...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                runPrompt(chatMessageContext, promptOutputPanel, enableButtons);
                progressIndicator.setText("Working...");
            }
        };
        task.queue();
    }

    /**
     * Clear the chat messages.
     */
    public void clearChatMessages() {
        promptExecutionService.clearChatMessages();
    }

    /**
     * Get the command from the prompt.
     * @param prompt the prompt
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
     * @param promptOutputPanel the prompt output panel
     * @param enableButtons the Enable buttons
     */
    private void runPrompt(@NotNull ChatMessageContext chatMessageContext,
                           PromptOutputPanel promptOutputPanel,
                           Runnable enableButtons) {

        try {
            promptExecutionService.executeQuery(chatMessageContext)
                .thenAccept(aiMessageOptional -> {
                    enableButtons.run();
                    if (aiMessageOptional.isPresent()) {
                        chatMessageContext.setAiMessage(aiMessageOptional.get());
                        promptOutputPanel.addChatResponse(chatMessageContext);
                    }
                }).exceptionally(e -> {
                    enableButtons.run();
                    promptOutputPanel.addWarningText(chatMessageContext, e.getMessage());
                    return null;
                });

        } catch (IllegalAccessException e) {
            enableButtons.run();
        }

        if (promptExecutionService.isRunning()) {
            promptOutputPanel.addUserPrompt(chatMessageContext);
        } else {
            enableButtons.run();
        }
    }
}
