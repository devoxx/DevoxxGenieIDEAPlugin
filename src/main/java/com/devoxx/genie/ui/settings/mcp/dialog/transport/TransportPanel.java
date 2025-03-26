package com.devoxx.genie.ui.settings.mcp.dialog.transport;

import com.devoxx.genie.model.mcp.MCPServer;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for transport-specific panels in the MCP server dialog.
 */
public interface TransportPanel {
    /**
     * Get the panel component to display in the UI
     * 
     * @return The panel component
     */
    JPanel getPanel();
    
    /**
     * Load settings from an existing server
     * 
     * @param server The server to load settings from
     */
    void loadSettings(MCPServer server);
    
    /**
     * Check if all required fields are valid
     * 
     * @return true if valid, false otherwise
     */
    boolean isValid();
    
    /**
     * Get validation error message if any
     * 
     * @return Error message or null if valid
     */
    String getErrorMessage();
    
    /**
     * Create an MCP client for testing the connection
     * 
     * @return The created MCP client
     * @throws Exception If client creation fails
     */
    McpClient createClient() throws Exception;
    
    /**
     * Apply settings to a server builder
     * 
     * @param builder The server builder to apply settings to
     */
    void applySettings(MCPServer.MCPServerBuilder builder);

    default JLabel getExampleLabel() {
        // Create hyperlink to documentation
        JLabel docsLinkLabel = new JLabel("<html><a href=''>Need example? View the FileSystem MCP guide</a></html>");
        docsLinkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Set up the URL for the docs - use GitHub URL
        String repoUrl = "https://github.com/stephanj/MCPJavaFileSystem/tree/master";
        docsLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(repoUrl));
                } catch (Exception ex) {
                    //
                }
            }
        });
        return docsLinkLabel;
    }

    /**
     * Process tool specifications from successful connection test
     * 
     * @param tools List of tool specifications
     * @param builder Server builder to update with tool information
     */
    default void processTools(List<ToolSpecification> tools, MCPServer.MCPServerBuilder builder) {
        // Extract tool names
        List<String> toolNames = tools.stream()
                .map(ToolSpecification::name)
                .toList();
        
        // Create tool descriptions map
        Map<String, String> toolDescs = new HashMap<>();
        for (ToolSpecification tool : tools) {
            toolDescs.put(tool.name(), tool.description());
        }
        
        // Generate description string
        StringBuilder description = new StringBuilder("Available tools: ");
        if (tools.isEmpty()) {
            description.append("None");
        } else {
            description.append(tools.size()).append(" - ");
            description.append(String.join(", ", toolNames));
        }
        
        // Apply to the builder
        builder.availableTools(toolNames)
               .toolDescriptions(toolDescs)
               .toolsDescription(description.toString());
    }
}
