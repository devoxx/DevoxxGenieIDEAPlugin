package com.devoxx.genie.service.prompt;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import org.jetbrains.annotations.NotNull;

public interface PromptExecutor {
    void executePrompt(@NotNull ChatMessageContext chatMessageContext,
                              PromptOutputPanel promptOutputPanel,
                              Runnable enableButtons);
}
