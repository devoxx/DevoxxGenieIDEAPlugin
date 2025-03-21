package com.devoxx.genie.service.prompt.streaming;

import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.StreamingException;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.panel.ChatStreamingResponsePanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class StreamingResponseHandler implements  dev.langchain4j.model.chat.response.StreamingChatResponseHandler {
    private final ChatMessageContext chatMessageContext;
    private final ChatStreamingResponsePanel streamingChatResponsePanel;
    private final PromptOutputPanel promptOutputPanel;
    private final long startTime;
    private final Project project;
    private final CompletableFuture<Void> completionFuture;
    private volatile boolean isStopped = false;

    public StreamingResponseHandler(ChatMessageContext chatMessageContext,
                                    @NotNull PromptOutputPanel promptOutputPanel,
                                    Runnable onComplete) {
        this.chatMessageContext = chatMessageContext;
        this.promptOutputPanel = promptOutputPanel;
        this.streamingChatResponsePanel = new ChatStreamingResponsePanel(chatMessageContext);
        this.completionFuture = new CompletableFuture<>();
        this.project = chatMessageContext.getProject();
        promptOutputPanel.addStreamResponse(streamingChatResponsePanel);
        startTime = System.currentTimeMillis();
        
        // Set up the completion handler
        completionFuture.thenRun(onComplete);
    }

    private void finalizeResponse(@NotNull ChatResponse response) {

        chatMessageContext.setAiMessage(response.aiMessage());

        project.getMessageBus()
            .syncPublisher(AppTopics.CONVERSATION_TOPIC)
            .onNewConversation(chatMessageContext);

        ChatMemoryService.getInstance().add(chatMessageContext.getProject(), response.aiMessage());
        completionFuture.complete(null);
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
        completionFuture.complete(null);
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
        // Convert to a streaming exception and handle with our standardized handler
        StreamingException streamingError = new StreamingException("Error during streaming response", error);
        PromptErrorHandler.handleException(chatMessageContext.getProject(), streamingError, chatMessageContext);
        completionFuture.completeExceptionally(streamingError);
    }
}
