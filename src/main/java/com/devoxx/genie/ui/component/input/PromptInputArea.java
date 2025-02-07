package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.devoxx.genie.ui.listener.ShortcutChangeListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import com.intellij.openapi.util.SystemInfo;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

@Getter
public class PromptInputArea extends JPanel implements ShortcutChangeListener {
    private final CommandAutoCompleteTextField inputField;
    private final SearchOptionsPanel searchOptionsPanel;
    private final ResourceBundle resourceBundle;

    public PromptInputArea(Project project, @NotNull ResourceBundle resourceBundle) {
        super(new BorderLayout());

        this.resourceBundle = resourceBundle;

        // Create main input area panel
        JPanel inputAreaPanel = new JPanel(new BorderLayout());
        inputField = new CommandAutoCompleteTextField(project);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addFocusListener(new PromptInputFocusListener(inputField));

        String submitShortcut;
        if (SystemInfo.isWindows) {
            submitShortcut = DevoxxGenieStateService.getInstance().getSubmitShortcutWindows();
        } else if (SystemInfo.isLinux) {
            submitShortcut = DevoxxGenieStateService.getInstance().getSubmitShortcutLinux();
        } else {
            submitShortcut = DevoxxGenieStateService.getInstance().getSubmitShortcutMac();
        }

        setPlaceholderWithKeyboardShortcut(submitShortcut);

        // Support DnD for images in input text area
        new ImagePreviewHandler(project, inputField);

        inputField.setRows(3);

        // Add components to main panel
        searchOptionsPanel = new SearchOptionsPanel(project);
        inputAreaPanel.add(searchOptionsPanel, BorderLayout.NORTH);
        inputAreaPanel.add(inputField, BorderLayout.CENTER);

        add(inputAreaPanel, BorderLayout.CENTER);

        MessageBusUtil.subscribe(project.getMessageBus().connect(),
                AppTopics.SHORTCUT_CHANGED_TOPIC, this);
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

    private void setPlaceholderWithKeyboardShortcut(String shortcut) {

        // Clean up the shortcut text
        shortcut = shortcut.replace("pressed", "+")
                .replace("meta", "command");

        // Format the shortcut text
        String[] parts = shortcut.split(" ");
        String formattedShortcut = String.join(" + ", parts)
                .substring(0, 1).toUpperCase()
                + shortcut.substring(1);

        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated())) {
            inputField.setPlaceholder(formattedShortcut + " " +
                    resourceBundle.getString("rag.prompt.placeholder"));
        } else {
            inputField.setPlaceholder(formattedShortcut + " " +
                    resourceBundle.getString("prompt.placeholder"));
        }
    }

    @Override
    public void onShortcutChanged(String shortcut) {
        if (shortcut == null || shortcut.isEmpty()) {
            return;
        }

        setPlaceholderWithKeyboardShortcut(shortcut);
    }
}
