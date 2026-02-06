package com.devoxx.genie.model.mcp.registry;

import lombok.Data;

import java.util.List;

@Data
public class MCPRegistryServerInfo {
    private String name;
    private String description;
    private String version;
    private MCPRegistryRepository repository;
    private List<MCPRegistryPackage> packages;
    private List<MCPRegistryRemote> remotes;
}
