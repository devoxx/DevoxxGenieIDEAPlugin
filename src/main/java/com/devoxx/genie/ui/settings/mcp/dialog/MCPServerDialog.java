package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.HttpSseTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.StdioTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.TransportPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Dialog for adding/editing an MCP server configuration
 */
@Slf4j
public class MCPServerDialog extends DialogWrapper {
    private final JTextField nameField = new JTextField();
    private final JComboBox<MCPServer.TransportType> transportTypeCombo = new JComboBox<>(MCPServer.TransportType.values());
    private final JButton testConnectionButton = new JButton("Test Connection & Fetch Tools");
    
    private final Map<MCPServer.TransportType, TransportPanel> transportPanels = new HashMap<>();
    private final JPanel cardPanel = new JPanel(new CardLayout());
    
    private final MCPServer existingServer;
    private MCPServer server;
    
    // Environment variables components
    private final EnvVarTableModel envVarTableModel = new EnvVarTableModel();
    private final JTable envVarTable = new JBTable(envVarTableModel);
    
    /**
     * Creates a new dialog for adding or editing an MCP server
     *
     * @param existingServer The server to edit, or null to add a new server
     */
    public MCPServerDialog(MCPServer existingServer) {
        super(true);
        this.existingServer = existingServer;
        setTitle(existingServer == null ? "Add MCP Server" : "Edit MCP Server");
        
        // Initialize transport panels
        transportPanels.put(MCPServer.TransportType.STDIO, new StdioTransportPanel());
        transportPanels.put(MCPServer.TransportType.HTTP_SSE, new HttpSseTransportPanel());
        
        // Initialize UI components
        initUI();
        
        // Load existing server settings if editing
        if (existingServer != null) {
            loadExistingServerSettings();
        }
        
        init();
    }
    
    /**
     * Initialize the UI components
     */
    private void initUI() {
        // Add transport panels to card layout
        for (Map.Entry<MCPServer.TransportType, TransportPanel> entry : transportPanels.entrySet()) {
            cardPanel.add(entry.getValue().getPanel(), entry.getKey().toString());
        }
        
        // Set up transport type combo box
        transportTypeCombo.addActionListener(e -> 
                ((CardLayout) cardPanel.getLayout()).show(cardPanel, Objects.requireNonNull(transportTypeCombo.getSelectedItem()).toString()));
        
        // Initially show the STDIO panel when dialog is first opened
        transportTypeCombo.setSelectedItem(MCPServer.TransportType.STDIO);
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, MCPServer.TransportType.STDIO.toString());
        
