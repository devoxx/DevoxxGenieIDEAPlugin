package com.devoxx.genie.model.mcp.registry;

import lombok.Data;

@Data
public class MCPRegistryHeader {
    private String name;
    private String description;
    private String value;
    private Boolean isRequired;
    private Boolean isSecret;
}
