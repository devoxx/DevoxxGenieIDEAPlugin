package com.devoxx.genie.ui.settings.llmconfig;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LLMConfigSettingsConfigurable implements Configurable {

    private final LLMConfigSettingsComponent llmConfigSettingsComponent = new LLMConfigSettingsComponent();

    /**
     * Get the display name
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "LLM Settings";
    }

    /**
     * Get the Prompt Settings component
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return llmConfigSettingsComponent.getPanel();
    }

    /**
     * Check if the settings have been modified
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        LLMConfigStateService settingsState = LLMConfigStateService.getInstance();

        boolean isModified = false;

        isModified |= ((Double)llmConfigSettingsComponent.getTemperatureField().getValue()) != settingsState.getTemperature();
        isModified |= ((Double)llmConfigSettingsComponent.getTopPField().getValue()) != settingsState.getTopP();
        isModified |= llmConfigSettingsComponent.getMaxOutputTokensField().getNumber() != settingsState.getMaxOutputTokens();
        isModified |= llmConfigSettingsComponent.getChatMemorySizeField().getNumber() != settingsState.getChatMemorySize();
        isModified |= llmConfigSettingsComponent.getTimeoutField().getNumber() != settingsState.getTimeout();
        isModified |= llmConfigSettingsComponent.getRetryField().getNumber() != settingsState.getMaxRetries();

        isModified |= !settingsState.getAstMode().equals(llmConfigSettingsComponent.getAstMode().isSelected());
        isModified |= !settingsState.getAstParentClass().equals(llmConfigSettingsComponent.getAstParentClassCheckBox().isSelected());
        isModified |= !settingsState.getAstClassReference().equals(llmConfigSettingsComponent.getAstReferenceClassesCheckBox().isSelected());
        isModified |= !settingsState.getAstFieldReference().equals(llmConfigSettingsComponent.getAstReferenceFieldCheckBox().isSelected());
        return isModified;
    }
    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        LLMConfigStateService settingsState = LLMConfigStateService.getInstance();

        settingsState.setTemperature(((Double)llmConfigSettingsComponent.getTemperatureField().getValue()));
        settingsState.setTopP(((Double)llmConfigSettingsComponent.getTopPField().getValue()));

        settingsState.setChatMemorySize(llmConfigSettingsComponent.getChatMemorySizeField().getNumber());
        settingsState.setMaxOutputTokens(llmConfigSettingsComponent.getMaxOutputTokensField().getNumber());
        settingsState.setTimeout(llmConfigSettingsComponent.getTimeoutField().getNumber());
        settingsState.setMaxRetries(llmConfigSettingsComponent.getRetryField().getNumber());
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        LLMConfigStateService settingsState = LLMConfigStateService.getInstance();

        llmConfigSettingsComponent.getTemperatureField().setValue(settingsState.getTemperature());
        llmConfigSettingsComponent.getTopPField().setValue(settingsState.getTopP());

        llmConfigSettingsComponent.getMaxOutputTokensField().setNumber(settingsState.getMaxOutputTokens());
        llmConfigSettingsComponent.getChatMemorySizeField().setNumber(settingsState.getChatMemorySize());
        llmConfigSettingsComponent.getTimeoutField().setNumber(settingsState.getTimeout());
        llmConfigSettingsComponent.getRetryField().setNumber(settingsState.getMaxRetries());
    }
}
