package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.exception.ModelNotActiveException;
import com.devoxx.genie.service.exception.ProviderUnavailableException;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.ClipboardUtil;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.bedrock.BedrockChatModel;
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
public class PromptExecutionService {

    private static final Logger LOG = Logger.getInstance(PromptExecutionService.class);
    private final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    private CompletableFuture<ChatResponse> queryFuture = null;

    @Getter
    private boolean running = false;

    private final ReentrantLock queryLock = new ReentrantLock();

    @NotNull
    static PromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(PromptExecutionService.class);
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

            ChatMemoryService chatMemoryService = ChatMemoryService.getInstance();

            // Add System Message if ChatMemoryService is empty
            if (ChatMemoryService.getInstance().isEmpty(chatMessageContext.getProject())) {
                LOG.debug("ChatMemoryService is empty, adding a new SystemMessage");

                if (includeSystemMessage(chatMessageContext)) {
                    String systemPrompt = DevoxxGenieStateService.getInstance().getSystemPrompt() + Constant.MARKDOWN;

                    // Add MCP instructions to system prompt if MCP is enabled
                    if (MCPService.isMCPEnabled()) {
                        systemPrompt += "<MCP_INSTRUCTION>The project base directory is " + chatMessageContext.getProject().getBasePath() +
                                "\nMake sure to use this information for your MCP tooling calls\n" +
                                "</MCP_INSTRUCTION>";
                        MCPService.logDebug("Added MCP instructions to system prompt");
                    }

                    chatMemoryService.add(chatMessageContext.getProject(), SystemMessage.from(systemPrompt));
                }
            }

            // Add User message to context
            MessageCreationService.getInstance().addUserMessageToContext(chatMessageContext);
            // chatMemoryService.add(chatMessageContext.getProject(), chatMessageContext.getUserMessage());

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
                    LOG.error("Error occurred while processing chat message", throwable);
                    ErrorHandler.handleError(chatMessageContext.getProject(), throwable);
                    return null;
                });
        } finally {
            queryLock.unlock();
        }
        return queryFuture;
    }

    private boolean includeSystemMessage(@NotNull ChatMessageContext chatMessageContext) {
        // If the language model is OpenAI o1 model, do not include system message
        if (ChatMessageContextUtil.isOpenAIo1Model(chatMessageContext.getLanguageModel())) {
            return false;
        }

        // If Bedrock Mistral AI model is selected, do not include system message
        if (chatMessageContext.getChatLanguageModel() instanceof BedrockChatModel bedrockChatModel) {
            // TODO Test if this refactoring still works because BedrockMistralChatModel is deprecated
            return bedrockChatModel.provider().name().startsWith("mistral.");
        }

        return true;
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

    private static @NotNull ChatResponse processChatMessage(ChatMessageContext chatMessageContext) {
        try {
            ChatLanguageModel chatLanguageModel = chatMessageContext.getChatLanguageModel();
            List<ChatMessage> messages = ChatMemoryService.getInstance().messages(chatMessageContext.getProject());

            // Get MCP tool provider if enabled
            ToolProvider mcpToolProvider = null;
            if (MCPService.isMCPEnabled()) {
                MCPService.logDebug("MCP is enabled, creating MCP tool provider");
                // Use project-specific tool provider with filesystem access
                mcpToolProvider = MCPExecutionService.getInstance().createMCPToolProvider();
                if (mcpToolProvider != null) {
                    MCPService.logDebug("Successfully created MCP tool provider with filesystem access");
                } else {
                    NotificationUtil.sendNotification(chatMessageContext.getProject(), "MCP is enabled, but no MCP tool provider could be created");
                }
            }

            ClipboardUtil.copyToClipboard(messages.toString());

            // ChatResponse chatResponse = chatLanguageModel.chat(messages);
            ChatMemoryService chatMemoryService = ChatMemoryService.getInstance();
            ChatMemory chatMemoryProvider = chatMemoryService.get(chatMessageContext.getProject().getLocationHash());

            // Build the AI service with or without MCP
            Bot bot;
            if (mcpToolProvider != null) {
                MCPService.logDebug( "Using MCP tool provider");
                // With MCP tool provider
                bot = AiServices.builder(Bot.class)
                        .chatLanguageModel(chatLanguageModel)
                        .chatMemory(chatMemoryProvider)
                        .toolProvider(mcpToolProvider)
                        .build();
            } else {
                log.debug("NOT USING MCP!");
                // Without MCP tool provider
                bot = AiServices.builder(Bot.class)
                        .chatLanguageModel(chatLanguageModel)
                        .chatMemory(chatMemoryProvider)
                        .build();
            }

            String query = chatMessageContext.getUserMessage().singleText();
            String queryResponse = bot.chat(query);

            return ChatResponse.builder()
                    .aiMessage(AiMessage.aiMessage(queryResponse))
                    .build();

        } catch (Exception e) {
            log.error(e.getMessage());
            if (chatMessageContext.getLanguageModel().getProvider().equals(ModelProvider.Jan)) {
                throw new ModelNotActiveException("Selected Jan model is not active. Download and make it active or add API Key in Jan settings.");
            }
            ChatMemoryService.getInstance().removeLast(chatMessageContext.getProject());
            throw new ProviderUnavailableException(e.getMessage());
        }
    }

    interface Bot {
        String chat(String message);
    }
}
