package com.devoxx.genie.ui.settings.llm;

import com.devoxx.genie.chatmodel.local.customopenai.CustomOpenAIContextWindow;
import com.devoxx.genie.chatmodel.local.nativ.NativChatModelFactory;
import com.devoxx.genie.model.enumarations.AwsBedrockAuthMode;
import com.devoxx.genie.service.PropertiesService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.intellij.ide.ui.UINumericRange;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

public class LLMProvidersComponent extends AbstractSettingsComponent {

    /**
     * Preferred width of text/password fields, expressed in columns. Keeps a long stored
     * value from dictating the field's preferred width; the field still grows to fill the
     * available horizontal space.
     */
    private static final int TEXT_FIELD_COLUMNS = 20;

    /**
     * Max line length (in characters) before a hint label wraps. Keeps long hints from
     * widening the input column — and thus the whole settings dialog — beyond the view.
     */
    private static final int HINT_WRAP_COLUMNS = 60;

    @Getter
    private final JTextField projectVersion = new JTextField(PropertiesService.getInstance().getVersion());
    @Getter
    private final JTextField ollamaModelUrlField = new JTextField(stateService.getOllamaModelUrl());
    @Getter
    private final JCheckBox ollamaContextWindowOverrideCheckBox = new JCheckBox("", Boolean.TRUE.equals(stateService.getOllamaContextWindowOverrideEnabled()));
    @Getter
    private final JTextField lmStudioModelUrlField = new JTextField(stateService.getLmstudioModelUrl());
    @Getter
    private final JCheckBox lmStudioFallbackContextEnabledCheckBox = new JCheckBox("", stateService.getLmStudioFallbackContextLength() != null);
    @Getter
    private final JBIntSpinner lmStudioFallbackContextField = new JBIntSpinner(
            new UINumericRange(
                    stateService.getLmStudioFallbackContextLength() != null ? stateService.getLmStudioFallbackContextLength() : 8000,
                    1,
                    2_000_000
            )
    );
    @Getter
    private final JTextField gpt4AllModelUrlField = new JTextField(stateService.getGpt4allModelUrl());
    @Getter
    private final JTextField janModelUrlField = new JTextField(stateService.getJanModelUrl());
    @Getter
    private final JTextField llamaCPPModelUrlField = new JTextField(stateService.getLlamaCPPUrl());
    @Getter
    private final JTextField nativModelUrlField = new JTextField(stateService.getNativModelUrl());
    @Getter
    private final JCheckBox nativFallbackContextEnabledCheckBox = new JCheckBox("", stateService.getNativFallbackContextLength() != null);
    @Getter
    private final JBIntSpinner nativFallbackContextField = new JBIntSpinner(
            new UINumericRange(
                    stateService.getNativFallbackContextLength() != null
                            ? stateService.getNativFallbackContextLength()
                            : NativChatModelFactory.DEFAULT_CONTEXT_LENGTH,
                    1,
                    2_000_000
            )
    );
    @Getter
    private final JTextField customOpenAIUrlField = new JTextField(stateService.getCustomOpenAIUrl());
    @Getter
    private final JTextField customOpenAIModelNameField = new JTextField(stateService.getCustomOpenAIModelName());
    @Getter
    private final JPasswordField customOpenAIApiKeyField = new JPasswordField(stateService.getCustomOpenAIApiKey());
    @Getter
    private final JCheckBox customOpenAIContextWindowEnabledCheckBox = new JCheckBox("", stateService.getCustomOpenAIContextWindow() != null);
    @Getter
    private final JBIntSpinner customOpenAIContextWindowField = new JBIntSpinner(
            new UINumericRange(
                    stateService.getCustomOpenAIContextWindow() != null
                            ? stateService.getCustomOpenAIContextWindow()
                            : CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW,
                    1,
                    2_000_000
            )
    );
    @Getter
    private final JSpinner customOpenAIInputCostField = new JSpinner(
            new SpinnerNumberModel(
                    stateService.getCustomOpenAIInputCost() != null ? stateService.getCustomOpenAIInputCost() : 0.0d,
                    0.0d, 1_000_000.0d, 0.01d));
    @Getter
    private final JSpinner customOpenAIOutputCostField = new JSpinner(
            new SpinnerNumberModel(
                    stateService.getCustomOpenAIOutputCost() != null ? stateService.getCustomOpenAIOutputCost() : 0.0d,
                    0.0d, 1_000_000.0d, 0.01d));
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
    private final JPasswordField grokApiKeyField = new JPasswordField(stateService.getGrokKey());
    @Getter
    private final JPasswordField kimiApiKeyField = new JPasswordField(stateService.getKimiKey());
    @Getter
    private final JPasswordField glmApiKeyField = new JPasswordField(stateService.getGlmKey());
    @Getter
    private final JPasswordField nvidiaApiKeyField = new JPasswordField(stateService.getNvidiaKey());
    @Getter
    private final JPasswordField cloudflareApiKeyField = new JPasswordField(stateService.getCloudflareKey());
    @Getter
    private final JTextField cloudflareAccountIdField = new JTextField(stateService.getCloudflareAccountId());
    @Getter
    private final JTextField cloudflareGatewayNameField = new JTextField(stateService.getCloudflareGatewayName());
    @Getter
    private final JTextField cloudflareModelNameField = new JTextField(stateService.getCloudflareModelName());
    @Getter
    private final JPasswordField awsSecretKeyField = new JPasswordField(stateService.getAwsSecretKey());
    @Getter
    private final JPasswordField awsBearerTokenField = new JPasswordField(stateService.getAwsBearerToken());
    @Getter
    private final JTextField awsProfileName = new JTextField(stateService.getAwsProfileName());
    @Getter
    private final JPasswordField awsAccessKeyIdField = new JPasswordField(stateService.getAwsAccessKeyId());
    @Getter
    private final JTextField awsRegion = new JTextField(stateService.getAwsRegion());
    @Getter
    private final JCheckBox streamModeCheckBox = new JCheckBox("", stateService.getStreamMode());
    @Getter
    private final JCheckBox showThinkingCheckBox = new JCheckBox("", Boolean.TRUE.equals(stateService.getShowThinkingEnabled()));
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
    private final JCheckBox nativEnabledCheckBox = new JCheckBox("", stateService.isNativEnabled());
    @Getter
    private final JTextField exoModelUrlField = new JTextField(stateService.getExoModelUrl());
    @Getter
    private final JCheckBox exoEnabledCheckBox = new JCheckBox("", stateService.isExoEnabled());
    @Getter
    private final JCheckBox customOpenAIUrlEnabledCheckBox = new JCheckBox("", stateService.isCustomOpenAIUrlEnabled());
    @Getter
    private final JCheckBox customOpenAIForceHttp11CheckBox = new JCheckBox("", stateService.isCustomOpenAIForceHttp11());
    @Getter
    private final JCheckBox customOpenAIModelNameEnabledCheckBox = new JCheckBox("", stateService.isCustomOpenAIModelNameEnabled());
    @Getter
    private final JCheckBox enableCustomOpenAIApiKeyCheckBox = new JCheckBox("", stateService.isCustomOpenAIApiKeyEnabled());
    @Getter
    private final JCheckBox customOpenAIUseMaxCompletionTokensCheckBox = new JCheckBox("", stateService.isCustomOpenAIUseMaxCompletionTokens());
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
    private final JCheckBox grokEnabledCheckBox = new JCheckBox("", stateService.isGrokEnabled());
    @Getter
    private final JCheckBox kimiEnabledCheckBox = new JCheckBox("", stateService.isKimiEnabled());
    @Getter
    private final JCheckBox glmEnabledCheckBox = new JCheckBox("", stateService.isGlmEnabled());
    @Getter
    private final JCheckBox nvidiaEnabledCheckBox = new JCheckBox("", stateService.isNvidiaEnabled());
    @Getter
    private final JCheckBox cloudflareEnabledCheckBox = new JCheckBox("", stateService.isCloudflareEnabled());
    @Getter
    private final JCheckBox cloudflareModelNameEnabledCheckBox = new JCheckBox("", stateService.isCloudflareModelNameEnabled());
    @Getter
    private final JCheckBox enableAzureOpenAICheckBox = new JCheckBox("", stateService.getShowAzureOpenAIFields());
    @Getter
    private final JCheckBox enableAWSCheckBox = new JCheckBox("", stateService.getShowAwsFields());
    @Getter
    private final JCheckBox enableAWSRegionalInferenceCheckBox = new JCheckBox("", stateService.getShouldEnableAWSRegionalInference());
    @Getter
    private final JComboBox<AwsBedrockAuthMode> awsAuthModeComboBox = new JComboBox<>(AwsBedrockAuthMode.values());

