package com.devoxx.genie.ui.settings.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MCPSettingsComponent extends AbstractSettingsComponent {
    private final MCPServerTableModel tableModel;
    private final JTable mcpTable;
    private boolean isModified = false;

    public MCPSettingsComponent() {
        super();
        tableModel = new MCPServerTableModel();
        mcpTable = new JBTable(tableModel);
        
        setupTable();
        loadCurrentSettings();
        
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(mcpTable)
                .setAddAction(button -> addMcpServer())
                .setEditAction(button -> editMcpServer())
                .setRemoveAction(button -> removeMcpServer())
                .addExtraAction(new AnActionButton("Edit Environment Variables", AllIcons.Nodes.Variable) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        editEnvironmentVariables();
                    }

                    @Override
                    public boolean isEnabled() {
                        return mcpTable.getSelectedRow() >= 0;
                    }
                });
                
        JPanel decoratedTablePanel = toolbarDecorator.createPanel();
        
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("<html>Configure MCP (Multi-Agent Communications Protocol) servers.<br>" +
                                 "Each MCP requires a name, command, and arguments.<br>" +
                                 "Environment variables are optional.</html>"), BorderLayout.CENTER);
        
        panel.setLayout(new BorderLayout());
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(decoratedTablePanel, BorderLayout.CENTER);
    }

    private void setupTable() {
        mcpTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        mcpTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Command
        mcpTable.getColumnModel().getColumn(2).setPreferredWidth(400); // Arguments
        mcpTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Env Count
        
        // Center align the environment count column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        mcpTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
    }

    private void loadCurrentSettings() {
        Map<String, MCPServer> servers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        tableModel.setMcpServers(new ArrayList<>(servers.values()));
    }

    private void addMcpServer() {
        MCPServerDialog dialog = new MCPServerDialog(null);
        if (dialog.showAndGet()) {
            tableModel.addMcpServer(dialog.getMcpServer());
            isModified = true;
        }
    }

    private void editMcpServer() {
        int selectedRow = mcpTable.getSelectedRow();
        if (selectedRow >= 0) {
            MCPServer selectedServer = tableModel.getMcpServerAt(selectedRow);
            MCPServerDialog dialog = new MCPServerDialog(selectedServer);
            if (dialog.showAndGet()) {
                tableModel.updateMcpServer(selectedRow, dialog.getMcpServer());
                isModified = true;
            }
        }
    }

    private void removeMcpServer() {
        int selectedRow = mcpTable.getSelectedRow();
        if (selectedRow >= 0) {
            int result = Messages.showYesNoDialog(
                    "Are you sure you want to remove this MCP server configuration?",
                    "Confirm Removal",
                    Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                tableModel.removeMcpServer(selectedRow);
                isModified = true;
            }
        }
    }

    private void editEnvironmentVariables() {
        int selectedRow = mcpTable.getSelectedRow();
        if (selectedRow >= 0) {
            MCPServer selectedServer = tableModel.getMcpServerAt(selectedRow);
            MCPEnvironmentVariablesDialog dialog = new MCPEnvironmentVariablesDialog(selectedServer);
            
            if (dialog.showAndGet()) {
                selectedServer.setEnv(dialog.getEnvironmentVariables());
                tableModel.fireTableRowsUpdated(selectedRow, selectedRow);
                isModified = true;
            }
        }
    }

    /**
     * Apply the settings from the UI to the DevoxxGenieStateService
     */
    public void apply() {
        if (isModified) {
            DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
            
            // Convert the list to a map by name
            Map<String, MCPServer> serverMap = tableModel.getMcpServers().stream()
                    .collect(Collectors.toMap(MCPServer::getName, server -> server));
            
            stateService.getMcpSettings().getMcpServers().clear();
            stateService.getMcpSettings().getMcpServers().putAll(serverMap);
            
            isModified = false;
        }
    }

    /**
     * Check if the settings have been modified
     */
    public boolean isModified() {
        return isModified;
    }

    /**
     * Reset the settings to the current ones from DevoxxGenieStateService
     */
    public void reset() {
        loadCurrentSettings();
        isModified = false;
    }

    /**
     * Table model for displaying MCP servers
     */
    private static class MCPServerTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Name", "Command", "Arguments", "Env Variables"};
        @Getter
        private List<MCPServer> mcpServers = new ArrayList<>();

        public void setMcpServers(List<MCPServer> mcpServers) {
            this.mcpServers = mcpServers;
            fireTableDataChanged();
        }

        public @Nullable MCPServer getMcpServerAt(int row) {
            if (row >= 0 && row < mcpServers.size()) {
                return mcpServers.get(row);
            }
            return null;
        }

        public void addMcpServer(MCPServer server) {
            mcpServers.add(server);
            fireTableRowsInserted(mcpServers.size() - 1, mcpServers.size() - 1);
        }

        public void updateMcpServer(int row, MCPServer server) {
            if (row >= 0 && row < mcpServers.size()) {
                mcpServers.set(row, server);
                fireTableRowsUpdated(row, row);
            }
        }

        public void removeMcpServer(int row) {
            if (row >= 0 && row < mcpServers.size()) {
                mcpServers.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        @Override
        public int getRowCount() {
            return mcpServers.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MCPServer server = mcpServers.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> server.getName();
                case 1 -> server.getCommand();
                case 2 -> server.getArgs() != null ? String.join(" ", server.getArgs()) : "";
                case 3 -> server.getEnv() != null ? server.getEnv().size() : 0;
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 1, 2 -> String.class;
                case 3 -> Integer.class;
                default -> Object.class;
            };
        }
    }

    /**
     * Dialog for adding/editing an MCP server
     */
    private static class MCPServerDialog extends DialogWrapper {
        private final JTextField nameField = new JTextField();
        private final JTextField commandField = new JTextField();
        private final JTextArea argsArea = new JTextArea(5, 40);
        private final MCPServer existingServer;

        public MCPServerDialog(MCPServer existingServer) {
            super(true);
            this.existingServer = existingServer;
            setTitle(existingServer == null ? "Add MCP Server" : "Edit MCP Server");
            init();
            
            if (existingServer != null) {
                nameField.setText(existingServer.getName());
                commandField.setText(existingServer.getCommand());
                if (existingServer.getArgs() != null) {
                    argsArea.setText(String.join("\n", existingServer.getArgs()));
                }
                
                // Disable name field when editing
                nameField.setEditable(false);
            }
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);
            
            // Name field
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Name:"), gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(nameField, gbc);
            
            // Command field
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0.0;
            panel.add(new JLabel("Command:"), gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            panel.add(commandField, gbc);
            
            // Arguments area
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            panel.add(new JLabel("Arguments:"), gbc);
            
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            argsArea.setLineWrap(false);
            JBScrollPane scrollPane = new JBScrollPane(argsArea);
            panel.add(scrollPane, gbc);
            
            // Help text
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(new JLabel("<html>Enter each argument on a new line or separated by spaces.</html>"), gbc);
            
            return panel;
        }
        
        @Override
        protected @NotNull java.util.List<ValidationInfo> doValidateAll() {
            List<ValidationInfo> validationInfos = new ArrayList<>();
            
            if (nameField.getText().trim().isEmpty()) {
                validationInfos.add(new ValidationInfo("Name cannot be empty", nameField));
            }
            
            if (commandField.getText().trim().isEmpty()) {
                validationInfos.add(new ValidationInfo("Command cannot be empty", commandField));
            }
            
            return validationInfos;
        }
        
        public MCPServer getMcpServer() {
            // Parse arguments from text area by splitting on whitespace or newlines
            List<String> args = Arrays.stream(
                    argsArea.getText().trim().split("\\s+|\\n+"))
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toList());
            
            Map<String, String> env = existingServer != null ? 
                    new HashMap<>(existingServer.getEnv()) : new HashMap<>();
            
            return MCPServer.builder()
                    .name(nameField.getText().trim())
                    .command(commandField.getText().trim())
                    .args(args)
                    .env(env)
                    .build();
        }
    }
    
    /**
     * Dialog for editing environment variables
     */
    private static class MCPEnvironmentVariablesDialog extends DialogWrapper {
        private final EnvVarTableModel tableModel;
        private final JTable envVarTable;
        private final MCPServer mcpServer;

        public MCPEnvironmentVariablesDialog(@NotNull MCPServer mcpServer) {
            super(true);
            this.mcpServer = mcpServer;
            setTitle("Environment Variables for " + mcpServer.getName());
            
            tableModel = new EnvVarTableModel(mcpServer.getEnv());
            envVarTable = new JBTable(tableModel);
            
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(envVarTable)
                    .setAddAction(button -> addEnvVar())
                    .setEditAction(button -> editEnvVar())
                    .setRemoveAction(button -> removeEnvVar());
            
            panel.add(new JLabel("Environment variables for MCP server: " + mcpServer.getName()), 
                    BorderLayout.NORTH);
            panel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER);
            
            panel.setPreferredSize(new Dimension(500, 300));
            
            return panel;
        }
        
        private void addEnvVar() {
            EnvVarDialog dialog = new EnvVarDialog(null, null);
            if (dialog.showAndGet()) {
                tableModel.addEnvVar(dialog.getKey(), dialog.getValue());
            }
        }
        
        private void editEnvVar() {
            int selectedRow = envVarTable.getSelectedRow();
            if (selectedRow >= 0) {
                String key = (String) tableModel.getValueAt(selectedRow, 0);
                String value = (String) tableModel.getValueAt(selectedRow, 1);
                
                EnvVarDialog dialog = new EnvVarDialog(key, value);
                if (dialog.showAndGet()) {
                    tableModel.updateEnvVar(selectedRow, dialog.getKey(), dialog.getValue());
                }
            }
        }
        
        private void removeEnvVar() {
            int selectedRow = envVarTable.getSelectedRow();
            if (selectedRow >= 0) {
                int result = Messages.showYesNoDialog(
                        "Are you sure you want to remove this environment variable?",
                        "Confirm Removal",
                        Messages.getQuestionIcon()
                );
                
                if (result == Messages.YES) {
                    tableModel.removeEnvVar(selectedRow);
                }
            }
        }
        
        public Map<String, String> getEnvironmentVariables() {
            return tableModel.getEnvVars();
        }
        
        /**
         * Dialog for adding/editing environment variables
         */
        private static class EnvVarDialog extends DialogWrapper {
            private final JTextField keyField = new JTextField();
            private final JTextField valueField = new JTextField();

            public EnvVarDialog(String key, String value) {
                super(true);
                // this.originalKey = key;
                setTitle(key == null ? "Add Environment Variable" : "Edit Environment Variable");
                init();
                
                if (key != null) {
                    keyField.setText(key);
                    valueField.setText(value);
                    
                    // Disable key field when editing
                    keyField.setEditable(false);
                }
            }

            @Override
            protected @Nullable JComponent createCenterPanel() {
                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);
                
                // Key field
                gbc.gridx = 0;
                gbc.gridy = 0;
                panel.add(new JLabel("Key:"), gbc);
                
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                keyField.setPreferredSize(new Dimension(300, keyField.getPreferredSize().height));
                panel.add(keyField, gbc);
                
                // Value field
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.weightx = 0.0;
                panel.add(new JLabel("Value:"), gbc);
                
                gbc.gridx = 1;
                gbc.weightx = 1.0;
                valueField.setPreferredSize(new Dimension(300, valueField.getPreferredSize().height));
                panel.add(valueField, gbc);
                
                return panel;
            }
            
            @Override
            protected @NotNull java.util.List<ValidationInfo> doValidateAll() {
                List<ValidationInfo> validationInfos = new ArrayList<>();
                
                if (keyField.getText().trim().isEmpty()) {
                    validationInfos.add(new ValidationInfo("Key cannot be empty", keyField));
                }
                
                return validationInfos;
            }
            
            public String getKey() {
                return keyField.getText().trim();
            }
            
            public String getValue() {
                return valueField.getText().trim();
            }
        }
        
        /**
         * Table model for environment variables
         */
        private static class EnvVarTableModel extends AbstractTableModel {
            private final String[] COLUMN_NAMES = {"Key", "Value"};
            private final List<Map.Entry<String, String>> entries;
            private final Map<String, String> envVars;

            public EnvVarTableModel(Map<String, String> envVars) {
                this.envVars = new HashMap<>(envVars);
                this.entries = new ArrayList<>(envVars.entrySet());
            }

            public Map<String, String> getEnvVars() {
                return new HashMap<>(envVars);
            }

            public void addEnvVar(String key, String value) {
                envVars.put(key, value);
                refreshEntries();
                fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
            }

            public void updateEnvVar(int row, String key, String value) {
                if (row >= 0 && row < entries.size()) {
                    String oldKey = entries.get(row).getKey();
                    envVars.remove(oldKey);
                    envVars.put(key, value);
                    refreshEntries();
                    fireTableRowsUpdated(row, row);
                }
            }

            public void removeEnvVar(int row) {
                if (row >= 0 && row < entries.size()) {
                    String key = entries.get(row).getKey();
                    envVars.remove(key);
                    refreshEntries();
                    fireTableRowsDeleted(row, row);
                }
            }

            private void refreshEntries() {
                entries.clear();
                entries.addAll(envVars.entrySet());
            }

            @Override
            public int getRowCount() {
                return entries.size();
            }

            @Override
            public int getColumnCount() {
                return COLUMN_NAMES.length;
            }

            @Override
            public String getColumnName(int column) {
                return COLUMN_NAMES[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Map.Entry<String, String> entry = entries.get(rowIndex);
                return switch (columnIndex) {
                    case 0 -> entry.getKey();
                    case 1 -> entry.getValue();
                    default -> null;
                };
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        }
    }
}
