package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.model.Persona;
import com.devoxx.genie.service.analyzer.DevoxxGenieGenerator;
import com.devoxx.genie.ui.dialog.PersonaDialog;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.PlusIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.RefreshIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.TrashIcon;

public class PromptSettingsComponent extends AbstractSettingsComponent {

    @Getter
    @Setter
    private String submitShortcutWindows;

    @Getter
    @Setter
    private String submitShortcutMac;

    @Getter
    @Setter
    private String submitShortcutLinux;

    @Getter
    @Setter
    private String newlineShortcutWindows;

    @Getter
    @Setter
    private String newlineShortcutMac;

    @Getter
    @Setter
    private String newlineShortcutLinux;

    @Getter
    private final JTextArea systemPromptField = new JTextArea(stateService.getSystemPrompt());

    @Getter
    private final JCheckBox createDevoxxGenieMdCheckbox = new JCheckBox("Generate DEVOXXGENIE.md file", stateService.getCreateDevoxxGenieMd());

    @Getter
    private final JCheckBox includeProjectTreeCheckbox = new JCheckBox("Include project tree", stateService.getIncludeProjectTree());

    @Getter
    private final JSpinner projectTreeDepthSpinner = new JSpinner(new SpinnerNumberModel(stateService.getProjectTreeDepth().intValue(), 1, 10, 1));

    @Getter
    private final JCheckBox useDevoxxGenieMdInPromptCheckbox = new JCheckBox("Use DEVOXXGENIE.md in prompt", stateService.getUseDevoxxGenieMdInPrompt());

    @Getter
    private final JCheckBox useClaudeOrAgentsMdInPromptCheckbox = new JCheckBox("Use CLAUDE.md or AGENTS.md in prompt", stateService.getUseClaudeOrAgentsMdInPrompt());

    @Getter
    private final JButton createDevoxxGenieMdButton = new JButton("Create DEVOXXGENIE.md");

    // --- Personas ---
    private static final int PERSONA_NAME_COLUMN = 0;
    private static final int PERSONA_PROMPT_COLUMN = 1;

    @Getter
    private final JCheckBox showPersonasCheckbox = new JCheckBox("Show personas", Boolean.TRUE.equals(stateService.getShowPersonas()));

    private final DefaultTableModel personasTableModel = new DefaultTableModel(new String[]{"Persona", "System Prompt"}, 0);

    private final JBTable personasTable = new JBTable(personasTableModel) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    @Getter
    private final ComboBox<String> defaultPersonaComboBox = new ComboBox<>();

    private final Project project;

    public PromptSettingsComponent(Project project) {
        this.project = project;
        createDevoxxGenieMdButton.addActionListener(e -> createDevoxxGenieMdFile());
        setupPersonasTable();
        setPersonas(stateService.getPersonas(), stateService.getDefaultPersonaName());
        addListeners();
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/configuration/prompts";
    }

    @Override
    public JPanel createPanel() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = JBUI.insets(4, 5);
        gbc.gridy = 0;

        // --- System Prompt ---
        addSection(contentPanel, gbc, "System Prompt");
        addPromptArea(contentPanel, gbc, systemPromptField);

        // --- Personas ---
        addSection(contentPanel, gbc, "Personas");

        addFullWidthRow(contentPanel, gbc, showPersonasCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, a persona dropdown appears in the tool window. The selected persona's " +
                "system prompt replaces the system prompt above. A persona change applies to new " +
                "conversations; ongoing conversations keep the persona they started with.");

        JPanel personaButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        personaButtonPanel.add(createActionButton("Add", PlusIcon, "Add persona", e -> addPersona()));
        personaButtonPanel.add(createActionButton("Remove", TrashIcon, "Remove persona", e -> removePersona()));
        personaButtonPanel.add(createActionButton("Restore", RefreshIcon, "Restore default personas", e -> restoreDefaultPersonas()));
        addFullWidthRow(contentPanel, gbc, personaButtonPanel);

        JBScrollPane personasScrollPane = new JBScrollPane(personasTable);
        personasScrollPane.setPreferredSize(new Dimension(-1, 150));
        addFullWidthRow(contentPanel, gbc, personasScrollPane);

