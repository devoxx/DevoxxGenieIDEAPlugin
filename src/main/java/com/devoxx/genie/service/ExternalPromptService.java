package com.devoxx.genie.service;

import com.devoxx.genie.ui.component.input.PromptInputArea;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.devoxx.genie.ui.window.DevoxxGenieToolWindowContent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
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
     * Routes to the currently selected tab's prompt input area.
     *
     * THIS METHOD IS NOT DEAD CODE. It is the public integration API called by
     * external plugins (e.g. SonarLint) via runtime reflection. Removing it
     * silently breaks the "Fix with DevoxxGenie" button.
     *
     * @see org.sonarlint.intellij.integration.DevoxxGenieBridge (SonarLint plugin caller)
     */
    public boolean setPromptText(@NotNull String text) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DevoxxGenie");
        if (toolWindow != null) {
            toolWindow.show(null);

            // Route to the active/selected tab
            Content selectedContent = toolWindow.getContentManager().getSelectedContent();
            if (selectedContent != null) {
                DevoxxGenieToolWindowContent twc = ConversationTabRegistry.getInstance().getToolWindowContent(selectedContent);
                if (twc != null && twc.getSubmitPanel() != null) {
                    PromptInputArea activeInput = twc.getSubmitPanel().getPromptInputArea();
                    activeInput.setText(text);
                    activeInput.requestInputFocus();
                    return true;
                }
            }
        }

        // Fallback to the legacy single-tab approach
        if (promptInputArea != null) {
            promptInputArea.setText(text);
            promptInputArea.requestInputFocus();
        } else {
            pendingText = text;
        }
        return true;
    }
}
