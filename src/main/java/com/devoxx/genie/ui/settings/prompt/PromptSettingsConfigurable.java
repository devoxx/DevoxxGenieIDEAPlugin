package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Consumer;

public class PromptSettingsConfigurable implements Configurable {

    private final PromptSettingsComponent promptSettingsComponent;
    private final Project project;

    public PromptSettingsConfigurable(Project project) {
        promptSettingsComponent = new PromptSettingsComponent(project);
        this.project = project;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return promptSettingsComponent.createPanelWithHelp();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Prompts";
    }

    @Override
    public boolean isModified() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !StringUtil.equals(promptSettingsComponent.getSystemPromptField().getText(), settings.getSystemPrompt());

        isModified |= settings.getCreateDevoxxGenieMd() != promptSettingsComponent.getCreateDevoxxGenieMdCheckbox().isSelected();
        isModified |= settings.getIncludeProjectTree() != promptSettingsComponent.getIncludeProjectTreeCheckbox().isSelected();
        isModified |= !Objects.equals(settings.getProjectTreeDepth(), promptSettingsComponent.getProjectTreeDepthSpinner().getValue());
        isModified |= settings.getUseDevoxxGenieMdInPrompt() != promptSettingsComponent.getUseDevoxxGenieMdInPromptCheckbox().isSelected();
        isModified |= settings.getUseClaudeOrAgentsMdInPrompt() != promptSettingsComponent.getUseClaudeOrAgentsMdInPromptCheckbox().isSelected();

        isModified |= !settings.getSubmitShortcutWindows().equals(promptSettingsComponent.getSubmitShortcutWindows());
        isModified |= !settings.getSubmitShortcutMac().equals(promptSettingsComponent.getSubmitShortcutMac());
        isModified |= !settings.getSubmitShortcutLinux().equals(promptSettingsComponent.getSubmitShortcutLinux());

        isModified |= !settings.getNewlineShortcutWindows().equals(promptSettingsComponent.getNewlineShortcutWindows());
        isModified |= !settings.getNewlineShortcutMac().equals(promptSettingsComponent.getNewlineShortcutMac());
        isModified |= !settings.getNewlineShortcutLinux().equals(promptSettingsComponent.getNewlineShortcutLinux());

        return isModified;
    }

    @Override
    public void apply() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        updateTextAreaIfModified(promptSettingsComponent.getSystemPromptField(), settings.getSystemPrompt(), settings::setSystemPrompt);

        settings.setCreateDevoxxGenieMd(promptSettingsComponent.getCreateDevoxxGenieMdCheckbox().isSelected());
        settings.setIncludeProjectTree(promptSettingsComponent.getIncludeProjectTreeCheckbox().isSelected());
        settings.setProjectTreeDepth((Integer) promptSettingsComponent.getProjectTreeDepthSpinner().getValue());
        settings.setUseDevoxxGenieMdInPrompt(promptSettingsComponent.getUseDevoxxGenieMdInPromptCheckbox().isSelected());
        settings.setUseClaudeOrAgentsMdInPrompt(promptSettingsComponent.getUseClaudeOrAgentsMdInPromptCheckbox().isSelected());

        String newShortcut;
        if (SystemInfo.isWindows) {
            settings.setSubmitShortcutWindows(promptSettingsComponent.getSubmitShortcutWindows());
            newShortcut = promptSettingsComponent.getSubmitShortcutWindows();
        } else if (SystemInfo.isMac) {
            settings.setSubmitShortcutMac(promptSettingsComponent.getSubmitShortcutMac());
            newShortcut = promptSettingsComponent.getSubmitShortcutMac();
        } else {
            settings.setSubmitShortcutLinux(promptSettingsComponent.getSubmitShortcutLinux());
            newShortcut = promptSettingsComponent.getSubmitShortcutLinux();
        }

        if (newShortcut != null) {
            project.getMessageBus()
                    .syncPublisher(AppTopics.SHORTCUT_CHANGED_TOPIC)
                    .onShortcutChanged(newShortcut);
        }

        String newNewlineShortcut;
        if (SystemInfo.isWindows) {
            settings.setNewlineShortcutWindows(promptSettingsComponent.getNewlineShortcutWindows());
            newNewlineShortcut = promptSettingsComponent.getNewlineShortcutWindows();
        } else if (SystemInfo.isMac) {
            settings.setNewlineShortcutMac(promptSettingsComponent.getNewlineShortcutMac());
            newNewlineShortcut = promptSettingsComponent.getNewlineShortcutMac();
        } else {
            settings.setNewlineShortcutLinux(promptSettingsComponent.getNewlineShortcutLinux());
            newNewlineShortcut = promptSettingsComponent.getNewlineShortcutLinux();
        }

        if (newNewlineShortcut != null) {
            project.getMessageBus()
                    .syncPublisher(AppTopics.NEWLINE_SHORTCUT_CHANGED_TOPIC)
                    .onNewlineShortcutChanged(newNewlineShortcut);
        }
    }

    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        promptSettingsComponent.getSystemPromptField().setText(settings.getSystemPrompt());

        promptSettingsComponent.getCreateDevoxxGenieMdCheckbox().setSelected(settings.getCreateDevoxxGenieMd());
        promptSettingsComponent.getIncludeProjectTreeCheckbox().setSelected(settings.getIncludeProjectTree());
        promptSettingsComponent.getProjectTreeDepthSpinner().setValue(settings.getProjectTreeDepth());
        promptSettingsComponent.getUseDevoxxGenieMdInPromptCheckbox().setSelected(settings.getUseDevoxxGenieMdInPrompt());
        promptSettingsComponent.getUseClaudeOrAgentsMdInPromptCheckbox().setSelected(settings.getUseClaudeOrAgentsMdInPrompt());

        boolean createMdEnabled = settings.getCreateDevoxxGenieMd();
        promptSettingsComponent.getIncludeProjectTreeCheckbox().setEnabled(createMdEnabled);
        promptSettingsComponent.getProjectTreeDepthSpinner().setEnabled(createMdEnabled && settings.getIncludeProjectTree());
        promptSettingsComponent.getUseDevoxxGenieMdInPromptCheckbox().setEnabled(createMdEnabled);
        promptSettingsComponent.getCreateDevoxxGenieMdButton().setEnabled(createMdEnabled);

        promptSettingsComponent.setSubmitShortcutWindows(settings.getSubmitShortcutWindows());
        promptSettingsComponent.setSubmitShortcutMac(settings.getSubmitShortcutMac());
        promptSettingsComponent.setSubmitShortcutLinux(settings.getSubmitShortcutLinux());
        promptSettingsComponent.setNewlineShortcutWindows(settings.getNewlineShortcutWindows());
        promptSettingsComponent.setNewlineShortcutMac(settings.getNewlineShortcutMac());
        promptSettingsComponent.setNewlineShortcutLinux(settings.getNewlineShortcutLinux());
    }

    public void updateTextAreaIfModified(@NotNull JTextArea textArea,
                                         Object currentValue,
                                         Consumer<String> updateAction) {
        String newValue = textArea.getText();
        if (newValue != null && !newValue.equals(currentValue)) {
            updateAction.accept(newValue);
        }
    }
}
