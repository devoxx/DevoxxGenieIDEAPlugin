package com.devoxx.genie.model.mcp.registry;

import lombok.Data;

import java.util.List;

@Data
public class MCPRegistryPackage {
    private String registryType;
    private String identifier;
    private String version;
    private MCPRegistryTransport transport;
    private List<MCPRegistryEnvVar> environmentVariables;
}
