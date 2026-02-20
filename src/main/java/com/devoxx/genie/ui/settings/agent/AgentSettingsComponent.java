package com.devoxx.genie.ui.settings.agent;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.SubAgentConfig;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.ui.renderer.ModelInfoRenderer;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.DevoxxGenieFontsUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.devoxx.genie.model.Constant.*;

public class AgentSettingsComponent extends AbstractSettingsComponent {

    private final JBCheckBox enableAgentModeCheckbox =
            new JBCheckBox("Enable agent mode", stateService.getAgentModeEnabled());
    private final JBIntSpinner maxToolCallsSpinner =
            new JBIntSpinner(stateService.getAgentMaxToolCalls() != null ? stateService.getAgentMaxToolCalls() : AGENT_MAX_TOOL_CALLS, 1, 100);
    private final JBCheckBox autoApproveReadOnlyCheckbox =
            new JBCheckBox("Auto-approve read-only tools (read_file, list_files, search_files, fetch_page)", stateService.getAgentAutoApproveReadOnly());
    private final JBCheckBox writeApprovalRequiredCheckbox =
            new JBCheckBox("Write tools always require approval (write_file, run_command)", Boolean.TRUE.equals(stateService.getAgentWriteApprovalRequired()));
    private final JBCheckBox enableDebugLogsCheckbox =
            new JBCheckBox("Enable agent debug logs", Boolean.TRUE.equals(stateService.getAgentDebugLogsEnabled()));

    // Test execution settings
    private final JBCheckBox enableTestExecutionCheckbox =
            new JBCheckBox("Enable run tests tool", Boolean.TRUE.equals(stateService.getTestExecutionEnabled()));
    private final JBIntSpinner testTimeoutSpinner =
            new JBIntSpinner(stateService.getTestExecutionTimeoutSeconds() != null ? stateService.getTestExecutionTimeoutSeconds() : TEST_EXECUTION_DEFAULT_TIMEOUT, 10, 600);
    private final JTextField customTestCommandField = new JTextField(
            stateService.getTestExecutionCustomCommand() != null ? stateService.getTestExecutionCustomCommand() : "", 30);

    // PSI tools settings
    private final JBCheckBox enablePsiToolsCheckbox =
            new JBCheckBox("Enable PSI Tools (find_symbols, document_symbols, find_references, find_definition, find_implementations)",
                    Boolean.TRUE.equals(stateService.getPsiToolsEnabled()));

    // Parallel exploration settings
    private final JBCheckBox enableParallelExploreCheckbox =
            new JBCheckBox("Enable parallel explore tool", Boolean.TRUE.equals(stateService.getParallelExploreEnabled()));
    private final JBIntSpinner subAgentMaxToolCallsSpinner =
            new JBIntSpinner(stateService.getSubAgentMaxToolCalls() != null ? stateService.getSubAgentMaxToolCalls() : SUB_AGENT_MAX_TOOL_CALLS, 1, 200);
    private final JBIntSpinner subAgentTimeoutSpinner =
            new JBIntSpinner(stateService.getSubAgentTimeoutSeconds() != null ? stateService.getSubAgentTimeoutSeconds() : SUB_AGENT_TIMEOUT_SECONDS, 10, 600);

    private static final String AUTO_DETECT_LABEL = "None (Auto-detect)";
    private static final String USE_DEFAULT_LABEL = "Use default";

    private final ComboBox<ModelProvider> subAgentProviderComboBox = new ComboBox<>();
    private final ComboBox<LanguageModel> subAgentModelComboBox = new ComboBox<>();

    // Built-in tool enable/disable checkboxes
    private static final String[][] CORE_AGENT_TOOLS = {
            {"read_file", "Read file contents from the project"},
            {"write_file", "Write content to files in the project"},
            {"edit_file", "Edit files by replacing exact string matches"},
            {"list_files", "List files and directories in the project"},
            {"search_files", "Search for regex patterns in project files"},
            {"run_command", "Execute terminal commands in the project directory"},
            {"fetch_page", "Fetch a web page and return its text content"}
    };
    private final Map<String, JBCheckBox> toolCheckboxes = new LinkedHashMap<>();

    // Per-agent model override rows
    private final JPanel agentConfigPanel = new JPanel();
    private final List<AgentConfigRow> agentConfigRows = new ArrayList<>();
    private final JButton addAgentButton = new JButton("+ Add");

