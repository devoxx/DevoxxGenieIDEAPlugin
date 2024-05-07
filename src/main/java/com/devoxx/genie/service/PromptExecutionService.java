package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.AiMessage;
import org.jetbrains.annotations.NotNull;

public interface PromptExecutionService {

    @NotNull
    static PromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(PromptExecutionService.class);
    }

    @NotNull
    AiMessage executeQuery(@NotNull ChatMessageContext chatMessageContext) throws IllegalAccessException;

    void clearChatMessages();
}
