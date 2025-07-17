package com.devoxx.genie.model.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a GitHub repository, typically used for marketplace entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepo {
    private String name;        // Repository name (e.g., "MCPJavaFileSystem")
    private String fullName;    // Full name (e.g., "stephanj/MCPJavaFileSystem")
    private String description; // Description from GitHub
    private String htmlUrl;     // URL to the repository on GitHub
    private String cloneUrl;    // HTTPS clone URL for git operations
    private int stars;          // Number of stargazers
    private String updatedAt;   // Last updated timestamp (formatted string)
    private String defaultBranch; // Default branch (e.g., "main" or "master")
}