package com.devoxx.genie.model.mcp;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class for MCP settings.
 */
@Data
public class MCPSettings {
    private Map<String, MCPServer> mcpServers = new HashMap<>();
    
    /**
     * Adds a new MCP server configuration
     * 
     * @param mcpServer The MCP server configuration to add
     */
    public void addMCPServer(MCPServer mcpServer) {
        mcpServers.put(mcpServer.getName(), mcpServer);
    }
    
    /**
     * Removes an MCP server configuration by name
     * 
     * @param name The name of the MCP server to remove
     * @return true if the server was removed, false if it wasn't found
     */
    public boolean removeMCPServer(String name) {
        return mcpServers.remove(name) != null;
    }
    
    /**
     * Gets an MCP server configuration by name
     * 
     * @param name The name of the MCP server to get
     * @return The MCP server configuration, or null if not found
     */
    public MCPServer getMCPServer(String name) {
        return mcpServers.get(name);
    }
}
