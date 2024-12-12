package com.devoxx.genie.service;

import com.devoxx.genie.error.ErrorHandler;
import com.devoxx.genie.model.Constant;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.exception.ModelNotActiveException;
import com.devoxx.genie.service.exception.ProviderUnavailableException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.ChatMessageContextUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class PromptExecutionService {

    private static final Logger LOG = Logger.getInstance(PromptExecutionService.class);
    private final ExecutorService queryExecutor = Executors.newSingleThreadExecutor();
    private CompletableFuture<Response<AiMessage>> queryFuture = null;

    @Getter
    private final AtomicBoolean running = new AtomicBoolean(false);
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
    public @NotNull CompletableFuture<Response<AiMessage>> executeQuery(@NotNull ChatMessageContext chatMessageContext) {
        LOG.info("Execute query : " + chatMessageContext);

        queryLock.lock();
        try {
            if (running.get()) {
                LOG.info("Another query is already running. Cancelling it.");
                cancelCurrentQuery();
            }

            running.set(true);

            MessageCreationService messageCreationService = MessageCreationService.getInstance();

            if (ChatMemoryService.getInstance().isEmpty(chatMessageContext.getProject())) {
                LOG.info("ChatMemoryService is empty, adding a new SystemMessage");

                if (!ChatMessageContextUtil.isOpenAIo1Model(chatMessageContext.getLanguageModel())) {
                    ChatMemoryService
                            .getInstance()
                            .add(chatMessageContext.getProject(),
                                    new SystemMessage(DevoxxGenieStateService.getInstance().getSystemPrompt() + Constant.MARKDOWN)
                            );
                }
            }

            UserMessage userMessage = messageCreationService.createUserMessage(chatMessageContext);
            LOG.info("Created UserMessage: " + userMessage);

            ChatMemoryService.getInstance().add(chatMessageContext.getProject(), userMessage);

            long startTime = System.currentTimeMillis();

            queryFuture = CompletableFuture
                    .supplyAsync(() -> {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new CancellationException("Query was cancelled before execution.");
                        }
                        return processChatMessage(chatMessageContext);
                    }, queryExecutor)
                    .orTimeout(
                            chatMessageContext.getTimeout() == null ? 60 : chatMessageContext.getTimeout(), TimeUnit.SECONDS)
                    .thenApply(result -> {
                        chatMessageContext.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                        return result;
                    })
                    .whenComplete((r, t) -> {
                        queryLock.lock();
                        try {
                            running.set(false);
                        } finally {
                            queryLock.unlock();
                        }
                    });
        } finally {
            queryLock.unlock();
        }
        return queryFuture;
    }

    /**
     * Cancels the current query if it's running.
     */
    public void cancelCurrentQuery() {
        queryLock.lock();
        try {
            if (queryFuture != null) {
                queryFuture.cancel(true);
                queryFuture = null;
            }
        } finally {
            queryLock.unlock();
        }
    }

    /**
     * Process the chat message.
     *
     * @param chatMessageContext the chat message context
     * @return the AI response
     */
    private @NotNull Response<AiMessage> processChatMessage(ChatMessageContext chatMessageContext) {
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Query was cancelled during execution.");
            }

            ChatLanguageModel chatLanguageModel = chatMessageContext.getChatLanguageModel();
            Response<AiMessage> response =
                    chatLanguageModel
                            .generate(ChatMemoryService.getInstance().messages(chatMessageContext.getProject()));
            ChatMemoryService.getInstance().add(chatMessageContext.getProject(), response.content());
            return response;
        } catch (CancellationException e) {
            throw e; // Re-throw cancellation exceptions
        } catch (Exception e) {
            if (chatMessageContext.getLanguageModel().getProvider().equals(ModelProvider.Jan)) {
                throw new ModelNotActiveException("Selected Jan model is not active. Download and make it active or add API Key in Jan settings.");
            }
            ChatMemoryService.getInstance().removeLast(chatMessageContext.getProject());
            throw new ProviderUnavailableException(e.getMessage());
        }
    }
}
