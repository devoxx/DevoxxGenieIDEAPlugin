package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

public class LLMProvidersComponent extends AbstractSettingsComponent {

    @Getter
    private final JTextField projectVersion = new JTextField(PropertiesService.getInstance().getVersion());
    @Getter
    private final JTextField ollamaModelUrlField = new JTextField(stateService.getOllamaModelUrl());
    @Getter
    private final JTextField lmStudioModelUrlField = new JTextField(stateService.getLmstudioModelUrl());
    @Getter
    private final JTextField gpt4AllModelUrlField = new JTextField(stateService.getGpt4allModelUrl());
    @Getter
    private final JTextField janModelUrlField = new JTextField(stateService.getJanModelUrl());
    @Getter
    private final JTextField llamaCPPModelUrlField = new JTextField(stateService.getLlamaCPPUrl());
    @Getter
    private final JTextField customOpenAIUrlField = new JTextField(stateService.getCustomOpenAIUrl());
    @Getter
    private final JTextField customOpenAIModelNameField = new JTextField(stateService.getCustomOpenAIModelName());
    @Getter
    private final JPasswordField customOpenAIApiKeyField = new JPasswordField(stateService.getCustomOpenAIApiKey());
    @Getter
    private final JPasswordField openAIKeyField = new JPasswordField(stateService.getOpenAIKey());
    @Getter
    private final JTextField azureOpenAIEndpointField = new JTextField(stateService.getAzureOpenAIEndpoint());
    @Getter
    private final JTextField azureOpenAIDeploymentField = new JTextField(stateService.getAzureOpenAIDeployment());
    @Getter
    private final JPasswordField azureOpenAIKeyField = new JPasswordField(stateService.getAzureOpenAIKey());
    @Getter
    private final JPasswordField mistralApiKeyField = new JPasswordField(stateService.getMistralKey());
    @Getter
    private final JPasswordField anthropicApiKeyField = new JPasswordField(stateService.getAnthropicKey());
    @Getter
    private final JPasswordField groqApiKeyField = new JPasswordField(stateService.getGroqKey());
    @Getter
    private final JPasswordField deepInfraApiKeyField = new JPasswordField(stateService.getDeepInfraKey());
    @Getter
    private final JPasswordField geminiApiKeyField = new JPasswordField(stateService.getGeminiKey());
    @Getter
    private final JPasswordField deepSeekApiKeyField = new JPasswordField(stateService.getDeepSeekKey());
    @Getter
    private final JPasswordField openRouterApiKeyField = new JPasswordField(stateService.getOpenRouterKey());
    @Getter
    private final JPasswordField awsSecretKeyField = new JPasswordField(stateService.getAwsSecretKey());
    @Getter
    private final JPasswordField awsAccessKeyIdField = new JPasswordField(stateService.getAwsAccessKeyId());
    @Getter
    private final JTextField awsRegion = new JTextField(stateService.getAwsRegion());
    @Getter
    private final JCheckBox streamModeCheckBox = new JCheckBox("", stateService.getStreamMode());
    @Getter
    private final JCheckBox ollamaEnabledCheckBox = new JCheckBox("", stateService.isOllamaEnabled());
    @Getter
    private final JCheckBox lmStudioEnabledCheckBox = new JCheckBox("", stateService.isLmStudioEnabled());
    @Getter
    private final JCheckBox gpt4AllEnabledCheckBox = new JCheckBox("", stateService.isGpt4AllEnabled());
    @Getter
    private final JCheckBox janEnabledCheckBox = new JCheckBox("", stateService.isJanEnabled());
    @Getter
    private final JCheckBox llamaCPPEnabledCheckBox = new JCheckBox("", stateService.isLlamaCPPEnabled());
    @Getter
    private final JCheckBox customOpenAIUrlEnabledCheckBox = new JCheckBox("", stateService.isCustomOpenAIUrlEnabled());
    @Getter
    private final JCheckBox customOpenAIModelNameEnabledCheckBox = new JCheckBox("", stateService.isCustomOpenAIModelNameEnabled());
    @Getter
    private final JCheckBox enableCustomOpenAIApiKeyCheckBox = new JCheckBox("", stateService.isCustomOpenAIApiKeyEnabled());
    @Getter
    private final JCheckBox openAIEnabledCheckBox = new JCheckBox("", stateService.isOpenAIEnabled());
    @Getter
    private final JCheckBox mistralEnabledCheckBox = new JCheckBox("", stateService.isMistralEnabled());
    @Getter
    private final JCheckBox anthropicEnabledCheckBox = new JCheckBox("", stateService.isAnthropicEnabled());
    @Getter
    private final JCheckBox groqEnabledCheckBox = new JCheckBox("", stateService.isGroqEnabled());
    @Getter
    private final JCheckBox deepInfraEnabledCheckBox = new JCheckBox("", stateService.isDeepInfraEnabled());
    @Getter
    private final JCheckBox geminiEnabledCheckBox = new JCheckBox("", stateService.isGoogleEnabled());
    @Getter
    private final JCheckBox deepSeekEnabledCheckBox = new JCheckBox("", stateService.isDeepSeekEnabled());
    @Getter
    private final JCheckBox openRouterEnabledCheckBox = new JCheckBox("", stateService.isOpenRouterEnabled());
    @Getter
    private final JCheckBox enableAzureOpenAICheckBox = new JCheckBox("", stateService.getShowAzureOpenAIFields());
    @Getter
    private final JCheckBox enableAWSCheckBox = new JCheckBox("", stateService.getShowAwsFields());

