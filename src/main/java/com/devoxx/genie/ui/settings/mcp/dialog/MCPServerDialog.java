package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBScrollPane;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for adding/editing an MCP server
 */
@Slf4j
public class MCPServerDialog extends DialogWrapper {
    private final JTextField nameField = new JTextField();
    private final JTextField commandField = new JTextField();
    private final JTextArea argsArea = new JTextArea(5, 40);
    private final MCPServer existingServer;
    private List<String> availableTools = new ArrayList<>();
    private String toolsDescription = "";
    private Map<String, String> toolDescriptions = new HashMap<>();
    private JButton testConnectionButton;
    private MCPServer server;

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
        // Create a button to test the connection and fetch tools
        testConnectionButton = new JButton("Test Connection & Fetch Tools");
        testConnectionButton.addActionListener(e -> testConnectionAndFetchTools());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Reset gridwidth for remaining components
        gbc.gridwidth = 1;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        // Command field
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Command:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(commandField, gbc);

        // Arguments area
        gbc.gridx = 0;
        gbc.gridy = 3;
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
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("<html>Enter each argument on a new line.</html>"), gbc);

        // Add test connection button
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(testConnectionButton, gbc);
        
        // Example area
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JEditorPane exampleArea = new JEditorPane();
        exampleArea.setContentType("text/html");
        exampleArea.setEditable(false);
        exampleArea.setText(
                "<html><b>Example MCP Command on Mac:</b><br>" +
                        "To set up the 'server-filesystem' tool, you first need to install the MCP using pip:" +
                        "the command should be:<br>" +
                        "<code>/Users/devoxx/.nvm/versions/node/v22.14.0/bin/npx</code><br><br>" +
                        "And the arguments should be:<br><code>" +
                        "-y<br>" +
                        "@modelcontextprotocol/server-filesystem<br>" +
                        "/Users/devoxx/IdeaProjects/DevoxxGenieIDEAPlugin</code><br><br>" +
                        "First execute this command (with args) in your terminal to see if it works!</html>");
        JBScrollPane exampleScrollPane = new JBScrollPane(exampleArea);
        panel.add(exampleScrollPane, gbc);

        return panel;
    }

    @Override
    protected @NotNull java.util.List<ValidationInfo> doValidateAll() {
        java.util.List<ValidationInfo> validationInfos = new ArrayList<>();

        if (nameField.getText().trim().isEmpty()) {
            validationInfos.add(new ValidationInfo("Name cannot be empty", nameField));
        }

        if (commandField.getText().trim().isEmpty()) {
            validationInfos.add(new ValidationInfo("Command cannot be empty", commandField));
        }

        return validationInfos;
    }

    /**
     * Tests the connection to the MCP server and fetches the available tools
     */
    private void testConnectionAndFetchTools() {
        try {
            testConnectionButton.setEnabled(false);
            testConnectionButton.setText("Connecting...");
            
            // Parse arguments from text area
            List<String> args = Arrays.stream(
                    argsArea.getText().trim().split("\\s+|\\n+"))
                    .filter(arg -> !arg.isEmpty())
                    .collect(Collectors.toList());
            
            // Create the command list
            List<String> commandList = new ArrayList<>();
            commandList.add(commandField.getText().trim());
            commandList.addAll(args);
            
            // Create environment map
            Map<String, String> env = new HashMap<>(System.getenv());
            String firstCommand = commandList.get(0);
            int lastSeparatorIndex = firstCommand.lastIndexOf(File.separator);
            if (lastSeparatorIndex > 0) {
                String directoryPath = firstCommand.substring(0, lastSeparatorIndex);
                env.put("PATH", directoryPath + File.pathSeparator + env.getOrDefault("PATH", ""));
            }
            
            // Build the bash command
            List<String> mcpCommand = new ArrayList<>();
            mcpCommand.add("/bin/bash");
            mcpCommand.add("-c");
            String cmdString = commandList.stream()
                    .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                    .collect(Collectors.joining(" "));
            mcpCommand.add(cmdString);
            
            // Create the transport and client
            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(mcpCommand)
                    .environment(env)
                    .logEvents(true)
                    .build();
            
            McpClient mcpClient = new DefaultMcpClient.Builder()
                    .clientName("DevoxxGenie")
                    .protocolVersion("2024-11-05")
                    .transport(transport)
                    .build();
            
            // Get the list of tools
            List<ToolSpecification> tools = mcpClient.listTools();
            
            // Update our fields with the tool information
            availableTools = tools.stream()
                    .map(ToolSpecification::name)
                    .collect(Collectors.toList());
            
            // Store tool descriptions
            Map<String, String> toolDescs = new HashMap<>();
            for (ToolSpecification tool : tools) {
                toolDescs.put(tool.name(), tool.description());
            }
            toolDescriptions = toolDescs;
            
            // Generate description string
            StringBuilder description = new StringBuilder("Available tools: ");
            if (tools.isEmpty()) {
                description.append("None");
            } else {
                description.append(tools.size()).append(" - ");
                description.append(String.join(", ", availableTools));
            }
            toolsDescription = description.toString();
            
            // Store tool descriptions for later use
            server = MCPServer.builder()
                    .name(nameField.getText().trim())
                    .command(commandField.getText().trim())
                    .args(args)
                    .env(existingServer != null ? new HashMap<>(existingServer.getEnv()) : new HashMap<>())
                    .availableTools(availableTools)
                    .toolDescriptions(toolDescs)
                    .toolsDescription(toolsDescription)
                    .build();
            
            // Close the transport
            transport.close();
            
            // Update the button and show success message
            testConnectionButton.setText("Connection Successful! " + tools.size() + " tools found");
            
        } catch (Exception ex) {
            log.error("Failed to connect to MCP server", ex);
            toolsDescription = "Connection failed: " + ex.getMessage();
            testConnectionButton.setText("Connection Failed - Try Again");
        } finally {
            testConnectionButton.setEnabled(true);
        }
    }
    
    public MCPServer getMcpServer() {
        // If we've already created a server via the test connection button, return that
        if (server != null) {
            return server;
        }

        // Otherwise, create a new server instance
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
                .availableTools(availableTools)
                .toolDescriptions(toolDescriptions)
                .toolsDescription(toolsDescription)
                .build();
    }
}
