---
type: C4 Component
title: MCP client integration
status: stable
groma:
  id: mcp-client
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/MCPMessage.java
      symbol: MCPMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/MCPServer.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/MCPSettings.java
      symbol: MCPSettings
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/MCPType.java
      symbol: MCPType
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/ApprovalRequiredToolProvider.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/FilteredMcpToolProvider.java
      symbol: FilteredMcpToolProvider
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/InstrumentedMcpToolProvider.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPApprovalService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPConfigurationParser.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPExecutionService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPListenerService.java
      symbol: MCPListenerService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPLoggingMessage.java
      symbol: MCPLoggingMessage
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPLogMessageHandler.java
      symbol: MCPLogMessageHandler
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPService.java
      symbol: MCPService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPTrafficLogger.java
      symbol: MCPTrafficLogger
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/mcp/MCPToolsManager.java
  group: MCP
  technology: langchain4j-mcp
description: Connects to configured MCP servers and exposes their tools to the LLM
---

Creates and caches langchain4j MCP clients per server (stdio or Streamable HTTP), filters disabled tools, wraps them with approval when agent mode is off, and logs JSON-RPC traffic to the activity log.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/mcp/MCPExecutionService.java](../../../../../../src/main/java/com/devoxx/genie/service/mcp/MCPExecutionService.java) | [mcp-servers](../../../../../externals/mcp-servers.md) | Calls MCP tools | MCP stdio / Streamable HTTP |
