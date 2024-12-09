package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class LLMProvidersConfigurable implements Configurable {

    private final LLMProvidersComponent llmSettingsComponent;

    public LLMProvidersConfigurable() {
        llmSettingsComponent = new LLMProvidersComponent();
    }

    /**
     * Get the display name
     *
     * @return the display name
     */
    @Nls
    @Override
    public String getDisplayName() {
        return "Large Language Models";
    }

    /**
     * Get the Prompt Settings component
     *
     * @return the component
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return llmSettingsComponent.createPanel();
    }

    /**
     * Check if the settings have been modified
     *
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !settings.getStreamMode().equals(llmSettingsComponent.getStreamModeCheckBox().isSelected());

        isModified |= isFieldModified(llmSettingsComponent.getOpenAIKeyField(), settings.getOpenAIKey());
        isModified |= isFieldModified(llmSettingsComponent.getMistralApiKeyField(), settings.getMistralKey());
        isModified |= isFieldModified(llmSettingsComponent.getAnthropicApiKeyField(), settings.getAnthropicKey());
        isModified |= isFieldModified(llmSettingsComponent.getGroqApiKeyField(), settings.getGroqKey());
        isModified |= isFieldModified(llmSettingsComponent.getDeepInfraApiKeyField(), settings.getDeepInfraKey());
        isModified |= isFieldModified(llmSettingsComponent.getGeminiApiKeyField(), settings.getGeminiKey());
        isModified |= isFieldModified(llmSettingsComponent.getDeepSeekApiKeyField(), settings.getDeepSeekKey());
        isModified |= isFieldModified(llmSettingsComponent.getLlamaCPPModelUrlField(), settings.getLlamaCPPUrl());
        isModified |= isFieldModified(llmSettingsComponent.getJlamaModelUrlField(), settings.getJlamaUrl());
        isModified |= isFieldModified(llmSettingsComponent.getOpenRouterApiKeyField(), settings.getOpenRouterKey());

        isModified |= isFieldModified(llmSettingsComponent.getOllamaModelUrlField(), settings.getOllamaModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getLmStudioModelUrlField(), settings.getLmstudioModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getGpt4AllModelUrlField(), settings.getGpt4allModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getJanModelUrlField(), settings.getJanModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getExoModelUrlField(), settings.getExoModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getCustomOpenAIUrlField(), settings.getCustomOpenAIUrl());

        isModified |= !settings.getShowAzureOpenAIFields().equals(llmSettingsComponent.getEnableAzureOpenAI().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIEndpointField(), settings.getAzureOpenAIEndpoint());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIDeploymentField(), settings.getAzureOpenAIDeployment());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIKeyField(), settings.getAzureOpenAIKey());

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() {
        boolean isModified = isModified();

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        settings.setStreamMode(llmSettingsComponent.getStreamModeCheckBox().isSelected());

        settings.setOllamaModelUrl(llmSettingsComponent.getOllamaModelUrlField().getText());
        settings.setLmstudioModelUrl(llmSettingsComponent.getLmStudioModelUrlField().getText());
        settings.setGpt4allModelUrl(llmSettingsComponent.getGpt4AllModelUrlField().getText());
        settings.setJanModelUrl(llmSettingsComponent.getJanModelUrlField().getText());
        settings.setExoModelUrl(llmSettingsComponent.getExoModelUrlField().getText());
        settings.setLlamaCPPUrl(llmSettingsComponent.getLlamaCPPModelUrlField().getText());
        settings.setJlamaUrl(llmSettingsComponent.getJlamaModelUrlField().getText());
        settings.setCustomOpenAIUrl(llmSettingsComponent.getCustomOpenAIUrlField().getText());

        settings.setOpenAIKey(new String(llmSettingsComponent.getOpenAIKeyField().getPassword()));
        settings.setMistralKey(new String(llmSettingsComponent.getMistralApiKeyField().getPassword()));
        settings.setAnthropicKey(new String(llmSettingsComponent.getAnthropicApiKeyField().getPassword()));
        settings.setGroqKey(new String(llmSettingsComponent.getGroqApiKeyField().getPassword()));
        settings.setDeepInfraKey(new String(llmSettingsComponent.getDeepInfraApiKeyField().getPassword()));
        settings.setGeminiKey(new String(llmSettingsComponent.getGeminiApiKeyField().getPassword()));
        settings.setDeepSeekKey(new String(llmSettingsComponent.getDeepSeekApiKeyField().getPassword()));
        settings.setOpenRouterKey(new String(llmSettingsComponent.getOpenRouterApiKeyField().getPassword()));

        settings.setShowAzureOpenAIFields(llmSettingsComponent.getEnableAzureOpenAI().isSelected());
        settings.setAzureOpenAIEndpoint(llmSettingsComponent.getAzureOpenAIEndpointField().getText());
        settings.setAzureOpenAIDeployment(llmSettingsComponent.getAzureOpenAIDeploymentField().getText());
        settings.setAzureOpenAIKey(new String(llmSettingsComponent.getAzureOpenAIKeyField().getPassword()));

        // Only notify the listener if an API key has changed, so we can refresh the LLM providers list in the UI
        if (isModified) {
            boolean hasKey = !settings.getAnthropicKey().isBlank() ||
                    !settings.getOpenAIKey().isBlank() ||
                    !settings.getOpenRouterKey().isBlank() ||
                    !settings.getDeepSeekKey().isBlank() ||
                    !settings.getDeepInfraKey().isBlank() ||
                    !settings.getGeminiKey().isBlank();

            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)
                    .settingsChanged(hasKey);
        }
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        llmSettingsComponent.getStreamModeCheckBox().setSelected(settings.getStreamMode());

        llmSettingsComponent.getOllamaModelUrlField().setText(settings.getOllamaModelUrl());
        llmSettingsComponent.getLmStudioModelUrlField().setText(settings.getLmstudioModelUrl());
        llmSettingsComponent.getGpt4AllModelUrlField().setText(settings.getGpt4allModelUrl());
        llmSettingsComponent.getJanModelUrlField().setText(settings.getJanModelUrl());
        llmSettingsComponent.getExoModelUrlField().setText(settings.getExoModelUrl());
        llmSettingsComponent.getLlamaCPPModelUrlField().setText(settings.getLlamaCPPUrl());
        llmSettingsComponent.getJlamaModelUrlField().setText(settings.getJlamaUrl());
        llmSettingsComponent.getCustomOpenAIUrlField().setText(settings.getCustomOpenAIUrl());

        llmSettingsComponent.getOpenAIKeyField().setText(settings.getOpenAIKey());
        llmSettingsComponent.getMistralApiKeyField().setText(settings.getMistralKey());
        llmSettingsComponent.getAnthropicApiKeyField().setText(settings.getAnthropicKey());
        llmSettingsComponent.getGroqApiKeyField().setText(settings.getGroqKey());
        llmSettingsComponent.getDeepInfraApiKeyField().setText(settings.getDeepInfraKey());
        llmSettingsComponent.getGeminiApiKeyField().setText(settings.getGeminiKey());
        llmSettingsComponent.getDeepSeekApiKeyField().setText(settings.getDeepSeekKey());
        llmSettingsComponent.getOpenRouterApiKeyField().setText(settings.getOpenRouterKey());

        llmSettingsComponent.getEnableAzureOpenAI().setSelected(settings.getShowAzureOpenAIFields());
        llmSettingsComponent.getAzureOpenAIEndpointField().setText(settings.getAzureOpenAIEndpoint());
        llmSettingsComponent.getAzureOpenAIDeploymentField().setText(settings.getAzureOpenAIDeployment());
        llmSettingsComponent.getAzureOpenAIKeyField().setText(settings.getAzureOpenAIKey());
    }
}
