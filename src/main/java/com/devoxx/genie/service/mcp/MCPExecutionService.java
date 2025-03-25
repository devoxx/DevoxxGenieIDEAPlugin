package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for creating and managing MCP clients based on user configuration
 */
@Slf4j
public class MCPExecutionService implements Disposable {

    // Cache of MCP clients keyed by server name
    private final Map<String, McpClient> clientCache = new ConcurrentHashMap<>();

    public static MCPExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(MCPExecutionService.class);
    }
    
    /**
     * Clears the client cache, forcing new client creation on next request
     */
    public void clearClientCache() {
        MCPService.logDebug("Clearing MCP client cache: " + clientCache.size() + " clients");
        // Close each client to clean up resources
        for (Map.Entry<String, McpClient> entry : clientCache.entrySet()) {
            try {
                // Attempt to close client if it has a close method
                closeClientSafely(entry.getValue());
            } catch (Exception e) {
                log.warn("Error closing MCP client for: " + entry.getKey(), e);
            }
        }
        clientCache.clear();
    }

    /**
     * Safely close an MCP client, handling exceptions
     * 
     * @param client The client to close
     */
    private void closeClientSafely(McpClient client) {
        if (client == null) return;

        try {
            client.close();
        } catch (Exception e) {
            log.warn("Error closing MCP client", e);
        }
    }
    
    /**
     * Cleanup resources when the component is disposed
     */
    @Override
    public void dispose() {
        log.info("Disposing MCPExecutionService, closing all clients");
        clearClientCache();
    }
    
    /**
     * Creates tool providers for all configured MCP servers
     * 
     * @return A ToolProvider that includes all enabled MCP tools, or null if MCP is disabled or no servers are configured
     */
    public ToolProvider createMCPToolProvider() {
        log.debug("Creating MCP Tool Provider");

        // Get all configured MCP servers
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance()
                .getMcpSettings().getMcpServers();
        
        if (mcpServers.isEmpty()) {
            MCPService.logDebug("No MCP servers configured");
            return null;
        }
        
        // Create MCP clients for each enabled server
        List<McpClient> mcpClients = new ArrayList<>();
        
        for (MCPServer mcpServer : mcpServers.values()) {
            // Skip disabled servers
            if (!mcpServer.isEnabled()) {
                MCPService.logDebug("Skipping disabled MCP server: " + mcpServer.getName());
                continue;
            }
            
            McpClient mcpClient = createMcpClient(mcpServer);
            if (mcpClient != null) {
                mcpClients.add(mcpClient);
                MCPService.logDebug("Added MCP client for: " + mcpServer.getName());
            }
        }
        
        if (mcpClients.isEmpty()) {
            MCPService.logDebug("No MCP clients could be created");
            return null;
        }
        
        // Build the tool provider with all MCP clients
        MCPService.logDebug("Creating MCP Tool Provider with " + mcpClients.size() + " clients");
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }

    /**
     * Create an MCP client from an MCPServer configuration
     * 
     * @param mcpServer The MCP server configuration
     * @return An initialized MCP client or null if creation fails
     */
    @Nullable
    private McpClient createMcpClient(@NotNull MCPServer mcpServer) {
        String serverName = mcpServer.getName();
        
        // Check if we already have a client for this server
        if (clientCache.containsKey(serverName)) {
            MCPService.logDebug("Reusing existing MCP client for: " + serverName);
            return clientCache.get(serverName);
        }
        
        try {
            MCPService.logDebug("Creating new MCP client for: " + serverName);
            
            // Handle bash commands differently based on working implementation
            List<String> commandList;

            // For other commands, use the standard format
            commandList = new ArrayList<>();
            commandList.add(mcpServer.getCommand());
            if (mcpServer.getArgs() != null) {
                commandList.addAll(mcpServer.getArgs());
            }
            
            MCPService.logDebug("Command list: " + commandList);
            
            // Create the client using the helper method and cache it
            McpClient client = initStdioClient(commandList, mcpServer.getEnv());
            if (client != null) {
                clientCache.put(serverName, client);
                MCPService.logDebug("Added new MCP client to cache for: " + serverName);
            }
            return client;
        } catch (Exception e) {
            log.error("Failed to create MCP client for: " + serverName, e);
            return null;
        }
    }
    
    /**
     * Helper method to initialize a stdio client with error handling
     * 
     * @param command The command list to use
     * @param customEnv Custom environment variables to add
     * @return An initialized MCP client or null if creation fails
     */
    @Nullable
    private static McpClient initStdioClient(List<String> command, Map<String, String> customEnv) {

        try {
            // Create environment map
            Map<String, String> env = new HashMap<>(System.getenv());

            String firstCommand = command.get(0);
            int lastSeparatorIndex = firstCommand.lastIndexOf(File.separator);
            String directoryPath = firstCommand.substring(0, lastSeparatorIndex);

            // We add the path of the command to the environment PATH
            env.put("PATH", directoryPath + File.pathSeparator + env.getOrDefault("PATH", ""));
            if (customEnv != null) {
                env.putAll(customEnv);
                MCPService.logDebug("Added " + customEnv.size() + " environment variables");
            }

            MCPService.logDebug("MCP environment : " + env);

            List<String> mcpCommand = new ArrayList<>();
            mcpCommand.add("/bin/bash");
            mcpCommand.add("-c");
            String cmdString = command.stream()
                    .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                    .collect(Collectors.joining(" "));
            mcpCommand.add(cmdString);
            log.debug("MCP command : {}", mcpCommand);

            // Create the transport
            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(mcpCommand)
                    .environment(env)
                    .logEvents(MCPService.isDebugLogsEnabled())
                    .build();

            // Create and return the client
            return new DefaultMcpClient.Builder()
                    .clientName("DevoxxGenie")
                    .protocolVersion("2024-11-05")
                    .transport(transport)
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize stdio client with command: {}", command, e);
            MCPService.logDebug("Failed to initialize stdio client with command: " + command + " - " + e.getMessage());
            return null;
        }
    }
}