    private final List<JComponent> azureComponents = new ArrayList<>();

    private final List<JComponent> awsCommonComponents = new ArrayList<>();
    private final List<JComponent> awsAccessKeyComponents = new ArrayList<>();
    private final List<JComponent> awsProfileComponents = new ArrayList<>();
    private final List<JComponent> awsBearerTokenComponents = new ArrayList<>();

    public LLMProvidersComponent() {
        awsAuthModeComboBox.setSelectedItem(stateService.getAwsBedrockAuthMode());
        addListeners();
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/llm-providers/overview";
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new BorderLayout());

        // Everything is stacked in NORTH so the page is exactly as tall as its content: in CENTER
        // the stack would be stretched to the dialog height, stranding the Plugin version footer
        // at the bottom with a wide gap above it on the shorter tabs.
        JPanel stack = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSectionConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        stack.add(createResponsePanel(), gbc);
        gbc.gridy++;
        stack.add(createProviderTabs(), gbc);
        gbc.gridy++;
        stack.add(createVersionPanel(), gbc);

        panel.add(stack, BorderLayout.NORTH);

        // Walks the whole tree, so every tab's fields are bounded regardless of which tab is
        // selected: a JTabbedPane keeps all its pages as children, not just the visible one.
        boundTextFieldWidths(panel);