        // Set up test connection button
        testConnectionButton.addActionListener(e -> testConnection());
    }
    
    /**
     * Load settings from an existing server
     */
    private void loadExistingServerSettings() {
        nameField.setText(existingServer.getName());
        nameField.setEditable(false); // Disable name field when editing
        
        transportTypeCombo.setSelectedItem(existingServer.getTransportType());
        
        // Explicitly show the correct panel in the CardLayout
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, existingServer.getTransportType().toString());
        
        // Load settings into the appropriate transport panel
        TransportPanel panel = transportPanels.get(existingServer.getTransportType());
        if (panel != null) {
            panel.loadSettings(existingServer);
        }
        
        // Load environment variables if any exist
        if (existingServer.getEnv() != null && !existingServer.getEnv().isEmpty()) {
            envVarTableModel.setEnvVars(existingServer.getEnv());
        }
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        mainPanel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(nameField, gbc);
        
        // Add transport type selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Transport Type:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(transportTypeCombo, gbc);
        
        // Add transport panel cards
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(cardPanel, gbc);
        
        // Add environment variables section
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(new JLabel("Environment Variables:"), gbc);
        
        // Create toolbar decorator for the environment variables table
        ToolbarDecorator envVarDecorator = ToolbarDecorator.createDecorator(envVarTable)
                .setAddAction(button -> addEnvVar())
                .setEditAction(button -> editEnvVar())
                .setRemoveAction(button -> removeEnvVar());
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weighty = 0.6;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(envVarDecorator.createPanel(), gbc);
        
        // Add test connection button
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(testConnectionButton, gbc);
        
        return mainPanel;
    }
    
    /**
     * Add a new environment variable
     */
    private void addEnvVar() {
        EnvVarDialog dialog = new EnvVarDialog(null, null);
        if (dialog.showAndGet()) {
            envVarTableModel.addEnvVar(dialog.getKey(), dialog.getValue());
        }
    }
    
    /**
     * Edit an existing environment variable
     */
    private void editEnvVar() {
        int selectedRow = envVarTable.getSelectedRow();
        if (selectedRow >= 0) {
            String key = (String) envVarTableModel.getValueAt(selectedRow, 0);
            String value = (String) envVarTableModel.getValueAt(selectedRow, 1);
            
            EnvVarDialog dialog = new EnvVarDialog(key, value);
            if (dialog.showAndGet()) {
                envVarTableModel.updateEnvVar(selectedRow, dialog.getKey(), dialog.getValue());
            }
        }
    }
    
    /**
     * Remove an environment variable
     */
    private void removeEnvVar() {
        int selectedRow = envVarTable.getSelectedRow();
        if (selectedRow >= 0) {
            int result = Messages.showYesNoDialog(
                    "Are you sure you want to remove this environment variable?",
                    "Confirm Removal",
                    Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                envVarTableModel.removeEnvVar(selectedRow);
            }
        }
    }
    
    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> validationInfos = new ArrayList<>();

        // Validate name field
        if (nameField.getText().trim().isEmpty()) {
            validationInfos.add(new ValidationInfo("Name cannot be empty", nameField));
        }
        
        // Validate transport-specific fields
        MCPServer.TransportType selectedTransport = (MCPServer.TransportType) transportTypeCombo.getSelectedItem();
        TransportPanel panel = transportPanels.get(selectedTransport);
        if (panel != null && !panel.isValid()) {
            validationInfos.add(new ValidationInfo(panel.getErrorMessage(), panel.getPanel()));
        }
        
        return validationInfos;
    }
    
    /**
     * Test the connection to the MCP server and fetch tools
     */
    private void testConnection() {
        // Get the current transport panel
        MCPServer.TransportType selectedTransport = (MCPServer.TransportType) transportTypeCombo.getSelectedItem();
        TransportPanel panel = transportPanels.get(selectedTransport);
        
        if (panel == null) {
            return;
        }
        
        // Validate required fields
        if (!panel.isValid()) {
            ErrorDialogUtil.showErrorDialog(
                    getContentPanel(),
                    "Validation Error",
                    "Please correct the following error before testing the connection:\n" + panel.getErrorMessage(),
                    null
            );
            return;
        }
        
        // Disable button and show connecting status
        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Connecting...");
        
        // Try to connect and fetch tools
        try {
            // Create client
            McpClient mcpClient = panel.createClient();
            
            // Get available tools
            List<ToolSpecification> tools = mcpClient.listTools();
            
            // Create server builder
            MCPServer.MCPServerBuilder builder = MCPServer.builder()
                    .name(nameField.getText().trim());
            
            // Apply settings from transport panel
            panel.applySettings(builder);
            
            // Process tool specifications
            panel.processTools(tools, builder);
            
            // Build the server
            if (existingServer != null) {
                // Preserve environment variables from existing server
                builder.env(new HashMap<>(existingServer.getEnv()));
            }
            
            // Save the server
            server = builder.build();
            
            // Close the client
            mcpClient.close();
            
            // Show success message
            testConnectionButton.setText("Connection Successful! " + tools.size() + " tools found");
            
        } catch (Exception ex) {
            log.error("Failed to connect to MCP server", ex);
            
            // Show error message
            ErrorDialogUtil.showErrorDialog(
                    getContentPanel(),
                    "Connection Failed",
                    "Failed to connect to MCP server",
                    ex
            );
            
            testConnectionButton.setText("Connection Failed - Try Again");
        } finally {
            testConnectionButton.setEnabled(true);
        }
    }
    
    /**
     * Get the configured MCP server
     *
     * @return The configured server
     */
    public MCPServer getMcpServer() {
        // If we've already created a server via the test connection button, return that
        if (server != null) {
            // Update environment variables in the server
            server.setEnv(envVarTableModel.getEnvVars());
            return server;
        }
        
        // Otherwise create a new server from the UI fields
        MCPServer.TransportType selectedTransport = (MCPServer.TransportType) transportTypeCombo.getSelectedItem();
        TransportPanel panel = transportPanels.get(selectedTransport);
        
        // Create builder with name
        MCPServer.MCPServerBuilder builder = MCPServer.builder()
                .name(nameField.getText().trim());
        
        // Apply transport settings
        if (panel != null) {
            panel.applySettings(builder);
        }
        
        // Apply environment variables from the table
        builder.env(envVarTableModel.getEnvVars());
        
        return builder.build();
    }
    
    /**
     * Table model for environment variables
     */
    private static class EnvVarTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Key", "Value"};
        private final List<Map.Entry<String, String>> entries = new ArrayList<>();
        private final Map<String, String> envVars = new HashMap<>();

        public EnvVarTableModel() {
        }
        
        public void setEnvVars(Map<String, String> envVars) {
            this.envVars.clear();
            this.envVars.putAll(envVars);
            refreshEntries();
            fireTableDataChanged();
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
    
    /**
     * Dialog for adding/editing environment variables
     */
    private static class EnvVarDialog extends DialogWrapper {
        private final JTextField keyField = new JTextField();
        private final JTextField valueField = new JTextField();

        public EnvVarDialog(String key, String value) {
            super(true);
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
        protected @NotNull List<ValidationInfo> doValidateAll() {
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
}
