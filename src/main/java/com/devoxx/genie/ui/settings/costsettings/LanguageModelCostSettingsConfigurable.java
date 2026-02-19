package com.devoxx.genie.ui.settings.costsettings;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LanguageModelCostSettingsConfigurable implements Configurable {

    private final MessageBus messageBus;
    private LanguageModelCostSettingsComponent component;

    public LanguageModelCostSettingsConfigurable(@NotNull Project project) {
        this.messageBus = project.getMessageBus();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "LLM Costs";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        component = new LanguageModelCostSettingsComponent();
        return component.createPanelWithHelp();
    }

    @Override
    public boolean isModified() {
        if (component == null) {
            return false;
        }
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        return stateService.getShowCalcTokensButton() != component.getShowCalcTokensButtonCheckBox().isSelected()
                || stateService.getShowAddFileButton() != component.getShowAddFileButtonCheckBox().isSelected();
    }

    @Override
    public void apply() {
        if (component != null) {
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
            stateService.setShowCalcTokensButton(component.getShowCalcTokensButtonCheckBox().isSelected());
            stateService.setShowAddFileButton(component.getShowAddFileButtonCheckBox().isSelected());
        }

        // Notify listeners that settings have changed
        messageBus.syncPublisher(AppTopics.LLM_SETTINGS_CHANGED_TOPIC).llmSettingsChanged();
        messageBus.syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC).settingsChanged(true);
    }

    @Override
    public void reset() {
        if (component != null) {
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
            component.getShowCalcTokensButtonCheckBox().setSelected(stateService.getShowCalcTokensButton());
            component.getShowAddFileButtonCheckBox().setSelected(stateService.getShowAddFileButton());
        }
    }
}
