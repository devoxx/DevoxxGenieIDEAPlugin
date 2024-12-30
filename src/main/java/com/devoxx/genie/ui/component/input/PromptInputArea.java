// PromptInputArea.java
package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.RAGStateListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ResourceBundle;

public class PromptInputArea extends AbstractPromptInputArea {

    public PromptInputArea(Project project, @NotNull ResourceBundle resourceBundle) {
        super(project, resourceBundle);
        this.subscribeToRagStateChanges();
    }

    @Override
    protected void customizeInputField() {
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated())) {
            inputField.setPlaceholder(resourceBundle.getString("rag.prompt.placeholder"));
        } else {
            inputField.setPlaceholder(resourceBundle.getString("prompt.placeholder"));
        }
        inputField.setRows(3);
        // Add components to main panel
        inputAreaPanel.add(new SearchOptionsPanel(project), BorderLayout.NORTH);
        inputAreaPanel.add(inputField, BorderLayout.CENTER);

        add(inputAreaPanel, BorderLayout.CENTER);

        this.subscribeToRagStateChanges();
    }


    public void subscribeToRagStateChanges() {
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(AppTopics.RAG_ACTIVATED_CHANGED_TOPIC,
                (RAGStateListener) isEnabled ->
                        inputField.setPlaceholder(
                                isEnabled ? resourceBundle.getString("rag.prompt.placeholder") : resourceBundle.getString("prompt.placeholder")
                        ));
    }
}
