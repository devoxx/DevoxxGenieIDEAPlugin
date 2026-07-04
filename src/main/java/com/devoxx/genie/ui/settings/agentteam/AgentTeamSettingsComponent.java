package com.devoxx.genie.ui.settings.agentteam;

import com.devoxx.genie.chatmodel.ChatModelFactoryProvider;
import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentToolsetPreset;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.agent.team.AgentRegistry;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.devoxx.genie.model.Constant.SUB_AGENT_MAX_TOOL_CALLS;
import static com.devoxx.genie.model.Constant.SUB_AGENT_TIMEOUT_SECONDS;

/**
 * Settings → DevoxxGenie → Agent Team: list + editor for {@link AgentDefinition}s
 * (TASK-245). Works on an in-memory copy of the registry; apply() validates and
 * persists, reset() reloads. Built-ins can be edited, disabled and reset to their
 * shipped defaults, but not deleted or renamed.
 */
public class AgentTeamSettingsComponent extends AbstractSettingsComponent {

    private final DefaultListModel<AgentDefinition> listModel = new DefaultListModel<>();
    private final JBList<AgentDefinition> agentList = new JBList<>(listModel);

    private final JBTextField nameField = new JBTextField();
    private final JBTextField descriptionField = new JBTextField();
    private final JBTextArea instructionArea = new JBTextArea(10, 60);
    private final ComboBox<ModelProvider> providerComboBox = new ComboBox<>();
    private final ComboBox<LanguageModel> modelComboBox = new ComboBox<>();
    private final Map<AgentToolsetPreset, JBCheckBox> presetCheckboxes = new LinkedHashMap<>();
    private final JBCheckBox readOnlyCheckbox = new JBCheckBox("Read-only (strips write/run tools from the selected presets)");
    private final JBCheckBox enabledCheckbox = new JBCheckBox("Enabled (visible in the orchestrator catalog)");
    private final JBIntSpinner maxToolCallsSpinner = new JBIntSpinner(SUB_AGENT_MAX_TOOL_CALLS, 1, 500);
    private final JBIntSpinner timeoutSpinner = new JBIntSpinner(SUB_AGENT_TIMEOUT_SECONDS, 10, 3600);
    private final JBTextField temperatureField = new JBTextField();
    private final JBLabel resolvedToolsLabel = new JBLabel();
    private final JButton addButton = new JButton("Add");
    private final JButton copyButton = new JButton("Copy");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton resetAgentButton = new JButton("Reset to Default");

    /** Index of the agent currently shown in the editor; -1 = none. */
    private int editedIndex = -1;
    /** Guards against form-sync feedback loops while loading a selection. */
    private boolean loadingForm = false;

    public AgentTeamSettingsComponent() {
        buildUi();
        loadWorkingCopy();
    }

    // ---------------------------------------------------------------- UI --

