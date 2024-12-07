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
    private final JTextField exoModelUrlField = new JTextField(stateService.getExoModelUrl());
    @Getter
    private final JTextField llamaCPPModelUrlField = new JTextField(stateService.getLlamaCPPUrl());
    @Getter
    private final JTextField jlamaModelUrlField = new JTextField(stateService.getJlamaUrl());
    @Getter
    private final JTextField customOpenAIUrlField = new JTextField(stateService.getCustomOpenAIUrl());
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
    private final JCheckBox streamModeCheckBox = new JCheckBox("", stateService.getStreamMode());
    @Getter
    private final JCheckBox enableAzureOpenAI = new JCheckBox("", stateService.getShowAzureOpenAIFields());

    private final java.util.List<JComponent> azureComponents = new ArrayList<>();

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
        addSettingRow(panel, gbc, "Enable Stream Mode (Beta)", streamModeCheckBox);

        addSection(panel, gbc, "Local LLM Providers");
        addSettingRow(panel, gbc, "Ollama URL", createTextWithLinkButton(ollamaModelUrlField, "https://ollama.com"));
        addSettingRow(panel, gbc, "LMStudio URL", createTextWithLinkButton(lmStudioModelUrlField, "https://lmstudio.ai/"));
        addSettingRow(panel, gbc, "GPT4All URL", createTextWithLinkButton(gpt4AllModelUrlField, "https://gpt4all.io/"));
        addSettingRow(panel, gbc, "Jan URL", createTextWithLinkButton(janModelUrlField, "https://jan.ai/download"));
        addSettingRow(panel, gbc, "Exo URL", createTextWithLinkButton(exoModelUrlField, "https://github.com/exo-explore/exo"));
        addSettingRow(panel, gbc, "LLaMA.c++ URL", createTextWithLinkButton(llamaCPPModelUrlField, "https://github.com/ggerganov/llama.cpp/blob/master/examples/server/README.md"));
        addSettingRow(panel, gbc, "JLama URL", createTextWithLinkButton(jlamaModelUrlField, "https://github.com/tjake/Jlama"));
        addSettingRow(panel, gbc, "Custom OpenAI URL", customOpenAIUrlField);

        addSection(panel, gbc, "Cloud LLM Providers");
        addSettingRow(panel, gbc, "OpenAI API Key", createTextWithPasswordButton(openAIKeyField, "https://platform.openai.com/api-keys"));
        addSettingRow(panel, gbc, "Mistral API Key", createTextWithPasswordButton(mistralApiKeyField, "https://console.mistral.ai/api-keys"));
        addSettingRow(panel, gbc, "Anthropic API Key", createTextWithPasswordButton(anthropicApiKeyField, "https://console.anthropic.com/settings/keys"));
        addSettingRow(panel, gbc, "Groq API Key", createTextWithPasswordButton(groqApiKeyField, "https://console.groq.com/keys"));
        addSettingRow(panel, gbc, "DeepInfra API Key", createTextWithPasswordButton(deepInfraApiKeyField, "https://deepinfra.com/dash/api_keys"));
        addSettingRow(panel, gbc, "Google Gemini API Key", createTextWithPasswordButton(geminiApiKeyField, "https://aistudio.google.com/app/apikey"));
        addSettingRow(panel, gbc, "Deep Seek API Key", createTextWithPasswordButton(deepSeekApiKeyField, "https://platform.deepseek.com/api_keys"));
        addSettingRow(panel, gbc, "Open Router API Key", createTextWithPasswordButton(openRouterApiKeyField, "https://openrouter.ai/settings/keys"));

        addAzureOpenAIPanel(panel, gbc);

        addSection(panel, gbc, "Plugin version");
        addSettingRow(panel, gbc, "v" + projectVersion.getText(), createTextWithLinkButton(new JLabel("View on GitHub"), "https://github.com/devoxx/DevoxxGenieIDEAPlugin"));

        return panel;
    }

    private void addAzureOpenAIPanel(JPanel panel, GridBagConstraints gbc) {
        addSettingRow(panel, gbc, "Enable Azure OpenAI Provider", enableAzureOpenAI);

        addAzureComponentsSettingRow(panel, gbc, "Azure OpenAI Endpoint", createTextWithLinkButton(azureOpenAIEndpointField, "https://learn.microsoft.com/en-us/azure/ai-services/openai/overview"));
        addAzureComponentsSettingRow(panel, gbc, "Azure OpenAI Deployment", createTextWithLinkButton(azureOpenAIDeploymentField, "https://learn.microsoft.com/en-us/azure/ai-services/openai/overview"));
        addAzureComponentsSettingRow(panel, gbc, "Azure OpenAI API Key", createTextWithPasswordButton(azureOpenAIKeyField, "https://learn.microsoft.com/en-us/azure/ai-services/openai/overview"));

        // Set initial visibility
        boolean azureEnabled = enableAzureOpenAI.isSelected();
        for (JComponent comp : azureComponents) {
            comp.setVisible(azureEnabled);
        }
    }

    private void addAzureComponentsSettingRow(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String label, JComponent component) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.insets = JBUI.insets(5, 20, 5, 5); // Indent by 20 pixels on the left
        JLabel jLabel = new JLabel(label);
        panel.add(jLabel, gbc);
        azureComponents.add(jLabel);

        gbc.gridx = 1;
        panel.add(component, gbc);
        azureComponents.add(component);
        gbc.gridy++;

        gbc.insets = JBUI.insets(5);
    }

    @Override
    public void addListeners() {
        enableAzureOpenAI.addItemListener(event -> {
            azureComponents.forEach(comp -> comp.setVisible(event.getStateChange() == ItemEvent.SELECTED));
            panel.revalidate();
            panel.repaint();
        });

    }
}
