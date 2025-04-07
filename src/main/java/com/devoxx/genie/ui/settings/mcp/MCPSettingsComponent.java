package com.devoxx.genie.ui.settings.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.service.mcp.MCPService;
import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.mcp.dialog.MCPEnvironmentVariablesDialog;
import com.devoxx.genie.ui.settings.mcp.dialog.MCPServerDialog;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        mcpTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Enabled
        mcpTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Name
        mcpTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Transport Type
        mcpTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Connection Info
        mcpTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Env Count
        mcpTable.getColumnModel().getColumn(5).setPreferredWidth(230);  // Tools
        mcpTable.getColumnModel().getColumn(6).setPreferredWidth(70);   // View Button
        
        // Create button renderer and editor for the view button column
        mcpTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        mcpTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));
        
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
            
            // Get the old MCP enabled state for comparison
            boolean oldMcpEnabled = stateService.getMcpEnabled();
            
            // Convert the list to a map by name
            Map<String, MCPServer> serverMap = tableModel.getMcpServers().stream()
                    .collect(Collectors.toMap(MCPServer::getName, server -> server));
            
            stateService.getMcpSettings().getMcpServers().clear();
            stateService.getMcpSettings().getMcpServers().putAll(serverMap);
            
            // Save checkbox settings
            stateService.setMcpEnabled(enableMcpCheckbox.isSelected());
            stateService.setMcpDebugLogsEnabled(enableDebugLogsCheckbox.isSelected());
            
            // Refresh the tool window visibility if MCP enabled state changed
            if (oldMcpEnabled != enableMcpCheckbox.isSelected()) {
                // Reset notification flag if MCP was disabled
                if (!enableMcpCheckbox.isSelected()) {
                    MCPService.resetNotificationFlag();
                }
                
                // Update tool window visibility
                MCPService.refreshToolWindowVisibility();
            }
            
            isModified = false;
            
            // Update the Open MCP Log Panel button if it exists
            for (Component component : panel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel subPanel = (JPanel) component;
                    for (Component c : subPanel.getComponents()) {
                        if (c instanceof JButton && ((JButton) c).getText().equals("Open MCP Log Panel")) {
                            updateButtonBasedOnState((JButton) c);
                            break;
                        }
                    }
                }
            }
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
     * Shows a dialog with detailed tools information for the selected server
     * 
     * @param server The MCP server to show tools for
     */
    private void showToolsInfoDialog(MCPServer server) {
        if (server == null) {
            return;
        }
        
        // Create a dialog instead of using Messages.showMessageDialog
        DialogWrapper dialog = new DialogWrapper(true) {
            {
                init();
                setTitle("MCP Tools Information - " + server.getName());
            }
            
            @Override
            protected @Nullable JComponent createCenterPanel() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                if (server.getAvailableTools() == null || server.getAvailableTools().isEmpty()) {
                    // Show a simple message if no tools are available
                    JLabel noToolsLabel = new JLabel("No tools available for this MCP server.");
                    noToolsLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    panel.add(noToolsLabel, BorderLayout.CENTER);
                } else {
                    // Create a table to display tool names and descriptions
                    String[] columnNames = {"Tool Name", "Description"};
                    Object[][] data = new Object[server.getAvailableTools().size()][2];
                    
                    // Fill the table data
                    for (int i = 0; i < server.getAvailableTools().size(); i++) {
                        String toolName = server.getAvailableTools().get(i);
                        String description = server.getToolDescriptions() != null ? 
                                server.getToolDescriptions().getOrDefault(toolName, "") : "";
                        
                        data[i][0] = toolName;
                        data[i][1] = description;
                    }
                    
                    // Create and configure the table
                    JTable toolsTable = new JTable(data, columnNames);
                    toolsTable.setRowHeight(24);
                    toolsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
                    toolsTable.getColumnModel().getColumn(1).setPreferredWidth(350);
                    
                    // Make description column wrap text
                    toolsTable.getColumnModel().getColumn(1).setCellRenderer(new MultiLineTableCellRenderer());
                    
                    JScrollPane scrollPane = new JBScrollPane(toolsTable);
                    scrollPane.setPreferredSize(new Dimension(600, 400));
                    
                    // Add a header
                    JLabel headerLabel = new JLabel("Available Tools: " + server.getAvailableTools().size());
                    headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
                    
                    panel.add(headerLabel, BorderLayout.NORTH);
                    panel.add(scrollPane, BorderLayout.CENTER);
                }
                
                return panel;
            }
            
            @Override
            protected Action @NotNull [] createActions() {
                // Only show OK button
                return new Action[]{ getOKAction() };
            }
        };
        
        dialog.show();
    }
    
    /**
     * A table cell renderer that supports multi-line text
     */
    private static class MultiLineTableCellRenderer extends JTextArea implements TableCellRenderer {
        public MultiLineTableCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value != null ? value.toString() : "");
            setSize(table.getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
            
            int preferredHeight = getPreferredSize().height;
            if (preferredHeight > table.getRowHeight(row)) {
                table.setRowHeight(row, preferredHeight);
            }
            
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            
            return this;
        }
    }
    
    /**
     * Table cell renderer for buttons
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    /**
     * Table cell editor for buttons
     */
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            currentRow = row;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Show the tools info dialog when button is clicked
                MCPServer server = tableModel.getMcpServerAt(currentRow);
                if (server != null) {
                    showToolsInfoDialog(server);
                }
            }
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
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
        
        // Find and update the Open MCP Log Panel button if it exists
        for (Component component : panel.getComponents()) {
            if (component instanceof JPanel) {
                JPanel subPanel = (JPanel) component;
                for (Component c : subPanel.getComponents()) {
                    if (c instanceof JButton && ((JButton) c).getText().equals("Open MCP Log Panel")) {
                        updateButtonBasedOnState((JButton) c);
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Updates the Open MCP log panel button's state based on current settings
     * 
     * @param button The button to update
     */
    private void updateLogButtonState(JButton button) {
        boolean logsEnabled = MCPService.isDebugLogsEnabled();
        
        // Set tooltip based on current state
        if (logsEnabled) {
            button.setToolTipText("Open the MCP log panel to view MCP communication logs");
        } else {
            button.setToolTipText("Enable MCP and MCP logging to view logs");
        }
        
        // Listen for checkbox changes to update button state
        enableMcpCheckbox.addActionListener(e -> {
            updateButtonBasedOnState(button);
        });
        
        enableDebugLogsCheckbox.addActionListener(e -> {
            updateButtonBasedOnState(button);
        });
        
        // Initial state
        updateButtonBasedOnState(button);
    }
    
    /**
     * Helper method to update button based on current checkbox states
     * 
     * @param button The button to update
     */
    private void updateButtonBasedOnState(JButton button) {
        boolean logsEnabled = enableMcpCheckbox.isSelected() && enableDebugLogsCheckbox.isSelected();
        
        // Update tooltip and visual state
        if (logsEnabled) {
            button.setToolTipText("Open the MCP log panel to view MCP communication logs");
            button.setEnabled(true);
        } else {
            button.setToolTipText("Enable MCP and MCP logging to view logs");
            button.setEnabled(false);
        }
    }

    /**
     * Table model for displaying MCP servers
     */
    private static class MCPServerTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Enabled", "Name", "Transport Type", "Connection Info", "Env Variables", "Tools", ""};
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
                case 2 -> server.getTransportType();
                case 3 -> getConnectionInfo(server);
                case 4 -> server.getEnv() != null ? server.getEnv().size() : 0;
                case 5 -> getToolsSummary(server);
                case 6 -> "View";
                default -> null;
            };
        }

        private String getConnectionInfo(MCPServer server) {
            if (server.getTransportType() == MCPServer.TransportType.HTTP_SSE) {
                return server.getSseUrl();
            } else {
                // STDIO transport
                return server.getCommand();
            }
        }
        
        private String getToolsSummary(MCPServer server) {
            if (server.getAvailableTools() == null || server.getAvailableTools().isEmpty()) {
                return "No tools info";
            }
            return "Available tools: " + server.getAvailableTools().size();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> Boolean.class;
                case 1, 3, 5, 6 -> String.class;
                case 2 -> MCPServer.TransportType.class;
                case 4 -> Integer.class;
                default -> Object.class;
            };
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Make the 'Enabled' column (index 0) and the 'View' button column (index 6) editable
            return columnIndex == 0 || columnIndex == 6;
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
