package com.devoxx.genie.ui.settings.llmconfig;

import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LLMConfigSettingsConfigurable implements Configurable {

    private final LLMConfigSettingsComponent llmConfigSettingsComponent;

    public LLMConfigSettingsConfigurable() {
        llmConfigSettingsComponent = new LLMConfigSettingsComponent();
    }

    /**
     * Get the display name
     *
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "LLM Settings";
    }

    /**
     * Get the Prompt Settings component
     *
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return llmConfigSettingsComponent.createPanelWithHelp();
    }

    /**
     * Check if the settings have been modified
     *
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieSettingsService stateService = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= llmConfigSettingsComponent.getTemperatureField().getValue() != stateService.getTemperature();
        isModified |= llmConfigSettingsComponent.getTopPField().getValue() != stateService.getTopP();
        isModified |= llmConfigSettingsComponent.getMaxOutputTokensField().getNumber() != stateService.getMaxOutputTokens();
        isModified |= llmConfigSettingsComponent.getChatMemorySizeField().getNumber() != stateService.getChatMemorySize();
        isModified |= llmConfigSettingsComponent.getTimeoutField().getNumber() != stateService.getTimeout();
        isModified |= llmConfigSettingsComponent.getRetryField().getNumber() != stateService.getMaxRetries();
        isModified |= llmConfigSettingsComponent.getUseFileInEditorCheckBox().isSelected() != stateService.getUseFileInEditor();
        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        stateService.setTemperature(((Double) llmConfigSettingsComponent.getTemperatureField().getValue()));
        stateService.setTopP(((Double) llmConfigSettingsComponent.getTopPField().getValue()));

        stateService.setChatMemorySize(llmConfigSettingsComponent.getChatMemorySizeField().getNumber());
        stateService.setMaxOutputTokens(llmConfigSettingsComponent.getMaxOutputTokensField().getNumber());
        stateService.setTimeout(llmConfigSettingsComponent.getTimeoutField().getNumber());
        stateService.setMaxRetries(llmConfigSettingsComponent.getRetryField().getNumber());

        stateService.setUseFileInEditor(llmConfigSettingsComponent.getUseFileInEditorCheckBox().isSelected());
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        llmConfigSettingsComponent.getTemperatureField().setValue(stateService.getTemperature());
        llmConfigSettingsComponent.getTopPField().setValue(stateService.getTopP());

        llmConfigSettingsComponent.getMaxOutputTokensField().setNumber(stateService.getMaxOutputTokens());
        llmConfigSettingsComponent.getChatMemorySizeField().setNumber(stateService.getChatMemorySize());
        llmConfigSettingsComponent.getTimeoutField().setNumber(stateService.getTimeout());
        llmConfigSettingsComponent.getRetryField().setNumber(stateService.getMaxRetries());

        llmConfigSettingsComponent.getUseFileInEditorCheckBox().setSelected(stateService.getUseFileInEditor());
    }
}
