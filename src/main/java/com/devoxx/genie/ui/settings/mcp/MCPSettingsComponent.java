package com.devoxx.genie.ui.settings.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.mcp.dialog.MCPEnvironmentVariablesDialog;
import com.devoxx.genie.ui.settings.mcp.dialog.MCPServerDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MCPSettingsComponent extends AbstractSettingsComponent {
    private final MCPServerTableModel tableModel;
    private final JTable mcpTable;
    private boolean isModified = false;
    private final JCheckBox enableMcpCheckbox;
    private final JCheckBox enableDebugLogsCheckbox;

    public MCPSettingsComponent() {
        super();
        tableModel = new MCPServerTableModel();
        mcpTable = new JBTable(tableModel);
        
        // Initialize checkboxes
        enableMcpCheckbox = new JCheckBox("Enable MCP Support");
        enableMcpCheckbox.setSelected(DevoxxGenieStateService.getInstance().getMcpEnabled());
        enableMcpCheckbox.addActionListener(e -> isModified = true);
        
        enableDebugLogsCheckbox = new JCheckBox("Enable MCP Logging");
        enableDebugLogsCheckbox.setSelected(DevoxxGenieStateService.getInstance().getMcpDebugLogsEnabled());
        enableDebugLogsCheckbox.addActionListener(e -> isModified = true);
        
        setupTable();
        loadCurrentSettings();
        
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(mcpTable)
                .setAddAction(button -> addMcpServer())
                .setEditAction(button -> editMcpServer())
                .setRemoveAction(button -> removeMcpServer())
                .addExtraAction(new AnActionButton("Edit Environment Settings", AllIcons.General.Settings) {
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
        
        // Create the description panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("<html>Configure MCP (Model Context Protocol) servers.<br>" +
                                 "Each MCP requires a name, command and arguments.<br>" +
                                 "Environment variables are optional.</html>"), BorderLayout.CENTER);
        
        // Create the checkbox panel
        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkboxPanel.add(enableMcpCheckbox);
        checkboxPanel.add(enableDebugLogsCheckbox);
        
        // Create the top panel that combines both
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoPanel, BorderLayout.NORTH);
        topPanel.add(checkboxPanel, BorderLayout.CENTER);

        JButton infoButton = new JButton("What is MCP", AllIcons.Actions.Help);
        infoButton.addActionListener(e -> {
            // Open browser
            try {
                Desktop.getDesktop().browse(new URI("https://modelcontextprotocol.io/introduction"));
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        });

        JButton availableMCPButtons = new JButton("Available MCP servers", AllIcons.Actions.Search);
        availableMCPButtons.addActionListener(e -> {
            // https://github.com/modelcontextprotocol/servers
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/modelcontextprotocol/servers"));
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(infoButton);
        buttonPanel.add(availableMCPButtons);

        // Build the main panel
        panel.setLayout(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(decoratedTablePanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupTable() {
        mcpTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Enabled
        mcpTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Name
        mcpTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Command
        mcpTable.getColumnModel().getColumn(3).setPreferredWidth(400); // Arguments
        mcpTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Env Count
        
        // Center align the environment count column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        mcpTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        
        // Add listener for checkbox changes
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) { // Enabled column
                isModified = true;
            }
        });
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
            
            // Save checkbox settings
            stateService.setMcpEnabled(enableMcpCheckbox.isSelected());
            stateService.setMcpDebugLogsEnabled(enableDebugLogsCheckbox.isSelected());
            
            isModified = false;
        }
    }

    /**
     * Check if the settings have been modified
     */
    public boolean isModified() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        return isModified || 
               enableMcpCheckbox.isSelected() != stateService.getMcpEnabled() ||
               enableDebugLogsCheckbox.isSelected() != stateService.getMcpDebugLogsEnabled();
    }

    /**
     * Reset the settings to the current ones from DevoxxGenieStateService
     */
    public void reset() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        loadCurrentSettings();
        enableMcpCheckbox.setSelected(stateService.getMcpEnabled());
        enableDebugLogsCheckbox.setSelected(stateService.getMcpDebugLogsEnabled());
        isModified = false;
    }

    /**
     * Table model for displaying MCP servers
     */
    private static class MCPServerTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Enabled", "Name", "Command", "Arguments", "Env Variables"};
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
                case 0 -> server.isEnabled();
                case 1 -> server.getName();
                case 2 -> server.getCommand();
                case 3 -> server.getArgs() != null ? String.join(" ", server.getArgs()) : "";
                case 4 -> server.getEnv() != null ? server.getEnv().size() : 0;
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Boolean.class;
                case 1, 2, 3 -> String.class;
                case 4 -> Integer.class;
                default -> Object.class;
            };
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Only make the 'Enabled' column (index 0) editable
            return columnIndex == 0;
        }
        
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && value instanceof Boolean enabled) {
                MCPServer server = mcpServers.get(rowIndex);
                server.setEnabled(enabled);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