    private final List<JComponent> azureComponents = new ArrayList<>();

    private final List<JComponent> awsComponents = new ArrayList<>();

    public LLMProvidersComponent() {
        addListeners();
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);

        addSection(panel, gbc, "Local Large Language Response");
        addSettingRow(panel, gbc, "Enable Stream Mode", streamModeCheckBox);
        addSettingRow(panel, gbc, "⚠️MCP is currently not enabled in streaming mode.");

        // Local LLM Providers section
        addSection(panel, gbc, "Local LLM Providers");

        addProviderSettingRow(panel, gbc, "Ollama URL", ollamaEnabledCheckBox,
                createTextWithLinkButton(ollamaModelUrlField, "https://ollama.com"));
        addProviderSettingRow(panel, gbc, "LMStudio URL", lmStudioEnabledCheckBox,
                createTextWithLinkButton(lmStudioModelUrlField, "https://lmstudio.ai/"));
        addProviderSettingRow(panel, gbc, "GPT4All URL", gpt4AllEnabledCheckBox,
                createTextWithLinkButton(gpt4AllModelUrlField, "https://gpt4all.io/"));
        addProviderSettingRow(panel, gbc, "Jan URL", janEnabledCheckBox,
                createTextWithLinkButton(janModelUrlField, "https://jan.ai/download"));
        addProviderSettingRow(panel, gbc, "LLaMA.c++ URL", llamaCPPEnabledCheckBox,
                createTextWithLinkButton(llamaCPPModelUrlField, "https://github.com/ggerganov/llama.cpp/blob/master/examples/server/README.md"));
        addProviderSettingRow(panel, gbc, "Custom OpenAI URL", customOpenAIUrlEnabledCheckBox, customOpenAIUrlField);
        addProviderSettingRow(panel, gbc, "Custom OpenAI Model", customOpenAIModelNameEnabledCheckBox, customOpenAIModelNameField);
        addProviderSettingRow(panel, gbc, "Custom OpenAI API Key", enableCustomOpenAIApiKeyCheckBox, customOpenAIApiKeyField);

