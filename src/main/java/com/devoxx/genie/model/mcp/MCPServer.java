// File: src/main/java/com/devoxx/genie/model/mcp/MCPServer.java
package com.devoxx.genie.model.mcp;

import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a configuration for an MCP server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MCPServer {

    public enum TransportType {
        STDIO,    // Standard I/O communication with a subprocess
        HTTP_SSE  // HTTP Server-Sent Events for communication
    }

    private boolean enabled;
    private String name;
    @Builder.Default
    private TransportType transportType = TransportType.STDIO;


    // For HTTP_SSE
    private String sseUrl;

    // For STDIO
    private String command;
    private List<String> args;
    private String workingDirectory; // Optional working directory for STDIO command

    private Map<String, String> env = new ConcurrentHashMap<>();

    @Builder.Default
    private List<String> environment = new java.util.ArrayList<>();

    @Builder.Default
    private List<String> availableTools = new ArrayList<>(); // List of tool names provided by the server
    private Map<String, String> toolDescriptions; // Map of toolName to description
    private String toolsDescription;

    // --- New Fields for Installed Servers via Marketplace (persisted) ---
    @Property(surroundWithTag = false) // Ensures it's stored directly, not wrapped
    @Nullable
    private String installationPathString; // Stores the Path as a String for persistence

    @Property(surroundWithTag = false)
    @Nullable
    private String gitHubUrl; // Original GitHub URL for installed servers, for tracking provenance

    @Property(surroundWithTag = false)
    @Nullable
    private String repositoryName; // Name of the GitHub repo, often used for local folder name

    // --- Transient Runtime Fields (not persisted) ---
    @Transient
    private transient Process currentProcess; // The actual running process instance

    @Transient
    private transient volatile boolean isRunning; // Current running state of the server

    @Transient
    private transient StringBuilder consoleOutputBuffer = new StringBuilder(); // In-memory buffer for console output (optional, as logs go to file)

    @Transient
    private transient Path installationPath; // Resolved Path object from installationPathString (convenience)

    /**
     * Sets the installation path string and updates the transient Path object.
     * This ensures the Path object is always consistent with the persisted string.
     * @param installationPathString The string representation of the installation path.
     */
    public void setInstallationPathString(@Nullable String installationPathString) {
        this.installationPathString = installationPathString;
        this.installationPath = (installationPathString != null) ? Paths.get(installationPathString) : null;
    }

    /**
     * Returns the Path object for the installation path. If it's null but a string is present,
     * it will be reconstructed.
     * @return The Path object representing the installation path.
     */
    public Path getInstallationPath() {
        if (installationPath == null && installationPathString != null) {
            installationPath = Paths.get(installationPathString);
        }
        return installationPath;
    }

    /**
     * Custom equals method based on server name for uniqueness in collections.
     * @param o The object to compare.
     * @return True if names are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCPServer mcpServer = (MCPServer) o;
        return Objects.equals(name, mcpServer.name);
    }

    /**
     * Custom hashCode method based on server name.
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}