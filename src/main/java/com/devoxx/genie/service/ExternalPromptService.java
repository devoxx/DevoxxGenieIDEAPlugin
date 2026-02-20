package com.devoxx.genie.service;

import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project-level service that allows external plugins to set prompt text
 * in the DevoxxGenie chat input area.
 */
public class ExternalPromptService {

    private @Nullable String pendingText;

    public ExternalPromptService(@NotNull Project project) {
    }

    public static ExternalPromptService getInstance(@NotNull Project project) {
        return project.getService(ExternalPromptService.class);
    }

    public void setPromptInputArea(@Nullable PromptInputArea promptInputArea) {
        if (promptInputArea != null && pendingText != null) {
            String text = pendingText;
            pendingText = null;
            promptInputArea.setText(text);
            promptInputArea.requestInputFocus();
        }
    }
}
