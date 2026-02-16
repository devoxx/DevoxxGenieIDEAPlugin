package com.devoxx.genie.service;

import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project-level service that allows external plugins to set prompt text
 * in the DevoxxGenie chat input area.
 */
public class ExternalPromptService {

    private final Project project;
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
     * Sets the prompt text in the DevoxxGenie chat input and opens the tool window.
     * Does NOT auto-submit — the user reviews the prompt first.
     * If the tool window hasn't been initialized yet, the text is buffered and
     * applied once initialization completes.
     *
     * @param text the prompt text to set
     * @return true if the text was set or buffered successfully
     */
    public boolean setPromptText(@NotNull String text) {
        ToolWindowManager twm = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = twm.getToolWindow("DevoxxGenie");
        if (toolWindow != null) {
            toolWindow.show();
        }

        if (promptInputArea != null) {
            promptInputArea.setText(text);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (promptInputArea != null) {
                    promptInputArea.requestInputFocus();
                }
            });
            return true;
        }

        // Tool window is initializing asynchronously — buffer the text
        pendingText = text;
        return true;
    }
}
