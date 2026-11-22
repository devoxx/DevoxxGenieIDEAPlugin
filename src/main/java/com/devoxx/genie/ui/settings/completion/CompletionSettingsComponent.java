package com.devoxx.genie.ui.settings.completion;

import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.chatmodel.local.lmstudio.LMStudioModelService;
import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBLabel;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CompletionSettingsComponent extends AbstractSettingsComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CompletionSettingsComponent.class);

    private static final String NONE = "None";
    private static final String OLLAMA = "Ollama";
    private static final String LMSTUDIO = "LM Studio";
    private static final String CUSTOM_OPEN_AI = "Custom OpenAI";

    private final JComboBox<String> providerComboBox;
    private final JComboBox<String> modelComboBox;
    private final JButton refreshButton;
    private final JBIntSpinner maxTokensSpinner;
    private final JBIntSpinner timeoutSpinner;
    private final JBIntSpinner debounceSpinner;
    private final JTextField customUrlField;
    private final JPasswordField customApiKeyField;
    private final JCheckBox contextEnabledCheckBox;
    private final JBIntSpinner contextBudgetSpinner;
    private final JBIntSpinner contextMaxFilesSpinner;

    public CompletionSettingsComponent() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        providerComboBox = new JComboBox<>(new String[]{NONE, OLLAMA, LMSTUDIO, CUSTOM_OPEN_AI});
        providerComboBox.setSelectedItem(providerToDisplayName(state.getInlineCompletionProvider()));

        modelComboBox = new JComboBox<>();
        modelComboBox.setEditable(false);
        refreshButton = new JButton("Refresh Models");
        maxTokensSpinner = new JBIntSpinner(state.getInlineCompletionMaxTokens(), 16, 256, 8);
        timeoutSpinner = new JBIntSpinner(state.getInlineCompletionTimeoutMs(), 1000, 30000, 500);
        debounceSpinner = new JBIntSpinner(state.getInlineCompletionDebounceMs(), 100, 2000, 50);

        customUrlField = new JTextField(
                state.getInlineCompletionCustomUrl() != null ? state.getInlineCompletionCustomUrl() : "http://localhost:4000/v1");
        customApiKeyField = new JPasswordField(state.getInlineCompletionCustomApiKey());
        contextEnabledCheckBox = new JCheckBox("Include referenced class signatures in the completion context",
                state.getInlineCompletionContextEnabled() != false);
        contextBudgetSpinner = new JBIntSpinner(
                state.getInlineCompletionContextBudget() != null ? state.getInlineCompletionContextBudget() : 2048,
                256, 16384, 256);
        contextMaxFilesSpinner = new JBIntSpinner(
                state.getInlineCompletionContextMaxClasses() != null ? state.getInlineCompletionContextMaxClasses() : 4,
                1, 10, 1);

        setupPanel();
        loadModelsForProvider(state.getInlineCompletionModel());
    }

    private void setupPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- Provider ---
        addSection(contentPanel, gbc, "Fill-in-the-Middle Provider");

        JPanel providerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        providerRow.add(new JBLabel("Provider:"));
        providerRow.add(providerComboBox);
        addFullWidthRow(contentPanel, gbc, providerRow);
        addHelpText(contentPanel, gbc,
                "Select the local LLM provider for inline code completion, or None to disable.");

        // --- Model ---
        addSection(contentPanel, gbc, "Model");

        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        modelRow.add(new JBLabel("Model name:"));
        modelRow.add(modelComboBox);
        modelRow.add(refreshButton);
        addFullWidthRow(contentPanel, gbc, modelRow);
        addHelpText(contentPanel, gbc,
                "Requires a FIM-trained model. Recommended: starcoder2:3b, qwen2.5-coder:7b, " +
                "deepseek-coder:6.7b-base. For Ollama install with: ollama pull starcoder2:3b");

        // --- Custom OpenAI-compatible endpoint ---
        JPanel customUrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        customUrlRow.add(new JBLabel("Endpoint URL:"));
        customUrlField.setColumns(34);
        customUrlRow.add(customUrlField);
        addFullWidthRow(contentPanel, gbc, customUrlRow);

        JPanel customKeyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        customKeyRow.add(new JBLabel("API key:"));
        customApiKeyField.setColumns(24);
        customKeyRow.add(customApiKeyField);
        addFullWidthRow(contentPanel, gbc, customKeyRow);
        addHelpText(contentPanel, gbc,
                "OpenAI-compatible FIM endpoint (/v1/completions), e.g. llama.cpp " +
                "(http://127.0.0.1:1234/v1) or a LiteLLM gateway (http://localhost:4000/v1). " +
                "The API key is sent as an Authorization bearer header when non-empty.");

        // --- Context ---
        addSection(contentPanel, gbc, "Context");
        addFullWidthRow(contentPanel, gbc, contextEnabledCheckBox);

        JPanel contextBudgetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        contextBudgetRow.add(new JBLabel("Context budget (chars):"));
        contextBudgetRow.add(contextBudgetSpinner);
        addFullWidthRow(contentPanel, gbc, contextBudgetRow);

        JPanel contextFilesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        contextFilesRow.add(new JBLabel("Max classes:"));
        contextFilesRow.add(contextMaxFilesSpinner);
        addFullWidthRow(contentPanel, gbc, contextFilesRow);
        addHelpText(contentPanel, gbc,
                "Prepends signatures (declaration + public methods) of the classes referenced in " +
                "the method under the cursor, resolved via PSI. Language-agnostic; standard " +
                "libraries and test frameworks are skipped. The current file's cursor window is " +
                "already part of the request and is never duplicated.");

        // --- Performance ---
        addSection(contentPanel, gbc, "Performance");

        JPanel maxTokensRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        maxTokensRow.add(new JBLabel("Max tokens:"));
        maxTokensRow.add(maxTokensSpinner);
        addFullWidthRow(contentPanel, gbc, maxTokensRow);

        JPanel timeoutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timeoutRow.add(new JBLabel("Timeout (ms):"));
        timeoutRow.add(timeoutSpinner);
        addFullWidthRow(contentPanel, gbc, timeoutRow);

        JPanel debounceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        debounceRow.add(new JBLabel("Debounce delay (ms):"));
        debounceRow.add(debounceSpinner);
        addFullWidthRow(contentPanel, gbc, debounceRow);

        addHelpText(contentPanel, gbc,
                "Inline completion uses Fill-in-the-Middle (FIM) APIs from Ollama or LM Studio. " +
                "Provider URLs are configured in the main DevoxxGenie LLM Providers settings.");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);

        updateEnabledState();
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/inline-completion";
    }

    @Override
    public void addListeners() {
        providerComboBox.addActionListener(e -> {
            updateEnabledState();
            loadModelsForProvider(null);
        });
        refreshButton.addActionListener(e -> loadModelsForProvider(getSelectedModel()));
    }

    private void addFullWidthRow(JPanel panel, GridBagConstraints gbc, JComponent component) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        panel.add(component, gbc);
        gbc.gridy++;
    }

    private void addHelpText(JPanel panel, GridBagConstraints gbc, String text) {
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 25, 8, 5);
        JTextArea helpArea = new JTextArea(text);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setEditable(false);
        helpArea.setFocusable(false);
        helpArea.setOpaque(false);
        helpArea.setBorder(null);
        helpArea.setFont(UIManager.getFont("Label.font").deriveFont((float) UIManager.getFont("Label.font").getSize() - 1));
        helpArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        panel.add(helpArea, gbc);
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy++;
    }

    private void loadModelsForProvider(String selectedModel) {
        String provider = getSelectedProvider();
        if (NONE.equals(provider)) {
            modelComboBox.removeAllItems();
            return;
        }

        refreshButton.setEnabled(false);
        refreshButton.setText("Loading...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<String> modelNames = fetchModelNames(provider);
                SwingUtilities.invokeLater(() -> updateModelComboBox(modelNames, selectedModel));
            } catch (Exception ex) {
                LOG.debug("Failed to fetch models for {}: {}", provider, ex.getMessage());
                SwingUtilities.invokeLater(() -> handleModelLoadFailure(selectedModel));
            }
        });
    }

    private List<String> fetchModelNames(String provider) throws Exception {
        if (OLLAMA.equals(provider)) {
            return Arrays.stream(OllamaModelService.getInstance().getModels())
                    .map(OllamaModelEntryDTO::getName)
                    .collect(Collectors.toList());
        }
        if (LMSTUDIO.equals(provider)) {
            return Arrays.stream(LMStudioModelService.getInstance().getModels())
                    .map(LMStudioModelEntryDTO::resolveModelName)
                    .collect(Collectors.toList());
        }
        if (CUSTOM_OPEN_AI.equals(provider)) {
            return fetchCustomOpenAIModelNames();
        }
        return Collections.emptyList();
    }

    private List<String> fetchCustomOpenAIModelNames() throws Exception {
        String url = customUrlField.getText().trim();
        if (url.isBlank()) {
            return Collections.emptyList();
        }
        String modelsUrl = (url.endsWith("/") ? url : url + "/") + "models";
        String token = new String(customApiKeyField.getPassword());
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(5))
                .retryOnConnectionFailure(false)
                .build();
        ResponseDTO response = LocalLLMProviderUtil.getModelsFromUrl(modelsUrl, ResponseDTO.class, client,
                token.isBlank() ? null : token);
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData().stream()
                .filter(model -> model != null && model.getId() != null && !model.getId().isBlank())
                .map(Model::getId)
                .collect(Collectors.toList());
    }

    private void updateModelComboBox(List<String> modelNames, String selectedModel) {
        modelComboBox.removeAllItems();
        for (String name : modelNames) {
            modelComboBox.addItem(name);
        }
        selectModelIfPresent(selectedModel);
        restoreRefreshButton();
    }

    private void handleModelLoadFailure(String selectedModel) {
        restoreRefreshButton();
        if (modelComboBox.getItemCount() == 0 && selectedModel != null && !selectedModel.isBlank()) {
            modelComboBox.addItem(selectedModel);
            modelComboBox.setSelectedItem(selectedModel);
        }
    }

    private void selectModelIfPresent(String selectedModel) {
        if (selectedModel != null && !selectedModel.isBlank()) {
            modelComboBox.setSelectedItem(selectedModel);
        }
    }

    private void restoreRefreshButton() {
        refreshButton.setText("Refresh Models");
        refreshButton.setEnabled(!NONE.equals(getSelectedProvider()));
    }

    private String getSelectedProvider() {
        Object selected = providerComboBox.getSelectedItem();
        return selected != null ? selected.toString() : NONE;
    }

    private String getSelectedModel() {
        Object selected = modelComboBox.getSelectedItem();
        return selected != null ? selected.toString() : "";
    }

    private void updateEnabledState() {
        boolean enabled = !NONE.equals(getSelectedProvider());
        boolean customSelected = CUSTOM_OPEN_AI.equals(getSelectedProvider());
        modelComboBox.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        maxTokensSpinner.setEnabled(enabled);
        timeoutSpinner.setEnabled(enabled);
        debounceSpinner.setEnabled(enabled);
        customUrlField.setEnabled(customSelected);
        customApiKeyField.setEnabled(customSelected);
    }

    private static String displayNameToProvider(String displayName) {
        if (OLLAMA.equals(displayName)) return "Ollama";
        if (LMSTUDIO.equals(displayName)) return "LMStudio";
        if (CUSTOM_OPEN_AI.equals(displayName)) return "CustomOpenAI";
        return "";
    }

    private static String providerToDisplayName(String provider) {
        if ("Ollama".equals(provider)) return OLLAMA;
        if ("LMStudio".equals(provider)) return LMSTUDIO;
        if ("CustomOpenAI".equals(provider)) return CUSTOM_OPEN_AI;
        return NONE;
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String currentProvider = displayNameToProvider(getSelectedProvider());
        return !state.getInlineCompletionProvider().equals(currentProvider) ||
                !state.getInlineCompletionModel().equals(getSelectedModel()) ||
                !state.getInlineCompletionMaxTokens().equals(maxTokensSpinner.getNumber()) ||
                !state.getInlineCompletionTimeoutMs().equals(timeoutSpinner.getNumber()) ||
                !state.getInlineCompletionDebounceMs().equals(debounceSpinner.getNumber()) ||
                !Objects.equals(state.getInlineCompletionCustomUrl(), customUrlField.getText().trim()) ||
                !Objects.equals(state.getInlineCompletionCustomApiKey(), new String(customApiKeyField.getPassword())) ||
                !Objects.equals(state.getInlineCompletionContextEnabled(), contextEnabledCheckBox.isSelected()) ||
                !Objects.equals(state.getInlineCompletionContextBudget(), contextBudgetSpinner.getNumber()) ||
                !Objects.equals(state.getInlineCompletionContextMaxClasses(), contextMaxFilesSpinner.getNumber());
    }

    public void apply() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        state.setInlineCompletionProvider(displayNameToProvider(getSelectedProvider()));
        state.setInlineCompletionModel(getSelectedModel());
        state.setInlineCompletionMaxTokens(maxTokensSpinner.getNumber());
        state.setInlineCompletionTimeoutMs(timeoutSpinner.getNumber());
        state.setInlineCompletionDebounceMs(debounceSpinner.getNumber());
        state.setInlineCompletionCustomUrl(customUrlField.getText().trim());
        state.setInlineCompletionCustomApiKey(new String(customApiKeyField.getPassword()));
        state.setInlineCompletionContextEnabled(contextEnabledCheckBox.isSelected());
        state.setInlineCompletionContextBudget(contextBudgetSpinner.getNumber());
        state.setInlineCompletionContextMaxClasses(contextMaxFilesSpinner.getNumber());
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        providerComboBox.setSelectedItem(providerToDisplayName(state.getInlineCompletionProvider()));
        maxTokensSpinner.setNumber(state.getInlineCompletionMaxTokens());
        timeoutSpinner.setNumber(state.getInlineCompletionTimeoutMs());
        debounceSpinner.setNumber(state.getInlineCompletionDebounceMs());
        customUrlField.setText(state.getInlineCompletionCustomUrl());
        customApiKeyField.setText(state.getInlineCompletionCustomApiKey());
        contextEnabledCheckBox.setSelected(state.getInlineCompletionContextEnabled() != false);
        contextBudgetSpinner.setNumber(state.getInlineCompletionContextBudget());
        contextMaxFilesSpinner.setNumber(state.getInlineCompletionContextMaxClasses());
        loadModelsForProvider(state.getInlineCompletionModel());
        updateEnabledState();
    }
}
