package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.SettingsComponent;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.JBIntSpinner;
import com.intellij.util.ui.FormBuilder;
import lombok.Getter;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class LLMSettingsComponent implements SettingsComponent {

    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

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

    public LLMSettingsComponent() {
        addListeners();
    }

    @Override
    public JPanel createSettingsPanel() {
        return FormBuilder.createFormBuilder()
            .addComponent(new JXTitledSeparator("Local Large Language Response"))
            .addVerticalGap(5)
            .addComponent(new JLabel("Enable Stream Mode (Beta)"))
            .addComponent(streamModeCheckBox)
            .addVerticalGap(20)
            .addComponent(new JXTitledSeparator("Local Large Language Models"))
            .addVerticalGap(5)
            .addComponent(new JLabel("Ollama URL"))
            .addComponent(createTextWithLinkButton(ollamaModelUrlField, "https://ollama.com"))
            .addComponent(new JLabel("LMStudio URL"))
            .addComponent(createTextWithLinkButton(lmStudioModelUrlField,"https://lmstudio.ai/"))
            .addComponent(new JLabel("GPT4All URL"))
            .addComponent(createTextWithLinkButton(gpt4AllModelUrlField, "https://gpt4all.io/"))
            .addComponent(new JLabel("Jan URL"))
            .addComponent(createTextWithLinkButton(janModelUrlField, "https://jan.ai/download"))
            .addVerticalGap(20)
            .addComponent(new JXTitledSeparator("Cloud Large Language Models"))
            .addVerticalGap(5)
            .addComponent(new JLabel("OpenAI API Key"))
            .addComponent(createTextWithPasswordButton(openAIKeyField, "https://platform.openai.com/api-keys"))
            .addComponent(new JLabel("Mistral API Key"))
            .addComponent(createTextWithPasswordButton(mistralApiKeyField, "https://console.mistral.ai/api-keys"))
            .addComponent(new JLabel("Anthropic API Key"))
            .addComponent(createTextWithPasswordButton(anthropicApiKeyField, "https://console.anthropic.com/settings/keys"))
            .addComponent(new JLabel("Groq API Key"))
            .addComponent(createTextWithPasswordButton(groqApiKeyField, "https://console.groq.com/keys"))
            .addComponent(new JLabel("DeepInfra API Key"))
            .addComponent(createTextWithPasswordButton(deepInfraApiKeyField, "https://deepinfra.com/dash/api_keys"))
            .addComponent(new JLabel("Gemini API Key"))
            .addComponent(createTextWithPasswordButton(geminiApiKeyField, "https://aistudio.google.com/app/apikey"))
            .addVerticalGap(20)
            .addComponent(new JXTitledSeparator("Search Providers"))
            .addVerticalGap(5)
            .addComponent(new JLabel("Tavily Web Search API Key"))
            .addComponent(createTextWithPasswordButton(tavilySearchApiKeyField, "https://app.tavily.com/home"))
            .addComponent(new JLabel("Google Web Search API Key"))
            .addComponent(createTextWithPasswordButton(googleSearchApiKeyField, "https://developers.google.com/custom-search/docs/paid_element#api_key"))
            .addComponent(new JLabel("Google Custom Search Engine ID"))
            .addComponent(createTextWithPasswordButton(googleCSIApiKeyField, "https://programmablesearchengine.google.com/controlpanel/create"))
            .addComponent(new JLabel("Max search results"))
            .addComponent(maxSearchResults)
            .addComponent(new JLabel("Hide Search Providers"))
            .addComponent(hideSearchButtonsField)
            .addVerticalGap(20)
            .addComponent(new JXTitledSeparator("Plugin version"))
            .addComponent(new JLabel("v" + projectVersion.getText()))
            .addComponent(createTextWithLinkButton(new JLabel("View on GitHub"), "https://github.com/devoxx/DevoxxGenieIDEAPlugin"))
            .getPanel();
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

    /**
     * Create a button with emoji and tooltip message and open the URL in the browser
     * @param jComponent the component to add the button to
     * @param toolTipMsg the tooltip message
     * @param url        the url to open when clicked
     * @return the created button
     */
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
