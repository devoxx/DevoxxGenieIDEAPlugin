package com.devoxx.genie.service;

import com.devoxx.genie.model.request.CompletionResult;
import com.devoxx.genie.model.request.PromptContext;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public interface PromptExecutionService {

    @NotNull
    static PromptExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(PromptExecutionService.class);
    }

    boolean isRunning();

    @NotNull
    CompletionResult executeQuery(@NotNull PromptContext promptContext) throws IllegalAccessException;
}
