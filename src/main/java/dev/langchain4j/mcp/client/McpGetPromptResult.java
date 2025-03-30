package dev.langchain4j.mcp.client;

import dev.langchain4j.mcp.client.McpPromptMessage;

import java.util.List;

/**
 * The 'GetPromptResult' object from the MCP protocol schema.
 */
public record McpGetPromptResult(String description, List<McpPromptMessage> messages) {}