    private void buildUi() {
        agentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        agentList.setCellRenderer(new AgentListCellRenderer());
        agentList.setVisibleRowCount(6);
        agentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });

        addButton.addActionListener(e -> addAgent(null));
        copyButton.addActionListener(e -> {
            AgentDefinition selected = agentList.getSelectedValue();
            if (selected != null) {
                syncFormIntoEditedAgent();
                addAgent(selected);
            }
        });
        deleteButton.addActionListener(e -> deleteSelectedAgent());
        resetAgentButton.addActionListener(e -> resetSelectedAgentToDefault());

        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        listButtons.add(addButton);
        listButtons.add(copyButton);
        listButtons.add(deleteButton);
        listButtons.add(resetAgentButton);

        JPanel listPanel = new JPanel(new BorderLayout(0, 4));
        listPanel.add(new JBScrollPane(agentList), BorderLayout.CENTER);
        listPanel.add(listButtons, BorderLayout.SOUTH);

        // --- editor form ---
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insets(2, 4);

        addSection(form, gbc, "Agent Definition");
        addSettingRow(form, gbc, "Name", nameField);
        addSettingRow(form, gbc, "Description", descriptionField);

        instructionArea.setLineWrap(true);
        instructionArea.setWrapStyleWord(true);
        JBScrollPane instructionScroll = new JBScrollPane(instructionArea);
        instructionScroll.setMinimumSize(new Dimension(200, 150));
        addSettingRow(form, gbc, "Persona / system instruction", instructionScroll);

        addSection(form, gbc, "Model Binding");
        providerComboBox.setRenderer(new ProviderRenderer());
        providerComboBox.addActionListener(e -> {
            if (!loadingForm) {
                updateModelComboBox(null);
            }
        });
        addSettingRow(form, gbc, "Provider", providerComboBox);
        addSettingRow(form, gbc, "Model", modelComboBox);
        addSettingRow(form, gbc, "",
                new JBLabel("Leave provider on 'Inherit' to use the conversation's active model. " +
                        "Binding agents to different providers enables hybrid local/cloud teams."));

        addSection(form, gbc, "Tools");
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (AgentToolsetPreset preset : AgentToolsetPreset.values()) {
            JBCheckBox cb = new JBCheckBox(preset.getKey());
            cb.setToolTipText(String.join(", ", preset.getTools()));
            cb.addActionListener(e -> {
                if (!loadingForm) {
                    updateResolvedToolsLabel();
                }
            });
            presetCheckboxes.put(preset, cb);
            presetPanel.add(cb);
        }
        addSettingRow(form, gbc, "Toolset presets", presetPanel);
        readOnlyCheckbox.addActionListener(e -> {
            if (!loadingForm) {
                updateResolvedToolsLabel();
            }
        });
        addSettingRow(form, gbc, "", readOnlyCheckbox);
        addSettingRow(form, gbc, "Resolved tools", resolvedToolsLabel);

        addSection(form, gbc, "Budgets");
        addSettingRow(form, gbc, "Max tool calls", maxToolCallsSpinner);
        addSettingRow(form, gbc, "Timeout (seconds)", timeoutSpinner);
        addSettingRow(form, gbc, "Temperature (empty = global)", temperatureField);
        addSettingRow(form, gbc, "", enabledCheckbox);

        // absorb remaining vertical space
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        form.add(new JPanel(), gbc);

        populateProviderComboBox();

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(listPanel, BorderLayout.NORTH);
        content.add(new JBScrollPane(form), BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/agent-mode";
    }

    // ------------------------------------------------------ working copy --

    private void loadWorkingCopy() {
        listModel.clear();
        for (AgentDefinition def : AgentRegistry.getInstance().getAll()) {
            listModel.addElement(def);
        }
        editedIndex = -1;
        if (!listModel.isEmpty()) {
            agentList.setSelectedIndex(0);
        }
    }

    private List<AgentDefinition> workingCopy() {
        List<AgentDefinition> result = new ArrayList<>(listModel.size());
        for (int i = 0; i < listModel.size(); i++) {
            result.add(listModel.get(i));
        }
        return result;
    }

    private void onSelectionChanged() {
        syncFormIntoEditedAgent();
        AgentDefinition selected = agentList.getSelectedValue();
        editedIndex = agentList.getSelectedIndex();
        loadForm(selected);
    }

    private void loadForm(@Nullable AgentDefinition def) {
        loadingForm = true;
        try {
            boolean hasSelection = def != null;
            for (JComponent c : List.of(nameField, descriptionField, instructionArea, providerComboBox,
                    modelComboBox, readOnlyCheckbox, enabledCheckbox, maxToolCallsSpinner,
                    timeoutSpinner, temperatureField)) {
                c.setEnabled(hasSelection);
            }
            presetCheckboxes.values().forEach(cb -> cb.setEnabled(hasSelection));
            deleteButton.setEnabled(hasSelection && !def.isBuiltIn());
            copyButton.setEnabled(hasSelection);
            resetAgentButton.setEnabled(hasSelection && def.isBuiltIn());
            if (!hasSelection) {
                return;
            }

            nameField.setText(def.getName());
            nameField.setEditable(!def.isBuiltIn()); // built-in names are contract, not free text
            descriptionField.setText(def.getDescription());
            instructionArea.setText(def.getInstruction());
            instructionArea.setCaretPosition(0);
            selectProvider(def.getModelProvider());
            updateModelComboBox(def.getModelName());
            presetCheckboxes.forEach((preset, cb) ->
                    cb.setSelected(def.getToolsetPresets() != null
                            && def.getToolsetPresets().contains(preset.getKey())));
            readOnlyCheckbox.setSelected(def.isReadOnly());
            enabledCheckbox.setSelected(def.isEnabled());
            maxToolCallsSpinner.setNumber(def.getMaxToolCalls() != null ? def.getMaxToolCalls() : SUB_AGENT_MAX_TOOL_CALLS);
            timeoutSpinner.setNumber(def.getTimeoutSeconds() != null ? def.getTimeoutSeconds() : SUB_AGENT_TIMEOUT_SECONDS);
            temperatureField.setText(def.getTemperature() != null ? String.valueOf(def.getTemperature()) : "");
            updateResolvedToolsLabel();
        } finally {
            loadingForm = false;
        }
    }

    /** Writes the form fields back into the agent the form was loaded from. */
    private void syncFormIntoEditedAgent() {
        if (editedIndex < 0 || editedIndex >= listModel.size() || loadingForm) {
            return;
        }
        AgentDefinition def = listModel.get(editedIndex);
        if (!def.isBuiltIn()) {
            def.setName(nameField.getText().trim());
        }
        def.setDescription(descriptionField.getText().trim());
        def.setInstruction(instructionArea.getText());
        ModelProvider provider = (ModelProvider) providerComboBox.getSelectedItem();
        def.setModelProvider(provider != null ? provider.getName() : "");
        LanguageModel model = (LanguageModel) modelComboBox.getSelectedItem();
        def.setModelName(provider != null && model != null ? model.getModelName() : "");
        List<String> presets = new ArrayList<>();
        presetCheckboxes.forEach((preset, cb) -> {
            if (cb.isSelected()) {
                presets.add(preset.getKey());
            }
        });
        def.setToolsetPresets(presets);
        def.setReadOnly(readOnlyCheckbox.isSelected());
        def.setEnabled(enabledCheckbox.isSelected());
        def.setMaxToolCalls(maxToolCallsSpinner.getNumber());
        def.setTimeoutSeconds(timeoutSpinner.getNumber());
        def.setTemperature(parseTemperature(temperatureField.getText()));
        listModel.set(editedIndex, def); // trigger renderer refresh
    }

    private static @Nullable Double parseTemperature(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------- actions --

    private void addAgent(@Nullable AgentDefinition template) {
        syncFormIntoEditedAgent();
        AgentDefinition fresh = template != null ? template.copy() : AgentDefinition.builder()
                .instruction("You are a specialist agent. Complete the delegated task and return "
                        + "a terse, self-contained summary.")
                .toolsetPresets(new ArrayList<>(List.of("filesystem-ro")))
                .readOnly(true)
                .build();
        fresh.setBuiltIn(false);
        fresh.setName(uniqueName(template != null ? template.getName() + "-copy" : "new-agent"));
        listModel.addElement(fresh);
        agentList.setSelectedIndex(listModel.size() - 1);
    }

    private @NotNull String uniqueName(@NotNull String base) {
        List<String> names = workingCopy().stream().map(AgentDefinition::getName).toList();
        if (!names.contains(base)) {
            return base;
        }
        for (int i = 2; ; i++) {
            String candidate = base + "-" + i;
            if (!names.contains(candidate)) {
                return candidate;
            }
        }
    }

    private void deleteSelectedAgent() {
        int index = agentList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        AgentDefinition def = listModel.get(index);
        if (def.isBuiltIn()) {
            return; // button is disabled for built-ins; defensive double-check
        }
        editedIndex = -1; // the form content belongs to the row being removed
        listModel.remove(index);
        if (!listModel.isEmpty()) {
            agentList.setSelectedIndex(Math.min(index, listModel.size() - 1));
        } else {
            loadForm(null);
        }
    }

    private void resetSelectedAgentToDefault() {
        int index = agentList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        AgentDefinition current = listModel.get(index);
        AgentRegistry.getInstance().shippedDefault(current.getName()).ifPresent(shipped -> {
            editedIndex = -1; // discard pending form edits for this row
            listModel.set(index, shipped);
            agentList.setSelectedIndex(index);
            onSelectionChanged();
        });
    }

    // -------------------------------------------------- provider / model --

    private void populateProviderComboBox() {
        providerComboBox.removeAllItems();
        providerComboBox.addItem(null); // "Inherit conversation model" sentinel

        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        LLMProviderService.getInstance().getAvailableModelProviders().stream()
                .filter(provider -> switch (provider) {
                    case Ollama -> state.isOllamaEnabled();
                    case LMStudio -> state.isLmStudioEnabled();
                    case GPT4All -> state.isGpt4AllEnabled();
                    case Jan -> state.isJanEnabled();
                    case LLaMA -> state.isLlamaCPPEnabled();
                    case Exo -> state.isExoEnabled();
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
                    case Nvidia -> state.isNvidiaEnabled();
                    case AzureOpenAI -> state.isAzureOpenAIEnabled();
                    case Bedrock -> state.isAwsEnabled();
                    case CLIRunners, ACPRunners -> false;
                })
                .distinct()
                .sorted(Comparator.comparing(ModelProvider::getName))
                .forEach(providerComboBox::addItem);
    }

    private void selectProvider(@Nullable String providerName) {
        if (providerName == null || providerName.isBlank()) {
            providerComboBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < providerComboBox.getItemCount(); i++) {
            ModelProvider item = providerComboBox.getItemAt(i);
            if (item != null && providerName.equals(item.getName())) {
                providerComboBox.setSelectedIndex(i);
                return;
            }
        }
        providerComboBox.setSelectedIndex(0);
    }

    private void updateModelComboBox(@Nullable String modelToSelect) {
        modelComboBox.removeAllItems();
        ModelProvider provider = (ModelProvider) providerComboBox.getSelectedItem();
        if (provider == null) {
            modelComboBox.setEnabled(false);
            return;
        }
        modelComboBox.setEnabled(true);
        ChatModelFactoryProvider.getFactoryByProvider(provider.getName())
                .ifPresent(factory -> {
                    List<LanguageModel> models = new ArrayList<>(factory.getModels());
                    models.sort(Comparator.naturalOrder());
                    models.forEach(modelComboBox::addItem);
                });
        if (modelToSelect != null && !modelToSelect.isBlank()) {
            for (int i = 0; i < modelComboBox.getItemCount(); i++) {
                LanguageModel model = modelComboBox.getItemAt(i);
                if (model != null && modelToSelect.equals(model.getModelName())) {
                    modelComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void updateResolvedToolsLabel() {
        List<String> presets = new ArrayList<>();
        presetCheckboxes.forEach((preset, cb) -> {
            if (cb.isSelected()) {
                presets.add(preset.getKey());
            }
        });
        var tools = AgentToolsetPreset.resolveTools(presets, readOnlyCheckbox.isSelected());
        resolvedToolsLabel.setText(tools.isEmpty()
                ? "(no tools — agent answers from its own knowledge)"
                : String.join(", ", tools));
    }

    // -------------------------------------------------- settings lifecycle --

    public boolean isModified() {
        syncFormIntoEditedAgent();
        List<AgentDefinition> persisted = AgentRegistry.getInstance().getAll();
        List<AgentDefinition> working = workingCopy();
        if (persisted.size() != working.size()) {
            return true;
        }
        for (int i = 0; i < working.size(); i++) {
            if (!Objects.equals(persisted.get(i), working.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates and persists the working copy.
     *
     * @throws IllegalArgumentException with a user-presentable message on invalid names,
     *         duplicates, empty personas or deleted built-ins — the configurable converts
     *         it into a {@code ConfigurationException} so the settings dialog stays open.
     */
    public void apply() {
        syncFormIntoEditedAgent();
        AgentRegistry.getInstance().saveAll(workingCopy());
    }

    public void reset() {
        populateProviderComboBox();
        loadWorkingCopy();
    }

    // -------------------------------------------------------- renderers --

    private static class AgentListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AgentDefinition def) {
                String model = def.getModelProvider() == null || def.getModelProvider().isBlank()
                        ? "conversation model"
                        : def.getModelProvider() + (def.getModelName() == null || def.getModelName().isBlank()
                            ? "" : " · " + def.getModelName());
                String suffix = def.isEnabled() ? "" : "  [disabled]";
                String badge = def.isBuiltIn() ? "" : "  [custom]";
                setText(def.getName() + "  —  " + model + badge + suffix);
                setEnabled(def.isEnabled());
            }
            return this;
        }
    }

    private static class ProviderRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText("Inherit conversation model");
            } else if (value instanceof ModelProvider provider) {
                setText(provider.getName());
            }
            return this;
        }
    }
}
