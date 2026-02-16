package com.devoxx.genie.service.acp.model;

import java.util.List;
import java.util.Map;

/**
 * Parameters for the ACP {@code session/new} request, specifying the working
 * directory and MCP server configurations for the new session.
 */
public class SessionNewParams {
    public String cwd;
    public List<Object> mcpServers;

    public SessionNewParams() {}

    public SessionNewParams(String cwd) {
        this.cwd = cwd;
        this.mcpServers = List.of();
    }

    /**
     * Creates params with the Backlog MCP server included.
     * The stdio MCP server format requires: name, command, args, env (all required).
     * The "type" field is NOT used for stdio (only for http/sse variants).
     */
    public SessionNewParams(String cwd, boolean includeBacklogMcp) {
        this.cwd = cwd;
        if (includeBacklogMcp) {
            this.mcpServers = List.of(
                    Map.of(
                            "name", "backlog",
                            "command", "backlog",
                            "args", List.of("mcp", "start"),
                            "env", List.of()
                    )
            );
        } else {
            this.mcpServers = List.of();
        }
    }
}