        return panel;
    }

    /**
     * The provider settings outgrew a single scrolling column, so they are split across tabs (same
     * {@link JBTabbedPane} treatment as the Appearance page). Tab order follows how a provider is
     * usually chosen: local first, then the hosted ones, with the custom endpoint last.
     */
    private @NotNull JTabbedPane createProviderTabs() {
        JTabbedPane tabbedPane = new SelectedTabSizedPane();
        tabbedPane.addTab("Local", topAnchored(createLocalProvidersPanel()));
        tabbedPane.addTab("Cloud", topAnchored(createCloudProvidersPanel()));
        tabbedPane.addTab("Custom OpenAI", topAnchored(createCustomOpenAIPanel()));
        // Preferred height changes with the selection, so the parent has to lay out again.
        tabbedPane.addChangeListener(event -> {
            tabbedPane.revalidate();
            tabbedPane.repaint();
        });
        return tabbedPane;
    }

    /**
     * A tabbed pane that is only as tall as the tab currently shown.
     *
     * <p>{@link JTabbedPane#getPreferredSize()} reserves room for the <em>tallest</em> page, so the
     * short tabs (Local, Custom OpenAI) inherited the very long Cloud tab's height and rendered a
     * large empty band under their last row. Swapping the tallest page's height for the selected
     * one's keeps the surrounding layout — notably the Plugin version footer — tight against the
     * content actually on screen.</p>
     */
    private static class SelectedTabSizedPane extends JBTabbedPane {

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            Component selected = getSelectedComponent();
            if (selected == null) {
                return preferred;
            }

            int tallestPage = 0;
            for (int i = 0; i < getTabCount(); i++) {
                tallestPage = Math.max(tallestPage, getComponentAt(i).getPreferredSize().height);
            }

            // super's height is the tab strip plus the tallest page; keep the strip, swap the page.
            preferred.height += selected.getPreferredSize().height - tallestPage;
            return preferred;
        }
    }

    /**
     * Pins a tab's rows to the top of the tab.
     *
     * <p>{@link GridBagLayout} centers its grid vertically when no row claims the slack via
     * {@code weighty}, so a tab shorter than the pane floated in the middle with a gap under the
     * tab strip. {@code BorderLayout.NORTH} gives the content its preferred height instead.</p>
     */
    private static @NotNull JPanel topAnchored(@NotNull JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(content, BorderLayout.NORTH);
        return wrapper;
    }

    /**
     * Response settings apply to every provider, so they stay above the tabs rather than being
     * filed under one of them.
     */
    private @NotNull JPanel createResponsePanel() {
        JPanel responsePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSectionConstraints();

        addSection(responsePanel, gbc, "Large Language Model Response");
        addSettingRow(responsePanel, gbc, "Enable Stream Mode", streamModeCheckBox);
        addSettingRow(responsePanel, gbc, "Show Thinking", showThinkingCheckBox);
        addHintText(responsePanel, gbc, "When enabled, reasoning models (Ollama, LMStudio, Jan, Llama.cpp, DeepSeek, Mistral, ...) show their thinking in a separate section before the final answer.");

        return responsePanel;
    }

    private @NotNull JPanel createVersionPanel() {
        JPanel versionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSectionConstraints();

        addSection(versionPanel, gbc, "Plugin version");
        addSettingRow(versionPanel, gbc, "v" + projectVersion.getText(), createTextWithLinkButton(new JLabel("View on GitHub"), "https://github.com/devoxx/DevoxxGenieIDEAPlugin"));

        return versionPanel;
    }

    /**
     * Constraints for a tab's own {@link GridBagLayout}. Each tab lays out independently, so its
     * rows start at {@code gridy = 0} instead of continuing the single running counter the whole
     * page used to share.
     */
    private static @NotNull GridBagConstraints createSectionConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(5);
        return gbc;
    }

    private @NotNull JPanel createLocalProvidersPanel() {
        JPanel localPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSectionConstraints();

        addProviderSettingRow(localPanel, gbc, "Ollama URL", ollamaEnabledCheckBox,
                createTextWithDownloadButton(ollamaModelUrlField, "https://ollama.com"));
        addProviderSettingRow(localPanel, gbc, "Ollama Request Context Override", ollamaContextWindowOverrideCheckBox);
        addHintText(localPanel, gbc, "When enabled, DevoxxGenie sends Ollama num_ctx from discovered model metadata; when disabled, Ollama keeps its own runtime default.");
        addProviderSettingRow(localPanel, gbc, "LMStudio URL", lmStudioEnabledCheckBox,
                createTextWithDownloadButton(lmStudioModelUrlField, "https://lmstudio.ai/"));
        // Add hint text for LMStudio URL
        addHintText(localPanel, gbc, "Base URL for OpenAI-compatible chat; model metadata is always fetched from /api/v1/models");
        addProviderSettingRow(localPanel, gbc, "LMStudio Fallback Context", lmStudioFallbackContextEnabledCheckBox, lmStudioFallbackContextField);
        addHintText(localPanel, gbc, "Used only when LMStudio model metadata does not expose context length");
        addProviderSettingRow(localPanel, gbc, "GPT4All URL", gpt4AllEnabledCheckBox,
                createTextWithDownloadButton(gpt4AllModelUrlField, "https://gpt4all.io/"));
        addProviderSettingRow(localPanel, gbc, "Jan URL", janEnabledCheckBox,
                createTextWithDownloadButton(janModelUrlField, "https://jan.ai/download"));
        addProviderSettingRow(localPanel, gbc, "LLaMA.c++ URL", llamaCPPEnabledCheckBox,
                createTextWithDownloadButton(llamaCPPModelUrlField, "https://github.com/ggml-org/llama.cpp"));
        addProviderSettingRow(localPanel, gbc, "Nativ URL", nativEnabledCheckBox,
                createTextWithDownloadButton(nativModelUrlField, "https://blaizzy.github.io/nativ/"));
        addHintText(localPanel, gbc, "Run MLX models locally on Apple Silicon. Nativ defaults to port 8080 — the same port as Llama.c++ — so change one of the two if you want both enabled.");
        addProviderSettingRow(localPanel, gbc, "Nativ Fallback Context", nativFallbackContextEnabledCheckBox, nativFallbackContextField);
        addHintText(localPanel, gbc, "Nativ's <code>/v1/models</code> does not report a context length; DevoxxGenie assumes " + NativChatModelFactory.DEFAULT_CONTEXT_LENGTH + " tokens unless you set it here.");
        addProviderSettingRow(localPanel, gbc, "Exo URL", exoEnabledCheckBox,
                createTextWithInfoButton(exoModelUrlField, "https://genie.devoxx.com/docs/llm-providers/exo"));
        addHintText(localPanel, gbc, "Distributed AI cluster — auto-creates model instances across connected devices");

        return localPanel;
    }

    private @NotNull JPanel createCustomOpenAIPanel() {
        JPanel customOpenAIPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSectionConstraints();

        addProviderSettingRow(customOpenAIPanel, gbc, "URL", customOpenAIUrlEnabledCheckBox, customOpenAIUrlField);
        addHintText(customOpenAIPanel, gbc, "Base URL only — do <b>not</b> include <code>/chat/completions</code>; DevoxxGenie appends the OpenAI paths itself. " +
                "Example (Cloudflare AI Gateway): <code>https://gateway.ai.cloudflare.com/v1/&lt;account_id&gt;/&lt;gateway&gt;/compat</code>. " +
                "Gateways that require authentication also need the API key below.");
        addProviderSettingRow(customOpenAIPanel, gbc, "Model", customOpenAIModelNameEnabledCheckBox, customOpenAIModelNameField);
        addHintText(customOpenAIPanel, gbc, "When enabled, this exact model name is used and the model dropdown is not auto-discovered from the endpoint's <code>/models</code> — set this if the endpoint has no <code>/models</code> or returns 401.");
        addProviderSettingRow(customOpenAIPanel, gbc, "API Key", enableCustomOpenAIApiKeyCheckBox, customOpenAIApiKeyField);
        addProviderSettingRow(customOpenAIPanel, gbc, "HTTP 1.1", customOpenAIForceHttp11CheckBox);
        addHintText(customOpenAIPanel, gbc, "Use HTTP/2 when unchecked");
        addProviderSettingRow(customOpenAIPanel, gbc, "max_completion_tokens", customOpenAIUseMaxCompletionTokensCheckBox);
        addHintText(customOpenAIPanel, gbc, "Send the output token limit as <code>max_completion_tokens</code> instead of <code>max_tokens</code>. " +
                "Enable this for reasoning models (o1, o3, GPT-5) and gateways such as LiteLLM fronting them, which reject <code>max_tokens</code> with " +
                "<i>\"Unsupported parameter: 'max_tokens' is not supported with this model\"</i>.");
        addProviderSettingRow(customOpenAIPanel, gbc, "Context Window", customOpenAIContextWindowEnabledCheckBox, customOpenAIContextWindowField);
        addHintText(customOpenAIPanel, gbc, "Token window used for the usage bar and token calculation. When unchecked, DevoxxGenie assumes " + CustomOpenAIContextWindow.DEFAULT_CONTEXT_WINDOW + " tokens. Set this to your internal model's real context size to avoid a false red 'context exceeded' warning (the request is sent either way).");
        addSettingRow(customOpenAIPanel, gbc, "Input Cost", customOpenAIInputCostField);
        addSettingRow(customOpenAIPanel, gbc, "Output Cost", customOpenAIOutputCostField);
        addHintText(customOpenAIPanel, gbc, "Cost in US dollars per 1,000,000 tokens (e.g. 3 for $3/1M input, 15 for $15/1M output). Leave at 0 to hide the cost. When set, the estimated cost is shown in each AI response bubble.");

        return customOpenAIPanel;
    }

    private @NotNull JPanel createCloudProvidersPanel() {
        JPanel cloudPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createSectionConstraints();

        addProviderSettingRow(cloudPanel, gbc, "OpenAI API Key", openAIEnabledCheckBox,
                createTextWithPasswordButton(openAIKeyField, "https://platform.openai.com/api-keys"));
        addProviderSettingRow(cloudPanel, gbc, "Mistral API Key", mistralEnabledCheckBox,
                createTextWithPasswordButton(mistralApiKeyField, "https://console.mistral.ai/api-keys"));
        addProviderSettingRow(cloudPanel, gbc, "Anthropic API Key", anthropicEnabledCheckBox,
                createTextWithPasswordButton(anthropicApiKeyField, "https://console.anthropic.com/settings/keys"));
        addProviderSettingRow(cloudPanel, gbc, "Groq API Key", groqEnabledCheckBox,
                createTextWithPasswordButton(groqApiKeyField, "https://console.groq.com/keys"));
        addProviderSettingRow(cloudPanel, gbc, "DeepInfra API Key", deepInfraEnabledCheckBox,
                createTextWithPasswordButton(deepInfraApiKeyField, "https://deepinfra.com/dash/api_keys"));
        addProviderSettingRow(cloudPanel, gbc, "Google Gemini API Key", geminiEnabledCheckBox,
                createTextWithPasswordButton(geminiApiKeyField, "https://aistudio.google.com/app/apikey"));
        addProviderSettingRow(cloudPanel, gbc, "Deep Seek API Key", deepSeekEnabledCheckBox,
                createTextWithPasswordButton(deepSeekApiKeyField, "https://platform.deepseek.com/api_keys"));
        addProviderSettingRow(cloudPanel, gbc, "Open Router API Key", openRouterEnabledCheckBox,
                createTextWithPasswordButton(openRouterApiKeyField, "https://openrouter.ai/settings/keys"));
        addProviderSettingRow(cloudPanel, gbc, "Grok API Key", grokEnabledCheckBox,
                createTextWithPasswordButton(grokApiKeyField, "https://accounts.x.ai/sign-in"));
        addProviderSettingRow(cloudPanel, gbc, "Kimi API Key", kimiEnabledCheckBox,
                createTextWithPasswordButton(kimiApiKeyField, "https://platform.moonshot.ai/console/api-keys"));
        addHintText(cloudPanel, gbc, "Uses Moonshot AI platform API; get your key at platform.moonshot.ai");
        addProviderSettingRow(cloudPanel, gbc, "GLM API Key", glmEnabledCheckBox,
                createTextWithPasswordButton(glmApiKeyField, "https://z.ai/manage-apikey/apikey-list"));
        addHintText(cloudPanel, gbc, "Uses Zhipu AI (Z.AI) platform API; get your key at z.ai");
        addProviderSettingRow(cloudPanel, gbc, "NVIDIA API Key", nvidiaEnabledCheckBox,
                createTextWithPasswordButton(nvidiaApiKeyField, "https://build.nvidia.com"));
        addHintText(cloudPanel, gbc, "Uses NVIDIA NIM OpenAI-compatible API (integrate.api.nvidia.com); get your key at build.nvidia.com");
        addProviderSettingRow(cloudPanel, gbc, "Cloudflare API Key", cloudflareEnabledCheckBox,
                createTextWithPasswordButton(cloudflareApiKeyField, "https://dash.cloudflare.com/profile/api-tokens"));
        addHintText(cloudPanel, gbc, "Cloudflare API token sent as <code>Authorization: Bearer</code>. Downstream provider keys (OpenAI, Anthropic, …) are stored in your Cloudflare dashboard (BYOK).");
        addSettingRow(cloudPanel, gbc, "Cloudflare Account ID", cloudflareAccountIdField);
        addSettingRow(cloudPanel, gbc, "Cloudflare Gateway Name", cloudflareGatewayNameField);
        addHintText(cloudPanel, gbc, "Base URL is built as <code>https://gateway.ai.cloudflare.com/v1/&lt;account&gt;/&lt;gateway&gt;/compat</code>. Gateway defaults to <code>default</code> (auto-created by Cloudflare).");
        addProviderSettingRow(cloudPanel, gbc, "Cloudflare Model", cloudflareModelNameEnabledCheckBox, cloudflareModelNameField);
        addHintText(cloudPanel, gbc, "When enabled, this exact provider/model name (e.g. <code>openai/gpt-4o-mini</code>) is used and the dropdown is not auto-discovered from <code>/compat/models</code>.");

        addAzureOpenAIPanel(cloudPanel, gbc);
        addAWSPanel(cloudPanel, gbc);

        return cloudPanel;
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
            setNestedComponentsVisibility(awsCommonComponents, event.getStateChange() == ItemEvent.SELECTED, true);
            updateAwsAuthComponentsVisibility(true);
        });
        awsAuthModeComboBox.addActionListener(event -> updateAwsAuthComponentsVisibility(true));

        // Add new listeners for enable/disable checkboxes
        ollamaEnabledCheckBox.addItemListener(e -> updateUrlFieldState(ollamaEnabledCheckBox, ollamaModelUrlField));
        lmStudioEnabledCheckBox.addItemListener(e -> updateUrlFieldState(lmStudioEnabledCheckBox, lmStudioModelUrlField));
        lmStudioFallbackContextEnabledCheckBox.addItemListener(e -> updateUrlFieldState(lmStudioFallbackContextEnabledCheckBox, lmStudioFallbackContextField));
        gpt4AllEnabledCheckBox.addItemListener(e -> updateUrlFieldState(gpt4AllEnabledCheckBox, gpt4AllModelUrlField));
        janEnabledCheckBox.addItemListener(e -> updateUrlFieldState(janEnabledCheckBox, janModelUrlField));
        llamaCPPEnabledCheckBox.addItemListener(e -> updateUrlFieldState(llamaCPPEnabledCheckBox, llamaCPPModelUrlField));
        nativEnabledCheckBox.addItemListener(e -> updateUrlFieldState(nativEnabledCheckBox, nativModelUrlField));
        nativFallbackContextEnabledCheckBox.addItemListener(e -> updateUrlFieldState(nativFallbackContextEnabledCheckBox, nativFallbackContextField));
        exoEnabledCheckBox.addItemListener(e -> updateUrlFieldState(exoEnabledCheckBox, exoModelUrlField));

        customOpenAIUrlEnabledCheckBox.addItemListener(e -> updateUrlFieldState(customOpenAIUrlEnabledCheckBox, customOpenAIUrlField));
        customOpenAIModelNameEnabledCheckBox.addItemListener(e -> updateUrlFieldState(customOpenAIModelNameEnabledCheckBox, customOpenAIModelNameField));
        enableCustomOpenAIApiKeyCheckBox.addItemListener(e -> updateUrlFieldState(enableCustomOpenAIApiKeyCheckBox, customOpenAIApiKeyField));
        customOpenAIContextWindowEnabledCheckBox.addItemListener(e -> updateUrlFieldState(customOpenAIContextWindowEnabledCheckBox, customOpenAIContextWindowField));

        openAIEnabledCheckBox.addItemListener(e -> updateUrlFieldState(openAIEnabledCheckBox, openAIKeyField));
        mistralEnabledCheckBox.addItemListener(e -> updateUrlFieldState(mistralEnabledCheckBox, mistralApiKeyField));
        anthropicEnabledCheckBox.addItemListener(e -> updateUrlFieldState(anthropicEnabledCheckBox, anthropicApiKeyField));
        groqEnabledCheckBox.addItemListener(e -> updateUrlFieldState(groqEnabledCheckBox, groqApiKeyField));
        deepInfraEnabledCheckBox.addItemListener(e -> updateUrlFieldState(deepInfraEnabledCheckBox, deepInfraApiKeyField));
        geminiEnabledCheckBox.addItemListener(e -> updateUrlFieldState(geminiEnabledCheckBox, geminiApiKeyField));
        deepSeekEnabledCheckBox.addItemListener(e -> updateUrlFieldState(deepSeekEnabledCheckBox, deepSeekApiKeyField));
        openRouterEnabledCheckBox.addItemListener(e -> updateUrlFieldState(openRouterEnabledCheckBox, openRouterApiKeyField));
        grokEnabledCheckBox.addItemListener(e -> updateUrlFieldState(grokEnabledCheckBox, grokApiKeyField));
        kimiEnabledCheckBox.addItemListener(e -> updateUrlFieldState(kimiEnabledCheckBox, kimiApiKeyField));
        glmEnabledCheckBox.addItemListener(e -> updateUrlFieldState(glmEnabledCheckBox, glmApiKeyField));
        nvidiaEnabledCheckBox.addItemListener(e -> updateUrlFieldState(nvidiaEnabledCheckBox, nvidiaApiKeyField));
        cloudflareEnabledCheckBox.addItemListener(e -> updateUrlFieldState(cloudflareEnabledCheckBox, cloudflareApiKeyField));
        cloudflareModelNameEnabledCheckBox.addItemListener(e -> updateUrlFieldState(cloudflareModelNameEnabledCheckBox, cloudflareModelNameField));
        enableAzureOpenAICheckBox.addItemListener(e -> updateUrlFieldState(enableAzureOpenAICheckBox, azureOpenAIEndpointField));

        updateUrlFieldState(lmStudioFallbackContextEnabledCheckBox, lmStudioFallbackContextField);
        updateUrlFieldState(nativFallbackContextEnabledCheckBox, nativFallbackContextField);
    }

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
        final String awsProfileURL = "https://docs.aws.amazon.com/cli/v1/userguide/cli-configure-files.html";
        addSettingRow(panel, gbc, "Enable AWS Bedrock", enableAWSCheckBox);

        addNestedSettingsRow(panel, gbc, "Authentication Method", awsAuthModeComboBox, awsCommonComponents);
        addNestedSettingsRow(panel, gbc, "Enable Regional Inference", enableAWSRegionalInferenceCheckBox, awsCommonComponents);
        addNestedSettingsRow(panel, gbc, "AWS region", createTextWithPasswordButton(awsRegion, bedrockURL), awsCommonComponents);

        addNestedSettingsRow(panel, gbc, "AWS Access Key ID", createTextWithLinkButton(awsAccessKeyIdField, bedrockURL), awsAccessKeyComponents);
        addNestedSettingsRow(panel, gbc, "AWS Secret Access Key", createTextWithLinkButton(awsSecretKeyField, bedrockURL), awsAccessKeyComponents);

        addNestedSettingsRow(panel, gbc, "AWS Profile Name", createTextWithLinkButton(awsProfileName, awsProfileURL), awsProfileComponents);

        addNestedSettingsRow(panel, gbc, "AWS Bearer Token", createTextWithPasswordButton(awsBearerTokenField, bedrockURL), awsBearerTokenComponents);

        // Set initial visibility
        setNestedComponentsVisibility(awsCommonComponents, enableAWSCheckBox.isSelected(), false);
        updateAwsAuthComponentsVisibility(false);
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
        gbc.weightx = 0;
        gbc.insets = JBUI.insets(5, 20, 5, 5); // Indent by 20 pixels on the left
        JLabel jLabel = new JLabel(label);
        panel.add(jLabel, gbc);
        componentsGroup.add(jLabel);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(component, gbc);
        componentsGroup.add(component);
        gbc.weightx = 0;
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

    public void refreshAwsSettingsVisibility() {
        updateAwsAuthComponentsVisibility(true);
    }

    private void updateAwsAuthComponentsVisibility(boolean repaintPanel) {
        AwsBedrockAuthMode authMode = (AwsBedrockAuthMode) awsAuthModeComboBox.getSelectedItem();
        if (authMode == null) {
            authMode = AwsBedrockAuthMode.defaultMode();
        }
        boolean awsEnabled = enableAWSCheckBox.isSelected();

        setNestedComponentsVisibility(awsAccessKeyComponents,
                awsEnabled && authMode == AwsBedrockAuthMode.ACCESS_KEY, repaintPanel);
        setNestedComponentsVisibility(awsProfileComponents,
                awsEnabled && authMode == AwsBedrockAuthMode.PROFILE, repaintPanel);
        setNestedComponentsVisibility(awsBearerTokenComponents,
                awsEnabled && authMode == AwsBedrockAuthMode.BEARER_TOKEN, repaintPanel);
    }
    
    /**
     * Adds a hint text below a setting row.
     * The hint text is indented and styled as a smaller, gray label.
     *
     * @param panel The panel to add the hint text to.
     * @param gbc   The GridBagConstraints for layout.
     * @param text  The hint text to display.
     */
    private void addHintText(
            @NotNull JPanel panel,
            @NotNull GridBagConstraints gbc,
            String text
    ) {
        // Use a word-wrapping comment label so long hints reflow to multiple lines
        // instead of stretching the input column (and the whole dialog) past its edge.
        JComponent hintLabel = createWrappingHint(text);

        JPanel providerPanel = new JPanel(new BorderLayout(5, 0));
        providerPanel.add(hintLabel, BorderLayout.CENTER);

        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(providerPanel, gbc);
        gbc.weightx = 0;
        gbc.gridy++;
    }

    /**
     * Creates a small, gray, word-wrapping hint label that mimics the look of the (now deprecated)
     * {@code ComponentPanelBuilder.createCommentComponent}. The HTML body width is capped at roughly
     * {@link #HINT_WRAP_COLUMNS} characters so long hints reflow onto multiple lines instead of
     * widening the input column — and thus the whole settings dialog — beyond the view.
     *
     * @param text the hint text (may contain HTML).
     * @return a configured comment-style label.
     */
    private @NotNull JComponent createWrappingHint(String text) {
        JBLabel hint = new JBLabel();
        hint.setComponentStyle(UIUtil.ComponentStyle.SMALL);
        hint.setForeground(UIUtil.getContextHelpForeground());
        int wrapWidth = hint.getFontMetrics(hint.getFont()).charWidth('a') * HINT_WRAP_COLUMNS;
        hint.setText("<html><body style='width:" + wrapWidth + "px'>" + text + "</body></html>");
        return hint;
    }

    /**
     * Bound the preferred width of every text/password field so a long stored value
     * (e.g. a 150-char API key) cannot blow up the settings form width and push the
     * input borders off the edge of the dialog. The fields still expand to fill the
     * available width via {@code fill=HORIZONTAL} + {@code weightx}; this only caps
     * their <em>preferred</em> width. Spinners are left untouched.
     */
    private void boundTextFieldWidths(@NotNull Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JSpinner) {
                continue; // spinners manage their own editor size
            }
            if (child instanceof JTextField textField) {
                textField.setColumns(TEXT_FIELD_COLUMNS);
            } else if (child instanceof Container nested) {
                boundTextFieldWidths(nested);
            }
        }
    }
}
