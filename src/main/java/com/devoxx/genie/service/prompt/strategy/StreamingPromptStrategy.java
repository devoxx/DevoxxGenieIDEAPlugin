package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.AgentToolProviderFactory;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.prompt.error.ModelException;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.response.streaming.StreamingResponseHandler;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.panel.PromptOutputPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.TemplateVariableEscaper;
import com.intellij.openapi.project.Project;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Strategy for executing streaming prompts.
 */
@Slf4j
public class StreamingPromptStrategy extends AbstractPromptExecutionStrategy {

    private final AtomicReference<StreamingResponseHandler> currentHandler = new AtomicReference<>();
    private final AtomicReference<AgentLoopTracker> currentTracker = new AtomicReference<>();

    public StreamingPromptStrategy(Project project) {
        super(project);
    }

    /**
     * Constructor for full dependency injection, primarily used for testing.
     *
     * @param project The IntelliJ project
     * @param chatMemoryManager The chat memory manager
     * @param threadPoolManager The thread pool manager
     * @param messageCreationService The message creation service
     */
    protected StreamingPromptStrategy(
            @NotNull Project project,
            @NotNull ChatMemoryManager chatMemoryManager,
            @NotNull ThreadPoolManager threadPoolManager,
            @NotNull MessageCreationService messageCreationService) {
        super(project, chatMemoryManager, threadPoolManager, messageCreationService);
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

        StreamingChatModel streamingModel = context.getStreamingChatModel();
        if (streamingModel == null) {
            NotificationUtil.sendNotification(project,
                "Streaming model not available, please select another provider or turn off streaming mode.");
            ModelException error = new ModelException("Streaming model not available");
            resultTask.complete(PromptResult.failure(context, error));
            return;
        }

        prepareMemory(context);

        StreamingResponseHandler handler = createStreamingResponseHandler(context, panel, resultTask);
        currentHandler.set(handler);

        if (resultTask.isCancelled()) {
            handler.stop();
            return;
        }

        threadPoolManager.getPromptExecutionPool().execute(
            () -> executeStreamingInBackground(context, streamingModel, handler));

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
     * Creates the streaming response handler, using a test handler when available (test environments)
     * or the standard handler otherwise.
     */
    private StreamingResponseHandler createStreamingResponseHandler(
            @NotNull ChatMessageContext context,
            @NotNull PromptOutputPanel panel,
            @NotNull PromptTask<PromptResult> resultTask) {

        Consumer<ChatResponse> onComplete = response -> {
            log.debug("Streaming completed successfully for context: {}", context.getId());
            resultTask.complete(PromptResult.success(context));
        };
        Consumer<Throwable> onError = error -> {
            log.error("Streaming error for context {}: {}", context.getId(), error.getMessage());
            resultTask.completeExceptionally(error);
        };

        try {
            Class<?> testHandlerClass = Class.forName(
                "com.devoxx.genie.service.prompt.response.streaming.TestStreamingResponseHandler");
            return (StreamingResponseHandler) testHandlerClass
                .getConstructor(ChatMessageContext.class, Consumer.class, Consumer.class)
                .newInstance(context, onComplete, onError);
        } catch (ClassNotFoundException e) {
            return new StreamingResponseHandler(
                context, panel.getConversationPanel().viewController, onComplete, onError);
        } catch (Exception e) {
            log.error("Failed to create test handler, viewController is null!", e);
            throw new IllegalStateException("ConversationViewController is null in the panel", e);
        }
    }

    /**
     * Executes the streaming chat in a background thread, handling both multimodal
     * and standard (AiServices) cases.
     */
    private void executeStreamingInBackground(
            @NotNull ChatMessageContext context,
            @NotNull StreamingChatModel streamingModel,
            @NotNull StreamingResponseHandler handler) {
        try {
            ChatMemory chatMemory = chatMemoryManager.getChatMemory(project.getLocationHash());

            if (ChatMessageContextUtil.hasMultimodalContent(context)) {
                log.info("Multimodal content detected — using direct streaming model call (bypassing AiServices)");
                chatMemory.add(context.getUserMessage());
                streamingModel.chat(chatMemory.messages(), handler);
                return;
            }

            Assistant assistant = buildAssistant(context, streamingModel, chatMemory);
            String cleanText = TemplateVariableEscaper.escape(context.getUserMessage().singleText());

            assistant.chat(cleanText)
                .onPartialResponse(handler::onPartialResponse)
                .onToolExecuted(this::logToolExecution)
                .onCompleteResponse(handler::onCompleteResponse)
                .onError(handler::onError)
                .start();

        } catch (Exception e) {
            log.error("Error in streaming prompt execution", e);
            handler.onError(e);
        }
    }

    /**
     * Resolves the tool provider: tries agent mode first, falls back to MCP-only.
     * Also sets file references on the context when a provider is available.
     */
    private ToolProvider resolveToolProvider(@NotNull ChatMessageContext context) {
        ToolProvider toolProvider = AgentToolProviderFactory.createToolProvider(project);
        if (toolProvider instanceof AgentLoopTracker tracker) {
            currentTracker.set(tracker);
        }
        if (toolProvider == null) {
            toolProvider = MCPExecutionService.getInstance().createMCPToolProvider(project);
        }
        if (toolProvider != null) {
            log.debug("Tool provider created for streaming prompt");
            if (!FileListManager.getInstance().isEmpty(project)) {
                context.setFileReferences(FileListManager.getInstance().getFiles(project));
            }
        }
        return toolProvider;
    }

    /**
     * Builds the AiServices assistant with or without a tool provider.
     */
    private Assistant buildAssistant(
            @NotNull ChatMessageContext context,
            @NotNull StreamingChatModel streamingModel,
            @NotNull ChatMemory chatMemory) {

        ToolProvider toolProvider = resolveToolProvider(context);

        var builder = AiServices.builder(Assistant.class)
            .streamingChatModel(streamingModel)
            .chatMemoryProvider(memoryId -> chatMemory);

        if (toolProvider != null) {
            builder.toolProvider(toolProvider);
        }

        return builder.build();
    }

    private void logToolExecution(@NotNull ToolExecution toolExecution) {
        String result = toolExecution.result();
        String resultPreview = result != null ? result.substring(0, Math.min(100, result.length())) : "null";
        log.debug("Tool executed: {} -> {}", toolExecution.request().name(), resultPreview);
    }

    /**
     * Cancel the streaming execution.
     * Stops the response handler AND cancels the agent loop tracker
     * so tool executors stop immediately.
     */
    @Override
    public void cancel() {
        log.info("Cancelling streaming strategy");
        // Cancel the agent loop tracker first so tool executors stop
        AgentLoopTracker tracker = currentTracker.getAndSet(null);
        if (tracker != null) {
            tracker.cancel();
        }

        StreamingResponseHandler handler = currentHandler.getAndSet(null);
        if (handler != null) {
            handler.stop();
        }
    }

    interface Assistant {
        TokenStream chat(String userMessage);
    }
}
