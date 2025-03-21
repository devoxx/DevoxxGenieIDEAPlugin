package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.ModelException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.streaming.StreamingResponseHandler;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
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
public class StreamingPromptStrategy implements PromptExecutionStrategy {

    private final ChatMemoryManager chatMemoryManager;
    private final ThreadPoolManager threadPoolManager;
    private final Project project;
    private final AtomicReference<StreamingResponseHandler> currentHandler = new AtomicReference<>();

    public StreamingPromptStrategy(Project project) {
        this.project = project;
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
    }

    /**
     * Execute the prompt using streaming approach.
     */
    @Override
    public PromptTask<PromptResult> execute(@NotNull ChatMessageContext context,
                                          @NotNull PromptOutputPanel panel) {
        log.debug("Executing streaming prompt for context: {}", context.getId());
        
        // Create a self-managed prompt task
        PromptTask<PromptResult> resultTask = new PromptTask<>(project);
        
        StreamingChatLanguageModel streamingModel = context.getStreamingChatLanguageModel();
        if (streamingModel == null) {
            NotificationUtil.sendNotification(project, 
                "Streaming model not available, please select another provider or turn off streaming mode.");
            
            ModelException error = new ModelException("Streaming model not available");
            resultTask.complete(PromptResult.failure(context, error));
            return resultTask;
        }

        // Prepare memory and add user prompt
        chatMemoryManager.prepareMemory(context);
        chatMemoryManager.addUserMessage(context);
        
        // Display the user prompt
        panel.addUserPrompt(context);

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
            return resultTask;
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
        
        // Handle cancellation
        resultTask.whenComplete((result, error) -> {
            if (resultTask.isCancelled()) {
                StreamingResponseHandler h = currentHandler.getAndSet(null);
                if (h != null) {
                    h.stop();
                }
            }
        });
        
        return resultTask;
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
