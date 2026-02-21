package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.HttpSseTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.StdioTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.StreamableHttpTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.TransportPanel;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
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
        transportPanels.put(MCPServer.TransportType.HTTP, new StreamableHttpTransportPanel());
        
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
            
            // Always retrieve the real value from the map, not the displayed (potentially masked) value
            Map.Entry<String, String> entry = envVarTableModel.getEntryAt(selectedRow);
            String value = entry.getValue();
            
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
     * Test the connection to the MCP server and fetch tools.
     * Runs in a background thread with a cancellable progress dialog
     * so the IDE stays responsive during potentially long-running connections.
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

        // Disable button while test is in progress
        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Connecting...");

        // Capture field values on EDT before background work
        String serverName = nameField.getText().trim();

        // Use single-element arrays to pass results out of the background runnable
        final MCPServer[] result = {null};
        final Exception[] error = {null};
        final int[] toolCount = {0};
        boolean cancelled = false;

        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                McpClient mcpClient = null;

                try {
                    // Step 1: Create client (pass headers from existing server for auth)
                    indicator.setText("Connecting to MCP server...");
                    indicator.setIndeterminate(true);
                    Map<String, String> headers = existingServer != null && existingServer.getHeaders() != null
                            ? existingServer.getHeaders() : Map.of();
                    mcpClient = panel.createClient(headers);

                    indicator.checkCanceled();

                    // Step 2: Fetch tools
                    indicator.setText("Fetching available tools...");
                    List<ToolSpecification> tools = mcpClient.listTools();

                    indicator.checkCanceled();

                    // Step 3: Build server configuration
                    indicator.setText("Processing tool specifications...");
                    MCPServer.MCPServerBuilder builder = MCPServer.builder()
                            .name(serverName);

                    panel.applySettings(builder);
                    panel.processTools(tools, builder);

                    if (existingServer != null) {
                        builder.env(new HashMap<>(existingServer.getEnv()));
                        if (existingServer.getHeaders() != null && !existingServer.getHeaders().isEmpty()) {
                            builder.headers(new HashMap<>(existingServer.getHeaders()));
                        }
                    }

                    result[0] = builder.build();
                    toolCount[0] = tools.size();

                } catch (ProcessCanceledException e) {
                    throw e; // Let ProgressManager handle cancellation
                } catch (Exception ex) {
                    error[0] = ex;
                } finally {
                    // Always close the client to prevent resource leaks
                    if (mcpClient != null) {
                        try {
                            mcpClient.close();
                        } catch (Exception closeEx) {
                            log.warn("Failed to close MCP client", closeEx);
                        }
                    }
                }
            }, "Testing MCP Connection", true, null);
        } catch (ProcessCanceledException e) {
            cancelled = true;
        }

        // Back on EDT — update UI based on outcome
        if (cancelled) {
            testConnectionButton.setText("Connection Test Cancelled");
        } else if (error[0] != null) {
            log.error("Failed to connect to MCP server", error[0]);
            ErrorDialogUtil.showErrorDialog(
                    getContentPanel(),
                    "Connection Failed",
                    "Failed to connect to MCP server",
                    error[0]
            );
            testConnectionButton.setText("Connection Failed - Try Again");
        } else {
            server = result[0];
            testConnectionButton.setText("Connection Successful! " + toolCount[0] + " tools found");
        }

        testConnectionButton.setEnabled(true);
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

        // Preserve headers from existing server
        if (existingServer != null && existingServer.getHeaders() != null && !existingServer.getHeaders().isEmpty()) {
            builder.headers(new HashMap<>(existingServer.getHeaders()));
        }

        return builder.build();
    }
    
    /**
     * Table model for environment variables
     */
    private static class EnvVarTableModel extends AbstractTableModel {
        private final String[] COLUMN_NAMES = {"Key", "Value"};
        private final List<Map.Entry<String, String>> entries = new ArrayList<>();
        private final Map<String, String> envVars = new HashMap<>();
        private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
                "key", "secret", "token", "password", "pwd", "pass", "credential", "api", "auth"
        );

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
        
        /**
         * Gets the entry at the specified row index
         * @param row The row index
         * @return The Map.Entry at the specified row or null if invalid index
         */
        public @Nullable Map.Entry<String, String> getEntryAt(int row) {
            if (row >= 0 && row < entries.size()) {
                return entries.get(row);
            }
            return null;
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
            
            if (columnIndex == 0) {
                return entry.getKey();
            } else if (columnIndex == 1) {
                // For the value column, check if the key indicates sensitive data
                String key = entry.getKey().toLowerCase();
                if (isSensitiveKey(key)) {
                    // Mask the actual value with asterisks
                    return "••••••••";
                } else {
                    return entry.getValue();
                }
            }
            
            return null;
        }
        
        /**
         * Determines if a key indicates sensitive information
         * @param key The key to check (should be lowercase)
         * @return true if sensitive, false otherwise
         */
        private boolean isSensitiveKey(String key) {
            if (key == null) return false;
            return SENSITIVE_KEYWORDS.stream().anyMatch(key::contains);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
    
    /**
     * Pure sensitivity-check logic extracted for testability.
     * Determines whether an environment variable key refers to sensitive data.
     */
    static class EnvVarSensitivityChecker {
        private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
                "key", "secret", "token", "password", "pwd", "pass", "credential", "api", "auth"
        );

        boolean isSensitive(String key) {
            if (key == null) return false;
            String lowerKey = key.toLowerCase();
            return SENSITIVE_KEYWORDS.stream().anyMatch(lowerKey::contains);
        }
    }

    private static final EnvVarSensitivityChecker SENSITIVITY_CHECKER = new EnvVarSensitivityChecker();

    /**
     * Dialog for adding/editing environment variables
     */
    private static class EnvVarDialog extends DialogWrapper {
        private final JTextField keyField = new JTextField();
        private JComponent valueField;  // Can be JTextField or JPasswordField
        private final JCheckBox showPasswordCheckbox = new JCheckBox("Show Value");

        public EnvVarDialog(String key, String value) {
            super(true);
            setTitle(key == null ? "Add Environment Variable" : "Edit Environment Variable");
            initValueField(SENSITIVITY_CHECKER.isSensitive(key));
            init();
            initKeyAndValue(key, value);
            keyField.getDocument().addDocumentListener(createKeyDocumentListener());
        }

        private void initValueField(boolean isSensitive) {
            if (isSensitive) {
                valueField = new JPasswordField();
                showPasswordCheckbox.setSelected(false);
                showPasswordCheckbox.addActionListener(e -> togglePasswordVisibility());
            } else {
                valueField = new JTextField();
                showPasswordCheckbox.setVisible(false);
            }
        }

        private void initKeyAndValue(String key, String value) {
            if (key == null) return;
            keyField.setText(key);
            setValueFieldText(value);
            keyField.setEditable(false);
        }

        private void setValueFieldText(String text) {
            if (valueField instanceof JPasswordField) {
                ((JPasswordField) valueField).setText(text);
            } else if (valueField instanceof JTextField) {
                ((JTextField) valueField).setText(text);
            }
        }

        private void togglePasswordVisibility() {
            String currentValue = getValue();
            boolean show = showPasswordCheckbox.isSelected();
            replaceValueField(show ? new JTextField(currentValue) : new JPasswordField(currentValue));
        }

        private void updateFieldType() {
            boolean shouldBeSensitive = SENSITIVITY_CHECKER.isSensitive(keyField.getText().trim());
            boolean isCurrentlySensitive = valueField instanceof JPasswordField;
            if (shouldBeSensitive == isCurrentlySensitive) return;

            String currentValue = getValue();
            if (shouldBeSensitive) {
                replaceValueField(new JPasswordField(currentValue));
                showPasswordCheckbox.setVisible(true);
                showPasswordCheckbox.setSelected(false);
            } else {
                replaceValueField(new JTextField(currentValue));
                showPasswordCheckbox.setVisible(false);
            }
        }

        private void replaceValueField(JComponent newField) {
            Container parent = valueField.getParent();
            if (parent == null) return;

            int index = Arrays.asList(parent.getComponents()).indexOf(valueField);
            if (index < 0) return;

            parent.remove(valueField);
            valueField = newField;
            valueField.setPreferredSize(new Dimension(300, valueField.getPreferredSize().height));
            parent.add(valueField, getConstraints(index));
            parent.revalidate();
            parent.repaint();
        }

        private javax.swing.event.DocumentListener createKeyDocumentListener() {
            return new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateFieldType(); }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateFieldType(); }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateFieldType(); }
            };
        }

        /**
         * Helper method to get the GridBagConstraints for a component at a specific index
         */
        private GridBagConstraints getConstraints(int index) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1.0;
            return gbc;
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
            
            // Show password checkbox (only visible for sensitive fields)
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.weightx = 1.0;
            panel.add(showPasswordCheckbox, gbc);

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
            if (valueField instanceof JTextField) {
                return ((JTextField) valueField).getText().trim();
            } else if (valueField instanceof JPasswordField) {
                return new String(((JPasswordField) valueField).getPassword()).trim();
            }
            return "";
        }
    }
}
