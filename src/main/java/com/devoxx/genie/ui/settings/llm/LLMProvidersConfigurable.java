package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.options.Configurable.isFieldModified;

public class LLMProvidersConfigurable implements Configurable {

    private final LLMProvidersComponent llmSettingsComponent;
    private final Project project;

    public LLMProvidersConfigurable(Project project) {
        this.project = project;
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
        return AbstractSettingsComponent.wrapWithHelpButton(
            llmSettingsComponent.createPanel(),
            "https://genie.devoxx.com/docs/llm-providers/overview");
    }

    /**
     * Check if the settings have been modified
     *
     * @return true if the settings have been modified
     */
    @Override
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

        boolean isModified = false;

        isModified |= !stateService.getStreamMode().equals(llmSettingsComponent.getStreamModeCheckBox().isSelected());

        isModified |= isFieldModified(llmSettingsComponent.getOpenAIKeyField(), stateService.getOpenAIKey());
        isModified |= isFieldModified(llmSettingsComponent.getMistralApiKeyField(), stateService.getMistralKey());
        isModified |= isFieldModified(llmSettingsComponent.getAnthropicApiKeyField(), stateService.getAnthropicKey());
        isModified |= isFieldModified(llmSettingsComponent.getGroqApiKeyField(), stateService.getGroqKey());
        isModified |= isFieldModified(llmSettingsComponent.getDeepInfraApiKeyField(), stateService.getDeepInfraKey());
        isModified |= isFieldModified(llmSettingsComponent.getGeminiApiKeyField(), stateService.getGeminiKey());
        isModified |= isFieldModified(llmSettingsComponent.getDeepSeekApiKeyField(), stateService.getDeepSeekKey());
        isModified |= isFieldModified(llmSettingsComponent.getLlamaCPPModelUrlField(), stateService.getLlamaCPPUrl());
        isModified |= isFieldModified(llmSettingsComponent.getOpenRouterApiKeyField(), stateService.getOpenRouterKey());
        isModified |= isFieldModified(llmSettingsComponent.getGrokApiKeyField(), stateService.getGrokKey());
        isModified |= isFieldModified(llmSettingsComponent.getKimiApiKeyField(), stateService.getKimiKey());
        isModified |= isFieldModified(llmSettingsComponent.getGlmApiKeyField(), stateService.getGlmKey());

        isModified |= isFieldModified(llmSettingsComponent.getOllamaModelUrlField(), stateService.getOllamaModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getLmStudioModelUrlField(), stateService.getLmstudioModelUrl());
        isModified |= (stateService.getLmStudioFallbackContextLength() != null) != llmSettingsComponent.getLmStudioFallbackContextEnabledCheckBox().isSelected();
        if (llmSettingsComponent.getLmStudioFallbackContextEnabledCheckBox().isSelected()) {
            Integer savedFallback = stateService.getLmStudioFallbackContextLength();
            isModified |= savedFallback == null || !savedFallback.equals(llmSettingsComponent.getLmStudioFallbackContextField().getNumber());
        }
        isModified |= isFieldModified(llmSettingsComponent.getGpt4AllModelUrlField(), stateService.getGpt4allModelUrl());
        isModified |= isFieldModified(llmSettingsComponent.getJanModelUrlField(), stateService.getJanModelUrl());

        isModified |= stateService.isCustomOpenAIApiKeyEnabled() != llmSettingsComponent.getEnableCustomOpenAIApiKeyCheckBox().isSelected();
        isModified |= isFieldModified(llmSettingsComponent.getCustomOpenAIUrlField(), stateService.getCustomOpenAIUrl());
        isModified |= isFieldModified(llmSettingsComponent.getCustomOpenAIModelNameField(), stateService.getCustomOpenAIModelName());
        isModified |= isFieldModified(llmSettingsComponent.getCustomOpenAIApiKeyField(), stateService.getCustomOpenAIApiKey());

