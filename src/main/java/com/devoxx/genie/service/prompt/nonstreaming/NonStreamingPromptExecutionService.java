package com.devoxx.genie.service.prompt.nonstreaming;

import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.MemoryException;
import com.devoxx.genie.service.prompt.error.ModelException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.exception.ModelNotActiveException;
import com.devoxx.genie.service.exception.ProviderUnavailableException;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ClipboardUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class NonStreamingPromptExecutionService {

    private static final Logger LOG = Logger.getInstance(NonStreamingPromptExecutionService.class);
    private final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    private CompletableFuture<ChatResponse> queryFuture = null;

    @Getter
    private boolean running = false;

    private final ReentrantLock queryLock = new ReentrantLock();
    private final ChatMemoryManager chatMemoryManager;

    public NonStreamingPromptExecutionService() {
        this.chatMemoryManager = ChatMemoryManager.getInstance();
    }

    @NotNull
    public static NonStreamingPromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(NonStreamingPromptExecutionService.class);
    }

    /**
     * Execute the query with the given language text pair and chat language model.
     *
     * @param chatMessageContext the chat message context
     * @return the response
     */
    public @NotNull CompletableFuture<ChatResponse> executeQuery(@NotNull ChatMessageContext chatMessageContext) {
        LOG.debug("Execute query : " + chatMessageContext);

        queryLock.lock();
        try {
            if (isCanceled()) return CompletableFuture.completedFuture(null);

            Project project = chatMessageContext.getProject();
            
            // Let the ChatMemoryManager handle memory preparation
            chatMemoryManager.prepareMemory(chatMessageContext);
            
            // Let the ChatMemoryManager handle adding the user message
            chatMemoryManager.addUserMessage(chatMessageContext);

            long startTime = System.currentTimeMillis();

            queryFuture = CompletableFuture
                .supplyAsync(() -> processChatMessage(chatMessageContext), queryExecutor)
                .orTimeout(
                    chatMessageContext.getTimeout() == null ? 60 : chatMessageContext.getTimeout(), TimeUnit.SECONDS)
                .thenApply(result -> {
                    chatMessageContext.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                    return result;
                })
                .exceptionally(throwable -> {
                    // Create a specific execution exception and handle it with our standardized handler
                    ExecutionException executionError = new ExecutionException(
                        "Error occurred while processing chat message", throwable);
                    PromptErrorHandler.handleException(project, executionError, chatMessageContext);
                    // The PromptErrorHandler will handle appropriate recovery like removing failed exchanges
                    return null;
                });
        } finally {
            queryLock.unlock();
        }
        return queryFuture;
    }

    /**
     * If the future task is not null this means we need to cancel it
     *
     * @return true if the task is canceled
     */
    private boolean isCanceled() {
        if (queryFuture != null && !queryFuture.isDone()) {
            queryFuture.cancel(true);
            running = false;
            return true;
        }
        running = true;
        return false;
    }

    private @NotNull ChatResponse processChatMessage(ChatMessageContext chatMessageContext) {
        try {
            Project project = chatMessageContext.getProject();
            ChatLanguageModel chatLanguageModel = chatMessageContext.getChatLanguageModel();
            List<ChatMessage> messages = chatMemoryManager.getMessages(project);

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

            ClipboardUtil.copyToClipboard(messages.toString());

            // Get the ChatMemory from the ChatMemoryManager
            ChatMemory chatMemory = chatMemoryManager.getChatMemory(project.getLocationHash());

            // Build the AI service with or without MCP
            Assistant assistant;
            if (mcpToolProvider != null) {
                MCPService.logDebug("Using MCP tool provider");
                // With MCP tool provider
                assistant = AiServices.builder(Assistant.class)
                        .chatLanguageModel(chatLanguageModel)
                        .chatMemory(chatMemory)
                        .toolProvider(mcpToolProvider)
                        .build();
            } else {
                log.debug("NOT USING MCP!");
                // Without MCP tool provider
                assistant = AiServices.builder(Assistant.class)
                        .chatLanguageModel(chatLanguageModel)
                        .chatMemory(chatMemory)
                        .build();
            }

            String query = chatMessageContext.getUserMessage().singleText();
            String queryResponse = assistant.chat(query);

            return ChatResponse.builder()
                    .aiMessage(AiMessage.aiMessage(queryResponse))
                    .build();

        } catch (Exception e) {
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

    interface Assistant {
        String chat(String message);
    }
}
