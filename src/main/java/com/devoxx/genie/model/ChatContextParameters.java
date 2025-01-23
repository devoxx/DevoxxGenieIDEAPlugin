package com.devoxx.genie.model;

import com.devoxx.genie.chatmodel.ChatModelProvider;
import com.devoxx.genie.ui.EditorFileButtonManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public record ChatContextParameters(
        Project project,
        String userPromptText,
        LanguageModel languageModel,
        ChatModelProvider chatModelProvider,
        @NotNull String actionCommand,
        EditorFileButtonManager editorFileButtonManager,
        String projectContext,
        boolean isProjectContextAdded
) {}

