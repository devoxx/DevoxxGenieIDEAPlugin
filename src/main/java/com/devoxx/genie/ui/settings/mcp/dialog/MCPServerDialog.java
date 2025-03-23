package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBScrollPane;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
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
            
            // Update test button state based on existing command
            updateTestButtonState();
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // Create a button to test the connection and fetch tools
        testConnectionButton = new JButton("Test Connection & Fetch Tools");
        testConnectionButton.addActionListener(e -> testConnectionAndFetchTools());
        
        // Initially disable the button until a full command path is provided
        testConnectionButton.setEnabled(isFullCommandPath(commandField.getText()));
        
        // Add document listener to command field to enable/disable button
        commandField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateTestButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateTestButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateTestButtonState();
            }
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Reset grid width for remaining components
        gbc.gridwidth = 1;
        int row = 1;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = row++;
        panel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        // Command field
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Command:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(commandField, gbc);

        gbc.gridx = 1;
        gbc.gridy = row++;
        gbc.weightx = 0.0;
        panel.add(new JLabel("⚠️Use full path to your command"), gbc);

        // Arguments area
        gbc.gridx = 0;
        gbc.gridy = row++;
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
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("<html>Enter each argument on a new line.</html>"), gbc);

        // Add test connection button
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(testConnectionButton, gbc);
        
        // Example area
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JEditorPane exampleArea = new JEditorPane();
        exampleArea.setContentType("text/html");
        exampleArea.setEditable(false);
        exampleArea.setText(
                "<html><b>Example MCP Command on Mac:</b><br>" +
                "1) To set up the 'server-filesystem' tool, you first need to install the MCP using pip.<BR>" +
                "2) Example command: /Users/devoxx/.nvm/versions/node/v22.14.0/bin/npx<br>" +
                "3) Arguments should be:<br>" +
                "-y<br>" +
                "@modelcontextprotocol/server-filesystem<br>" +
                "/Users/devoxx/IdeaProjects/DevoxxGenieIDEAPlugin<br>" +
                "‼️First execute the full command (with args) in your terminal to see if it works!</html>");
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
                    .toList();
            
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
            
            // Log the command for debugging
            log.debug("Executing command: {}", mcpCommand);
            
            // First, test if the command exists by running it directly
            boolean commandExists = testIfCommandExists(commandField.getText().trim());
            if (!commandExists) {
                // Command doesn't exist, show error and return
                String errorMessage = "Command not found: " + commandField.getText().trim() + 
                                    "\nPlease check if the path is correct and the command exists.";
                showErrorDialog("MCP Connection Failed", errorMessage, null);
                toolsDescription = "Connection failed: Command not found";
                testConnectionButton.setText("Connection Failed - Try Again");
                return;
            }
            
            // Create the transport and client with logging enabled
            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(mcpCommand)
                    .environment(env)
                    .logEvents(true) // Enable logging to capture errors in the logs
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
                    .toList();
            
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
            String errorMessage = ex.getMessage();
            
            // Check for common errors and provide more helpful messages
            if (errorMessage != null) {
                if (errorMessage.contains("No such file or directory") || 
                    errorMessage.contains("not found") || 
                    errorMessage.contains("cannot find")) {
                    errorMessage = "Command not found or cannot be executed.\n" +
                                  "Please check if the path is correct and the command exists.";
                } else if (errorMessage.contains("timed out") || errorMessage.contains("timeout")) {
                    errorMessage = "Connection timed out. The command may be hanging or not responding properly.";
                }
            }
            
            // Show error dialog with details
            showErrorDialog("MCP Connection Failed", errorMessage, ex);
            
            toolsDescription = "Connection failed: " + errorMessage;
            testConnectionButton.setText("Connection Failed - Try Again");
        } finally {
            testConnectionButton.setEnabled(true);
        }
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
     * Shows an error dialog with detailed information
     * 
     * @param title Dialog title
     * @param message Error message
     * @param exception The exception that occurred
     */
    private void showErrorDialog(String title, String message, Exception exception) {
        // Create a text area for the stack trace
        JTextArea textArea = new JTextArea(15, 50);
        textArea.setEditable(false);
        
        // Prepare detailed error message with command info and stack trace
        StringBuilder detailedMessage = new StringBuilder();
        detailedMessage.append(message).append("\n\n");
        detailedMessage.append("Command: ").append(commandField.getText().trim()).append("\n");
        
        // Add arguments if present
        if (!argsArea.getText().trim().isEmpty()) {
            detailedMessage.append("Arguments:\n");
            for (String line : argsArea.getText().trim().split("\n")) {
                if (!line.trim().isEmpty()) {
                    detailedMessage.append("  ").append(line.trim()).append("\n");
                }
            }
        }
        
        detailedMessage.append("\nError details:\n");
        
        // Add stack trace
        if (exception != null) {
            // Root cause message
            Throwable cause = exception;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            detailedMessage.append(cause.getMessage()).append("\n\n");
            
            // Full stack trace for debugging
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            detailedMessage.append(sw);
        }
        
        textArea.setText(detailedMessage.toString());
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        // Show the error dialog
        JOptionPane.showMessageDialog(
                this.getRootPane(),
                scrollPane,
                title,
                JOptionPane.ERROR_MESSAGE
        );
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
    
    /**
     * Updates the test button state based on whether a full command path has been provided
     */
    private void updateTestButtonState() {
        if (testConnectionButton != null) {
            testConnectionButton.setEnabled(isFullCommandPath(commandField.getText()));
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
                .toList();

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
