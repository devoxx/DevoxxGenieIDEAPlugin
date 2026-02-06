package com.devoxx.genie.model.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an MCP server configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPServer {
    /**
     * Transport type for MCP communication
     */
    public enum TransportType {
        STDIO,    // Standard I/O communication with a subprocess
        HTTP_SSE, // HTTP Server-Sent Events for communication (deprecated)
        HTTP      // Streamable HTTP
    }
    
    @Builder.Default
    private TransportType transportType = TransportType.STDIO;
    private String name;
    
    // STDIO transport properties
    private String command;
    private List<String> args;
    
    // HTTP transport properties
    private String url;
    
    @Builder.Default
    private Map<String, String> env = new HashMap<>();

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();
    
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
