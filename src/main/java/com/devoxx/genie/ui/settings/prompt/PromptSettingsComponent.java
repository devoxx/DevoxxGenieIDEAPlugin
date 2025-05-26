package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.service.analyzer.DevoxxGenieGenerator;
import com.devoxx.genie.ui.dialog.CustomPromptDialog;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.*;

public class PromptSettingsComponent extends AbstractSettingsComponent {

    private static final int PROMPT_COLUMN = 1;

    private final DevoxxGenieStateService settings;

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
    private final JTextArea testPromptField = new JTextArea(stateService.getTestPrompt());
    @Getter
    private final JTextArea explainPromptField = new JTextArea(stateService.getExplainPrompt());
    @Getter
    private final JTextArea reviewPromptField = new JTextArea(stateService.getReviewPrompt());
    
    // DEVOXXGENIE.md generation options
    @Getter
    private final JCheckBox createDevoxxGenieMdCheckbox = new JCheckBox("Generate DEVOXXGENIE.md file", stateService.getCreateDevoxxGenieMd());
    @Getter
    private final JCheckBox includeProjectTreeCheckbox = new JCheckBox("Include project tree", stateService.getIncludeProjectTree());
    @Getter
    private final JSpinner projectTreeDepthSpinner = new JSpinner(new SpinnerNumberModel(stateService.getProjectTreeDepth().intValue(), 1, 10, 1));
    @Getter
    private final JCheckBox useDevoxxGenieMdInPromptCheckbox = new JCheckBox("Use DEVOXXGENIE.md in prompt", stateService.getUseDevoxxGenieMdInPrompt());
    @Getter
    private final JButton createDevoxxGenieMdButton = new JButton("Create DEVOXXGENIE.md");

    private final DefaultTableModel customPromptsTableModel = new DefaultTableModel(new String[]{"Command", "Prompt"}, 0);

    private final JBTable customPromptsTable = new JBTable(customPromptsTableModel) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final Project project;

    public PromptSettingsComponent(Project project) {
        this.project = project;

        settings = DevoxxGenieStateService.getInstance();

        setupCustomPromptsTable();
        setCustomPrompts(settings.getCustomPrompts());
        
        // Set up the action listener for the Create DEVOXXGENIE.md button
        createDevoxxGenieMdButton.addActionListener(e -> createDevoxxGenieMdFile());

        addListeners();
    }

