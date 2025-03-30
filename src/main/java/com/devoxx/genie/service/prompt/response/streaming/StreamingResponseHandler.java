package com.devoxx.genie.service.prompt.response.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.StreamingException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.panel.ChatStreamingResponsePanel;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Handles streaming responses from the LLM.
 * Processes tokens as they arrive and manages completion.
 */
@Slf4j
public class StreamingResponseHandler implements StreamingChatResponseHandler {
    private final ChatMessageContext context;
    private final ChatStreamingResponsePanel streamingPanel;
    private final PromptOutputPanel outputPanel;
    private final long startTime;
    private final Project project;
    private final Consumer<ChatResponse> onCompleteCallback;
    private final Consumer<Throwable> onErrorCallback;
    private volatile boolean isStopped = false;

    /**
     * Creates a new streaming response handler
     *
     * @param context The chat message context
     * @param outputPanel The UI panel to display response
     * @param onCompleteCallback Called when streaming completes successfully
     * @param onErrorCallback Called when streaming encounters an error
     */
    public StreamingResponseHandler(
            @NotNull ChatMessageContext context,
            @NotNull PromptOutputPanel outputPanel,
            @NotNull Consumer<ChatResponse> onCompleteCallback,
            @NotNull Consumer<Throwable> onErrorCallback) {
        
        this.context = context;
        this.outputPanel = outputPanel;
        this.streamingPanel = new ChatStreamingResponsePanel(context);
        this.project = context.getProject();
        this.onCompleteCallback = onCompleteCallback;
        this.onErrorCallback = onErrorCallback;
        this.startTime = System.currentTimeMillis();
        
        outputPanel.addStreamResponse(streamingPanel);
        log.debug("Created streaming handler for context {}", context.getId());
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        if (!isStopped) {
            streamingPanel.insertToken(partialResponse);
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
        if (isStopped) {
            return;
        }
        
        try {
            long endTime = System.currentTimeMillis();
            context.setExecutionTimeMs(endTime - startTime);
            context.setAiMessage(response.aiMessage());
            
            project.getMessageBus()
                .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                .onNewConversation(context);

            ChatMemoryManager.getInstance().addAiResponse(context);
            
            // Add file references if any
            if (!FileListManager.getInstance().isEmpty(context.getProject())) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    ExpandablePanel fileListPanel = new ExpandablePanel(
                        context, 
                        FileListManager.getInstance().getFiles(context.getProject())
                    );
                    fileListPanel.setName(context.getId());
                    outputPanel.addStreamFileReferencesResponse(fileListPanel);
                });
            }
            
            log.debug("Streaming completed for context {}", context.getId());
            onCompleteCallback.accept(response);
        } catch (Exception e) {
            log.error("Error processing streaming completion", e);
            onErrorCallback.accept(e);
        }
    }

    @Override
    public void onError(Throwable error) {
        log.error("Streaming error for context {}: {}", context.getId(), error.getMessage());
        StreamingException streamingError = new StreamingException(
            "Error during streaming response", error);
        PromptErrorHandler.handleException(context.getProject(), streamingError, context);
        onErrorCallback.accept(streamingError);
    }

    /**
     * Stops the streaming response handler
     */
    public void stop() {
        if (!isStopped) {
            isStopped = true;
            
            // Clean up partial response from memory
            if (context.getAiMessage() != null) {
                ChatMemoryService.getInstance().removeLastMessage(context.getProject());
                log.debug("Cleaned up partial AI response from memory");
            }
            
            log.debug("Stopped streaming handler for context {}", context.getId());
        }
    }
}
