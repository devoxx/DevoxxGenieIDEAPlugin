package com.devoxx.genie.service.prompt.streaming;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.ChatMemoryService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.panel.ChatStreamingResponsePanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jetbrains.annotations.NotNull;

public class StreamingResponseHandler implements  dev.langchain4j.model.chat.response.StreamingChatResponseHandler {
    private final ChatMessageContext chatMessageContext;
    private final Runnable enableButtons;
    private final ChatStreamingResponsePanel streamingChatResponsePanel;
    private final PromptOutputPanel promptOutputPanel;
    private final long startTime;
    private final Project project;
    private volatile boolean isStopped = false;

    public StreamingResponseHandler(ChatMessageContext chatMessageContext,
                                    @NotNull PromptOutputPanel promptOutputPanel,
                                    Runnable enableButtons) {
        this.chatMessageContext = chatMessageContext;
        this.enableButtons = enableButtons;
        this.promptOutputPanel = promptOutputPanel;
        this.streamingChatResponsePanel = new ChatStreamingResponsePanel(chatMessageContext);
        project = chatMessageContext.getProject();
        promptOutputPanel.addStreamResponse(streamingChatResponsePanel);
        startTime = System.currentTimeMillis();
    }

    private void finalizeResponse(@NotNull ChatResponse response) {

        chatMessageContext.setAiMessage(response.aiMessage());

        project.getMessageBus()
            .syncPublisher(AppTopics.CONVERSATION_TOPIC)
            .onNewConversation(chatMessageContext);

        ChatMemoryService.getInstance().add(chatMessageContext.getProject(), response.aiMessage());
        enableButtons.run();
    }

    private void addExpandablePanelIfNeeded() {
        if (!FileListManager.getInstance().isEmpty(chatMessageContext.getProject())) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ExpandablePanel fileListPanel =
                    new ExpandablePanel(chatMessageContext, FileListManager.getInstance().getFiles(chatMessageContext.getProject()));
                fileListPanel.setName(chatMessageContext.getId());
                promptOutputPanel.addStreamFileReferencesResponse(fileListPanel);
            });
        }
    }

    public void stop() {
        isStopped = true;
        enableButtons.run();
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        if (!isStopped) {
            streamingChatResponsePanel.insertToken(partialResponse);
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        if (isStopped) {
            return;
        }
        long endTime = System.currentTimeMillis();
        chatMessageContext.setExecutionTimeMs(endTime - startTime);
        finalizeResponse(completeResponse);
        addExpandablePanelIfNeeded();
    }

    @Override
    public void onError(Throwable error) {
        enableButtons.run();
        ErrorHandler.handleError(chatMessageContext.getProject(), error);
    }
}