    @Override
    public JPanel createPanel() {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = JBUI.insets(5);

        addSection(panel, gbc, "Prompts");
        addPromptArea(panel, gbc, systemPromptField);
        addSection(panel, gbc, "Custom Prompts");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JEditorPane(
                "text/html",
                "<html><body>Create custom command prompts which can be called using the / prefix in the prompt input field.</body></html>"),
            gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(createActionButton("Add", PlusIcon, "Add custom prompt", e -> addCustomPrompt()));
        buttonPanel.add(createActionButton("Remove", TrashIcon, "Remove custom prompt", e -> removeCustomPrompt()));
        buttonPanel.add(createActionButton("Restore", RefreshIcon, "Restore custom prompts", e -> restoreDefaultPrompts()));

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        JBScrollPane tableScrollPane = new JBScrollPane(customPromptsTable);
        tableScrollPane.setPreferredSize(new Dimension(-1, 200));
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(tableScrollPane, gbc);

        // Add DEVOXXGENIE.md section
        addSection(panel, gbc, "DEVOXXGENIE.md Generation");
        
        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(createDevoxxGenieMdCheckbox, gbc);
        
        // Create a panel for the project tree options
        JPanel projectTreePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        projectTreePanel.add(includeProjectTreeCheckbox);
        projectTreePanel.add(new JLabel("Tree depth:"));
        projectTreePanel.add(projectTreeDepthSpinner);
        
        gbc.gridy++;
        panel.add(projectTreePanel, gbc);
        
        // Add the use in prompt checkbox with explanation
        gbc.gridy++;
        panel.add(useDevoxxGenieMdInPromptCheckbox, gbc);
        
        gbc.gridy++;
        JEditorPane explanationPane = new JEditorPane(
                "text/html",
                "<html><body style='margin: 5px'>When enabled, the content of DEVOXXGENIE.md will be included in the prompt sent to the AI, "
                + "providing it with context about your project structure and important files.</body></html>"
        );
        explanationPane.setEditable(false);
        explanationPane.setBackground(null);
        explanationPane.setBorder(null);
        panel.add(explanationPane, gbc);
        
        // Add a button to manually create DEVOXXGENIE.md
        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(createDevoxxGenieMdButton, gbc);
        
        // Reset to default settings
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Add event listener to enable/disable tree options based on checkbox state
        createDevoxxGenieMdCheckbox.addChangeListener(e -> {
            boolean enabled = createDevoxxGenieMdCheckbox.isSelected();
            includeProjectTreeCheckbox.setEnabled(enabled);
            projectTreeDepthSpinner.setEnabled(enabled && includeProjectTreeCheckbox.isSelected());
            useDevoxxGenieMdInPromptCheckbox.setEnabled(enabled);
            createDevoxxGenieMdButton.setEnabled(enabled);
        });
        
        includeProjectTreeCheckbox.addChangeListener(e -> {
            projectTreeDepthSpinner.setEnabled(createDevoxxGenieMdCheckbox.isSelected() && 
                                              includeProjectTreeCheckbox.isSelected());
        });
        
        // Initialize component states
        includeProjectTreeCheckbox.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
        projectTreeDepthSpinner.setEnabled(createDevoxxGenieMdCheckbox.isSelected() && 
                                        includeProjectTreeCheckbox.isSelected());
        useDevoxxGenieMdInPromptCheckbox.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
        createDevoxxGenieMdButton.setEnabled(createDevoxxGenieMdCheckbox.isSelected());

        // Add keyboard shortcuts section
        addSection(panel, gbc, "Configure keyboard submit shortcut");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;

        if (SystemInfo.isWindows) {
            panel.add(createShortcutPanel("Windows", stateService.getSubmitShortcutWindows(), true), gbc);
        } else if (SystemInfo.isMac) {
            panel.add(createShortcutPanel("Mac", stateService.getSubmitShortcutMac(), true), gbc);
        } else {
            panel.add(createShortcutPanel("Linux", stateService.getSubmitShortcutLinux(), true), gbc);
        }
        
        // Add keyboard shortcuts section for newline
        addSection(panel, gbc, "Configure keyboard newline shortcut");

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        if (SystemInfo.isWindows) {
            panel.add(createNewlineShortcutPanel("Windows", stateService.getNewlineShortcutWindows()), gbc);
        } else if (SystemInfo.isMac) {
            panel.add(createNewlineShortcutPanel("Mac", stateService.getNewlineShortcutMac()), gbc);
        } else {
            panel.add(createNewlineShortcutPanel("Linux", stateService.getNewlineShortcutLinux()), gbc);
        }

        JEditorPane addFilesInfoPane = new JEditorPane(
                "text/html",
                "<html><body style='margin: 5px'>You can also trigger the add files popup dialog using @ in the input field.</body></html>"
        );
        addFilesInfoPane.setEditable(false);
        addFilesInfoPane.setBackground(null);
        addFilesInfoPane.setBorder(null);

        gbc.gridy++;
        panel.add(addFilesInfoPane, gbc);

        return panel;
    }

    private @NotNull JPanel createShortcutPanel(String os, String initialShortcut, boolean isSubmitShortcut) {
        KeyboardShortcutPanel shortcutPanel = new KeyboardShortcutPanel(project, os, initialShortcut, shortcut -> {
            // Update the appropriate shortcut based on OS
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

        // Set initial values from state service
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
    
    private @NotNull JPanel createShortcutPanel(String os, String initialShortcut) {
        return createShortcutPanel(os, initialShortcut, true);
    }
    
    private @NotNull JPanel createNewlineShortcutPanel(String os, String initialShortcut) {
        return createShortcutPanel(os, initialShortcut, false);
    }
    
    private void notifyNewlineShortcutChanged(String shortcut) {
        project.getMessageBus()
                .syncPublisher(AppTopics.NEWLINE_SHORTCUT_CHANGED_TOPIC)
                .onNewlineShortcutChanged(shortcut);
    }

    private void setupCustomPromptsTable() {
        customPromptsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customPromptsTable.setStriped(true);
        customPromptsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        customPromptsTable.getColumnModel().getColumn(1).setPreferredWidth(550);

        // Add double-click listener
        customPromptsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editCustomPrompt();
                }
            }
        });

