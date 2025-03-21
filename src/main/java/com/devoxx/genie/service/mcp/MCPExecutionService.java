package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for creating and managing MCP clients based on user configuration
 */
public class MCPExecutionService {
    private static final Logger LOG = Logger.getInstance(MCPExecutionService.class);
    
    public static MCPExecutionService getInstance() {
        return ApplicationManager.getApplication().getService(MCPExecutionService.class);
    }
    
    /**
     * Creates tool providers for all configured MCP servers
     * 
     * @return A ToolProvider that includes all enabled MCP tools, or null if MCP is disabled or no servers are configured
     */
    public ToolProvider createMCPToolProvider() {

        // Get all configured MCP servers
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance()
                .getMcpSettings().getMcpServers();
        
        if (mcpServers.isEmpty()) {
            MCPService.logDebug("No MCP servers configured");
            return null;
        }
        
        // Create MCP clients for each server
        List<McpClient> mcpClients = new ArrayList<>();
        
        for (MCPServer mcpServer : mcpServers.values()) {
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
        try {
            MCPService.logDebug("Creating MCP client for: " + mcpServer.getName());
            
            // Handle bash commands differently based on working implementation
            List<String> commandList;

            // For other commands, use the standard format
            commandList = new ArrayList<>();
            commandList.add(mcpServer.getCommand());
            if (mcpServer.getArgs() != null) {
                commandList.addAll(mcpServer.getArgs());
            }
            
            MCPService.logDebug("Command list: " + commandList);
            
            // Create the client using the helper method
            return initStdioClient(commandList, mcpServer.getEnv());
        } catch (Exception e) {
            LOG.error("Failed to create MCP client for: " + mcpServer.getName(), e);
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
            if (customEnv != null) {
                env.putAll(customEnv);
                MCPService.logDebug("Added " + customEnv.size() + " environment variables");
            }
            
            // Get timeout from settings or use default
            int timeoutSeconds = DevoxxGenieStateService.getInstance().getTimeout();
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 60; // Default timeout
            }

            List<String> mcpCommand = List.of(
                    "/bin/bash",
                    "-c",
                    command.get(0));

            // Create the transport
            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(mcpCommand)
                    .environment(env)
                    .logEvents(MCPService.isDebugLogsEnabled())
                    .build();

//            List<String> fileSystemCommand = List.of(
//                    "/bin/bash",
//                    "-c",
//                    "/Users/stephan/.nvm/versions/node/v22.14.0/bin/npx -y @modelcontextprotocol/server-filesystem " + chatMessageContext.getProject().getBasePath());
//
//            McpClient mcpFileSystemClient = initStdioClient(fileSystemCommand);
//
//            List<String> thinkingCommand = List.of(
//                    "/bin/bash",
//                    "-c",
//                    "/Users/stephan/.nvm/versions/node/v22.14.0/bin/npx -y @modelcontextprotocol/server-sequential-thinking");
//
//            McpClient mcpThinkingSystem = initStdioClient(thinkingCommand);
//
//            ToolProvider fileSystemToolProvider = McpToolProvider.builder()
//                    .mcpClients(List.of(mcpFileSystemClient, mcpThinkingSystem))
//                    .build();

            // Create and return the client
            return new DefaultMcpClient.Builder()
                    .clientName("DevoxxGenie")
                    .protocolVersion("2024-11-05")
                    .toolExecutionTimeout(Duration.ofSeconds(timeoutSeconds))
                    .transport(transport)
                    .build();
        } catch (Exception e) {
            LOG.error("Failed to initialize stdio client with command: " + command, e);
            return null;
        }
    }
}
