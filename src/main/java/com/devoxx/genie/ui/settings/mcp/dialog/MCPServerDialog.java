package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.HttpSseTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.StdioTransportPanel;
import com.devoxx.genie.ui.settings.mcp.dialog.transport.TransportPanel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
        gbc.weighty = 1.0;
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
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(cardPanel, gbc);
        
        // Add test connection button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(testConnectionButton, gbc);
        
        return mainPanel;
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
        
        // Preserve environment variables from existing server
        if (existingServer != null) {
            builder.env(new HashMap<>(existingServer.getEnv()));
        }
        
        return builder.build();
    }
}
