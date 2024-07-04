package com.devoxx.genie.service;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ChatPromptExecutor {

    private final StreamingPromptExecutor streamingPromptExecutor;
    private final NonStreamingPromptExecutor nonStreamingPromptExecutor;
    private volatile boolean isRunning = false;

    public ChatPromptExecutor() {
        this.streamingPromptExecutor = new StreamingPromptExecutor();
        this.nonStreamingPromptExecutor = new NonStreamingPromptExecutor();
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

        isRunning = true;
        new Task.Backgroundable(chatMessageContext.getProject(), "Working...", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                if (chatMessageContext.isWebSearchRequested()) {
                    new WebSearchExecutor().execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunning = false;
                        enableButtons.run();
                    });
                } else if (DevoxxGenieStateService.getInstance().getStreamMode()) {
                    streamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunning = false;
                        enableButtons.run();
                    });
                } else {
                    nonStreamingPromptExecutor.execute(chatMessageContext, promptOutputPanel, () -> {
                        isRunning = false;
                        enableButtons.run();
                    });
                }
            }
        }.queue();
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
     * Stop streaming or the non-streaming prompt execution
     */
    public void stopPromptExecution() {
        if (isRunning) {
            isRunning = false;
            streamingPromptExecutor.stopStreaming();
            nonStreamingPromptExecutor.stopExecution();
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
            DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

            if (prompt.equalsIgnoreCase("/test")) {
                prompt = settings.getTestPrompt();
            } else if (prompt.equalsIgnoreCase("/review")) {
                prompt = settings.getReviewPrompt();
            } else if (prompt.equalsIgnoreCase("/explain")) {
                prompt = settings.getExplainPrompt();
            } else {
                // Check for custom prompts
                for (CustomPrompt customPrompt : settings.getCustomPrompts()) {
                    if (prompt.equalsIgnoreCase("/" + customPrompt.getName())) {
                        prompt = customPrompt.getPrompt();
                        return Optional.of(prompt);
                    }
                }
                promptOutputPanel.showHelpText();
                return Optional.empty();
            }
        }
        return Optional.of(prompt);
    }
}