        customPromptsTable.getColumnModel().getColumn(PROMPT_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea textArea = new JTextArea((String) value);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                if (isSelected) {
                    textArea.setBackground(table.getSelectionBackground());
                    textArea.setForeground(table.getSelectionForeground());
                } else {
                    textArea.setBackground(table.getBackground());
                    textArea.setForeground(table.getForeground());
                }
                return textArea;
            }
        });
    }

    private void addCustomPrompt() {
        CustomPromptDialog dialog = new CustomPromptDialog(project);
        if (dialog.showAndGet()) {
            String newName = dialog.getCommandName();
            String newPrompt = dialog.getPrompt();

            customPromptsTableModel.addRow(new Object[]{newName, newPrompt});
            int newRowIndex = customPromptsTableModel.getRowCount() - 1;
            customPromptsTable.setRowSelectionInterval(newRowIndex, newRowIndex);
            customPromptsTable.scrollRectToVisible(customPromptsTable.getCellRect(newRowIndex, 0, true));

            project
                .getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
        }
    }

    private void editCustomPrompt() {
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow != -1) {
            String commandName = (String) customPromptsTableModel.getValueAt(selectedRow, 0);
            String prompt = (String) customPromptsTableModel.getValueAt(selectedRow, PROMPT_COLUMN);

            CustomPromptDialog dialog = new CustomPromptDialog(project, commandName, prompt);
            if (dialog.showAndGet()) {
                String newName = dialog.getCommandName();
                String newPrompt = dialog.getPrompt();

                customPromptsTableModel.setValueAt(newName, selectedRow, 0);
                customPromptsTableModel.setValueAt(newPrompt, selectedRow, PROMPT_COLUMN);

                project
                        .getMessageBus()
                        .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                        .onCustomPromptsChanged();
            }
        }
    }


    private void removeCustomPrompt() {
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow != -1) {
            customPromptsTableModel.removeRow(selectedRow);
            project
                .getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
        }
    }

    private void restoreDefaultPrompts() {
        setCustomPrompts(settings.getDefaultPrompts());
    }

    public List<CustomPrompt> getCustomPrompts() {
        int NAME_COLUMN = 0;
        List<CustomPrompt> prompts = new ArrayList<>();
        for (int i = 0; i < customPromptsTableModel.getRowCount(); i++) {
            String name = (String) customPromptsTableModel.getValueAt(i, NAME_COLUMN);
            String prompt = (String) customPromptsTableModel.getValueAt(i, PROMPT_COLUMN);
            prompts.add(new CustomPrompt(name.toLowerCase(), prompt));
        }
        return prompts;
    }

    public void setCustomPrompts(@NotNull List<CustomPrompt> customPrompts) {
        customPromptsTableModel.setRowCount(0);
        for (CustomPrompt prompt : customPrompts) {
            customPromptsTableModel.addRow(new Object[]{prompt.getName(), prompt.getPrompt()});
        }
    }

    private void addPromptArea(@NotNull JPanel panel,
                               @NotNull GridBagConstraints gbc,
                               @NotNull JTextArea textArea) {
        gbc.gridy++;
        panel.add(new JLabel("System prompt"), gbc);

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
    
    /**
     * Creates the DEVOXXGENIE.md file in the project root directory
     * The method executes in a background task to avoid blocking the EDT
     */
    private void createDevoxxGenieMdFile() {
        // Disable the button to prevent multiple clicks
        createDevoxxGenieMdButton.setEnabled(false);
        
        // Use ProgressManager to run in a background task
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating DEVOXXGENIE.md", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Generating DEVOXXGENIE.md file...");
                    
                    // TODO: Implement actual file generation logic here
                    // This would include analyzing the project structure and generating the content
                    boolean includeTree = includeProjectTreeCheckbox.isSelected();
                    int treeDepth = (Integer) projectTreeDepthSpinner.getValue();
                    
                    // Get the project base path
                    String projectPath = project.getBasePath();

                    if (projectPath != null) {

                    }
                    DevoxxGenieGenerator devoxxGenieGenerator =
                            new DevoxxGenieGenerator(project, includeTree, treeDepth, indicator);
                    devoxxGenieGenerator.generate();

                    if (includeTree) {
                    }
                } finally {
                    // Re-enable the button on the EDT when done
                    ApplicationManager.getApplication().invokeLater(() -> {
                        createDevoxxGenieMdButton.setEnabled(createDevoxxGenieMdCheckbox.isSelected());
                    });
                }
            }
        });
    }
}
