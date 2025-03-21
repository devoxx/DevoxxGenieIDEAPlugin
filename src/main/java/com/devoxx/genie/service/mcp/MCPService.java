package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.MCPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for working with MCP servers
 */
public class MCPService {
    private static final Logger LOG = Logger.getInstance(MCPService.class);
    
    /**
     * Get an MCP server configuration by name
     * 
     * @param name The name of the MCP server
     * @return The MCP server configuration, or null if not found
     */
    public static MCPServer getMCPServer(String name) {
        return DevoxxGenieStateService.getInstance().getMcpSettings().getMCPServer(name);
    }
    
    /**
     * Add a sample GitHub MCP server configuration if no servers are configured
     */
    public static void initializeDefaultMCPServer() {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        MCPSettings mcpSettings = stateService.getMcpSettings();
        
        if (mcpSettings.getMcpServers().isEmpty()) {
            LOG.info("Initializing default MCP server configuration");
            
            // Create a sample GitHub MCP server
            List<String> args = new ArrayList<>();
            args.add("run");
            args.add("-i");
            args.add("--rm");
            args.add("-e");
            args.add("GITHUB_PERSONAL_ACCESS_TOKEN");
            args.add("mcp/github");
            
            Map<String, String> env = new HashMap<>();
            env.put("GITHUB_PERSONAL_ACCESS_TOKEN", "<YOUR_TOKEN>");
            
            MCPServer githubServer = MCPServer.builder()
                    .name("github")
                    .command("docker")
                    .args(args)
                    .env(env)
                    .build();
            
            mcpSettings.addMCPServer(githubServer);
        }
    }
}
