package com.devoxx.genie.model.mcp.registry;

import lombok.Data;

import java.util.List;

@Data
public class MCPRegistryResponse {
    private List<MCPRegistryServerEntry> servers;
    private MCPRegistryMetadata metadata;
}
