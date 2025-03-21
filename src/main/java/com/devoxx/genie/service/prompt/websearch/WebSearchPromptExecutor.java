package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.PromptExecutor;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

public class WebSearchPromptExecutor implements PromptExecutor {

    public void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                              @NotNull PromptOutputPanel promptOutputPanel,
                              Runnable enableButtons) {
        promptOutputPanel.addUserPrompt(chatMessageContext);
        long startTime = System.currentTimeMillis();
        WebSearchPromptExecutionService.getInstance().searchWeb(chatMessageContext)
            .ifPresent(aiMessage -> {
                chatMessageContext.setAiMessage(aiMessage);
                chatMessageContext.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                promptOutputPanel.addChatResponse(chatMessageContext);
                enableButtons.run();
            });
    }
}
