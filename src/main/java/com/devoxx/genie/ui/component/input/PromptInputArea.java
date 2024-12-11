package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

public class PromptInputArea extends JPanel {
    private final CommandAutoCompleteTextField inputField;

    public PromptInputArea(@NotNull ResourceBundle resourceBundle, Project project) {
        super(new BorderLayout());

        // Create main input area panel
        JPanel inputAreaPanel = new JPanel(new BorderLayout());
        inputField = new CommandAutoCompleteTextField(project);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));
        inputField.setPlaceholder(resourceBundle.getString("prompt.placeholder"));

        inputField.setRows(3);

        // Add components to main panel
        inputAreaPanel.add(new SearchOptionsPanel(), BorderLayout.NORTH);
        inputAreaPanel.add(inputField, BorderLayout.CENTER);

        add(inputAreaPanel, BorderLayout.CENTER);
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
