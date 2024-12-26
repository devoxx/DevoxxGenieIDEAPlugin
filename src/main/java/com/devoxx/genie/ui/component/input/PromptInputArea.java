package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class PromptInputArea extends JPanel {
    @Getter
    private final CommandAutoCompleteTextField inputField;
    @Getter
    private final SearchOptionsPanel searchOptionsPanel;

    public PromptInputArea(Project project, @NotNull ResourceBundle resourceBundle) {
        super(new BorderLayout());

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
        searchOptionsPanel = new SearchOptionsPanel(project);
        inputAreaPanel.add(searchOptionsPanel, BorderLayout.NORTH);
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