    public AgentSettingsComponent() {
        setupSubAgentComboBoxes();

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.gridy = 0;

        // --- Agent Mode ---
        addSection(contentPanel, gbc, "Agent Mode");

        addFullWidthRow(contentPanel, gbc, enableAgentModeCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the LLM gets built-in IDE tools (read_file, write_file, " +
                "list_files, search_files, run_command) to interact with your project autonomously.");

        // --- Built-in Tools ---
        addSection(contentPanel, gbc, "Built-in Tools");

        List<String> disabledTools = stateService.getDisabledAgentTools();
        Set<String> disabledSet = disabledTools != null ? new HashSet<>(disabledTools) : Collections.emptySet();

        for (String[] toolInfo : CORE_AGENT_TOOLS) {
            String toolName = toolInfo[0];
            String toolDesc = toolInfo[1];
            JBCheckBox cb = new JBCheckBox(toolName + " - " + toolDesc, !disabledSet.contains(toolName));
            toolCheckboxes.put(toolName, cb);
            addFullWidthRow(contentPanel, gbc, cb);
        }

        addHelpText(contentPanel, gbc,
                "Uncheck tools to prevent them from being available to the agent. " +
                "This does not affect the run_tests, parallel_explore, or backlog tools which have their own toggles above/below.");

        // --- Loop Controls ---
        addSection(contentPanel, gbc, "Loop Controls");

        JPanel spinnerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        spinnerRow.add(new JBLabel("Max tool calls per prompt:"));
        spinnerRow.add(maxToolCallsSpinner);
        addFullWidthRow(contentPanel, gbc, spinnerRow);
        addHelpText(contentPanel, gbc,
                "Maximum number of tool calls the LLM can make per prompt. " +
                "Prevents infinite loops. The LLM will provide its best answer when the limit is reached.");

        // --- Approval ---
        addSection(contentPanel, gbc, "Approval");

        addFullWidthRow(contentPanel, gbc, autoApproveReadOnlyCheckbox);

        addFullWidthRow(contentPanel, gbc, writeApprovalRequiredCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, a confirmation dialog is shown before executing write tools. " +
                "You can also disable this from the approval dialog itself via the \"Don't ask again\" checkbox.");

        // --- Test Execution ---
        addSection(contentPanel, gbc, "Test Execution");

        addFullWidthRow(contentPanel, gbc, enableTestExecutionCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the agent gets a 'run_tests' tool that auto-detects the project's " +
                "build system and runs tests. The agent is instructed to run tests after code changes.");

        JPanel testTimeoutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        testTimeoutRow.add(new JBLabel("Test timeout (seconds):"));
        testTimeoutRow.add(testTimeoutSpinner);
        addFullWidthRow(contentPanel, gbc, testTimeoutRow);

        JPanel customCommandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        customCommandRow.add(new JBLabel("Custom test command:"));
        customCommandRow.add(customTestCommandField);
        addFullWidthRow(contentPanel, gbc, customCommandRow);
        addHelpText(contentPanel, gbc,
                "Optional: override auto-detected test command. Use {target} as placeholder for specific test targets. " +
                "Example: './gradlew test --tests \"{target}\"' or 'npm run test -- {target}'");

        // --- PSI Tools ---
        addSection(contentPanel, gbc, "PSI Tools (Code Intelligence)");

        addFullWidthRow(contentPanel, gbc, enablePsiToolsCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the agent gets IDE-powered code intelligence tools that use the " +
                "Program Structure Interface (PSI) for semantic code analysis. These tools understand " +
                "language semantics (imports, types, inheritance) and work across all IDE-supported languages. " +
                "Tools: find_symbols (search definitions), document_symbols (list file structure), " +
                "find_references (find usages), find_definition (go to definition), " +
                "find_implementations (find interface/class implementations).");

        // --- Parallel Exploration ---
        addSection(contentPanel, gbc, "Parallel Exploration");

        addFullWidthRow(contentPanel, gbc, enableParallelExploreCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the agent gets a 'parallel_explore' tool that launches multiple " +
                "sub-agents in parallel to explore different aspects of the codebase simultaneously. " +
                "Each sub-agent has read-only tool access and its own model instance.");

        JPanel subAgentToolCallsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        subAgentToolCallsRow.add(new JBLabel("Max tool calls per sub-agent:"));
        subAgentToolCallsRow.add(subAgentMaxToolCallsSpinner);
        addFullWidthRow(contentPanel, gbc, subAgentToolCallsRow);

        JPanel subAgentTimeoutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        subAgentTimeoutRow.add(new JBLabel("Sub-agent timeout (seconds):"));
        subAgentTimeoutRow.add(subAgentTimeoutSpinner);
        addFullWidthRow(contentPanel, gbc, subAgentTimeoutRow);

        addFullWidthRow(contentPanel, gbc, new JBLabel("Default sub-agent provider:"));
        addFullWidthRow(contentPanel, gbc, subAgentProviderComboBox);
        addHelpText(contentPanel, gbc,
                "Default provider for sub-agents. Select 'None (Auto-detect)' to let the plugin " +
                "choose automatically (tries Ollama, then OpenAI).");

        addFullWidthRow(contentPanel, gbc, new JBLabel("Default sub-agent model:"));
        addFullWidthRow(contentPanel, gbc, subAgentModelComboBox);
        addHelpText(contentPanel, gbc,
                "Default model for sub-agents. Using a different (cheaper/faster) model for sub-agents is recommended.");

        // --- Per-Agent Model Overrides ---
        JPanel overridesHeaderPanel = new JPanel(new BorderLayout());
        JBLabel overridesLabel = new JBLabel("Per-agent model overrides");
        overridesLabel.setFont(overridesLabel.getFont().deriveFont(Font.BOLD));
        overridesHeaderPanel.add(overridesLabel, BorderLayout.WEST);
        addAgentButton.setToolTipText("Add a new sub-agent slot (max " + SUB_AGENT_MAX_PARALLELISM + ")");
        addAgentButton.addActionListener(e -> addAgentConfigRow());
        overridesHeaderPanel.add(addAgentButton, BorderLayout.EAST);
        addFullWidthRow(contentPanel, gbc, overridesHeaderPanel);

        addHelpText(contentPanel, gbc,
                "Assign a specific model to each sub-agent slot, or leave as \"Use default\" to inherit the default provider/model above. " +
                "The number of rows determines how many sub-agents run in parallel.");

        agentConfigPanel.setLayout(new BoxLayout(agentConfigPanel, BoxLayout.Y_AXIS));
        addFullWidthRow(contentPanel, gbc, agentConfigPanel);

        // Build initial rows from saved configs (or default parallelism)
        initAgentConfigRows();

        // --- Debug ---
        addSection(contentPanel, gbc, "Debug");

        addFullWidthRow(contentPanel, gbc, enableDebugLogsCheckbox);
        addHelpText(contentPanel, gbc,
                "Agent tool calls, arguments, and results are logged in the " +
                "'DevoxxGenie Agent Logs' tool window (View → Tool Windows).");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
    }

    private void setupSubAgentComboBoxes() {
        // Configure provider combo box with custom renderer that handles null as auto-detect
        subAgentProviderComboBox.setRenderer(new SubAgentProviderRenderer());
        subAgentProviderComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());

        // Configure model combo box
        subAgentModelComboBox.setRenderer(new ModelInfoRenderer());
        subAgentModelComboBox.setFont(DevoxxGenieFontsUtil.getDropdownFont());

        // Populate provider combo box
        populateProviderComboBox();

        // Restore saved selection
        restoreProviderSelection();

        // Listen for provider changes
        subAgentProviderComboBox.addActionListener(e -> {
            if ("comboBoxChanged".equals(e.getActionCommand())) {
                updateSubAgentModelComboBox();
            }
        });
    }