        isModified |= !stateService.getShowAzureOpenAIFields().equals(llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIEndpointField(), stateService.getAzureOpenAIEndpoint());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIDeploymentField(), stateService.getAzureOpenAIDeployment());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIKeyField(), stateService.getAzureOpenAIKey());

        isModified |= !stateService.getShowAwsFields().equals(llmSettingsComponent.getEnableAWSCheckBox().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAwsSecretKeyField(), stateService.getAwsSecretKey());
        isModified |= isFieldModified(llmSettingsComponent.getAwsAccessKeyIdField(), stateService.getAwsAccessKeyId());
        isModified |= !stateService.getShouldPowerFromAWSProfile().equals(llmSettingsComponent.getEnableAWSProfileCheckBox().isSelected());
        isModified |= !stateService.getShouldEnableAWSRegionalInference().equals(llmSettingsComponent.getEnableAWSRegionalInferenceCheckBox().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAwsProfileName(), stateService.getAwsProfileName());
        isModified |= isFieldModified(llmSettingsComponent.getAwsRegion(), stateService.getAwsRegion());

        isModified |= stateService.isOllamaEnabled() != llmSettingsComponent.getOllamaEnabledCheckBox().isSelected();
        isModified |= stateService.isLmStudioEnabled() != llmSettingsComponent.getLmStudioEnabledCheckBox().isSelected();
        isModified |= stateService.isGpt4AllEnabled() != llmSettingsComponent.getGpt4AllEnabledCheckBox().isSelected();
        isModified |= stateService.isJanEnabled() != llmSettingsComponent.getJanEnabledCheckBox().isSelected();
        isModified |= stateService.isLlamaCPPEnabled() != llmSettingsComponent.getLlamaCPPEnabledCheckBox().isSelected();

        isModified |= stateService.isCustomOpenAIUrlEnabled() != llmSettingsComponent.getCustomOpenAIUrlEnabledCheckBox().isSelected();
        isModified |= stateService.isCustomOpenAIModelNameEnabled() != llmSettingsComponent.getCustomOpenAIModelNameEnabledCheckBox().isSelected();
        isModified |= stateService.isCustomOpenAIForceHttp11() != llmSettingsComponent.getCustomOpenAIForceHttp11CheckBox().isSelected();

        isModified |= stateService.isOpenAIEnabled() != llmSettingsComponent.getOpenAIEnabledCheckBox().isSelected();
        isModified |= stateService.isMistralEnabled() != llmSettingsComponent.getMistralEnabledCheckBox().isSelected();
        isModified |= stateService.isAnthropicEnabled() != llmSettingsComponent.getAnthropicEnabledCheckBox().isSelected();
        isModified |= stateService.isGroqEnabled() != llmSettingsComponent.getGroqEnabledCheckBox().isSelected();
        isModified |= stateService.isDeepInfraEnabled() != llmSettingsComponent.getDeepInfraEnabledCheckBox().isSelected();
        isModified |= stateService.isGoogleEnabled() != llmSettingsComponent.getGeminiEnabledCheckBox().isSelected();
        isModified |= stateService.isDeepSeekEnabled() != llmSettingsComponent.getDeepSeekEnabledCheckBox().isSelected();
        isModified |= stateService.isOpenRouterEnabled() != llmSettingsComponent.getOpenRouterEnabledCheckBox().isSelected();
        isModified |= stateService.isGrokEnabled() != llmSettingsComponent.getGrokEnabledCheckBox().isSelected();
        isModified |= stateService.isKimiEnabled() != llmSettingsComponent.getKimiEnabledCheckBox().isSelected();
        isModified |= stateService.isGlmEnabled() != llmSettingsComponent.getGlmEnabledCheckBox().isSelected();
        isModified |= stateService.getShowAzureOpenAIFields() != llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected();

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
        settings.setLmStudioFallbackContextLength(
                llmSettingsComponent.getLmStudioFallbackContextEnabledCheckBox().isSelected()
                        ? llmSettingsComponent.getLmStudioFallbackContextField().getNumber()
                        : null
        );
        settings.setGpt4allModelUrl(llmSettingsComponent.getGpt4AllModelUrlField().getText());
        settings.setJanModelUrl(llmSettingsComponent.getJanModelUrlField().getText());
        settings.setLlamaCPPUrl(llmSettingsComponent.getLlamaCPPModelUrlField().getText());

        settings.setCustomOpenAIUrl(llmSettingsComponent.getCustomOpenAIUrlField().getText());
        settings.setCustomOpenAIModelName(llmSettingsComponent.getCustomOpenAIModelNameField().getText());
        settings.setCustomOpenAIApiKey(new String(llmSettingsComponent.getCustomOpenAIApiKeyField().getPassword()));
        settings.setCustomOpenAIApiKeyEnabled(llmSettingsComponent.getEnableCustomOpenAIApiKeyCheckBox().isSelected());
        settings.setCustomOpenAIForceHttp11(llmSettingsComponent.getCustomOpenAIForceHttp11CheckBox().isSelected());

        settings.setOpenAIKey(new String(llmSettingsComponent.getOpenAIKeyField().getPassword()));
        settings.setMistralKey(new String(llmSettingsComponent.getMistralApiKeyField().getPassword()));
        settings.setAnthropicKey(new String(llmSettingsComponent.getAnthropicApiKeyField().getPassword()));
        settings.setGroqKey(new String(llmSettingsComponent.getGroqApiKeyField().getPassword()));
        settings.setDeepInfraKey(new String(llmSettingsComponent.getDeepInfraApiKeyField().getPassword()));
        settings.setGeminiKey(new String(llmSettingsComponent.getGeminiApiKeyField().getPassword()));
        settings.setDeepSeekKey(new String(llmSettingsComponent.getDeepSeekApiKeyField().getPassword()));
        settings.setOpenRouterKey(new String(llmSettingsComponent.getOpenRouterApiKeyField().getPassword()));
        settings.setGrokKey(new String(llmSettingsComponent.getGrokApiKeyField().getPassword()));
        settings.setKimiKey(new String(llmSettingsComponent.getKimiApiKeyField().getPassword()));
        settings.setGlmKey(new String(llmSettingsComponent.getGlmApiKeyField().getPassword()));

        settings.setShowAzureOpenAIFields(llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected());
        settings.setAzureOpenAIEndpoint(llmSettingsComponent.getAzureOpenAIEndpointField().getText());
        settings.setAzureOpenAIDeployment(llmSettingsComponent.getAzureOpenAIDeploymentField().getText());
        settings.setAzureOpenAIKey(new String(llmSettingsComponent.getAzureOpenAIKeyField().getPassword()));

        settings.setShowAwsFields(llmSettingsComponent.getEnableAWSCheckBox().isSelected());
        settings.setAwsAccessKeyId(new String(llmSettingsComponent.getAwsAccessKeyIdField().getPassword()));
        settings.setAwsSecretKey(new String(llmSettingsComponent.getAwsSecretKeyField().getPassword()));
        settings.setAwsRegion(llmSettingsComponent.getAwsRegion().getText());
        settings.setShouldPowerFromAWSProfile(llmSettingsComponent.getEnableAWSProfileCheckBox().isSelected());
        settings.setShouldEnableAWSRegionalInference(llmSettingsComponent.getEnableAWSRegionalInferenceCheckBox().isSelected());
        settings.setAwsProfileName(llmSettingsComponent.getAwsProfileName().getText());

        settings.setOllamaEnabled(llmSettingsComponent.getOllamaEnabledCheckBox().isSelected());
        settings.setLmStudioEnabled(llmSettingsComponent.getLmStudioEnabledCheckBox().isSelected());
        settings.setGpt4AllEnabled(llmSettingsComponent.getGpt4AllEnabledCheckBox().isSelected());
        settings.setJanEnabled(llmSettingsComponent.getJanEnabledCheckBox().isSelected());
        settings.setLlamaCPPEnabled(llmSettingsComponent.getLlamaCPPEnabledCheckBox().isSelected());

        settings.setCustomOpenAIUrlEnabled(llmSettingsComponent.getCustomOpenAIUrlEnabledCheckBox().isSelected());
        settings.setCustomOpenAIModelNameEnabled(llmSettingsComponent.getCustomOpenAIModelNameEnabledCheckBox().isSelected());

        settings.setOpenAIEnabled(llmSettingsComponent.getOpenAIEnabledCheckBox().isSelected());
        settings.setMistralEnabled(llmSettingsComponent.getMistralEnabledCheckBox().isSelected());
        settings.setAnthropicEnabled(llmSettingsComponent.getAnthropicEnabledCheckBox().isSelected());
        settings.setGroqEnabled(llmSettingsComponent.getGroqEnabledCheckBox().isSelected());
        settings.setDeepInfraEnabled(llmSettingsComponent.getDeepInfraEnabledCheckBox().isSelected());
        settings.setGoogleEnabled(llmSettingsComponent.getGeminiEnabledCheckBox().isSelected());
        settings.setDeepSeekEnabled(llmSettingsComponent.getDeepSeekEnabledCheckBox().isSelected());
        settings.setOpenRouterEnabled(llmSettingsComponent.getOpenRouterEnabledCheckBox().isSelected());
        settings.setGrokEnabled(llmSettingsComponent.getGrokEnabledCheckBox().isSelected());
        settings.setKimiEnabled(llmSettingsComponent.getKimiEnabledCheckBox().isSelected());
        settings.setGlmEnabled(llmSettingsComponent.getGlmEnabledCheckBox().isSelected());
        settings.setShowAzureOpenAIFields(llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected());

        // Only notify the listener if an API key has changed, so we can refresh the LLM providers list in the UI
        if (isModified) {
            boolean hasKey = (!settings.getAnthropicKey().isBlank() && settings.isAnthropicEnabled()) ||
                    (!settings.getOpenAIKey().isBlank() && settings.isOpenAIEnabled()) ||
                    (!settings.getCustomOpenAIApiKey().isBlank() && settings.isCustomOpenAIApiKeyEnabled()) ||
                    (!settings.getOpenRouterKey().isBlank() && settings.isOpenRouterEnabled()) ||
                    (!settings.getDeepSeekKey().isBlank() && settings.isDeepSeekEnabled()) ||
                    (!settings.getDeepInfraKey().isBlank() && settings.isDeepInfraEnabled()) ||
                    (!settings.getGeminiKey().isBlank() && settings.isGoogleEnabled()) ||
                    (!settings.getGroqKey().isBlank() && settings.isGroqEnabled()) ||
                    (!settings.getGrokKey().isBlank() && settings.isGrokEnabled()) ||
                    (!settings.getKimiKey().isBlank() && settings.isKimiEnabled()) ||
                    (!settings.getGlmKey().isBlank() && settings.isGlmEnabled()) ||
                    (!settings.getMistralKey().isBlank() && settings.isMistralEnabled()) ||
                    (!settings.getAwsAccessKeyId().isBlank() && !settings.getAwsSecretKey().isBlank() && settings.isAwsEnabled()) ||
                    (!settings.getAwsAccessKeyId().isBlank() && settings.getShowAwsFields()) ||
                    (!settings.getAwsProfileName().isBlank() && settings.getShowAwsFields() && settings.getShouldPowerFromAWSProfile()) ||
                    (!settings.getAwsRegion().isBlank() && settings.getShowAwsFields()) ||
                    (!settings.getAzureOpenAIKey().isBlank() && settings.getShowAzureOpenAIFields());

            project.getMessageBus()
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
        llmSettingsComponent.getLmStudioFallbackContextEnabledCheckBox().setSelected(settings.getLmStudioFallbackContextLength() != null);
        llmSettingsComponent.getLmStudioFallbackContextField().setNumber(
                settings.getLmStudioFallbackContextLength() != null ? settings.getLmStudioFallbackContextLength() : 8000
        );
        llmSettingsComponent.getLmStudioFallbackContextField().setEnabled(settings.getLmStudioFallbackContextLength() != null);
        llmSettingsComponent.getGpt4AllModelUrlField().setText(settings.getGpt4allModelUrl());
        llmSettingsComponent.getJanModelUrlField().setText(settings.getJanModelUrl());
        llmSettingsComponent.getLlamaCPPModelUrlField().setText(settings.getLlamaCPPUrl());

        llmSettingsComponent.getCustomOpenAIUrlField().setText(settings.getCustomOpenAIUrl());
        llmSettingsComponent.getCustomOpenAIModelNameField().setText(settings.getCustomOpenAIModelName());
        llmSettingsComponent.getCustomOpenAIApiKeyField().setText(settings.getCustomOpenAIApiKey());

        llmSettingsComponent.getOpenAIKeyField().setText(settings.getOpenAIKey());
        llmSettingsComponent.getMistralApiKeyField().setText(settings.getMistralKey());
        llmSettingsComponent.getAnthropicApiKeyField().setText(settings.getAnthropicKey());
        llmSettingsComponent.getGroqApiKeyField().setText(settings.getGroqKey());
        llmSettingsComponent.getDeepInfraApiKeyField().setText(settings.getDeepInfraKey());
        llmSettingsComponent.getGeminiApiKeyField().setText(settings.getGeminiKey());
        llmSettingsComponent.getDeepSeekApiKeyField().setText(settings.getDeepSeekKey());
        llmSettingsComponent.getOpenRouterApiKeyField().setText(settings.getOpenRouterKey());
        llmSettingsComponent.getKimiApiKeyField().setText(settings.getKimiKey());
        llmSettingsComponent.getGlmApiKeyField().setText(settings.getGlmKey());

        llmSettingsComponent.getEnableAzureOpenAICheckBox().setSelected(settings.getShowAzureOpenAIFields());
        llmSettingsComponent.getAzureOpenAIEndpointField().setText(settings.getAzureOpenAIEndpoint());
        llmSettingsComponent.getAzureOpenAIDeploymentField().setText(settings.getAzureOpenAIDeployment());
        llmSettingsComponent.getAzureOpenAIKeyField().setText(settings.getAzureOpenAIKey());

        llmSettingsComponent.getOllamaEnabledCheckBox().setSelected(settings.isOllamaEnabled());
        llmSettingsComponent.getLmStudioEnabledCheckBox().setSelected(settings.isLmStudioEnabled());
        llmSettingsComponent.getGpt4AllEnabledCheckBox().setSelected(settings.isGpt4AllEnabled());
        llmSettingsComponent.getJanEnabledCheckBox().setSelected(settings.isJanEnabled());
        llmSettingsComponent.getLlamaCPPEnabledCheckBox().setSelected(settings.isLlamaCPPEnabled());

        llmSettingsComponent.getCustomOpenAIUrlEnabledCheckBox().setSelected(settings.isCustomOpenAIUrlEnabled());
        llmSettingsComponent.getCustomOpenAIModelNameEnabledCheckBox().setSelected(settings.isCustomOpenAIModelNameEnabled());
        llmSettingsComponent.getCustomOpenAIForceHttp11CheckBox().setSelected(settings.isCustomOpenAIForceHttp11());


        llmSettingsComponent.getOpenAIEnabledCheckBox().setSelected(settings.isOpenAIEnabled());
        llmSettingsComponent.getMistralEnabledCheckBox().setSelected(settings.isMistralEnabled());
        llmSettingsComponent.getAnthropicEnabledCheckBox().setSelected(settings.isAnthropicEnabled());
        llmSettingsComponent.getGroqEnabledCheckBox().setSelected(settings.isGroqEnabled());
        llmSettingsComponent.getDeepInfraEnabledCheckBox().setSelected(settings.isDeepInfraEnabled());
        llmSettingsComponent.getGeminiEnabledCheckBox().setSelected(settings.isGoogleEnabled());
        llmSettingsComponent.getDeepSeekEnabledCheckBox().setSelected(settings.isDeepSeekEnabled());
        llmSettingsComponent.getOpenRouterEnabledCheckBox().setSelected(settings.isOpenRouterEnabled());
        llmSettingsComponent.getGrokEnabledCheckBox().setSelected(settings.isGrokEnabled());
        llmSettingsComponent.getKimiEnabledCheckBox().setSelected(settings.isKimiEnabled());
        llmSettingsComponent.getGlmEnabledCheckBox().setSelected(settings.isGlmEnabled());
        llmSettingsComponent.getEnableAzureOpenAICheckBox().setSelected(settings.getShowAzureOpenAIFields());
    }
}
