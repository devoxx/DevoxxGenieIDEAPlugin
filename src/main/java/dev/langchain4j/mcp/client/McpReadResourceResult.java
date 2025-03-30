package dev.langchain4j.mcp.client;

import dev.langchain4j.mcp.client.McpResourceContents;

import java.util.List;

/**
 * The 'ReadResourceResult' object from the MCP protocol schema.
 */
public record McpReadResourceResult(List<McpResourceContents> contents) {}