    private void populateProviderComboBox() {
        subAgentProviderComboBox.removeAllItems();

        // Add null as the "Auto-detect" sentinel
        subAgentProviderComboBox.addItem(null);

        // Add enabled providers (same filter logic as LlmProviderPanel)
        LLMProviderService providerService = LLMProviderService.getInstance();
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

        providerService.getAvailableModelProviders().stream()
                .filter(provider -> switch (provider) {
                    case Ollama -> state.isOllamaEnabled();
                    case LMStudio -> state.isLmStudioEnabled();
                    case GPT4All -> state.isGpt4AllEnabled();
                    case Jan -> state.isJanEnabled();
                    case LLaMA -> state.isLlamaCPPEnabled();
                    case CustomOpenAI -> state.isCustomOpenAIUrlEnabled();
                    case OpenAI -> state.isOpenAIEnabled();
                    case Mistral -> state.isMistralEnabled();
                    case Anthropic -> state.isAnthropicEnabled();
                    case Groq -> state.isGroqEnabled();
                    case DeepInfra -> state.isDeepInfraEnabled();
                    case Google -> state.isGoogleEnabled();
                    case DeepSeek -> state.isDeepSeekEnabled();
                    case OpenRouter -> state.isOpenRouterEnabled();
                    case Grok -> state.isGrokEnabled();
                    case Kimi -> state.isKimiEnabled();
                    case GLM -> state.isGlmEnabled();
                    case AzureOpenAI -> state.isAzureOpenAIEnabled();
                    case Bedrock -> state.isAwsEnabled();
                    case CLIRunners -> false;
                    case ACPRunners -> false;
                })
                .distinct()
                .sorted(Comparator.comparing(ModelProvider::getName))
                .forEach(subAgentProviderComboBox::addItem);
    }

