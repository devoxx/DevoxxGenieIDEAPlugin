package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.panel.StreamingChatResponsePanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;

public class StreamingResponseHandler implements dev.langchain4j.model.StreamingResponseHandler<AiMessage> {

    private final Runnable enableButtons;
    private final StreamingChatResponsePanel streamingChatResponsePanel;

    public StreamingResponseHandler(ChatMessageContext chatMessageContext,
                                    Runnable enableButtons,
                                    @NotNull PromptOutputPanel promptOutputPanel) {
        this.enableButtons = enableButtons;
        streamingChatResponsePanel = new StreamingChatResponsePanel(chatMessageContext);
        promptOutputPanel.addStreamResponse(streamingChatResponsePanel);
    }

    @Override
    public void onNext(String token) {
        streamingChatResponsePanel.insertToken(token);
    }

    @Override
    public void onComplete(@NotNull Response<AiMessage> response) {
        AiMessage content = response.content();
        ChatMemoryService.getInstance().add(content);
        enableButtons.run();
    }

    @Override
    public void onError(Throwable error) {
        // TODO Show error message
        enableButtons.run();
    }
}

