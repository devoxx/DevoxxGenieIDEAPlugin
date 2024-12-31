// InlineChatPromptInputArea.java
package com.devoxx.genie.ui.component.input;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

public class InlineChatPromptInputArea extends AbstractPromptInputArea {

    public InlineChatPromptInputArea(Project project, @NotNull ResourceBundle resourceBundle) {
        super(project, resourceBundle);
    }

    @Override
    protected void customizeInputField() {
        // No specific customization for InlineChatPromptInputArea
    }
}
