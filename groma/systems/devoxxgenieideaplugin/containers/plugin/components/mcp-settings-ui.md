---
type: C4 Component
title: MCP settings
status: stable
groma:
  id: mcp-settings-ui
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/ErrorDialogUtil.java
      symbol: ErrorDialogUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPServerDialog.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/transport/StdioTransportPanel.java
      symbol: StdioTransportPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/transport/StreamableHttpTransportPanel.java
      symbol: StreamableHttpTransportPanel
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/transport/TransportPanel.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsConfigurable.java
      symbol: MCPSettingsConfigurable
  group: MCP
  technology: Swing
description: Settings page and dialogs to add, edit and test MCP servers
---

Lists configured servers, edits their transport (stdio command or HTTP URL and headers) and runs a test connection that discovers available tools.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java](../../../../../../src/main/java/com/devoxx/genie/ui/settings/mcp/MCPSettingsComponent.java) | [src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPMarketplaceDialog.java](../../../../../../src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPMarketplaceDialog.java) | Opens marketplace | Swing dialog |
