package com.devoxx.genie.service;

import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project-level service that allows external plugins to set prompt text
 * in the DevoxxGenie chat input area.
 */
public class ExternalPromptService {

    private final @NotNull Project project;
    private @Nullable PromptInputArea promptInputArea;
    private @Nullable String pendingText;

    public ExternalPromptService(@NotNull Project project) {
        this.project = project;
    }

    public static ExternalPromptService getInstance(@NotNull Project project) {
        return project.getService(ExternalPromptService.class);
    }

    public void setPromptInputArea(@Nullable PromptInputArea promptInputArea) {
        this.promptInputArea = promptInputArea;
        if (promptInputArea != null && pendingText != null) {
            String text = pendingText;
            pendingText = null;
            promptInputArea.setText(text);
            promptInputArea.requestInputFocus();
        }
    }

    /**
     * Sets prompt text in the DevoxxGenie chat input and opens the tool window.
     *
     * THIS METHOD IS NOT DEAD CODE. It is the public integration API called by
     * external plugins (e.g. SonarLint) via runtime reflection. Removing it
     * silently breaks the "Fix with DevoxxGenie" button.
     *
     * @see org.sonarlint.intellij.integration.DevoxxGenieBridge (SonarLint plugin caller)
     */
    public boolean setPromptText(@NotNull String text) {
        ToolWindowManager.getInstance(project).getToolWindow("DevoxxGenie").show(null);
        if (promptInputArea != null) {
            promptInputArea.setText(text);
            promptInputArea.requestInputFocus();
        } else {
            pendingText = text;
        }
        return true;
    }
}
