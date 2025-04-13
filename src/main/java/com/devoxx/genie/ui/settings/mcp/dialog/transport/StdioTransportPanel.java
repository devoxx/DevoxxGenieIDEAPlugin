package com.devoxx.genie.ui.settings.mcp.dialog.transport;

import com.devoxx.genie.model.mcp.MCPServer;

import com.devoxx.genie.service.mcp.MCPExecutionService;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBScrollPane;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for configuring STDIO MCP transport
 */
@Slf4j
public class StdioTransportPanel implements TransportPanel {
    private final JPanel panel;
    private final JTextField commandField = new JTextField();
    private final JTextArea argsArea = new JTextArea(5, 40);
    
    // Define a fixed label width to ensure consistency with name and transport type labels
    private static final int LABEL_WIDTH = 150;
    
    public StdioTransportPanel() {
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridwidth = 1;
        int row = 0;
        
        // Command field with fixed-width label
        JLabel commandLabel = new JLabel("Command:");
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(commandLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(commandField, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("⚠️Use full path to your command"), gbc);

        // Arguments area with fixed-width label
        JLabel argumentsLabel = new JLabel("Arguments:");
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(argumentsLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        argsArea.setLineWrap(false);
        JBScrollPane scrollPane = new JBScrollPane(argsArea);
        panel.add(scrollPane, gbc);

        // Help text
        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("<html>Enter each argument on a new line.</html>"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(getExampleLabel(), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(getExamplePanel(), gbc);
    }

    private JPanel getExamplePanel() {
        JEditorPane textPane = new JEditorPane("text/html",
                """
                    <HTML>
                    <b>Java Example</b>
                    <UL>
                        <LI>Command: Full path to JAVA executable</LI>
                        <LI>Arguments: '-jar /path/to/file.jar'</LI>
                    </UL>
                    <b>NPX Example</b>
                    <UL>
                        <LI>Command: Full path to the NPX command</LI>
                        <LI>Arguments: '-y @modelcontextprotocol/server-filesystem /path/to/project'</LI>
                    </UL>
                </HTML>
                """);

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linkPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        linkPanel.add(textPane);
        return linkPanel;
    }
    
    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void loadSettings(MCPServer server) {
        commandField.setText(server.getCommand());
        if (server.getArgs() != null) {
            argsArea.setText(String.join("\n", server.getArgs()));
        }
    }

    @Override
    public boolean isValid() {
        return !commandField.getText().trim().isEmpty() && isFullCommandPath(commandField.getText().trim());
    }

    @Override
    public String getErrorMessage() {
        if (commandField.getText().trim().isEmpty()) {
            return "Command cannot be empty";
        } else if (!isFullCommandPath(commandField.getText().trim())) {
            return "Command must be a full path";
        }
        return null;
    }

    @Override
    public McpClient createClient() throws Exception {
        // Parse arguments and create command list
        List<String> args = parseArguments();
        List<String> mcpCommand = buildCommand(args);
        
        // Log the command for debugging
        log.debug("Executing command: {}", mcpCommand);
        
        // First, test if the command exists by running it directly
        if (!testIfCommandExists(commandField.getText().trim())) {
            throw new Exception("Command not found: " + commandField.getText().trim());
        }
        
        // Create the transport and connect to the server
        Map<String, String> env = createEnvironmentMap();
        StdioMcpTransport transport = new StdioMcpTransport.Builder()
                .command(mcpCommand)
                .environment(env)
                .logEvents(true) // Enable logging to capture errors in the logs
                .build();
        
        return new DefaultMcpClient.Builder()
                .clientName("DevoxxGenie")
                .protocolVersion("2024-11-05")
                .transport(transport)
                .build();
    }

    @Override
    public void applySettings(MCPServer.MCPServerBuilder builder) {
        List<String> args = parseArguments();
        
        builder.command(commandField.getText().trim())
               .args(args)
               .transportType(MCPServer.TransportType.STDIO);
    }
    
    /**
     * Parses arguments from the text area
     * @return List of parsed arguments
     */
    private List<String> parseArguments() {
        return Arrays.stream(
                argsArea.getText().trim().split("\\s+|\\n+"))
                .filter(arg -> !arg.isEmpty())
                .toList();
    }
    
    /**
     * Builds the command to execute
     * @param args Arguments to include in the command
     * @return The complete command list
     */
    private @NotNull List<String> buildCommand(List<String> args) {
        // Create the command list
        List<String> commandList = new ArrayList<>();
        commandList.add(commandField.getText().trim());
        commandList.addAll(args);
        
        // Build the bash command
        return MCPExecutionService.createMCPCommand(commandList);
    }
    
    /**
     * Creates environment map with PATH updated to include command directory
     * @return Environment variables map
     */
    private @NotNull Map<String, String> createEnvironmentMap() {
        Map<String, String> env = new HashMap<>(System.getenv());
        String command = commandField.getText().trim();
        int lastSeparatorIndex = command.lastIndexOf(File.separator);
        
        if (lastSeparatorIndex > 0) {
            String directoryPath = command.substring(0, lastSeparatorIndex);
            env.put("PATH", directoryPath + File.pathSeparator + env.getOrDefault("PATH", ""));
        }
        
        return env;
    }
    
    /**
     * Tests if a command exists and is executable
     * @param command The command to test
     * @return true if the command exists and is executable, false otherwise
     */
    private boolean testIfCommandExists(String command) {
        try {
            // Create a process to check if the command exists
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (SystemInfo.isWindows) {
                // Windows
                processBuilder.command("cmd.exe", "/c", "where", command);
            } else {
                // Unix/Mac
                processBuilder.command("/bin/sh", "-c", "which " + command);
            }
            
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Error testing if command exists: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if the given command text represents a full path
     * @param commandText The command text to check
     * @return true if the command appears to be a full path, false otherwise
     */
    private boolean isFullCommandPath(String commandText) {
        if (commandText == null || commandText.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCommand = commandText.trim();
        
        // For Windows paths (starts with drive letter followed by colon)
        if (trimmedCommand.matches("^[A-Za-z]:.*")) {
            // Check if it's followed by path separators
            return trimmedCommand.length() > 2 && 
                  (trimmedCommand.charAt(2) == '/' || trimmedCommand.charAt(2) == '\\');
        }
        
        // Check for Unix/Mac full path (starts with forward slash)
        if (trimmedCommand.startsWith("/")) {
            return true;
        }
        
        // Check for Unix/Mac home directory path
        if (trimmedCommand.startsWith("~/")) {
            return true;
        }
        
        // If no path separators found at all, it's not a full path
        if (!trimmedCommand.contains("/") && !trimmedCommand.contains("\\")) {
            return false;
        }
        
        // For other cases, check if it contains more than just the command name
        // For example: path/to/command
        return trimmedCommand.contains("/") || trimmedCommand.contains("\\");
    }
}