    private void restoreProviderSelection() {
        String savedProvider = stateService.getSubAgentModelProvider();
        if (savedProvider != null && !savedProvider.isEmpty()) {
            try {
                ModelProvider provider = ModelProvider.fromString(savedProvider);
                for (int i = 0; i < subAgentProviderComboBox.getItemCount(); i++) {
                    if (provider.equals(subAgentProviderComboBox.getItemAt(i))) {
                        subAgentProviderComboBox.setSelectedIndex(i);
                        updateSubAgentModelComboBox();
                        restoreModelSelection();
                        return;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Provider not found, fall through to auto-detect
            }
        }
        // Default to auto-detect (null, index 0)
        subAgentProviderComboBox.setSelectedIndex(0);
        updateSubAgentModelComboBox();
    }

    private void restoreModelSelection() {
        String savedModel = stateService.getSubAgentModelName();
        if (savedModel != null && !savedModel.isEmpty()) {
            for (int i = 0; i < subAgentModelComboBox.getItemCount(); i++) {
                LanguageModel model = subAgentModelComboBox.getItemAt(i);
                if (model != null && savedModel.equals(model.getModelName())) {
                    subAgentModelComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void updateSubAgentModelComboBox() {
        subAgentModelComboBox.removeAllItems();
        ModelProvider selectedProvider = (ModelProvider) subAgentProviderComboBox.getSelectedItem();

        if (selectedProvider == null) {
            // Auto-detect mode: disable model combo box
            subAgentModelComboBox.setEnabled(false);
            return;
        }

        subAgentModelComboBox.setEnabled(true);
        ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.getName())
                .ifPresent(factory -> {
                    List<LanguageModel> models = new ArrayList<>(factory.getModels());
                    if (!models.isEmpty()) {
                        models.sort(Comparator.naturalOrder());
                        models.forEach(subAgentModelComboBox::addItem);
                    }
                });
    }

    // --- Per-agent config row management ---

    private void initAgentConfigRows() {
        List<SubAgentConfig> savedConfigs = stateService.getSubAgentConfigs();
        int count = (savedConfigs != null && !savedConfigs.isEmpty())
                ? savedConfigs.size()
                : stateService.getSubAgentParallelism();

        agentConfigPanel.removeAll();
        agentConfigRows.clear();

        for (int i = 0; i < count; i++) {
            AgentConfigRow row = new AgentConfigRow(i);
            agentConfigRows.add(row);
            agentConfigPanel.add(row.getPanel());

            if (savedConfigs != null && i < savedConfigs.size()) {
                row.restoreConfig(savedConfigs.get(i));
            }
        }

        updateAddRemoveButtonState();
        agentConfigPanel.revalidate();
        agentConfigPanel.repaint();
    }

    private void addAgentConfigRow() {
        if (agentConfigRows.size() >= SUB_AGENT_MAX_PARALLELISM) {
            return;
        }
        AgentConfigRow row = new AgentConfigRow(agentConfigRows.size());
        agentConfigRows.add(row);
        agentConfigPanel.add(row.getPanel());
        updateAddRemoveButtonState();
        agentConfigPanel.revalidate();
        agentConfigPanel.repaint();
    }

    private void updateAddRemoveButtonState() {
        addAgentButton.setEnabled(agentConfigRows.size() < SUB_AGENT_MAX_PARALLELISM);
        for (AgentConfigRow row : agentConfigRows) {
            row.setRemoveEnabled(agentConfigRows.size() > 1);
        }
    }

    private List<SubAgentConfig> getPerAgentConfigs() {
        List<SubAgentConfig> configs = new ArrayList<>();
        for (AgentConfigRow row : agentConfigRows) {
            configs.add(row.toConfig());
        }
        return configs;
    }

    private boolean isPerAgentConfigsModified() {
        List<SubAgentConfig> saved = stateService.getSubAgentConfigs();
        List<SubAgentConfig> current = getPerAgentConfigs();

        if (saved == null || saved.isEmpty()) {
            return isModifiedFromDefaults(current);
        }

        if (saved.size() != current.size()) {
            return true;
        }

        return hasConfigDifferences(saved, current);
    }

    private boolean isModifiedFromDefaults(List<SubAgentConfig> current) {
        int defaultCount = stateService.getSubAgentParallelism();
        if (current.size() != defaultCount) {
            return true;
        }
        return current.stream().anyMatch(c -> c.getModelProvider() != null && !c.getModelProvider().isEmpty());
    }

    private boolean hasConfigDifferences(List<SubAgentConfig> saved, List<SubAgentConfig> current) {
        for (int i = 0; i < saved.size(); i++) {
            SubAgentConfig s = saved.get(i);
            SubAgentConfig c = current.get(i);
            String sp = s.getModelProvider() != null ? s.getModelProvider() : "";
            String sm = s.getModelName() != null ? s.getModelName() : "";
            String cp = c.getModelProvider() != null ? c.getModelProvider() : "";
            String cm = c.getModelName() != null ? c.getModelName() : "";
            if (!sp.equals(cp) || !sm.equals(cm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A single row in the per-agent overrides section.
     * Contains a label, provider dropdown, model dropdown, and remove button.
     */
    private class AgentConfigRow {
        private final JPanel rowPanel;
        private final ComboBox<ModelProvider> providerCombo;
        private final ComboBox<LanguageModel> modelCombo;
        private final JButton removeButton;

        AgentConfigRow(int index) {
            rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));

            JBLabel label = new JBLabel("Sub-agent #" + (index + 1) + ":");
            label.setPreferredSize(new Dimension(100, label.getPreferredSize().height));
            rowPanel.add(label);

            providerCombo = new ComboBox<>();
            providerCombo.setRenderer(new UseDefaultProviderRenderer());
            providerCombo.setFont(DevoxxGenieFontsUtil.getDropdownFont());
            providerCombo.setPreferredSize(new Dimension(180, providerCombo.getPreferredSize().height));
            populateRowProviderComboBox();
            rowPanel.add(providerCombo);

            modelCombo = new ComboBox<>();
            modelCombo.setRenderer(new ModelInfoRenderer());
            modelCombo.setFont(DevoxxGenieFontsUtil.getDropdownFont());
            modelCombo.setPreferredSize(new Dimension(250, modelCombo.getPreferredSize().height));
            modelCombo.setEnabled(false);
            rowPanel.add(modelCombo);

            removeButton = new JButton("-");
            removeButton.setToolTipText("Remove this sub-agent slot");
            removeButton.setPreferredSize(new Dimension(45, removeButton.getPreferredSize().height));
            removeButton.addActionListener(e -> remove());
            rowPanel.add(removeButton);

            providerCombo.addActionListener(e -> {
                if ("comboBoxChanged".equals(e.getActionCommand())) {
                    updateRowModelComboBox();
                }
            });
        }

        private void populateRowProviderComboBox() {
            providerCombo.removeAllItems();
            // null = "Use default"
            providerCombo.addItem(null);

            LLMProviderService providerService = LLMProviderService.getInstance();
            DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();

            providerService.getAvailableModelProviders().stream()
                    .filter(provider -> switch (provider) {
                        case Ollama -> state.isOllamaEnabled();
                        case LMStudio -> state.isLmStudioEnabled();
                        case GPT4All -> state.isGpt4AllEnabled();
                        case Jan -> state.isJanEnabled();
                        case LLaMA -> state.isLlamaCPPEnabled();
                        case CustomOpenAI -> state.isCustomOpenAIUrlEnabled();
                        case OpenAI -> state.isOpenAIEnabled();
                        case Mistral -> state.isMistralEnabled();
                        case Anthropic -> state.isAnthropicEnabled();
                        case Groq -> state.isGroqEnabled();
                        case DeepInfra -> state.isDeepInfraEnabled();
                        case Google -> state.isGoogleEnabled();
                        case DeepSeek -> state.isDeepSeekEnabled();
                        case OpenRouter -> state.isOpenRouterEnabled();
                        case Grok -> state.isGrokEnabled();
                        case Kimi -> state.isKimiEnabled();
                        case GLM -> state.isGlmEnabled();
                        case AzureOpenAI -> state.isAzureOpenAIEnabled();
                        case Bedrock -> state.isAwsEnabled();
                        case CLIRunners -> false;
                        case ACPRunners -> false;
                    })
                    .distinct()
                    .sorted(Comparator.comparing(ModelProvider::getName))
                    .forEach(providerCombo::addItem);
        }

        private void updateRowModelComboBox() {
            modelCombo.removeAllItems();
            ModelProvider selectedProvider = (ModelProvider) providerCombo.getSelectedItem();

            if (selectedProvider == null) {
                modelCombo.setEnabled(false);
                return;
            }

            modelCombo.setEnabled(true);
            ChatModelFactoryProvider.getFactoryByProvider(selectedProvider.getName())
                    .ifPresent(factory -> {
                        List<LanguageModel> models = new ArrayList<>(factory.getModels());
                        if (!models.isEmpty()) {
                            models.sort(Comparator.naturalOrder());
                            models.forEach(modelCombo::addItem);
                        }
                    });
        }

        JPanel getPanel() {
            return rowPanel;
        }

        void setRemoveEnabled(boolean enabled) {
            removeButton.setEnabled(enabled);
        }

        SubAgentConfig toConfig() {
            ModelProvider provider = (ModelProvider) providerCombo.getSelectedItem();
            LanguageModel model = (LanguageModel) modelCombo.getSelectedItem();
            return new SubAgentConfig(
                    provider != null ? provider.getName() : "",
                    model != null ? model.getModelName() : ""
            );
        }

        void restoreConfig(SubAgentConfig config) {
            if (config == null || config.getModelProvider() == null || config.getModelProvider().isEmpty()) {
                providerCombo.setSelectedIndex(0); // "Use default"
                updateRowModelComboBox();
                return;
            }

            try {
                ModelProvider provider = ModelProvider.fromString(config.getModelProvider());
                if (selectProvider(provider, config.getModelName())) {
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // Provider not found
            }
            providerCombo.setSelectedIndex(0);
            updateRowModelComboBox();
        }

        private boolean selectProvider(ModelProvider provider, String modelName) {
            for (int i = 0; i < providerCombo.getItemCount(); i++) {
                if (provider.equals(providerCombo.getItemAt(i))) {
                    providerCombo.setSelectedIndex(i);
                    updateRowModelComboBox();
                    selectModel(modelName);
                    return true;
                }
            }
            return false;
        }

        private void selectModel(String modelName) {
            if (modelName == null || modelName.isEmpty()) {
                return;
            }
            for (int j = 0; j < modelCombo.getItemCount(); j++) {
                LanguageModel m = modelCombo.getItemAt(j);
                if (m != null && modelName.equals(m.getModelName())) {
                    modelCombo.setSelectedIndex(j);
                    return;
                }
            }
        }

        void remove() {
            int index = agentConfigRows.indexOf(this);
            if (agentConfigRows.size() <= 1 || index < 0 || index >= agentConfigRows.size()) {
                return;
            }

            // Capture current configs before rebuilding
            List<SubAgentConfig> currentConfigs = new ArrayList<>();
            for (AgentConfigRow row : agentConfigRows) {
                currentConfigs.add(row.toConfig());
            }
            currentConfigs.remove(index);

            // Rebuild all rows with re-numbered labels
            agentConfigPanel.removeAll();
            agentConfigRows.clear();

            for (int i = 0; i < currentConfigs.size(); i++) {
                AgentConfigRow row = new AgentConfigRow(i);
                agentConfigRows.add(row);
                agentConfigPanel.add(row.getPanel());
                row.restoreConfig(currentConfigs.get(i));
            }

            updateAddRemoveButtonState();
            agentConfigPanel.revalidate();
            agentConfigPanel.repaint();
        }
    }

    /**
     * Renderer for per-agent provider dropdown that shows "Use default" for null entries.
     */
    private static class UseDefaultProviderRenderer extends JPanel implements ListCellRenderer<ModelProvider> {
        private final JLabel nameLabel = new JBLabel();

        UseDefaultProviderRenderer() {
            setLayout(new BorderLayout());
            add(nameLabel, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            nameLabel.setFont(DevoxxGenieFontsUtil.getDropdownFont());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ModelProvider> list,
                                                      ModelProvider provider,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            nameLabel.setText(provider == null ? USE_DEFAULT_LABEL : provider.getName());

            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setEnabled(list.isEnabled());
            nameLabel.setFont(DevoxxGenieFontsUtil.getDropdownFont());

            return this;
        }
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

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/agent-mode";
    }

    public boolean isModified() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return enableAgentModeCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentModeEnabled())
                || maxToolCallsSpinner.getNumber() != (state.getAgentMaxToolCalls() != null ? state.getAgentMaxToolCalls() : AGENT_MAX_TOOL_CALLS)
                || autoApproveReadOnlyCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentAutoApproveReadOnly())
                || writeApprovalRequiredCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentWriteApprovalRequired())
                || enableDebugLogsCheckbox.isSelected() != Boolean.TRUE.equals(state.getAgentDebugLogsEnabled())
                || enableTestExecutionCheckbox.isSelected() != Boolean.TRUE.equals(state.getTestExecutionEnabled())
                || testTimeoutSpinner.getNumber() != (state.getTestExecutionTimeoutSeconds() != null ? state.getTestExecutionTimeoutSeconds() : TEST_EXECUTION_DEFAULT_TIMEOUT)
                || !Objects.equals(customTestCommandField.getText(), state.getTestExecutionCustomCommand() != null ? state.getTestExecutionCustomCommand() : "")
                || enablePsiToolsCheckbox.isSelected() != Boolean.TRUE.equals(state.getPsiToolsEnabled())
                || enableParallelExploreCheckbox.isSelected() != Boolean.TRUE.equals(state.getParallelExploreEnabled())
                || subAgentMaxToolCallsSpinner.getNumber() != (state.getSubAgentMaxToolCalls() != null ? state.getSubAgentMaxToolCalls() : SUB_AGENT_MAX_TOOL_CALLS)
                || subAgentTimeoutSpinner.getNumber() != (state.getSubAgentTimeoutSeconds() != null ? state.getSubAgentTimeoutSeconds() : SUB_AGENT_TIMEOUT_SECONDS)
                || !Objects.equals(getSelectedProviderName(), state.getSubAgentModelProvider() != null ? state.getSubAgentModelProvider() : "")
                || !Objects.equals(getSelectedModelName(), state.getSubAgentModelName() != null ? state.getSubAgentModelName() : "")
                || isPerAgentConfigsModified()
                || isToolCheckboxesModified();
    }

    private boolean isToolCheckboxesModified() {
        List<String> saved = stateService.getDisabledAgentTools();
        Set<String> savedSet = saved != null ? new HashSet<>(saved) : Collections.emptySet();
        for (Map.Entry<String, JBCheckBox> entry : toolCheckboxes.entrySet()) {
            boolean currentlyDisabled = !entry.getValue().isSelected();
            boolean wasDisabled = savedSet.contains(entry.getKey());
            if (currentlyDisabled != wasDisabled) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        stateService.setAgentModeEnabled(enableAgentModeCheckbox.isSelected());
        stateService.setAgentMaxToolCalls(maxToolCallsSpinner.getNumber());
        stateService.setAgentAutoApproveReadOnly(autoApproveReadOnlyCheckbox.isSelected());
        stateService.setAgentWriteApprovalRequired(writeApprovalRequiredCheckbox.isSelected());
        stateService.setAgentDebugLogsEnabled(enableDebugLogsCheckbox.isSelected());
        stateService.setTestExecutionEnabled(enableTestExecutionCheckbox.isSelected());
        stateService.setTestExecutionTimeoutSeconds(testTimeoutSpinner.getNumber());
        stateService.setTestExecutionCustomCommand(customTestCommandField.getText());
        stateService.setPsiToolsEnabled(enablePsiToolsCheckbox.isSelected());
        stateService.setParallelExploreEnabled(enableParallelExploreCheckbox.isSelected());
        stateService.setSubAgentMaxToolCalls(subAgentMaxToolCallsSpinner.getNumber());
        stateService.setSubAgentTimeoutSeconds(subAgentTimeoutSpinner.getNumber());
        stateService.setSubAgentModelProvider(getSelectedProviderName());
        stateService.setSubAgentModelName(getSelectedModelName());
        // setSubAgentConfigs also syncs subAgentParallelism to configs.size()
        stateService.setSubAgentConfigs(getPerAgentConfigs());

        // Save disabled agent tools
        List<String> disabledTools = new ArrayList<>();
        for (Map.Entry<String, JBCheckBox> entry : toolCheckboxes.entrySet()) {
            if (!entry.getValue().isSelected()) {
                disabledTools.add(entry.getKey());
            }
        }
        stateService.setDisabledAgentTools(disabledTools);
    }

    public void reset() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        enableAgentModeCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentModeEnabled()));
        maxToolCallsSpinner.setNumber(state.getAgentMaxToolCalls() != null ? state.getAgentMaxToolCalls() : AGENT_MAX_TOOL_CALLS);
        autoApproveReadOnlyCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentAutoApproveReadOnly()));
        writeApprovalRequiredCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentWriteApprovalRequired()));
        enableDebugLogsCheckbox.setSelected(Boolean.TRUE.equals(state.getAgentDebugLogsEnabled()));
        enableTestExecutionCheckbox.setSelected(Boolean.TRUE.equals(state.getTestExecutionEnabled()));
        testTimeoutSpinner.setNumber(state.getTestExecutionTimeoutSeconds() != null ? state.getTestExecutionTimeoutSeconds() : TEST_EXECUTION_DEFAULT_TIMEOUT);
        customTestCommandField.setText(state.getTestExecutionCustomCommand() != null ? state.getTestExecutionCustomCommand() : "");
        enablePsiToolsCheckbox.setSelected(Boolean.TRUE.equals(state.getPsiToolsEnabled()));
        enableParallelExploreCheckbox.setSelected(Boolean.TRUE.equals(state.getParallelExploreEnabled()));
        subAgentMaxToolCallsSpinner.setNumber(state.getSubAgentMaxToolCalls() != null ? state.getSubAgentMaxToolCalls() : SUB_AGENT_MAX_TOOL_CALLS);
        subAgentTimeoutSpinner.setNumber(state.getSubAgentTimeoutSeconds() != null ? state.getSubAgentTimeoutSeconds() : SUB_AGENT_TIMEOUT_SECONDS);