        JPanel defaultPersonaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        defaultPersonaPanel.add(new JLabel("Default persona:"));
        defaultPersonaPanel.add(defaultPersonaComboBox);
        addFullWidthRow(contentPanel, gbc, defaultPersonaPanel);
        addHelpText(contentPanel, gbc,
                "The default persona is pre-selected in the dropdown when the tool window opens.");

        // --- DEVOXXGENIE.md Generation ---
        addSection(contentPanel, gbc, "DEVOXXGENIE.md Generation");

        addFullWidthRow(contentPanel, gbc, createDevoxxGenieMdCheckbox);

        JPanel projectTreePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        projectTreePanel.add(includeProjectTreeCheckbox);
        projectTreePanel.add(new JLabel("Tree depth:"));
        projectTreePanel.add(projectTreeDepthSpinner);
        addFullWidthRow(contentPanel, gbc, projectTreePanel);

        addFullWidthRow(contentPanel, gbc, useDevoxxGenieMdInPromptCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the content of DEVOXXGENIE.md will be included in the prompt sent to the AI, " +
                "providing it with context about your project structure and important files.");

        addFullWidthRow(contentPanel, gbc, createDevoxxGenieMdButton);

        // --- CLAUDE.md / AGENTS.md Inclusion ---
        addSection(contentPanel, gbc, "CLAUDE.md / AGENTS.md Inclusion");

        addFullWidthRow(contentPanel, gbc, useClaudeOrAgentsMdInPromptCheckbox);
        addHelpText(contentPanel, gbc,
                "When enabled, the plugin will check for CLAUDE.md or AGENTS.md files in your project root. " +
                "If both files exist, CLAUDE.md takes priority and AGENTS.md is skipped. " +
                "The content will be included in the prompt to provide AI-specific context and instructions.");

        createDevoxxGenieMdCheckbox.addChangeListener(e -> {
            boolean enabled = createDevoxxGenieMdCheckbox.isSelected();
            includeProjectTreeCheckbox.setEnabled(enabled);
            projectTreeDepthSpinner.setEnabled(enabled && includeProjectTreeCheckbox.isSelected());
            useDevoxxGenieMdInPromptCheckbox.setEnabled(enabled);
            createDevoxxGenieMdButton.setEnabled(enabled);
        });

        includeProjectTreeCheckbox.addChangeListener(e ->
                projectTreeDepthSpinner.setEnabled(createDevoxxGenieMdCheckbox.isSelected() && includeProjectTreeCheckbox.isSelected())
        );

        includeProjectTreeCheckbox.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
        projectTreeDepthSpinner.setEnabled(createDevoxxGenieMdCheckbox.isSelected() && includeProjectTreeCheckbox.isSelected());
        useDevoxxGenieMdInPromptCheckbox.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
        createDevoxxGenieMdButton.setEnabled(createDevoxxGenieMdCheckbox.isSelected());

        // --- Keyboard Shortcuts ---
        addSection(contentPanel, gbc, "Configure keyboard submit shortcut");

        if (SystemInfo.isWindows) {
            addFullWidthRow(contentPanel, gbc, createShortcutPanel("Windows", stateService.getSubmitShortcutWindows(), true));
        } else if (SystemInfo.isMac) {
            addFullWidthRow(contentPanel, gbc, createShortcutPanel("Mac", stateService.getSubmitShortcutMac(), true));
        } else {
            addFullWidthRow(contentPanel, gbc, createShortcutPanel("Linux", stateService.getSubmitShortcutLinux(), true));
        }

        addSection(contentPanel, gbc, "Configure keyboard newline shortcut");

        if (SystemInfo.isWindows) {
            addFullWidthRow(contentPanel, gbc, createNewlineShortcutPanel("Windows", stateService.getNewlineShortcutWindows()));
        } else if (SystemInfo.isMac) {
            addFullWidthRow(contentPanel, gbc, createNewlineShortcutPanel("Mac", stateService.getNewlineShortcutMac()));
        } else {
            addFullWidthRow(contentPanel, gbc, createNewlineShortcutPanel("Linux", stateService.getNewlineShortcutLinux()));
        }

