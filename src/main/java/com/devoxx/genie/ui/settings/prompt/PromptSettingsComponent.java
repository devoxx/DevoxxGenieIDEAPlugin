package com.devoxx.genie.ui.settings.prompt;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.ui.dialog.CustomPromptDialog;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
<<<<<<< HEAD
=======
import org.jdesktop.swingx.JXTitledSeparator;
>>>>>>> master
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PromptSettingsComponent extends AbstractSettingsComponent {

<<<<<<< HEAD
    private static final int NAME_COLUMN = 0;
    private static final int PROMPT_COLUMN = 1;
=======
    private final DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();

    private final int PROMPT_COLUMN = 1;
>>>>>>> master

    @Getter
    private final JTextArea systemPromptField = new JTextArea(stateService.getSystemPrompt());
    @Getter
    private final JTextArea testPromptField = new JTextArea(stateService.getTestPrompt());
    @Getter
    private final JTextArea explainPromptField = new JTextArea(stateService.getExplainPrompt());
    @Getter
    private final JTextArea reviewPromptField = new JTextArea(stateService.getReviewPrompt());

    private final DefaultTableModel customPromptsTableModel = new DefaultTableModel(new String[]{"Command", "Prompt"}, 0);

    private final JBTable customPromptsTable = new JBTable(customPromptsTableModel) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    };

    private final Project project;

    public PromptSettingsComponent(Project project) {
        this.project = project;

        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        setupCustomPromptsTable();
        setCustomPrompts(settings.getCustomPrompts());

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
<<<<<<< HEAD
        addPromptArea(panel, gbc, systemPromptField);
=======
        addPromptArea(panel, gbc, "System prompt", systemPromptField);
>>>>>>> master
        addSection(panel, gbc, "Custom Prompts");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JEditorPane(
                "text/html",
                "<html><body>Create custom command prompts which can be called using the / prefix in the prompt input field.</body></html>"),
            gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addCustomPromptBtn = new JButton("Add Custom Prompt");
        addCustomPromptBtn.addActionListener(e -> addCustomPrompt());
        buttonPanel.add(addCustomPromptBtn);

        JButton removeCustomPromptBtn = new JButton("Remove Custom Prompt");
        removeCustomPromptBtn.addActionListener(e -> removeCustomPrompt());
        buttonPanel.add(removeCustomPromptBtn);

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

        return panel;
    }

    private void setupCustomPromptsTable() {
        customPromptsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customPromptsTable.setStriped(true);
        customPromptsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        customPromptsTable.getColumnModel().getColumn(1).setPreferredWidth(550);

        customPromptsTable.getColumnModel().getColumn(PROMPT_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JTextArea textArea = new JTextArea((String) value);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setOpaque(true);
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

            ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
        }
    }

    private void removeCustomPrompt() {
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow != -1) {
            customPromptsTableModel.removeRow(selectedRow);
            ApplicationManager
                .getApplication()
                .getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
        }
    }

    public List<CustomPrompt> getCustomPrompts() {
<<<<<<< HEAD
=======
        int NAME_COLUMN = 0;
>>>>>>> master
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

<<<<<<< HEAD
    private void addPromptArea(@NotNull JPanel panel,
                               @NotNull GridBagConstraints gbc,
                               @NotNull JTextArea textArea) {
        gbc.gridy++;
        panel.add(new JLabel("System prompt"), gbc);
=======
    private void addSection(@NotNull JPanel panel,
                            @NotNull GridBagConstraints gbc,
                            String title) {
        gbc.gridy++;
        panel.add(new JXTitledSeparator(title), gbc);
        gbc.gridy++;
    }

    private void addPromptArea(@NotNull JPanel panel,
                               @NotNull GridBagConstraints gbc,
                               String label,
                               @NotNull JTextArea textArea) {
        gbc.gridy++;
        panel.add(new JLabel(label), gbc);
>>>>>>> master

        gbc.gridy++;
        textArea.setRows(5);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(-1, 100));
        panel.add(scrollPane, gbc);
    }
}
