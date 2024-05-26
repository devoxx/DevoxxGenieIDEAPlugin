package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.panel.ChatStreamingResponsePanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StreamingResponseHandler implements dev.langchain4j.model.StreamingResponseHandler<AiMessage> {

    private final ChatMessageContext chatMessageContext;
    private final Runnable enableButtons;
    private final ChatStreamingResponsePanel streamingChatResponsePanel;
    private final PromptOutputPanel promptOutputPanel;

    public StreamingResponseHandler(ChatMessageContext chatMessageContext,
                                    @NotNull PromptOutputPanel promptOutputPanel,
                                    Runnable enableButtons) {
        this.chatMessageContext = chatMessageContext;
        this.enableButtons = enableButtons;
        this.promptOutputPanel = promptOutputPanel;
        streamingChatResponsePanel = new ChatStreamingResponsePanel(chatMessageContext);
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
        if (chatMessageContext.hasFiles()) {
            SwingUtilities.invokeLater(() -> {
                ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext);
                fileListPanel.setName(chatMessageContext.getName());
                promptOutputPanel.addStreamFileReferencesResponse(fileListPanel);
            });
        }
    }

    @Override
    public void onError(Throwable error) {
        // TODO Show error message
        enableButtons.run();
    }
}