        addHelpText(contentPanel, gbc,
                "You can also trigger the add files popup dialog using @ in the input field.");

        // Filler
        gbc.weighty = 1.0;
        gbc.gridy++;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    // --- Personas support ---

    private void setupPersonasTable() {
        personasTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        personasTable.setStriped(true);
        personasTable.getColumnModel().getColumn(PERSONA_NAME_COLUMN).setPreferredWidth(120);
        personasTable.getColumnModel().getColumn(PERSONA_PROMPT_COLUMN).setPreferredWidth(480);

        personasTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editPersona();
                }
            }
        });
    }

    /**
     * @return the personas as shown in the table.
     */
    public List<Persona> getPersonas() {
        List<Persona> result = new ArrayList<>();
        for (int i = 0; i < personasTableModel.getRowCount(); i++) {
            String name = (String) personasTableModel.getValueAt(i, PERSONA_NAME_COLUMN);
            String prompt = (String) personasTableModel.getValueAt(i, PERSONA_PROMPT_COLUMN);
            result.add(new Persona(name, prompt));
        }
        return result;
    }

    /**
     * @return the default persona name selected in the combo, or {@code null} when the table is empty.
     */
    public @Nullable String getDefaultPersonaName() {
        return (String) defaultPersonaComboBox.getSelectedItem();
    }

    public void setPersonas(@NotNull List<Persona> personas, @Nullable String defaultPersonaName) {
        personasTableModel.setRowCount(0);
        for (Persona persona : personas) {
            personasTableModel.addRow(new Object[]{persona.getName(), persona.getPrompt()});
        }
        refreshDefaultPersonaComboBox(defaultPersonaName);
    }

    /**
     * Rebuilds the default-persona combo from the current table rows, keeping
     * {@code preferredSelection} (or the previous selection) when still present.
     */
    private void refreshDefaultPersonaComboBox(@Nullable String preferredSelection) {
        String selection = preferredSelection != null
                ? preferredSelection
                : (String) defaultPersonaComboBox.getSelectedItem();
        defaultPersonaComboBox.removeAllItems();
        for (int i = 0; i < personasTableModel.getRowCount(); i++) {
            defaultPersonaComboBox.addItem((String) personasTableModel.getValueAt(i, PERSONA_NAME_COLUMN));
        }
        if (selection != null) {
            defaultPersonaComboBox.setSelectedItem(selection);
        }
        if (defaultPersonaComboBox.getSelectedIndex() == -1 && defaultPersonaComboBox.getItemCount() > 0) {
            defaultPersonaComboBox.setSelectedIndex(0);
        }
    }

    private void addPersona() {
        PersonaDialog dialog = new PersonaDialog(project);
        if (dialog.showAndGet()) {
            personasTableModel.addRow(new Object[]{dialog.getPersonaName(), dialog.getPrompt()});
            int newRowIndex = personasTableModel.getRowCount() - 1;
            personasTable.setRowSelectionInterval(newRowIndex, newRowIndex);
            personasTable.scrollRectToVisible(personasTable.getCellRect(newRowIndex, 0, true));
            refreshDefaultPersonaComboBox(null);
        }
    }

    private void editPersona() {
        int selectedRow = personasTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        String name = (String) personasTableModel.getValueAt(selectedRow, PERSONA_NAME_COLUMN);
        String prompt = (String) personasTableModel.getValueAt(selectedRow, PERSONA_PROMPT_COLUMN);

        PersonaDialog dialog = new PersonaDialog(project, name, prompt);
        if (dialog.showAndGet()) {
            boolean wasDefault = name != null && name.equals(defaultPersonaComboBox.getSelectedItem());
            personasTableModel.setValueAt(dialog.getPersonaName(), selectedRow, PERSONA_NAME_COLUMN);
            personasTableModel.setValueAt(dialog.getPrompt(), selectedRow, PERSONA_PROMPT_COLUMN);
            refreshDefaultPersonaComboBox(wasDefault ? dialog.getPersonaName() : null);
        }
    }

    private void removePersona() {
        int selectedRow = personasTable.getSelectedRow();
        if (selectedRow != -1) {
            personasTableModel.removeRow(selectedRow);
            refreshDefaultPersonaComboBox(null);
        }
    }

    private void restoreDefaultPersonas() {
        setPersonas(stateService.getDefaultPersonas(), com.devoxx.genie.model.Constant.DEFAULT_PERSONA_NAME);
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
        gbc.insets = JBUI.insets(0, 25, 8, 5);
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
        gbc.insets = JBUI.insets(4, 5);
        gbc.gridy++;
    }

    private @NotNull JPanel createShortcutPanel(String os, String initialShortcut, boolean isSubmitShortcut) {
        KeyboardShortcutPanel shortcutPanel = new KeyboardShortcutPanel(project, os, initialShortcut, shortcut -> {
            if (isSubmitShortcut) {
                if ("Mac".equalsIgnoreCase(os)) {
                    setSubmitShortcutMac(shortcut);
                } else if ("Windows".equalsIgnoreCase(os)) {
                    setSubmitShortcutWindows(shortcut);
                } else {
                    setSubmitShortcutLinux(shortcut);
                }
                notifyShortcutChanged(shortcut);
            } else {
                if ("Mac".equalsIgnoreCase(os)) {
                    setNewlineShortcutMac(shortcut);
                } else if ("Windows".equalsIgnoreCase(os)) {
                    setNewlineShortcutWindows(shortcut);
                } else {
                    setNewlineShortcutLinux(shortcut);
                }
                notifyNewlineShortcutChanged(shortcut);
            }
        });

        if (isSubmitShortcut) {
            if ("Mac".equalsIgnoreCase(os)) {
                submitShortcutMac = shortcutPanel.getCurrentShortcut();
            } else if ("Windows".equalsIgnoreCase(os)) {
                submitShortcutWindows = shortcutPanel.getCurrentShortcut();
            } else {
                submitShortcutLinux = shortcutPanel.getCurrentShortcut();
            }
        } else {
            if ("Mac".equalsIgnoreCase(os)) {
                newlineShortcutMac = shortcutPanel.getCurrentShortcut();
            } else if ("Windows".equalsIgnoreCase(os)) {
                newlineShortcutWindows = shortcutPanel.getCurrentShortcut();
            } else {
                newlineShortcutLinux = shortcutPanel.getCurrentShortcut();
            }
        }

        return shortcutPanel;
    }

    private @NotNull JPanel createNewlineShortcutPanel(String os, String initialShortcut) {
        return createShortcutPanel(os, initialShortcut, false);
    }

    private void addPromptArea(@NotNull JPanel panel,
                               @NotNull GridBagConstraints gbc,
                               @NotNull JTextArea textArea) {
        gbc.gridy++;
        panel.add(new JLabel("System instruction"), gbc);

        gbc.gridy++;
        textArea.setRows(5);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(-1, 100));
        panel.add(scrollPane, gbc);
    }

    private void notifyShortcutChanged(String shortcut) {
        project.getMessageBus()
                .syncPublisher(AppTopics.SHORTCUT_CHANGED_TOPIC)
                .onShortcutChanged(shortcut);
    }

    private void notifyNewlineShortcutChanged(String shortcut) {
        project.getMessageBus()
                .syncPublisher(AppTopics.NEWLINE_SHORTCUT_CHANGED_TOPIC)
                .onNewlineShortcutChanged(shortcut);
    }

    private void createDevoxxGenieMdFile() {
        createDevoxxGenieMdButton.setEnabled(false);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating DEVOXXGENIE.md", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Generating DEVOXXGENIE.md file...");

                    boolean includeTree = includeProjectTreeCheckbox.isSelected();
                    int treeDepth = (Integer) projectTreeDepthSpinner.getValue();

                    DevoxxGenieGenerator devoxxGenieGenerator =
                            new DevoxxGenieGenerator(project, includeTree, treeDepth, indicator);
                    devoxxGenieGenerator.generate();
                } finally {
                    ApplicationManager.getApplication().invokeLater(() ->
                            createDevoxxGenieMdButton.setEnabled(createDevoxxGenieMdCheckbox.isSelected())
                    );
                }
            }
        });
    }
}
