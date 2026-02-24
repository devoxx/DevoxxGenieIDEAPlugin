package com.devoxx.genie.service.prompt.response.streaming;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.StreamingException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.ui.compose.ConversationViewController;
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
    private ChatMessageContext context;
    private long startTime;
    private Project project;
    private Consumer<ChatResponse> onCompleteCallback;
    private Consumer<Throwable> onErrorCallback;
    private volatile boolean isStopped = false;
    private ConversationViewController conversationViewController;

    // Track if we've added the initial message and accumulate the streamed tokens
    private boolean hasAddedInitialMessage = false;
    private final StringBuilder accumulatedResponse = new StringBuilder();

    /**
     * Creates a new streaming response handler
     *
     * @param context The chat message context
     * @param conversationViewController The web view controller to display conversation (can be null in tests)
     * @param onCompleteCallback Called when streaming completes successfully
     * @param onErrorCallback Called when streaming encounters an error
     */
    public StreamingResponseHandler(
            @NotNull ChatMessageContext context,
            ConversationViewController conversationViewController,
            @NotNull Consumer<ChatResponse> onCompleteCallback,
            @NotNull Consumer<Throwable> onErrorCallback) {
        log.debug("Created streaming handler for context {}", context.getId());
        this.context = context;
        this.project = context.getProject();
        this.onCompleteCallback = onCompleteCallback;
        this.onErrorCallback = onErrorCallback;
        this.startTime = System.currentTimeMillis();
        this.conversationViewController = conversationViewController;
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        if (isStopped) {
            return;
        }

        log.debug("Received partial response: '{}...'", 
                partialResponse.substring(0, Math.min(20, partialResponse.length())));
        
        // Accumulate the response tokens 
        accumulatedResponse.append(partialResponse);
        String fullText = accumulatedResponse.toString();
        
        // Only update the UI if we have a valid controller (might be null in tests)
        if (conversationViewController != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                // Set the AI message with accumulated tokens so far
                context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(fullText));
                
                // Always update the existing message - we already created a placeholder
                // when the user submitted the prompt
                conversationViewController.updateAiMessageContent(context);
                
                // Mark that we've started streaming
                hasAddedInitialMessage = true;
            });
        } else {
            // Still update the message in context even without UI
            context.setAiMessage(dev.langchain4j.data.message.AiMessage.from(fullText));
            hasAddedInitialMessage = true;
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

            // Update the view with the final response (if viewController is available)
            if (conversationViewController != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // If we've already shown partial responses, just update the AI content
                    // Otherwise add a new message pair (when we get complete response without partials)
                    if (hasAddedInitialMessage) {
                        conversationViewController.updateAiMessageContent(context);
                    } else {
                        conversationViewController.addChatMessage(context);
                    }
                    // Mark MCP logs as completed now that streaming is finished
                    conversationViewController.markMCPLogsAsCompleted(context.getId());
                });
            }

            project.getMessageBus()
                .syncPublisher(AppTopics.CONVERSATION_TOPIC)
                .onNewConversation(context);

            ChatMemoryManager.getInstance().addAiResponse(context);
            
            // Add file references if any
            if (!FileListManager.getInstance().isEmpty(context.getProject()) && conversationViewController != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // Add file references to the web view instead of creating a dialog
                    conversationViewController.addFileReferences(context, 
                        FileListManager.getInstance().getFiles(context.getProject()));
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
    public void onError(@NotNull Throwable error) {
        log.error("Streaming error for context {}: {}", context.getId(), error.getMessage());

        // Deactivate activity handlers BEFORE hiding to prevent race condition
        if (conversationViewController != null) {
            conversationViewController.deactivateActivityHandlers();
        }

        // Hide the loading indicator in the WebView
        hideLoadingIndicator();

        StreamingException streamingError = new StreamingException(
            "Error during streaming response", error);
        PromptErrorHandler.handleException(context.getProject(), streamingError, context);
        onErrorCallback.accept(streamingError);
    }

    /**
     * Hides the "Thinking..." loading indicator and any agent activity in the WebView.
     * Delegates to ConversationViewController.hideLoadingIndicator().
     */
    private void hideLoadingIndicator() {
        if (conversationViewController == null) {
            return;
        }
        conversationViewController.hideLoadingIndicator(context.getId());
    }

    /**
     * Stops the streaming response handler.
     * Deactivates activity handlers before hiding the indicator to prevent
     * stale events from re-showing it (EDT queue race condition).
     */
    public void stop() {
        if (!isStopped) {
            isStopped = true;
            log.info("Stopping streaming handler for context {}, deactivating activity handlers", context.getId());

            // Clean up partial response from memory
            if (context.getAiMessage() != null) {
                ChatMemoryService.getInstance().removeLastMessage(context.getProject());
                log.debug("Cleaned up partial AI response from memory");
            }

            // Deactivate activity handlers BEFORE hiding to prevent race condition
            if (conversationViewController != null) {
                conversationViewController.deactivateActivityHandlers();
            }

            // Hide the loading indicator in the WebView
            hideLoadingIndicator();
        }
    }
}