        // Cloud LLM Providers section
        addSection(panel, gbc, "Cloud LLM Providers");
        addProviderSettingRow(panel, gbc, "OpenAI API Key", openAIEnabledCheckBox,
                createTextWithPasswordButton(openAIKeyField, "https://platform.openai.com/api-keys"));
        addProviderSettingRow(panel, gbc, "Mistral API Key", mistralEnabledCheckBox,
                createTextWithPasswordButton(mistralApiKeyField, "https://console.mistral.ai/api-keys"));
        addProviderSettingRow(panel, gbc, "Anthropic API Key", anthropicEnabledCheckBox,
                createTextWithPasswordButton(anthropicApiKeyField, "https://console.anthropic.com/settings/keys"));
        addProviderSettingRow(panel, gbc, "Groq API Key", groqEnabledCheckBox,
                createTextWithPasswordButton(groqApiKeyField, "https://console.groq.com/keys"));
        addProviderSettingRow(panel, gbc, "DeepInfra API Key", deepInfraEnabledCheckBox,
                createTextWithPasswordButton(deepInfraApiKeyField, "https://deepinfra.com/dash/api_keys"));
        addProviderSettingRow(panel, gbc, "Google Gemini API Key", geminiEnabledCheckBox,
                createTextWithPasswordButton(geminiApiKeyField, "https://aistudio.google.com/app/apikey"));
        addProviderSettingRow(panel, gbc, "Deep Seek API Key", deepSeekEnabledCheckBox,
                createTextWithPasswordButton(deepSeekApiKeyField, "https://platform.deepseek.com/api_keys"));
        addProviderSettingRow(panel, gbc, "Open Router API Key", openRouterEnabledCheckBox,
                createTextWithPasswordButton(openRouterApiKeyField, "https://openrouter.ai/settings/keys"));

        addAzureOpenAIPanel(panel, gbc);
        addAWSPanel(panel, gbc);

        addSection(panel, gbc, "Plugin version");
        addSettingRow(panel, gbc, "v" + projectVersion.getText(), createTextWithLinkButton(new JLabel("View on GitHub"), "https://github.com/devoxx/DevoxxGenieIDEAPlugin"));

        return panel;
    }

    private void updateUrlFieldState(@NotNull JCheckBox checkbox,
                                     @NotNull JComponent urlComponent) {
        urlComponent.setEnabled(checkbox.isSelected());
    }

    @Override
    public void addListeners() {
        // Keep existing listeners
        enableAzureOpenAICheckBox.addItemListener(event -> {
            setNestedComponentsVisibility(azureComponents, event.getStateChange() == ItemEvent.SELECTED, true);
        });
        enableAWSCheckBox.addItemListener(event -> {
            setNestedComponentsVisibility(awsComponents, event.getStateChange() == ItemEvent.SELECTED, true);
        });

        // Add new listeners for enable/disable checkboxes
        ollamaEnabledCheckBox.addItemListener(e -> updateUrlFieldState(ollamaEnabledCheckBox, ollamaModelUrlField));
        lmStudioEnabledCheckBox.addItemListener(e -> updateUrlFieldState(lmStudioEnabledCheckBox, lmStudioModelUrlField));
        gpt4AllEnabledCheckBox.addItemListener(e -> updateUrlFieldState(gpt4AllEnabledCheckBox, gpt4AllModelUrlField));
        janEnabledCheckBox.addItemListener(e -> updateUrlFieldState(janEnabledCheckBox, janModelUrlField));
        llamaCPPEnabledCheckBox.addItemListener(e -> updateUrlFieldState(llamaCPPEnabledCheckBox, llamaCPPModelUrlField));

        customOpenAIUrlEnabledCheckBox.addItemListener(e -> updateUrlFieldState(customOpenAIUrlEnabledCheckBox, customOpenAIUrlField));
        customOpenAIModelNameEnabledCheckBox.addItemListener(e -> updateUrlFieldState(customOpenAIModelNameEnabledCheckBox, customOpenAIModelNameField));
        enableCustomOpenAIApiKeyCheckBox.addItemListener(e -> updateUrlFieldState(enableCustomOpenAIApiKeyCheckBox, customOpenAIApiKeyField));

        openAIEnabledCheckBox.addItemListener(e -> updateUrlFieldState(openAIEnabledCheckBox, openAIKeyField));
        mistralEnabledCheckBox.addItemListener(e -> updateUrlFieldState(mistralEnabledCheckBox, mistralApiKeyField));
        anthropicEnabledCheckBox.addItemListener(e -> updateUrlFieldState(anthropicEnabledCheckBox, anthropicApiKeyField));
        groqEnabledCheckBox.addItemListener(e -> updateUrlFieldState(groqEnabledCheckBox, groqApiKeyField));
        deepInfraEnabledCheckBox.addItemListener(e -> updateUrlFieldState(deepInfraEnabledCheckBox, deepInfraApiKeyField));
        geminiEnabledCheckBox.addItemListener(e -> updateUrlFieldState(geminiEnabledCheckBox, geminiApiKeyField));
        deepSeekEnabledCheckBox.addItemListener(e -> updateUrlFieldState(deepSeekEnabledCheckBox, deepSeekApiKeyField));
        openRouterEnabledCheckBox.addItemListener(e -> updateUrlFieldState(openRouterEnabledCheckBox, openRouterApiKeyField));
        enableAzureOpenAICheckBox.addItemListener(e -> updateUrlFieldState(enableAzureOpenAICheckBox, azureOpenAIEndpointField));
    }

