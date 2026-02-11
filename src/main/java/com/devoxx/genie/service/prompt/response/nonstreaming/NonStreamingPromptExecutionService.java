package com.devoxx.genie.service.prompt.response.nonstreaming;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.service.agent.AgentLoopTracker;
import com.devoxx.genie.service.agent.AgentToolProviderFactory;
import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.service.prompt.error.ExecutionException;
import com.devoxx.genie.service.prompt.error.ModelException;
import com.devoxx.genie.service.prompt.error.PromptErrorHandler;
import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.devoxx.genie.service.prompt.threading.ThreadPoolManager;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.devoxx.genie.util.TemplateVariableEscaper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;

import dev.langchain4j.service.tool.ToolProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
    private final AtomicReference<AgentLoopTracker> currentTracker = new AtomicReference<>();

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
            );

        // Only apply a blanket timeout for simple (non-agent, non-MCP) prompts.
        // Agent/MCP conversations involve many HTTP round trips; each individual
        // request already has a timeout via the Langchain4j SDK.
        boolean isAgentOrMcp = Boolean.TRUE.equals(
                DevoxxGenieStateService.getInstance().getAgentModeEnabled())
                || MCPService.isMCPEnabled();
        if (!isAgentOrMcp) {
            queryFuture = queryFuture.orTimeout(
                    chatMessageContext.getTimeout() == null ? 60 : chatMessageContext.getTimeout(),
                    TimeUnit.SECONDS);
        }

        queryFuture = queryFuture
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
                
                // Add file references if any, similar to StreamingResponseHandler
                if (response != null && !FileListManager.getInstance().isEmpty(project)) {
                    log.debug("Adding file references for non-streaming response");
                    // Store the file references in the context for later use
                    chatMessageContext.setFileReferences(FileListManager.getInstance().getFiles(project));
                }
            });
        
        // Store the future for potential cancellation
        currentQueryFuture.set(queryFuture);
        
        return queryFuture;
    }

    /**
     * Cancel the currently executing query if one exists.
     */
    public void cancelExecutingQuery() {
        // Cancel the agent loop tracker so tool executors stop immediately
        AgentLoopTracker tracker = currentTracker.getAndSet(null);
        if (tracker != null) {
            tracker.cancel();
        }

        CompletableFuture<ChatResponse> future = currentQueryFuture.get();
        if (future != null && !future.isDone()) {
            future.cancel(true);
            if (MCPService.isMCPEnabled()) {
                MCPExecutionService.getInstance().clearClientCache();
            }
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
            ChatModel chatModel = chatMessageContext.getChatModel();

            String projectId = project.getLocationHash();

            ChatMemory chatMemory = chatMemoryManager.getChatMemory(projectId);

            // When images are present, bypass AiServices (which only supports text)
            // and call chatModel directly with the multimodal UserMessage
            if (ChatMessageContextUtil.hasMultimodalContent(chatMessageContext)) {
                log.info("Multimodal content detected — using direct chat model call (bypassing AiServices)");
                chatMemory.add(chatMessageContext.getUserMessage());
                List<ChatMessage> messages = chatMemory.messages();
                ChatResponse response = chatModel.chat(messages);
                chatMemory.add(response.aiMessage());
                return response;
            }

            Assistant assistant = buildAssistant(chatModel, chatMemory);

            // Try agent mode first, then fall back to MCP-only
            ToolProvider toolProvider = AgentToolProviderFactory.createToolProvider(project);
            if (toolProvider instanceof AgentLoopTracker tracker) {
                currentTracker.set(tracker);
            }
            if (toolProvider == null && MCPService.isMCPEnabled()) {
                toolProvider = MCPExecutionService.getInstance().createMCPToolProvider(project);
            }

            if (toolProvider != null) {
                log.debug("Tool provider created for non-streaming prompt");
                String basePath = project.getBasePath();
                assistant = AiServices.builder(Assistant.class)
                        .chatModel(chatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .systemMessageProvider(memoryId -> buildToolSystemPrompt(basePath))
                        .toolProvider(toolProvider)
                        .build();
            }

            String userMessage = chatMessageContext.getUserMessage().singleText();
            String cleanText = TemplateVariableEscaper.escape(userMessage);

            String queryResponse = assistant.chat(cleanText);

            return ChatResponse.builder()
                    .aiMessage(AiMessage.aiMessage(queryResponse))
                    .build();

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

    private static @NotNull String buildToolSystemPrompt(String projectBasePath) {
        StringBuilder sb = new StringBuilder(DevoxxGenieStateService.getInstance().getSystemPrompt());
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled()) && projectBasePath != null) {
            sb.append("\n<PROJECT_ROOT>").append(projectBasePath).append("</PROJECT_ROOT>")
              .append("\nAll file paths in tool calls are relative to this project root directory.\n");
        }
        if (MCPService.isMCPEnabled() && projectBasePath != null) {
            sb.append("<MCP_INSTRUCTION>The project base directory is ").append(projectBasePath)
              .append("\nMake sure to use this information for your MCP tooling calls\n</MCP_INSTRUCTION>");
        }
        return TemplateVariableEscaper.escape(sb.toString());
    }

    private static Assistant buildAssistant(ChatModel chatModel, ChatMemory chatMemory) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .systemMessageProvider(memoryId -> TemplateVariableEscaper.escape(DevoxxGenieStateService.getInstance().getSystemPrompt()))
                .build();
    }

    /**
     * The Code Assistant chat method
     */
    interface Assistant {
        String chat(String userMessage);
    }
}
