package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPServer;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
                log.warn("Error closing MCP client for: {}", entry.getKey(), e);
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
     * @param project Holds the project information
     * @return A ToolProvider that includes all enabled MCP tools, or null if MCP is disabled or no servers are configured
     */
    public ToolProvider createMCPToolProvider(Project project) {
        log.debug("Creating MCP Tool Provider");

        // Get all configured MCP servers
        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance()
                .getMcpSettings()
                .getMcpServers();

        if (mcpServers.isEmpty()) {
            MCPService.logDebug("No MCP servers configured");
            return null;
        }

        // Filter enabled servers, map to clients, and collect
        List<McpClient> mcpClients = mcpServers.values().stream()
                .filter(MCPServer::isEnabled)
                .peek(server -> MCPService.logDebug("Processing MCP server: " + server.getName()))
                .map(this::createMcpClient)
                .filter(Objects::nonNull)
                .peek(client -> MCPService.logDebug("Added MCP client"))
                .collect(Collectors.toList());

        if (mcpClients.isEmpty()) {
            MCPService.logDebug("No MCP clients could be created");
            return null;
        }

        MCPService.logDebug("Creating MCP Tool Provider with " + mcpClients.size() + " clients");
        // Create the original MCP tool provider
        ToolProvider originalProvider = McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();

        // Wrap it with the custom approval-requiring provider
        return new ApprovalRequiredToolProvider(originalProvider, project);
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

            // Create client based on transport type
            McpClient client;

            if (mcpServer.getTransportType() == MCPServer.TransportType.HTTP_SSE) {
                // Create HTTP SSE client
                client = initHttpSseClient(mcpServer);
            } else if (mcpServer.getTransportType() == MCPServer.TransportType.HTTP) {
                // Create Streamable HTTP client
                client = initStreamableHttpClient(mcpServer);
            } else {
                // Default to STDIO transport
                // Handle bash commands differently based on working implementation
                List<String> commandList = new ArrayList<>();
                commandList.add(mcpServer.getCommand());
                if (mcpServer.getArgs() != null) {
                    commandList.addAll(mcpServer.getArgs());
                }

                MCPService.logDebug("Command list: " + commandList);

                // Create the client using the helper method
                client = initStdioClient(commandList, mcpServer.getEnv());
            }

            // Cache the client if not null
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
     * Helper method to initialize an HTTP SSE client with error handling
     * 
     * @param mcpServer The MCP server configuration
     * @return An initialized MCP client or null if creation fails
     */
    @Nullable
    private static McpClient initHttpSseClient(@NotNull MCPServer mcpServer) {
        try {
            String sseUrl = mcpServer.getUrl();
            if (sseUrl == null || sseUrl.trim().isEmpty()) {
                log.error("SSE URL cannot be empty for HTTP SSE transport");
                MCPService.logDebug("SSE URL cannot be empty for HTTP SSE transport");
                return null;
            }
            
            MCPService.logDebug("Initializing HTTP SSE transport with URL: " + sseUrl);
            
            // Create the transport
            HttpMcpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl(sseUrl)
                    .timeout(java.time.Duration.ofSeconds(DevoxxGenieStateService.getInstance().getTimeout()))
                    .logRequests(MCPService.isDebugLogsEnabled())
                    .logResponses(MCPService.isDebugLogsEnabled())
                    .trafficConsumer(createTrafficConsumer())
                    .build();

            // Create and return the client
            return new DefaultMcpClient.Builder()
                    .clientName("DevoxxGenie")
                    .protocolVersion("2024-11-05")
                    .transport(transport)
                    .logHandler(new MCPLogMessageHandler())
                    .toolExecutionTimeout(java.time.Duration.ofSeconds(DevoxxGenieStateService.getInstance().getTimeout()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to initialize HTTP SSE client with URL: {}", mcpServer.getUrl(), e);
            MCPService.logDebug("Failed to initialize HTTP SSE client with URL: " + mcpServer.getUrl() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to initialize a streamable HTTP client with error handling
     *
     * @param mcpServer The MCP server configuration
     * @return An initialized MCP client or null if creation fails
     */
    @Nullable
    private static McpClient initStreamableHttpClient(@NotNull MCPServer mcpServer) {
        try {
            String url = mcpServer.getUrl();
            if (url == null || url.trim().isEmpty()) {
                log.error("HTTP URL cannot be empty for streamable HTTP transport");
                MCPService.logDebug("HTTP URL cannot be empty for streamable HTTP transport");
                return null;
            }

            MCPService.logDebug("Initializing streamable HTTP transport with URL: " + url);

            // Create the transport
            McpTransport transport = new StreamableHttpMcpTransport.Builder()
                    .url(url)
                    .timeout(java.time.Duration.ofSeconds(DevoxxGenieStateService.getInstance().getTimeout()))
                    .logRequests(MCPService.isDebugLogsEnabled())
                    .logResponses(MCPService.isDebugLogsEnabled())
                    .logger(new MCPTrafficLogger(createTrafficConsumer()))
                    .build();

            // Create and return the client
            return new DefaultMcpClient.Builder()
                    .clientName("DevoxxGenie")
                    .protocolVersion("2024-11-05")
                    .transport(transport)
                    .logHandler(new MCPLogMessageHandler())
                    .toolExecutionTimeout(java.time.Duration.ofSeconds(DevoxxGenieStateService.getInstance().getTimeout()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to initialize streamable HTTP client with URL: {}", mcpServer.getUrl(), e);
            MCPService.logDebug("Failed to initialize streamable HTTP client with URL: " + mcpServer.getUrl() + " - " + e.getMessage());
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

            List<String> mcpCommand = createMCPCommand(command);
            log.debug("MCP command : {}", mcpCommand);

            // Create the transport
            StdioMcpTransport transport = new StdioMcpTransport.Builder()
                    .command(mcpCommand)
                    .environment(env)
                    .logEvents(MCPService.isDebugLogsEnabled())
                    .trafficConsumer(createTrafficConsumer())
                    .build();

            // Create and return the client
            return new DefaultMcpClient.Builder()
                    .clientName("DevoxxGenie")
                    .protocolVersion("2024-11-05")
                    .transport(transport)
                    .logHandler(new MCPLogMessageHandler())
                    .toolExecutionTimeout(Duration.ofSeconds(DevoxxGenieStateService.getInstance().getTimeout()))
                    .build();

        } catch (Exception e) {
            log.error("Failed to initialize stdio client with command: {}", command, e);
            MCPService.logDebug("Failed to initialize stdio client with command: " + command + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a consumer that publishes raw JSON-RPC traffic lines to the
     * MCP Log Panel via the application message bus.
     * <p>
     * Lines prefixed with {@code "> "} are outgoing requests (blue in the panel),
     * lines prefixed with {@code "< "} are incoming responses (green in the panel).
     */
    private static Consumer<String> createTrafficConsumer() {
        return line -> {
            if (!MCPService.isDebugLogsEnabled()) {
                return;
            }
            try {
                MCPType type = line.startsWith("> ") ? MCPType.TOOL_MSG : MCPType.AI_MSG;
                MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
                messageBus.syncPublisher(AppTopics.MCP_TRAFFIC_MSG)
                        .onMCPLoggingMessage(MCPMessage.builder()
                                .type(type)
                                .content(line)
                                .build());
            } catch (Exception e) {
                log.error("Error publishing MCP traffic to message bus", e);
            }
        };
    }

    public static @NotNull List<String> createMCPCommand(@NotNull List<String> command) {
        List<String> mcpCommand = new ArrayList<>();
        
        if (com.intellij.openapi.util.SystemInfo.isWindows) {
            // Windows platform handling
            mcpCommand.add("cmd.exe");
            mcpCommand.add("/c");
            String cmdString = command.stream()
                    .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                    .collect(Collectors.joining(" "));
            mcpCommand.add(cmdString);
        } else {
            // Unix/macOS platform handling
            mcpCommand.add("/bin/bash");
            mcpCommand.add("-c");
            String cmdString = command.stream()
                    .map(arg -> arg.contains(" ") ? "\"" + arg + "\"" : arg)
                    .collect(Collectors.joining(" "));
            mcpCommand.add(cmdString);
        }
        
        log.debug("Platform-specific command: {}", mcpCommand);
        return mcpCommand;
    }
}
