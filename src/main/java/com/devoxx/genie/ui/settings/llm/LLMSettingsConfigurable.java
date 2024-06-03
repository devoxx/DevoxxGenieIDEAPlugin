package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.service.settings.llm.LLMStateService;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class LLMSettingsConfigurable implements Configurable {

    private final LLMSettingsComponent llmSettingsComponent = new LLMSettingsComponent();

    /**
     * Get the display name
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Large Language Models";
    }

    /**
     * Get the Prompt Settings component
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return llmSettingsComponent.getPanel();
    }

    /**
     * Check if the settings have been modified
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        LLMStateService settings = LLMStateService.getInstance();

        boolean isModified = false;

        isModified |= isFieldModified(llmSettingsComponent.getOllamaModelUrlField(), settings.getOllamaModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getLmStudioModelUrlField(), settings.getLmstudioModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getGpt4AllModelUrlField(), settings.getGpt4allModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getJanModelUrlField(), settings.getJanModelUrl());

        isModified |= isFieldModified(llmSettingsComponent.getOpenAIKeyField(), settings.getOpenAIKey());
        isModified |= isFieldModified(llmSettingsComponent.getMistralApiKeyField(), settings.getMistralKey());
        isModified |= isFieldModified(llmSettingsComponent.getAnthropicApiKeyField(), settings.getAnthropicKey());
        isModified |= isFieldModified(llmSettingsComponent.getGroqApiKeyField(), settings.getGroqKey());
        isModified |= isFieldModified(llmSettingsComponent.getDeepInfraApiKeyField(), settings.getDeepInfraKey());
        isModified |= isFieldModified(llmSettingsComponent.getGeminiApiKeyField(), settings.getGeminiKey());

        isModified |= !settings.getHideSearchButtonsFlag().equals(llmSettingsComponent.getHideSearchButtonsField().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getTavilySearchApiKeyField(), settings.getTavilySearchKey());
        isModified |= isFieldModified(llmSettingsComponent.getGoogleSearchApiKeyField(), settings.getGoogleSearchKey());
        isModified |= isFieldModified(llmSettingsComponent.getGoogleCSIApiKeyField(), settings.getGoogleCSIKey());
        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        LLMStateService settings = LLMStateService.getInstance();

    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        LLMStateService settings = LLMStateService.getInstance();

    }
}
