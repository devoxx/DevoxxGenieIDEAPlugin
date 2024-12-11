package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.devoxx.genie.ui.listener.RAGStateListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class PromptInputArea extends JPanel {
    private final CommandAutoCompleteTextField inputField;
    private final transient Project project;
    private final transient ResourceBundle resourceBundle;

    public PromptInputArea(@NotNull ResourceBundle resourceBundle, Project project) {
        super(new BorderLayout());

        this.resourceBundle = resourceBundle;
        this.project = project;

        // Create main input area panel
        JPanel inputAreaPanel = new JPanel(new BorderLayout());
        inputField = new CommandAutoCompleteTextField(project);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));

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

    public String getText() {
        return inputField.getText();
    }

    public void setText(String text) {
        inputField.setText(text);
    }

    public void clear() {
        inputField.setText("");
    }

    @Override
    public void setEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
    }

    @Override
    public boolean requestFocusInWindow() {
        return inputField.requestFocusInWindow();
    }

    public void requestInputFocus() {
        ApplicationManager.getApplication().invokeLater(() -> {
            inputField.requestFocusInWindow();
            inputField.setCaretPosition(inputField.getText().length());
        });
    }
}