        // Repopulate providers (in case enabled providers changed) and restore selection
        populateProviderComboBox();
        restoreProviderSelection();

        // Rebuild per-agent config rows from saved state
        initAgentConfigRows();

        // Reset tool checkboxes
        List<String> disabledTools = state.getDisabledAgentTools();
        Set<String> disabledSet = disabledTools != null ? new HashSet<>(disabledTools) : Collections.emptySet();
        for (Map.Entry<String, JBCheckBox> entry : toolCheckboxes.entrySet()) {
            entry.getValue().setSelected(!disabledSet.contains(entry.getKey()));
        }

        // customTestCommandField.setText() above triggers caret-based scrollRectToVisible,
        // which scrolls IntelliJ's viewport to mid-panel. Scroll back to top
        // using the wrapper parent so the "More Info" header is also visible.
        SwingUtilities.invokeLater(() -> {
            Container parent = panel.getParent();
            if (parent instanceof JComponent wrapper) {
                wrapper.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            } else {
                panel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            }
        });
    }

    private String getSelectedProviderName() {
        ModelProvider provider = (ModelProvider) subAgentProviderComboBox.getSelectedItem();
        return provider != null ? provider.getName() : "";
    }

    private String getSelectedModelName() {
        LanguageModel model = (LanguageModel) subAgentModelComboBox.getSelectedItem();
        return model != null ? model.getModelName() : "";
    }

    @Override
    public void addListeners() {
        // No dynamic listeners needed
    }

    /**
     * Custom renderer for the sub-agent provider dropdown that shows "None (Auto-detect)" for null entries.
     */
    private static class SubAgentProviderRenderer extends JPanel implements ListCellRenderer<ModelProvider> {
        private final JLabel nameLabel = new JBLabel();

        SubAgentProviderRenderer() {
            setLayout(new BorderLayout());
            add(nameLabel, BorderLayout.CENTER);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            nameLabel.setFont(DevoxxGenieFontsUtil.getDropdownFont());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ModelProvider> list,
                                                      ModelProvider provider,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            nameLabel.setText(provider == null ? AUTO_DETECT_LABEL : provider.getName());

            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setEnabled(list.isEnabled());
            nameLabel.setFont(DevoxxGenieFontsUtil.getDropdownFont());

            return this;
        }
    }
}
