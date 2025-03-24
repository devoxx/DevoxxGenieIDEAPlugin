package com.devoxx.genie.service.prompt.nonstreaming;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.MessageCreationService;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.ModelException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ClipboardUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;

import dev.langchain4j.service.tool.ToolProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class NonStreamingPromptExecutionService {

    @Getter
    private volatile boolean running = false;
    
    private final ChatMemoryManager chatMemoryManager;
    private final ThreadPoolManager threadPoolManager;
    private final AtomicReference<CompletableFuture<ChatResponse>> currentQueryFuture = new AtomicReference<>();

    public NonStreamingPromptExecutionService() {
        this.chatMemoryManager = ChatMemoryManager.getInstance();
        this.threadPoolManager = ThreadPoolManager.getInstance();
    }

    @NotNull
    public static NonStreamingPromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(NonStreamingPromptExecutionService.class);
    }

    /**
     * Execute the query with the given language text pair and chat language model.
     *
     * @param chatMessageContext the chat message context
     * @return the response future
     */
    public @NotNull CompletableFuture<ChatResponse> executeQuery(@NotNull ChatMessageContext chatMessageContext) {
        log.debug("Execute query : {}", chatMessageContext);

        // Cancel any existing query
        cancelExecutingQuery();
        
        Project project = chatMessageContext.getProject();

        long startTime = System.currentTimeMillis();
        running = true;

        // Create new future for the current query
        CompletableFuture<ChatResponse> queryFuture = CompletableFuture
            .supplyAsync(
                () -> processChatMessage(chatMessageContext), 
                threadPoolManager.getPromptExecutionPool()
            )
            .orTimeout(
                chatMessageContext.getTimeout() == null ? 60 : chatMessageContext.getTimeout(), 
                TimeUnit.SECONDS
            )
            .thenApply(result -> {
                chatMessageContext.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                return result;
            })
            .exceptionally(throwable -> {
                // Ignore cancellation exceptions
                if (!(throwable instanceof CancellationException) &&
                    !(throwable.getCause() instanceof CancellationException)) {
                    // Create a specific execution exception and handle it with our standardized handler
                    ExecutionException executionError = new ExecutionException(
                        "Error occurred while processing chat message", throwable);
                    PromptErrorHandler.handleException(project, executionError, chatMessageContext);
                }
                // The PromptErrorHandler will handle appropriate recovery like removing failed exchanges
                return null;
            })
            .whenComplete((response, throwable) -> {
                // Always clear the current future reference when done
                currentQueryFuture.set(null);
                running = false;
            });
        
        // Store the future for potential cancellation
        currentQueryFuture.set(queryFuture);
        
        return queryFuture;
    }

    /**
     * Cancel the currently executing query if one exists.
     */
    public void cancelExecutingQuery() {
        CompletableFuture<ChatResponse> future = currentQueryFuture.get();
        if (future != null && !future.isDone()) {
            future.cancel(true);
            currentQueryFuture.set(null);
            running = false;
        }
    }

    /**
     * Process the chat message and generate a response.
     */
    private @NotNull ChatResponse processChatMessage(ChatMessageContext chatMessageContext) {
        try {
            Project project = chatMessageContext.getProject();
            ChatLanguageModel chatLanguageModel = chatMessageContext.getChatLanguageModel();

            // Get MCP tool provider if enabled
            ToolProvider mcpToolProvider = null;
            if (MCPService.isMCPEnabled()) {
                MCPService.logDebug("MCP is enabled, creating MCP tool provider");
                // Use project-specific tool provider with filesystem access
                mcpToolProvider = MCPExecutionService.getInstance().createMCPToolProvider();
                if (mcpToolProvider != null) {
                    MCPService.logDebug("Successfully created MCP tool provider with filesystem access");
                } else {
                    NotificationUtil.sendNotification(project, "MCP is enabled, but no MCP tool provider could be created");
                }
            }

            // Get the ChatMemory from the ChatMemoryManager
            ChatMemory chatMemory = chatMemoryManager.getChatMemory(project.getLocationHash());

            // For debugging - copy message list to clipboard
            ClipboardUtil.copyToClipboard(chatMemory.messages().toString());

            // Build the AI service with or without MCP
            Assistant assistant;
            if (mcpToolProvider != null) {
                MCPService.logDebug("Using MCP tool provider");
                // With MCP tool provider
                assistant = AiServices.builder(Assistant.class)
                        .chatLanguageModel(chatLanguageModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .systemMessageProvider(memoryId -> DevoxxGenieStateService.getInstance().getSystemPrompt())
                        .toolProvider(mcpToolProvider)
                        .build();
            } else {
                log.debug("NOT USING MCP!");
                // Without MCP tool provider
                assistant = AiServices.builder(Assistant.class)
                        .chatLanguageModel(chatLanguageModel)
                        .chatMemory(chatMemory)
                        .systemMessageProvider(memoryId -> DevoxxGenieStateService.getInstance().getSystemPrompt())
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .build();
            }

            // Add extra user message context
            MessageCreationService.getInstance().addUserMessageToContext(chatMessageContext);

            if (chatMessageContext.getUserMessage().hasSingleText()) {
                String queryResponse = assistant.chat(chatMessageContext.getUserMessage().singleText());
                return ChatResponse.builder()
                        .aiMessage(AiMessage.aiMessage(queryResponse))
                        .build();
            } else {
                return chatLanguageModel.chat(chatMessageContext.getUserMessage());
            }

        } catch (Exception e) {
            // Thread interruption is likely from cancellation, so we handle it specially
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Query was cancelled");
            }
            
            log.error(e.getMessage());
            if (chatMessageContext.getLanguageModel().getProvider().equals(ModelProvider.Jan)) {
                // Use our own ModelException instead of the generic ModelNotActiveException
                throw new ModelException(
                    "Selected Jan model is not active. Download and make it active or add API Key in Jan settings.", e);
            }

            // Let the ChatMemoryManager handle removing the last message on error
            chatMemoryManager.removeLastMessage(chatMessageContext.getProject());

            // Use our own ModelException instead of the generic ProviderUnavailableException
            throw new ModelException("Provider unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * The Code Assistant chat method
     */
    interface Assistant {
        String chat(String userMessage);
    }
}
