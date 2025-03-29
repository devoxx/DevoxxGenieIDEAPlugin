package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.ModelException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.streaming.StreamingResponseHandler;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Strategy for executing streaming prompts.
 */
@Slf4j
public class StreamingPromptStrategy extends AbstractPromptExecutionStrategy {

    private final AtomicReference<StreamingResponseHandler> currentHandler = new AtomicReference<>();

    public StreamingPromptStrategy(Project project) {
        super(project);
    }

    @Override
    protected String getStrategyName() {
        return "streaming prompt";
    }
    
    @Override
    protected void executeStrategySpecific(
            @NotNull ChatMessageContext context,
            @NotNull PromptOutputPanel panel,
            @NotNull PromptTask<PromptResult> resultTask) {
        
        StreamingChatLanguageModel streamingModel = context.getStreamingChatLanguageModel();
        if (streamingModel == null) {
            NotificationUtil.sendNotification(project, 
                "Streaming model not available, please select another provider or turn off streaming mode.");
            
            ModelException error = new ModelException("Streaming model not available");
            resultTask.complete(PromptResult.failure(context, error));
            return;
        }

        // Prepare memory and add user message
        prepareMemory(context);
        chatMemoryManager.addUserMessage(context);

        // Create the streaming handler that will process chunks of response
        StreamingResponseHandler handler = new StreamingResponseHandler(
            context, 
            panel,
            // On complete callback
            (ChatResponse response) -> {
                log.debug("Streaming completed successfully for context: {}", context.getId());
                resultTask.complete(PromptResult.success(context));
            },
            // On error callback
            (Throwable error) -> {
                log.error("Streaming error for context {}: {}", context.getId(), error.getMessage());
                resultTask.completeExceptionally(error);
            }
        );
        
        // Store reference for potential cancellation
        currentHandler.set(handler);
        
        // Check for early cancellation
        if (resultTask.isCancelled()) {
            handler.stop();
            return;
        }

        // Execute streaming using thread pool
        threadPoolManager.getPromptExecutionPool().execute(() -> {
            try {
                // Get all messages from memory
                List<ChatMessage> messages = ChatMemoryService.getInstance().getMessages(context.getProject());
                
                // Start streaming the response (this will execute until completion or error)
                streamingModel.chat(messages, handler);
            } catch (Exception e) {
                log.error("Error in streaming prompt execution", e);
                handler.onError(e);
            }
        });
        
        // Add additional cancellation handling
        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                StreamingResponseHandler h = currentHandler.getAndSet(null);
                if (h != null) {
                    h.stop();
                }
            }
        });
    }

    /**
     * Cancel the streaming execution.
     */
    @Override
    public void cancel() {
        StreamingResponseHandler handler = currentHandler.getAndSet(null);
        if (handler != null) {
            handler.stop();
        }
    }
}
