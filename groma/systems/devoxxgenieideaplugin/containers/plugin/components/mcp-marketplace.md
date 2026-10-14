---
type: C4 Component
title: MCP Marketplace
status: stable
groma:
  id: mcp-marketplace
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryEnvVar.java
      symbol: MCPRegistryEnvVar
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryHeader.java
      symbol: MCPRegistryHeader
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryMetadata.java
      symbol: MCPRegistryMetadata
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryPackage.java
      symbol: MCPRegistryPackage
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryRemote.java
      symbol: MCPRegistryRemote
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryRepository.java
      symbol: MCPRegistryRepository
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryResponse.java
      symbol: MCPRegistryResponse
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryServerEntry.java
      symbol: MCPRegistryServerEntry
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryServerInfo.java
      symbol: MCPRegistryServerInfo
    - scanner: java
      file: src/main/java/com/devoxx/genie/model/mcp/registry/MCPRegistryTransport.java
      symbol: MCPRegistryTransport
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/mcp/MCPRegistryService.java
      symbol: MCPRegistryService
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPMarketplaceDialog.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/mcp/dialog/MCPMarketplaceInstallDialog.java
  group: MCP
  technology: OkHttp, Gson
description: Browse and install MCP servers from the MCP Registry
---

Fetches the registry listing, filters locally for instant search, and turns a chosen entry into an MCP server configuration.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/mcp/MCPRegistryService.java](../../../../../../src/main/java/com/devoxx/genie/service/mcp/MCPRegistryService.java) | [mcp-registry](../../../../../externals/mcp-registry.md) | Searches MCP servers | HTTPS |
