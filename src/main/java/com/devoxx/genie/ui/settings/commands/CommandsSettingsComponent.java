package com.devoxx.genie.ui.settings.commands;

import com.devoxx.genie.model.Command;
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

/**
 * Settings panel for managing user-defined chat commands (formerly "Custom Prompts" /
 * "Skills"). Renamed in issue #1040 so the "Skills" tab can be dedicated to langchain4j
 * file-system skills.
 */
public class CommandsSettingsComponent extends AbstractSettingsComponent {

    private static final int COMMAND_COLUMN = 0;
    private static final int PROMPT_COLUMN = 1;

    private final DevoxxGenieStateService settings;
    private final Project project;

    private final DefaultTableModel commandsTableModel = new DefaultTableModel(new String[]{"Command", "Prompt"}, 0);

    private final JBTable commandsTable = new JBTable(commandsTableModel) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public CommandsSettingsComponent(Project project) {
        this.project = project;
        this.settings = DevoxxGenieStateService.getInstance();

        setupCommandsTable();
        setCommands(settings.getCommands());
    }

    @Override
    protected String getHelpUrl() {
        return "https://genie.devoxx.com/docs/features/skills";
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

        addSection(panel, gbc, "Commands");

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JEditorPane(
                        "text/html",
                        "<html><body>"
                                + "Create user commands callable with the <code>/</code> prefix.<br/>"
                                + "Use <code>$ARGUMENT</code> in the prompt text to inject everything typed after the command.<br/>"
                                + "Example: <code>/ralph-runners create a PRD.json for my project</code><br/>"
                                + "Prompt text: <code>You are a product manager. Task: $ARGUMENT</code>"
                                + "</body></html>"),
                gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(createActionButton("Add", PlusIcon, "Add user command", e -> addCommand()));
        buttonPanel.add(createActionButton("Remove", TrashIcon, "Remove user command", e -> removeCommand()));
        buttonPanel.add(createActionButton("Restore", RefreshIcon, "Restore default commands", e -> restoreDefaultCommands()));

        gbc.gridy++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        JBScrollPane tableScrollPane = new JBScrollPane(commandsTable);
        tableScrollPane.setPreferredSize(new Dimension(-1, 300));
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(tableScrollPane, gbc);

        return panel;
    }

    public List<Command> getCommands() {
        List<Command> commands = new ArrayList<>();
        for (int i = 0; i < commandsTableModel.getRowCount(); i++) {
            String name = (String) commandsTableModel.getValueAt(i, COMMAND_COLUMN);
            String prompt = (String) commandsTableModel.getValueAt(i, PROMPT_COLUMN);
            commands.add(new Command(name.toLowerCase(), prompt));
        }
        return commands;
    }

    public void setCommands(@NotNull List<Command> commands) {
        commandsTableModel.setRowCount(0);
        for (Command command : commands) {
            commandsTableModel.addRow(new Object[]{command.getName(), command.getPrompt()});
        }
    }

    private void setupCommandsTable() {
        commandsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        commandsTable.setStriped(true);
        commandsTable.getColumnModel().getColumn(COMMAND_COLUMN).setPreferredWidth(120);
        commandsTable.getColumnModel().getColumn(PROMPT_COLUMN).setPreferredWidth(480);

        commandsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editCommand();
                }
            }
        });

        commandsTable.getColumnModel().getColumn(PROMPT_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {
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

    private void addCommand() {
        CustomPromptDialog dialog = new CustomPromptDialog(project);
        if (dialog.showAndGet()) {
            commandsTableModel.addRow(new Object[]{dialog.getCommandName(), dialog.getPrompt()});
            int newRowIndex = commandsTableModel.getRowCount() - 1;
            commandsTable.setRowSelectionInterval(newRowIndex, newRowIndex);
            commandsTable.scrollRectToVisible(commandsTable.getCellRect(newRowIndex, 0, true));
            publishCommandsChange();
        }
    }

    private void editCommand() {
        int selectedRow = commandsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }

        String commandName = (String) commandsTableModel.getValueAt(selectedRow, COMMAND_COLUMN);
        String prompt = (String) commandsTableModel.getValueAt(selectedRow, PROMPT_COLUMN);

        CustomPromptDialog dialog = new CustomPromptDialog(project, commandName, prompt);
        if (dialog.showAndGet()) {
            commandsTableModel.setValueAt(dialog.getCommandName(), selectedRow, COMMAND_COLUMN);
            commandsTableModel.setValueAt(dialog.getPrompt(), selectedRow, PROMPT_COLUMN);
            publishCommandsChange();
        }
    }

    private void removeCommand() {
        int selectedRow = commandsTable.getSelectedRow();
        if (selectedRow != -1) {
            commandsTableModel.removeRow(selectedRow);
            publishCommandsChange();
        }
    }

    private void restoreDefaultCommands() {
        setCommands(settings.getDefaultPrompts());
        publishCommandsChange();
    }

    private void publishCommandsChange() {
        project.getMessageBus()
                .syncPublisher(AppTopics.CUSTOM_PROMPT_CHANGED_TOPIC)
                .onCustomPromptsChanged();
    }
}
