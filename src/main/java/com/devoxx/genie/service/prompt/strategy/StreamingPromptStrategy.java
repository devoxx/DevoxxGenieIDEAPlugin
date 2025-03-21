package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.streaming.StreamingResponseHandler;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy for executing streaming prompts.
 */
public class StreamingPromptStrategy implements PromptExecutionStrategy {

    private static final Logger LOG = Logger.getInstance(StreamingPromptStrategy.class);
    
    private final ChatMemoryManager chatMemoryManager;
    private StreamingResponseHandler currentStreamingHandler;

    public StreamingPromptStrategy() {
        this.chatMemoryManager = ChatMemoryManager.getInstance();
    }

    /**
     * Execute the prompt using streaming approach.
     */
    @Override
    public CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext,
                                        @NotNull PromptOutputPanel promptOutputPanel,
                                        @NotNull Runnable onComplete) {
                                        
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        
        StreamingChatLanguageModel streamingModel = chatMessageContext.getStreamingChatLanguageModel();
        if (streamingModel == null) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                "Streaming model not available, please select another provider or turn off streaming mode.");
            onComplete.run();
            resultFuture.complete(null);
            return resultFuture;
        }

        // Prepare memory and add user prompt
        chatMemoryManager.prepareMemory(chatMessageContext);
        chatMemoryManager.addUserMessage(chatMessageContext);
        
        // Display the user prompt
        promptOutputPanel.addUserPrompt(chatMessageContext);

        // Create the streaming handler that will process chunks of response
        currentStreamingHandler = new StreamingResponseHandler(chatMessageContext, promptOutputPanel, () -> {
            onComplete.run();
            resultFuture.complete(null);
        });

        try {
            // Get all messages from memory
            List<ChatMessage> messages = ChatMemoryService.getInstance().getMessages(chatMessageContext.getProject());
            
            // Start streaming the response
            streamingModel.chat(messages, currentStreamingHandler);
        } catch (Exception e) {
            LOG.error("Error in streaming prompt execution", e);
            resultFuture.completeExceptionally(e);
            onComplete.run();
        }
        
        return resultFuture;
    }

    /**
     * Cancel the streaming execution.
     */
    @Override
    public void cancel() {
        if (currentStreamingHandler != null) {
            currentStreamingHandler.stop();
            currentStreamingHandler = null;
        }
    }
}
