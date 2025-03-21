package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.error.StreamingException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.memory.ChatMemoryService;
import com.devoxx.genie.service.prompt.streaming.StreamingResponseHandler;
import com.devoxx.genie.service.prompt.threading.PromptTaskTracker;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy for executing streaming prompts.
 */
@Slf4j
public class StreamingPromptStrategy implements PromptExecutionStrategy {

    private final ChatMemoryManager chatMemoryManager;
    private final ThreadPoolManager threadPoolManager;
    private final PromptTaskTracker taskTracker;
    private final Project project;
    private final String taskId;
    private StreamingResponseHandler currentStreamingHandler;

    public StreamingPromptStrategy(Project project) {
        this.project = project;
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
        this.taskTracker = PromptTaskTracker.getInstance();
        this.taskId = project.getLocationHash() + "-streaming-" + System.currentTimeMillis();
    }

    /**
     * Execute the prompt using streaming approach.
     */
    @Override
    public CompletableFuture<Void> execute(@NotNull ChatMessageContext chatMessageContext,
                                        @NotNull PromptOutputPanel promptOutputPanel) {
                                        
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        
        StreamingChatLanguageModel streamingModel = chatMessageContext.getStreamingChatLanguageModel();
        if (streamingModel == null) {
            NotificationUtil.sendNotification(chatMessageContext.getProject(),
                "Streaming model not available, please select another provider or turn off streaming mode.");
            resultFuture.complete(null);
            return resultFuture;
        }

        // Prepare memory and add user prompt
        chatMemoryManager.prepareMemory(chatMessageContext);
        chatMemoryManager.addUserMessage(chatMessageContext);
        
        // Display the user prompt
        promptOutputPanel.addUserPrompt(chatMessageContext);

        // Create cancellable task
        PromptTaskTracker.CancellableTask task = () -> {
            if (currentStreamingHandler != null) {
                currentStreamingHandler.stop();
                currentStreamingHandler = null;
            }
            if (!resultFuture.isDone()) {
                resultFuture.completeExceptionally(new InterruptedException("Streaming operation cancelled"));
            }
        };
        
        // Register task for tracking
        taskTracker.registerTask(project, taskId, task);

        // Create the streaming handler that will process chunks of response
        currentStreamingHandler = new StreamingResponseHandler(chatMessageContext, promptOutputPanel, () -> {
            resultFuture.complete(null);
            taskTracker.taskCompleted(project, taskId);
        });

        // Execute streaming using thread pool
        CompletableFuture.runAsync(() -> {
            try {
                // Get all messages from memory
                List<ChatMessage> messages = ChatMemoryService.getInstance().getMessages(chatMessageContext.getProject());
                
                // Start streaming the response
                streamingModel.chat(messages, currentStreamingHandler);
            } catch (Exception e) {
                log.error("Error in streaming prompt execution", e);
                StreamingException streamingError = new StreamingException("Error during streaming execution", e);
                PromptErrorHandler.handleException(project, streamingError, chatMessageContext);
                resultFuture.completeExceptionally(streamingError);
                taskTracker.taskCompleted(project, taskId);
            }
        }, threadPoolManager.getPromptExecutionPool());
        
        return resultFuture;
    }

    /**
     * Cancel the streaming execution.
     */
    @Override
    public void cancel() {
        taskTracker.cancelTask(project, taskId);
    }
}
