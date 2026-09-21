package com.devoxx.genie.model.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

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
        /**
         * Legacy 2024-11-05 HTTP+SSE transport. Kept only so persisted configurations still load;
         * langchain4j-mcp 1.19+ has no SSE transport, so it is served as {@link #HTTP}.
         */
        HTTP_SSE,
        HTTP;     // Streamable HTTP

        /** The transport actually used at runtime ({@code HTTP_SSE} resolves to {@code HTTP}). */
        public TransportType effective() {
            return this == HTTP_SSE ? HTTP : this;
        }
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

    @Builder.Default
    private Set<String> disabledTools = new HashSet<>();

    private String toolsDescription;
}
