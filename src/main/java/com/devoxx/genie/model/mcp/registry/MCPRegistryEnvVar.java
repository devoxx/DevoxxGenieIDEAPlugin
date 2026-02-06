package com.devoxx.genie.model.mcp.registry;

import lombok.Data;

@Data
public class MCPRegistryEnvVar {
    private String name;
    private String description;
    private String format;
    private Boolean isRequired;
    private Boolean isSecret;
}
