package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public abstract class AbstractPromptInputArea extends JPanel {
    protected final CommandAutoCompleteTextField inputField;
    protected final transient Project project;
    protected final transient ResourceBundle resourceBundle;

    public AbstractPromptInputArea(Project project, @NotNull ResourceBundle resourceBundle) {
        super(new BorderLayout());
        this.project = project;
        this.resourceBundle = resourceBundle;

        // Create main input area panel
        JPanel inputAreaPanel = new JPanel(new BorderLayout());
        inputField = new CommandAutoCompleteTextField(project);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));

        customizeInputField();

        // Add components to main panel
        inputAreaPanel.add(inputField, BorderLayout.CENTER);
        add(inputAreaPanel, BorderLayout.CENTER);
    }

    // Method to allow subclasses to customize the input field
    protected abstract void customizeInputField();

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
