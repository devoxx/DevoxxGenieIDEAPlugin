package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
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
    private volatile boolean isStopped = false;

    public StreamingResponseHandler(ChatMessageContext chatMessageContext,
                                    @NotNull PromptOutputPanel promptOutputPanel,
                                    Runnable enableButtons) {
        this.chatMessageContext = chatMessageContext;
        this.enableButtons = enableButtons;
        this.promptOutputPanel = promptOutputPanel;
        this.streamingChatResponsePanel = new ChatStreamingResponsePanel(chatMessageContext);
        promptOutputPanel.addStreamResponse(streamingChatResponsePanel);
    }

    @Override
    public void onNext(String token) {
        if (!isStopped) {
            streamingChatResponsePanel.insertToken(token);
        }
    }

    @Override
    public void onComplete(@NotNull Response<AiMessage> response) {
        if (isStopped) {
            return;
        }

        finalizeResponse(response);
        addExpandablePanelIfNeeded();
    }

    private void finalizeResponse(@NotNull Response<AiMessage> response) {
        chatMessageContext.setAiMessage(response.content());
        ChatMemoryService.getInstance().add(response.content());
        enableButtons.run();
    }

    private void addExpandablePanelIfNeeded() {
        if (chatMessageContext.hasFiles()) {
            SwingUtilities.invokeLater(() -> {
                ExpandablePanel fileListPanel =
                    new ExpandablePanel(chatMessageContext, FileListManager.getInstance().getFiles());
                fileListPanel.setName(chatMessageContext.getName());
                promptOutputPanel.addStreamFileReferencesResponse(fileListPanel);
            });
        }
    }

    public void stop() {
        isStopped = true;
        enableButtons.run();
    }

    @Override
    public void onError(Throwable error) {
        enableButtons.run();
        ErrorHandler.handleError(chatMessageContext.getProject(), error);
    }
}
