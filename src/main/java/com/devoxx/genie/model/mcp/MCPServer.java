package com.devoxx.genie.model.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Represents an MCP server configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPServer {
    private String name;
    private String command;
    private List<String> args;
    
    @Builder.Default
    private Map<String, String> env = new HashMap<>();
    
    @Builder.Default
    private List<String> environment = new java.util.ArrayList<>();
    
    @Builder.Default
    private boolean enabled = true;
    
    @Builder.Default
    private List<String> availableTools = new ArrayList<>();
    
    @Builder.Default
    private Map<String, String> toolDescriptions = new HashMap<>();
    
    private String toolsDescription;
}
