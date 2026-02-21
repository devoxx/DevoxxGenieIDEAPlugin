package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPServer;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Parser for MCP configuration JSON files following the Anthropic/Claude Desktop standard format.
 * Supports both the standard format and DevoxxGenie-specific extensions.
 *
 * Standard format:
 * {
 *   "mcpServers": {
 *     "server-name": {
 *       "command": "npx",
 *       "args": ["-y", "@modelcontextprotocol/server-filesystem"],
 *       "env": {
 *         "API_KEY": "value"
 *       }
 *     }
 *   }
 * }
 *
 * DevoxxGenie extensions (optional):
 * {
 *   "mcpServers": {
 *     "server-name": {
 *       "command": "npx",
 *       "args": ["..."],
 *       "env": {},
 *       "enabled": true,           // Optional: defaults to true
 *       "transport": "stdio",      // Optional: "stdio", "http", "http-sse"
 *       "url": "http://...",       // Optional: for HTTP transport
 *       "headers": {}              // Optional: for HTTP transport
 *     }
 *   }
 * }
 */
@Slf4j
public class MCPConfigurationParser {
    private final Gson gson;

    public MCPConfigurationParser() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Parses MCP configuration from JSON string following the Anthropic standard format.
     *
     * @param json The JSON string to parse
     * @return Map of server name to MCPServer objects
     * @throws MCPConfigurationException if parsing fails
     */
    public Map<String, MCPServer> parseFromJson(@NotNull String json) throws MCPConfigurationException {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("mcpServers")) {
                throw new MCPConfigurationException("Missing 'mcpServers' root object in JSON");
            }

