package com.devoxx.genie.ui.settings.skill;

import com.devoxx.genie.model.CustomPrompt;
import com.devoxx.genie.ui.dialog.CustomPromptDialog;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
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
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.PlusIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.RefreshIcon;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.TrashIcon;

public class SkillSettingsComponent extends AbstractSettingsComponent {

    private static final int COMMAND_COLUMN = 0;
    private static final int SKILL_COLUMN = 1;

    private final DevoxxGenieStateService settings;
    private final Project project;

    private final DefaultTableModel customPromptsTableModel = new DefaultTableModel(new String[]{"Command", "Skill"}, 0);

    private final JBTable customPromptsTable = new JBTable(customPromptsTableModel) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public SkillSettingsComponent(Project project) {
        this.project = project;
        this.settings = DevoxxGenieStateService.getInstance();

        setupCustomPromptsTable();
        setCustomPrompts(settings.getCustomPrompts());
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

        addSection(panel, gbc, "Skills");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JEditorPane(
                        "text/html",
                        "<html><body>"
                                + "Create custom user skills callable with the <code>/</code> prefix.<br/>"
                                + "Use <code>$ARGUMENT</code> in the skill text to inject everything typed after the command.<br/>"
                                + "Example: <code>/ralph-runners create a PRD.json for my project</code><br/>"
                                + "Skill text: <code>You are a product manager. Task: $ARGUMENT</code>"
                                + "</body></html>"),
                gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(createActionButton("Add", PlusIcon, "Add custom skill", e -> addCustomPrompt()));
        buttonPanel.add(createActionButton("Remove", TrashIcon, "Remove custom skill", e -> removeCustomPrompt()));
        buttonPanel.add(createActionButton("Restore", RefreshIcon, "Restore custom skills", e -> restoreDefaultPrompts()));

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        JBScrollPane tableScrollPane = new JBScrollPane(customPromptsTable);
        tableScrollPane.setPreferredSize(new Dimension(-1, 300));
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(tableScrollPane, gbc);

        return panel;
    }

    public List<CustomPrompt> getCustomPrompts() {
        List<CustomPrompt> prompts = new ArrayList<>();
        for (int i = 0; i < customPromptsTableModel.getRowCount(); i++) {
            String name = (String) customPromptsTableModel.getValueAt(i, COMMAND_COLUMN);
            String prompt = (String) customPromptsTableModel.getValueAt(i, SKILL_COLUMN);
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

    private void setupCustomPromptsTable() {
        customPromptsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customPromptsTable.setStriped(true);
        customPromptsTable.getColumnModel().getColumn(COMMAND_COLUMN).setPreferredWidth(120);
        customPromptsTable.getColumnModel().getColumn(SKILL_COLUMN).setPreferredWidth(480);

        customPromptsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editCustomPrompt();
                }
            }
        });

        customPromptsTable.getColumnModel().getColumn(SKILL_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {
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
            customPromptsTableModel.addRow(new Object[]{dialog.getCommandName(), dialog.getPrompt()});
            int newRowIndex = customPromptsTableModel.getRowCount() - 1;
            customPromptsTable.setRowSelectionInterval(newRowIndex, newRowIndex);
            customPromptsTable.scrollRectToVisible(customPromptsTable.getCellRect(newRowIndex, 0, true));
            publishCustomPromptChange();
        }
    }

    private void editCustomPrompt() {
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        String commandName = (String) customPromptsTableModel.getValueAt(selectedRow, COMMAND_COLUMN);
        String prompt = (String) customPromptsTableModel.getValueAt(selectedRow, SKILL_COLUMN);

        CustomPromptDialog dialog = new CustomPromptDialog(project, commandName, prompt);
        if (dialog.showAndGet()) {
            customPromptsTableModel.setValueAt(dialog.getCommandName(), selectedRow, COMMAND_COLUMN);
            customPromptsTableModel.setValueAt(dialog.getPrompt(), selectedRow, SKILL_COLUMN);
            publishCustomPromptChange();
        }
    }

    private void removeCustomPrompt() {
        int selectedRow = customPromptsTable.getSelectedRow();
        if (selectedRow != -1) {
            customPromptsTableModel.removeRow(selectedRow);
            publishCustomPromptChange();
        }
    }

    private void restoreDefaultPrompts() {
        setCustomPrompts(settings.getDefaultPrompts());
        publishCustomPromptChange();
    }

    private void publishCustomPromptChange() {
        project.getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
    }
}
