package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class LLMProvidersComponent extends AbstractSettingsComponent {

    private final DevoxxGenieSettingsService stateService = DevoxxGenieSettingsServiceProvider.getInstance();

    public static final String LINK_EMOJI = "\uD83D\uDD17";
    public static final String PASSWORD_EMOJI = "\uD83D\uDD11";

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
    private final JPasswordField openAIKeyField = new JPasswordField(stateService.getOpenAIKey());
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
    private final JCheckBox hideSearchButtonsField = new JCheckBox("", stateService.getHideSearchButtonsFlag());
    @Getter
    private final JPasswordField tavilySearchApiKeyField = new JPasswordField(stateService.getTavilySearchKey());
    @Getter
    private final JPasswordField googleSearchApiKeyField = new JPasswordField(stateService.getGoogleSearchKey());
    @Getter
    private final JPasswordField googleCSIApiKeyField = new JPasswordField(stateService.getGoogleCSIKey());
    @Getter
    private final JBIntSpinner maxSearchResults = new JBIntSpinner(new UINumericRange(stateService.getMaxSearchResults(), 1, 10));
    @Getter
    private final JCheckBox streamModeCheckBox = new JCheckBox("", stateService.getStreamMode());

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

        addSection(panel, gbc, "Cloud LLM Providers");
        addSettingRow(panel, gbc, "OpenAI API Key", createTextWithPasswordButton(openAIKeyField, "https://platform.openai.com/api-keys"));
        addSettingRow(panel, gbc, "Mistral API Key", createTextWithPasswordButton(mistralApiKeyField, "https://console.mistral.ai/api-keys"));
        addSettingRow(panel, gbc, "Anthropic API Key", createTextWithPasswordButton(anthropicApiKeyField, "https://console.anthropic.com/settings/keys"));
        addSettingRow(panel, gbc, "Groq API Key", createTextWithPasswordButton(groqApiKeyField, "https://console.groq.com/keys"));
        addSettingRow(panel, gbc, "DeepInfra API Key", createTextWithPasswordButton(deepInfraApiKeyField, "https://deepinfra.com/dash/api_keys"));
        addSettingRow(panel, gbc, "Google Gemini API Key", createTextWithPasswordButton(geminiApiKeyField, "https://aistudio.google.com/app/apikey"));

        addSection(panel, gbc, "Search Providers");
        addSettingRow(panel, gbc, "Tavily Web Search API Key", createTextWithPasswordButton(tavilySearchApiKeyField, "https://app.tavily.com/home"));
        addSettingRow(panel, gbc, "Google Web Search API Key", createTextWithPasswordButton(googleSearchApiKeyField, "https://developers.google.com/custom-search/docs/paid_element#api_key"));
        addSettingRow(panel, gbc, "Google Custom Search Engine ID", createTextWithPasswordButton(googleCSIApiKeyField, "https://programmablesearchengine.google.com/controlpanel/create"));
        addSettingRow(panel, gbc, "Max search results", maxSearchResults);
        addSettingRow(panel, gbc, "Hide Search Providers", hideSearchButtonsField);

        addSection(panel, gbc, "Plugin version");
        addSettingRow(panel, gbc, "v" + projectVersion.getText(), createTextWithLinkButton(new JLabel("View on GitHub"), "https://github.com/devoxx/DevoxxGenieIDEAPlugin"));

        return panel;
    }

    private void addSection(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String title) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(new JXTitledSeparator(title), gbc);
        gbc.gridy++;
    }

    private void addSettingRow(@NotNull JPanel panel, @NotNull GridBagConstraints gbc, String label, JComponent component) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
        gbc.gridy++;
    }

    @Override
    public void addListeners() {
        hideSearchButtonsField.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            tavilySearchApiKeyField.setEnabled(!selected);
            googleSearchApiKeyField.setEnabled(!selected);
            googleCSIApiKeyField.setEnabled(!selected);
        });
    }

    private @NotNull JComponent createTextWithPasswordButton(JComponent jComponent, String url) {
        return createTextWithLinkButton(jComponent, PASSWORD_EMOJI, "Get your API Key from ", url);
    }

    private @NotNull JComponent createTextWithLinkButton(JComponent jComponent, String url) {
        return createTextWithLinkButton(jComponent, LINK_EMOJI, "Download from ", url);
    }

    private @NotNull JComponent createTextWithLinkButton(JComponent jComponent,
                                                         String emoji,
                                                         String toolTipMsg,
                                                         String url) {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(jComponent, BorderLayout.CENTER);

        JButton btnApiKey = new JButton(emoji);
        btnApiKey.setToolTipText(toolTipMsg + " " + url);
        btnApiKey.addActionListener(e -> {
            try {
                BrowserUtil.open(url);
            } catch (Exception ex) {
                Project project = ProjectManager.getInstance().getOpenProjects()[0];
                NotificationUtil.sendNotification(project, "Error: Unable to open the link");
            }
        });
        jPanel.add(btnApiKey, BorderLayout.WEST);
        return jPanel;
    }
}
