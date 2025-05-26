package com.devoxx.genie.ui.component.input;

import com.devoxx.genie.ui.listener.NewlineShortcutChangeListener;
import com.devoxx.genie.ui.listener.PromptInputFocusListener;
import com.devoxx.genie.ui.listener.RAGStateListener;
import com.devoxx.genie.ui.listener.ShortcutChangeListener;
import com.devoxx.genie.ui.panel.SearchOptionsPanel;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.MessageBusUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

import static com.devoxx.genie.ui.util.WindowPluginUtil.TOOL_WINDOW_ID;

@Getter
public class PromptInputArea extends JPanel implements ShortcutChangeListener, NewlineShortcutChangeListener, ToolWindowManagerListener, RAGStateListener {
    private final CommandAutoCompleteTextField inputField;
    private final SearchOptionsPanel searchOptionsPanel;
    private final ResourceBundle resourceBundle;
    private final Project project;
    private String submitShortcut;
    private String newlineShortcut;
    private String lastActiveId = null;

    public PromptInputArea(Project project, @NotNull ResourceBundle resourceBundle) {
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

        ApplicationManager.getApplication().invokeLater(inputField::requestFocusInWindow);

        MessageBusUtil.subscribe(project.getMessageBus().connect(),
                AppTopics.SHORTCUT_CHANGED_TOPIC, this);
                
        MessageBusUtil.subscribe(project.getMessageBus().connect(),
                AppTopics.NEWLINE_SHORTCUT_CHANGED_TOPIC, this);

        MessageBusUtil.subscribe(project.getMessageBus().connect(),
                AppTopics.RAG_ACTIVATED_CHANGED_TOPIC, this);

        // Request focus when tool window is activated or switched from another plugin window
        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, this);
    }

    /**
     * Request input field focus when tool window is activated or switched from another plugin window
     * @param toolWindowManager the tool window manager
     */
    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        String currentActiveId = null;
        var activeToolWindowId = toolWindowManager.getActiveToolWindowId();

        if (activeToolWindowId != null) {
            currentActiveId = activeToolWindowId;
        }

        // Only focus when our window becomes active from another window
        if (TOOL_WINDOW_ID.equals(currentActiveId) && !TOOL_WINDOW_ID.equals(lastActiveId)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (inputField != null && inputField.isDisplayable()) {
                    inputField.requestFocusInWindow();
                }
            }, ModalityState.nonModal());
        }

        lastActiveId = currentActiveId;
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
    
    public void setFileSelectionCallback(Runnable fileSelectionCallback) {
        inputField.setFileSelectionCallback(fileSelectionCallback);
    }

    private void setPlaceholderWithKeyboardShortcut(String shortcut) {
        // Clean up the shortcut text
        shortcut = shortcut.replace("pressed", "+")
                .replace("meta", "command");

        // Format the shortcut text
        String[] parts = shortcut.split(" ");
        submitShortcut = String.join(" + ", parts)
                .substring(0, 1).toUpperCase()
                + shortcut.substring(1);

        // Also get newline shortcut for the placeholder
        String newlineShortcut;
        if (SystemInfo.isWindows) {
            newlineShortcut = DevoxxGenieStateService.getInstance().getNewlineShortcutWindows();
        } else if (SystemInfo.isLinux) {
            newlineShortcut = DevoxxGenieStateService.getInstance().getNewlineShortcutLinux();
        } else {
            newlineShortcut = DevoxxGenieStateService.getInstance().getNewlineShortcutMac();
        }
        
        // Format the newline shortcut text
        newlineShortcut = newlineShortcut.replace("pressed", "+")
                .replace("meta", "command");
        String[] newlineParts = newlineShortcut.split(" ");
        String formattedNewlineShortcut = String.join(" + ", newlineParts)
                .substring(0, 1).toUpperCase()
                + newlineShortcut.substring(1);

        // Store the formatted newline shortcut for use in updatePlaceHolder
        this.newlineShortcut = formattedNewlineShortcut;

        updatePlaceHolder(Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated()));
    }

    @Override
    public void onShortcutChanged(String shortcut) {
        if (shortcut == null || shortcut.isEmpty()) {
            return;
        }
        setPlaceholderWithKeyboardShortcut(shortcut);
    }

    private void updatePlaceHolder(boolean ragEnabled) {
        String placeholderText;
        
        if (ragEnabled) {
            placeholderText = submitShortcut + " to submit, " + newlineShortcut + " for newline. " +
                    resourceBundle.getString("rag.prompt.placeholder");
        } else {
            placeholderText = submitShortcut + " to submit, " + newlineShortcut + " for newline. " +
                    resourceBundle.getString("prompt.placeholder");
        }
        
        inputField.setPlaceholder(placeholderText);
    }

    @Override
    public void onRAGStateChanged(boolean enabled) {
        updatePlaceHolder(enabled);
    }
    
    @Override
    public void onNewlineShortcutChanged(String shortcut) {
        if (shortcut == null || shortcut.isEmpty()) {
            return;
        }
        // Format the newline shortcut text
        shortcut = shortcut.replace("pressed", "+")
                .replace("meta", "command");
        String[] newlineParts = shortcut.split(" ");
        String formattedNewlineShortcut = String.join(" + ", newlineParts)
                .substring(0, 1).toUpperCase()
                + shortcut.substring(1);

        // Store the formatted newline shortcut for use in updatePlaceHolder
        this.newlineShortcut = formattedNewlineShortcut;
        
        updatePlaceHolder(Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getRagActivated()));
    }
}
