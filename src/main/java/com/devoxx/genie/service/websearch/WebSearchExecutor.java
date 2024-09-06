package com.devoxx.genie.service.websearch;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

public class WebSearchExecutor {

    public WebSearchExecutor() {
    }

    public void execute(ChatMessageContext chatMessageContext,
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
}