            JsonObject mcpServers = root.getAsJsonObject("mcpServers");
            Map<String, MCPServer> result = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : mcpServers.entrySet()) {
                String serverName = entry.getKey();
                JsonObject serverConfig = entry.getValue().getAsJsonObject();

                MCPServer server = parseServerConfig(serverName, serverConfig);
                result.put(serverName, server);
            }

            return result;
        } catch (JsonSyntaxException e) {
            throw new MCPConfigurationException("Invalid JSON syntax: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new MCPConfigurationException("Error parsing MCP configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a single server configuration from JSON.
     */
    private @NotNull MCPServer parseServerConfig(@NotNull String serverName, @NotNull JsonObject config)
            throws MCPConfigurationException {
        MCPServer.MCPServerBuilder builder = MCPServer.builder().name(serverName);

        // Parse transport type (optional, defaults to STDIO)
        MCPServer.TransportType transportType = parseTransportType(config);
        builder.transportType(transportType);

        // Parse based on transport type
        if (transportType == MCPServer.TransportType.HTTP || transportType == MCPServer.TransportType.HTTP_SSE) {
            // HTTP transport requires URL
            if (!config.has("url")) {
                throw new MCPConfigurationException("Server '" + serverName + "' with HTTP transport requires 'url' field");
            }
            builder.url(config.get("url").getAsString());

            // Optional headers
            if (config.has("headers")) {
                builder.headers(parseStringMap(config.getAsJsonObject("headers")));
            }
        } else {
            // STDIO transport requires command
            if (!config.has("command")) {
                throw new MCPConfigurationException("Server '" + serverName + "' requires 'command' field");
            }
            builder.command(config.get("command").getAsString());

            // Optional args
            if (config.has("args")) {
                builder.args(parseStringList(config.getAsJsonArray("args")));
            }
        }

        // Parse environment variables (optional)
        if (config.has("env")) {
            builder.env(parseStringMap(config.getAsJsonObject("env")));
        }

        // Parse enabled flag (optional, defaults to true)
        if (config.has("enabled")) {
            builder.enabled(config.get("enabled").getAsBoolean());
        } else {
            builder.enabled(true);
        }

        // Parse disabled tools (optional, DevoxxGenie extension)
        if (config.has("disabledTools")) {
            Set<String> disabledTools = new HashSet<>();
            for (JsonElement element : config.getAsJsonArray("disabledTools")) {
                disabledTools.add(element.getAsString());
            }
            builder.disabledTools(disabledTools);
        }

        return builder.build();
    }

    /**
     * Parses transport type from config, defaulting to STDIO.
     */
    private MCPServer.@NotNull TransportType parseTransportType(@NotNull JsonObject config) {
        if (!config.has("transport")) {
            return MCPServer.TransportType.STDIO;
        }

        String transport = config.get("transport").getAsString().toLowerCase();
        return switch (transport) {
            case "http" -> MCPServer.TransportType.HTTP;
            case "http-sse", "http_sse" -> MCPServer.TransportType.HTTP_SSE;
            case "stdio" -> MCPServer.TransportType.STDIO;
            default -> {
                log.warn("Unknown transport type '{}', defaulting to STDIO", transport);
                yield MCPServer.TransportType.STDIO;
            }
        };
    }

    /**
     * Parses a JSON array into a list of strings.
     */
    private @NotNull List<String> parseStringList(@NotNull JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            result.add(element.getAsString());
        }
        return result;
    }

    /**
     * Parses a JSON object into a map of strings.
     */
    private @NotNull Map<String, String> parseStringMap(@NotNull JsonObject object) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }

    /**
     * Exports MCP servers to JSON string following the Anthropic standard format.
     *
     * @param servers Map of server name to MCPServer objects
     * @param includeExtensions Whether to include DevoxxGenie-specific extensions
     * @return JSON string representation
     */
    public @NotNull String exportToJson(@NotNull Map<String, MCPServer> servers, boolean includeExtensions) {
        JsonObject root = new JsonObject();
        JsonObject mcpServers = new JsonObject();

        for (Map.Entry<String, MCPServer> entry : servers.entrySet()) {
            mcpServers.add(entry.getKey(), buildServerConfig(entry.getValue(), includeExtensions));
        }

        root.add("mcpServers", mcpServers);
        return gson.toJson(root);
    }

    /**
     * Builds the JSON configuration object for a single MCP server.
     */
    private @NotNull JsonObject buildServerConfig(@NotNull MCPServer server, boolean includeExtensions) {
        JsonObject serverConfig = new JsonObject();

        if (includeExtensions && server.getTransportType() != MCPServer.TransportType.STDIO) {
            serverConfig.addProperty("transport", getTransportString(server.getTransportType()));
        }

        addTransportSpecificFields(serverConfig, server, includeExtensions);

        if (server.getEnv() != null && !server.getEnv().isEmpty()) {
            serverConfig.add("env", mapToJsonObject(server.getEnv()));
        }

        if (includeExtensions && !server.isEnabled()) {
            serverConfig.addProperty("enabled", false);
        }

        if (includeExtensions) {
            addDisabledToolsArray(serverConfig, server);
        }

        return serverConfig;
    }

    /**
     * Returns the transport string identifier for a given transport type.
     */
    private @NotNull String getTransportString(@NotNull MCPServer.TransportType transportType) {
        return switch (transportType) {
            case HTTP -> "http";
            case HTTP_SSE -> "http-sse";
            default -> "stdio";
        };
    }

    /**
     * Adds transport-specific fields (URL/headers for HTTP, command/args for STDIO).
     */
    private void addTransportSpecificFields(@NotNull JsonObject config, @NotNull MCPServer server,
                                            boolean includeExtensions) {
        if (server.getTransportType() == MCPServer.TransportType.HTTP ||
            server.getTransportType() == MCPServer.TransportType.HTTP_SSE) {
            if (server.getUrl() != null) {
                config.addProperty("url", server.getUrl());
            }
            if (includeExtensions && server.getHeaders() != null && !server.getHeaders().isEmpty()) {
                config.add("headers", mapToJsonObject(server.getHeaders()));
            }
        } else {
            if (server.getCommand() != null) {
                config.addProperty("command", server.getCommand());
            }
            if (server.getArgs() != null && !server.getArgs().isEmpty()) {
                config.add("args", listToJsonArray(server.getArgs()));
            }
        }
    }

    /**
     * Adds the disabledTools array to the config if the server has disabled tools.
     */
    private void addDisabledToolsArray(@NotNull JsonObject config, @NotNull MCPServer server) {
        if (server.getDisabledTools() == null || server.getDisabledTools().isEmpty()) {
            return;
        }
        JsonArray disabledToolsArray = new JsonArray();
        for (String tool : server.getDisabledTools()) {
            disabledToolsArray.add(tool);
        }
        config.add("disabledTools", disabledToolsArray);
    }

    /**
     * Converts a list of strings to JsonArray.
     */
    private @NotNull JsonArray listToJsonArray(@NotNull List<String> list) {
        JsonArray array = new JsonArray();
        for (String item : list) {
            array.add(item);
        }
        return array;
    }

    /**
     * Converts a map of strings to JsonObject.
     */
    private @NotNull JsonObject mapToJsonObject(@NotNull Map<String, String> map) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            object.addProperty(entry.getKey(), entry.getValue());
        }
        return object;
    }

    /**
     * Exception thrown when MCP configuration parsing fails.
     */
    public static class MCPConfigurationException extends Exception {
        public MCPConfigurationException(String message) {
            super(message);
        }

        public MCPConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
