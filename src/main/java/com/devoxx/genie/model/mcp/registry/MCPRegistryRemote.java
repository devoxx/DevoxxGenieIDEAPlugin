package com.devoxx.genie.model.mcp.registry;

import lombok.Data;

import java.util.List;

@Data
public class MCPRegistryRemote {
    private String type;
    private String url;
    private List<MCPRegistryHeader> headers;
}