//    // In LLMProvidersComponent.java
//    private boolean isAzureConfigValid() {
//        return !azureOpenAIKeyField.getPassword().toString().isEmpty()
//                && !azureOpenAIEndpointField.getText().trim().isEmpty()
//                && !azureOpenAIDeploymentField.getText().trim().isEmpty();
//    }

    private void addAzureOpenAIPanel(JPanel panel, GridBagConstraints gbc) {
        final String azureOpenAIUrl = "https://learn.microsoft.com/en-us/azure/ai-services/openai/overview";
        addSettingRow(panel, gbc, "Enable Azure OpenAI Provider", enableAzureOpenAICheckBox);

        addNestedSettingsRow(panel, gbc, "Azure OpenAI Endpoint",
                createTextWithLinkButton(azureOpenAIEndpointField, azureOpenAIUrl), azureComponents);
        addNestedSettingsRow(panel, gbc, "Azure OpenAI Deployment",
                createTextWithLinkButton(azureOpenAIDeploymentField, azureOpenAIUrl), azureComponents);
        addNestedSettingsRow(panel, gbc, "Azure OpenAI API Key",
                createTextWithPasswordButton(azureOpenAIKeyField, azureOpenAIUrl), azureComponents);

        // Set initial visibility
        setNestedComponentsVisibility(azureComponents, enableAzureOpenAICheckBox.isSelected(), false);
    }

    private void addAWSPanel(JPanel panel, GridBagConstraints gbc) {
        final String bedrockURL = "https://docs.aws.amazon.com/bedrock/latest/userguide/getting-started-api.html";
        addSettingRow(panel, gbc, "Enable AWS Bedrock", enableAWSCheckBox);

        addNestedSettingsRow(panel, gbc, "AWS Access Key ID",
                createTextWithLinkButton(awsAccessKeyIdField, bedrockURL), awsComponents);
        addNestedSettingsRow(panel, gbc, "AWS Secret Access Key",
                createTextWithLinkButton(awsSecretKeyField, bedrockURL), awsComponents);
        addNestedSettingsRow(panel, gbc, "AWS region",
                createTextWithPasswordButton(awsRegion, bedrockURL), awsComponents);

        // Set initial visibility
        setNestedComponentsVisibility(awsComponents, enableAWSCheckBox.isSelected(), false);
    }

    /**
     * Adds a row of settings to the panel with nested components.
     * The nested components are added to a list for visibility management.
     *
     * @param panel           The panel to add the settings row to.
     * @param gbc             The GridBagConstraints for layout.
     * @param label           The label for the setting.
     * @param component       The component associated with the setting.
     * @param componentsGroup The list to add the label and component to for visibility management.
     */
    private void addNestedSettingsRow(
            @NotNull JPanel panel,
            @NotNull GridBagConstraints gbc,
            String label,
            JComponent component,
            @NotNull final List<JComponent> componentsGroup
    ) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.insets = JBUI.insets(5, 20, 5, 5); // Indent by 20 pixels on the left
        JLabel jLabel = new JLabel(label);
        panel.add(jLabel, gbc);
        componentsGroup.add(jLabel);

        gbc.gridx = 1;
        panel.add(component, gbc);
        componentsGroup.add(component);
        gbc.gridy++;

        gbc.insets = JBUI.insets(5);
    }

    /**
     * Sets the visibility of a group of components.
     *
     * @param componentsGroup The list of components to modify.
     * @param isVisible       Whether the components should be visible.
     * @param rePaintPanel    Whether to force repaint panel after changing visibility.
     */
    private void setNestedComponentsVisibility(
            @NotNull final List<JComponent> componentsGroup,
            boolean isVisible,
            boolean rePaintPanel
    ) {
        componentsGroup.forEach(comp -> comp.setVisible(isVisible));

        if (rePaintPanel) {
            panel.revalidate();
            panel.repaint();
        }
    }
}
