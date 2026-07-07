package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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
        return llmSettingsComponent.createPanelWithHelp();
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
        isModified |= Boolean.TRUE.equals(stateService.getShowThinkingEnabled())
                != llmSettingsComponent.getShowThinkingCheckBox().isSelected();

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
        isModified |= isFieldModified(llmSettingsComponent.getNvidiaApiKeyField(), stateService.getNvidiaKey());

        isModified |= isFieldModified(llmSettingsComponent.getOllamaModelUrlField(), stateService.getOllamaModelUrl());
        isModified |= Boolean.TRUE.equals(stateService.getOllamaContextWindowOverrideEnabled())
                != llmSettingsComponent.getOllamaContextWindowOverrideCheckBox().isSelected();
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
        isModified |= (stateService.getCustomOpenAIContextWindow() != null) != llmSettingsComponent.getCustomOpenAIContextWindowEnabledCheckBox().isSelected();
        if (llmSettingsComponent.getCustomOpenAIContextWindowEnabledCheckBox().isSelected()) {
            Integer savedContextWindow = stateService.getCustomOpenAIContextWindow();
            isModified |= savedContextWindow == null || !savedContextWindow.equals(llmSettingsComponent.getCustomOpenAIContextWindowField().getNumber());
        }
        isModified |= costFieldModified(stateService.getCustomOpenAIInputCost(), llmSettingsComponent.getCustomOpenAIInputCostField());
        isModified |= costFieldModified(stateService.getCustomOpenAIOutputCost(), llmSettingsComponent.getCustomOpenAIOutputCostField());

        isModified |= !stateService.getShowAzureOpenAIFields().equals(llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIEndpointField(), stateService.getAzureOpenAIEndpoint());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIDeploymentField(), stateService.getAzureOpenAIDeployment());
        isModified |= isFieldModified(llmSettingsComponent.getAzureOpenAIKeyField(), stateService.getAzureOpenAIKey());

        isModified |= !stateService.getShowAwsFields().equals(llmSettingsComponent.getEnableAWSCheckBox().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAwsSecretKeyField(), stateService.getAwsSecretKey());
        isModified |= isFieldModified(llmSettingsComponent.getAwsBearerTokenField(), stateService.getAwsBearerToken());
        isModified |= isFieldModified(llmSettingsComponent.getAwsAccessKeyIdField(), stateService.getAwsAccessKeyId());
        isModified |= stateService.getAwsBedrockAuthMode() != llmSettingsComponent.getAwsAuthModeComboBox().getSelectedItem();
        isModified |= !stateService.getShouldEnableAWSRegionalInference().equals(llmSettingsComponent.getEnableAWSRegionalInferenceCheckBox().isSelected());
        isModified |= isFieldModified(llmSettingsComponent.getAwsProfileName(), stateService.getAwsProfileName());
        isModified |= isFieldModified(llmSettingsComponent.getAwsRegion(), stateService.getAwsRegion());

        isModified |= stateService.isOllamaEnabled() != llmSettingsComponent.getOllamaEnabledCheckBox().isSelected();
        isModified |= stateService.isLmStudioEnabled() != llmSettingsComponent.getLmStudioEnabledCheckBox().isSelected();
        isModified |= stateService.isGpt4AllEnabled() != llmSettingsComponent.getGpt4AllEnabledCheckBox().isSelected();
        isModified |= stateService.isJanEnabled() != llmSettingsComponent.getJanEnabledCheckBox().isSelected();
        isModified |= stateService.isLlamaCPPEnabled() != llmSettingsComponent.getLlamaCPPEnabledCheckBox().isSelected();
        isModified |= stateService.isExoEnabled() != llmSettingsComponent.getExoEnabledCheckBox().isSelected();
        isModified |= isFieldModified(llmSettingsComponent.getExoModelUrlField(), stateService.getExoModelUrl());

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
        isModified |= stateService.isNvidiaEnabled() != llmSettingsComponent.getNvidiaEnabledCheckBox().isSelected();
        isModified |= stateService.getShowAzureOpenAIFields() != llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected();

        return isModified;
    }

    /**
     * Apply the changes to the settings
     */
    @Override
    public void apply() throws ConfigurationException {
        validateEnabledProviders();

        boolean isModified = isModified();

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        settings.setStreamMode(llmSettingsComponent.getStreamModeCheckBox().isSelected());
        settings.setShowThinkingEnabled(llmSettingsComponent.getShowThinkingCheckBox().isSelected());

        settings.setOllamaModelUrl(llmSettingsComponent.getOllamaModelUrlField().getText());
        settings.setOllamaContextWindowOverrideEnabled(llmSettingsComponent.getOllamaContextWindowOverrideCheckBox().isSelected());
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
        settings.setCustomOpenAIContextWindow(
                llmSettingsComponent.getCustomOpenAIContextWindowEnabledCheckBox().isSelected()
                        ? llmSettingsComponent.getCustomOpenAIContextWindowField().getNumber()
                        : null
        );
        settings.setCustomOpenAIInputCost(costFieldValue(llmSettingsComponent.getCustomOpenAIInputCostField()));
        settings.setCustomOpenAIOutputCost(costFieldValue(llmSettingsComponent.getCustomOpenAIOutputCostField()));

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
        settings.setNvidiaKey(new String(llmSettingsComponent.getNvidiaApiKeyField().getPassword()));

        settings.setShowAzureOpenAIFields(llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected());
        settings.setAzureOpenAIEndpoint(llmSettingsComponent.getAzureOpenAIEndpointField().getText());
        settings.setAzureOpenAIDeployment(llmSettingsComponent.getAzureOpenAIDeploymentField().getText());
        settings.setAzureOpenAIKey(new String(llmSettingsComponent.getAzureOpenAIKeyField().getPassword()));

        settings.setShowAwsFields(llmSettingsComponent.getEnableAWSCheckBox().isSelected());
        settings.setAwsAccessKeyId(new String(llmSettingsComponent.getAwsAccessKeyIdField().getPassword()));
        settings.setAwsSecretKey(new String(llmSettingsComponent.getAwsSecretKeyField().getPassword()));
        settings.setAwsBearerToken(new String(llmSettingsComponent.getAwsBearerTokenField().getPassword()));
        settings.setAwsRegion(llmSettingsComponent.getAwsRegion().getText());
        settings.setAwsBedrockAuthMode((com.devoxx.genie.model.enumarations.AwsBedrockAuthMode) llmSettingsComponent.getAwsAuthModeComboBox().getSelectedItem());
        settings.setShouldEnableAWSRegionalInference(llmSettingsComponent.getEnableAWSRegionalInferenceCheckBox().isSelected());
        settings.setAwsProfileName(llmSettingsComponent.getAwsProfileName().getText());

        settings.setOllamaEnabled(llmSettingsComponent.getOllamaEnabledCheckBox().isSelected());
        settings.setLmStudioEnabled(llmSettingsComponent.getLmStudioEnabledCheckBox().isSelected());
        settings.setGpt4AllEnabled(llmSettingsComponent.getGpt4AllEnabledCheckBox().isSelected());
        settings.setJanEnabled(llmSettingsComponent.getJanEnabledCheckBox().isSelected());
        settings.setLlamaCPPEnabled(llmSettingsComponent.getLlamaCPPEnabledCheckBox().isSelected());
        settings.setExoEnabled(llmSettingsComponent.getExoEnabledCheckBox().isSelected());
        settings.setExoModelUrl(llmSettingsComponent.getExoModelUrlField().getText());

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
        settings.setNvidiaEnabled(llmSettingsComponent.getNvidiaEnabledCheckBox().isSelected());
        settings.setShowAzureOpenAIFields(llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected());

        // Only notify the listener if an API key has changed, so we can refresh the LLM providers list in the UI
        if (isModified) {
            project.getMessageBus()
                    .syncPublisher(AppTopics.SETTINGS_CHANGED_TOPIC)
                    .settingsChanged(isAnyApiKeyEnabled(settings));
        }
    }

    /**
     * Reject the Apply when an enabled cloud provider is missing its credential, so a
     * half-configured provider can never be saved (it would only fail later at prompt time).
     * Validates the dialog state, not the stored settings, so previously saved invalid
     * configurations are also flagged the next time this panel is applied.
     *
     * @throws ConfigurationException listing every enabled provider with a missing credential
     */
    private void validateEnabledProviders() throws ConfigurationException {
        List<String> problems = new ArrayList<>();

        checkApiKeyProvider(problems, "OpenAI", llmSettingsComponent.getOpenAIEnabledCheckBox(), llmSettingsComponent.getOpenAIKeyField());
        checkApiKeyProvider(problems, "Mistral", llmSettingsComponent.getMistralEnabledCheckBox(), llmSettingsComponent.getMistralApiKeyField());
        checkApiKeyProvider(problems, "Anthropic", llmSettingsComponent.getAnthropicEnabledCheckBox(), llmSettingsComponent.getAnthropicApiKeyField());
        checkApiKeyProvider(problems, "Groq", llmSettingsComponent.getGroqEnabledCheckBox(), llmSettingsComponent.getGroqApiKeyField());
        checkApiKeyProvider(problems, "DeepInfra", llmSettingsComponent.getDeepInfraEnabledCheckBox(), llmSettingsComponent.getDeepInfraApiKeyField());
        checkApiKeyProvider(problems, "Google Gemini", llmSettingsComponent.getGeminiEnabledCheckBox(), llmSettingsComponent.getGeminiApiKeyField());
        checkApiKeyProvider(problems, "DeepSeek", llmSettingsComponent.getDeepSeekEnabledCheckBox(), llmSettingsComponent.getDeepSeekApiKeyField());
        checkApiKeyProvider(problems, "OpenRouter", llmSettingsComponent.getOpenRouterEnabledCheckBox(), llmSettingsComponent.getOpenRouterApiKeyField());
        checkApiKeyProvider(problems, "Grok", llmSettingsComponent.getGrokEnabledCheckBox(), llmSettingsComponent.getGrokApiKeyField());
        checkApiKeyProvider(problems, "Kimi", llmSettingsComponent.getKimiEnabledCheckBox(), llmSettingsComponent.getKimiApiKeyField());
        checkApiKeyProvider(problems, "GLM", llmSettingsComponent.getGlmEnabledCheckBox(), llmSettingsComponent.getGlmApiKeyField());
        checkApiKeyProvider(problems, "NVIDIA", llmSettingsComponent.getNvidiaEnabledCheckBox(), llmSettingsComponent.getNvidiaApiKeyField());

        validateAzureOpenAI(problems);
        validateAwsBedrock(problems);

        if (!problems.isEmpty()) {
            throw new ConfigurationException(String.join("\n", problems), "Incomplete LLM Provider Configuration");
        }
    }

    private static void checkApiKeyProvider(List<String> problems,
                                            String displayName,
                                            @NotNull JCheckBox enabledCheckBox,
                                            @NotNull JPasswordField keyField) {
        if (enabledCheckBox.isSelected() && new String(keyField.getPassword()).isBlank()) {
            problems.add(displayName + " is enabled but has no API key.");
        }
    }

    private void validateAzureOpenAI(List<String> problems) {
        if (!llmSettingsComponent.getEnableAzureOpenAICheckBox().isSelected()) {
            return;
        }
        if (new String(llmSettingsComponent.getAzureOpenAIKeyField().getPassword()).isBlank()) {
            problems.add("Azure OpenAI is enabled but has no API key.");
        }
        if (llmSettingsComponent.getAzureOpenAIEndpointField().getText().isBlank()) {
            problems.add("Azure OpenAI is enabled but has no endpoint.");
        }
        if (llmSettingsComponent.getAzureOpenAIDeploymentField().getText().isBlank()) {
            problems.add("Azure OpenAI is enabled but has no deployment name.");
        }
    }

    private void validateAwsBedrock(List<String> problems) {
        if (!llmSettingsComponent.getEnableAWSCheckBox().isSelected()) {
            return;
        }
        AwsBedrockAuthMode authMode = (AwsBedrockAuthMode) llmSettingsComponent.getAwsAuthModeComboBox().getSelectedItem();
        if (authMode == null) {
            authMode = AwsBedrockAuthMode.defaultMode();
        }
        switch (authMode) {
            case ACCESS_KEY -> {
                if (new String(llmSettingsComponent.getAwsAccessKeyIdField().getPassword()).isBlank() ||
                        new String(llmSettingsComponent.getAwsSecretKeyField().getPassword()).isBlank()) {
                    problems.add("AWS Bedrock is enabled but has no access key ID and/or secret access key.");
                }
            }
            case PROFILE -> {
                if (llmSettingsComponent.getAwsProfileName().getText().isBlank()) {
                    problems.add("AWS Bedrock is enabled but has no profile name.");
                }
            }
            case BEARER_TOKEN -> {
                if (new String(llmSettingsComponent.getAwsBearerTokenField().getPassword()).isBlank()) {
                    problems.add("AWS Bedrock is enabled but has no bearer token.");
                }
            }
        }
    }

    /**
     * A cost spinner is "modified" when its value differs from the stored setting, treating a null
     * stored value as 0 (the "no cost" / hidden state).
     */
    private static boolean costFieldModified(Double storedCost, @NotNull JSpinner field) {
        double stored = storedCost != null ? storedCost : 0.0d;
        return Double.compare(stored, spinnerDouble(field)) != 0;
    }

    /**
     * Reads a cost spinner value, normalising 0 (or below) to {@code null} so an unset cost is not
     * persisted as an explicit 0 — keeping the "no cost configured" semantics.
     */
    private static Double costFieldValue(@NotNull JSpinner field) {
        double value = spinnerDouble(field);
        return value > 0 ? value : null;
    }

    private static double spinnerDouble(@NotNull JSpinner field) {
        return ((Number) field.getValue()).doubleValue();
    }

    private boolean isAnyApiKeyEnabled(DevoxxGenieStateService settings) {
        return hasEnabledMainCloudKey(settings) || hasEnabledAuxCloudKey(settings) || hasEnabledAwsOrAzureKey(settings);
    }

    private boolean hasEnabledMainCloudKey(DevoxxGenieStateService settings) {
        return (!settings.getAnthropicKey().isBlank() && settings.isAnthropicEnabled()) ||
                (!settings.getOpenAIKey().isBlank() && settings.isOpenAIEnabled()) ||
                (!settings.getMistralKey().isBlank() && settings.isMistralEnabled()) ||
                (!settings.getGeminiKey().isBlank() && settings.isGoogleEnabled()) ||
                (!settings.getGroqKey().isBlank() && settings.isGroqEnabled()) ||
                (!settings.getDeepInfraKey().isBlank() && settings.isDeepInfraEnabled());
    }

    private boolean hasEnabledAuxCloudKey(DevoxxGenieStateService settings) {
        return (!settings.getDeepSeekKey().isBlank() && settings.isDeepSeekEnabled()) ||
                (!settings.getOpenRouterKey().isBlank() && settings.isOpenRouterEnabled()) ||
                (!settings.getGrokKey().isBlank() && settings.isGrokEnabled()) ||
                (!settings.getKimiKey().isBlank() && settings.isKimiEnabled()) ||
                (!settings.getGlmKey().isBlank() && settings.isGlmEnabled()) ||
                (!settings.getNvidiaKey().isBlank() && settings.isNvidiaEnabled()) ||
                (!settings.getCustomOpenAIApiKey().isBlank() && settings.isCustomOpenAIApiKeyEnabled());
    }

    private boolean hasEnabledAwsOrAzureKey(DevoxxGenieStateService settings) {
        return settings.isAwsEnabled() ||
                (!settings.getAzureOpenAIKey().isBlank() && settings.getShowAzureOpenAIFields());
    }

    /**
     * Reset the text area to the default value
     */
    @Override
    public void reset() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        llmSettingsComponent.getStreamModeCheckBox().setSelected(settings.getStreamMode());
        llmSettingsComponent.getShowThinkingCheckBox().setSelected(Boolean.TRUE.equals(settings.getShowThinkingEnabled()));

        llmSettingsComponent.getOllamaModelUrlField().setText(settings.getOllamaModelUrl());
        llmSettingsComponent.getOllamaContextWindowOverrideCheckBox().setSelected(Boolean.TRUE.equals(settings.getOllamaContextWindowOverrideEnabled()));
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
        llmSettingsComponent.getCustomOpenAIContextWindowEnabledCheckBox().setSelected(settings.getCustomOpenAIContextWindow() != null);
        llmSettingsComponent.getCustomOpenAIContextWindowField().setNumber(
                settings.getCustomOpenAIContextWindow() != null
                        ? settings.getCustomOpenAIContextWindow()
                        : com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW
        );
        llmSettingsComponent.getCustomOpenAIContextWindowField().setEnabled(settings.getCustomOpenAIContextWindow() != null);
        llmSettingsComponent.getCustomOpenAIInputCostField().setValue(settings.getCustomOpenAIInputCost() != null ? settings.getCustomOpenAIInputCost() : 0.0d);
        llmSettingsComponent.getCustomOpenAIOutputCostField().setValue(settings.getCustomOpenAIOutputCost() != null ? settings.getCustomOpenAIOutputCost() : 0.0d);

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
        llmSettingsComponent.getNvidiaApiKeyField().setText(settings.getNvidiaKey());

        llmSettingsComponent.getEnableAzureOpenAICheckBox().setSelected(settings.getShowAzureOpenAIFields());
        llmSettingsComponent.getAzureOpenAIEndpointField().setText(settings.getAzureOpenAIEndpoint());
        llmSettingsComponent.getAzureOpenAIDeploymentField().setText(settings.getAzureOpenAIDeployment());
        llmSettingsComponent.getAzureOpenAIKeyField().setText(settings.getAzureOpenAIKey());

        llmSettingsComponent.getEnableAWSCheckBox().setSelected(settings.getShowAwsFields());
        llmSettingsComponent.getAwsAccessKeyIdField().setText(settings.getAwsAccessKeyId());
        llmSettingsComponent.getAwsSecretKeyField().setText(settings.getAwsSecretKey());
        llmSettingsComponent.getAwsBearerTokenField().setText(settings.getAwsBearerToken());
        llmSettingsComponent.getAwsProfileName().setText(settings.getAwsProfileName());
        llmSettingsComponent.getAwsRegion().setText(settings.getAwsRegion());
        llmSettingsComponent.getAwsAuthModeComboBox().setSelectedItem(settings.getAwsBedrockAuthMode());
        llmSettingsComponent.getEnableAWSRegionalInferenceCheckBox().setSelected(settings.getShouldEnableAWSRegionalInference());
        llmSettingsComponent.refreshAwsSettingsVisibility();

        llmSettingsComponent.getOllamaEnabledCheckBox().setSelected(settings.isOllamaEnabled());
        llmSettingsComponent.getLmStudioEnabledCheckBox().setSelected(settings.isLmStudioEnabled());
        llmSettingsComponent.getGpt4AllEnabledCheckBox().setSelected(settings.isGpt4AllEnabled());
        llmSettingsComponent.getJanEnabledCheckBox().setSelected(settings.isJanEnabled());
        llmSettingsComponent.getLlamaCPPEnabledCheckBox().setSelected(settings.isLlamaCPPEnabled());
        llmSettingsComponent.getExoEnabledCheckBox().setSelected(settings.isExoEnabled());
        llmSettingsComponent.getExoModelUrlField().setText(settings.getExoModelUrl());

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
        llmSettingsComponent.getNvidiaEnabledCheckBox().setSelected(settings.isNvidiaEnabled());
        llmSettingsComponent.getEnableAzureOpenAICheckBox().setSelected(settings.getShowAzureOpenAIFields());
    }
}
