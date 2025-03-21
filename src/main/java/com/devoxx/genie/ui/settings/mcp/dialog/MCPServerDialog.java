package com.devoxx.genie.ui.settings.mcp.dialog;

import com.devoxx.genie.model.mcp.MCPServer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog for adding/editing an MCP server
 */
public class MCPServerDialog extends DialogWrapper {
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

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
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
