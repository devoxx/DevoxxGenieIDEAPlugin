package com.devoxx.genie.model.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a CLI tool that can execute spec tasks externally.
 * CLI tools must have the Backlog MCP server installed so they can
 * get/update/list backlog tasks just like the internal LLM flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CliToolConfig {

    /**
     * Known CLI tool types with their MCP config format defaults.
     */
    @Getter
    public enum CliType {
        COPILOT("Copilot", "mcpServers", "--additional-mcp-config", true),
        CLAUDE("Claude", "mcpServers", "--mcp-config", false),
        CODEX("Codex", "servers", "--mcp-config", false),
        GEMINI("Gemini", "mcpServers", "--mcp-config", false),
        CUSTOM("Custom", "mcpServers", "", false);

        private final String displayName;
        /** JSON key for the MCP servers block (e.g., "mcpServers" or "servers"). */
        private final String mcpJsonKey;
        /** Default MCP config flag for this tool type. */
        private final String defaultMcpFlag;
        /** Whether to prefix the config file path with @ (copilot convention). */
        private final boolean useAtPrefix;

        CliType(String displayName, String mcpJsonKey, String defaultMcpFlag, boolean useAtPrefix) {
            this.displayName = displayName;
            this.mcpJsonKey = mcpJsonKey;
            this.defaultMcpFlag = defaultMcpFlag;
            this.useAtPrefix = useAtPrefix;
        }
    }

    @Builder.Default
    private CliType type = CliType.CUSTOM;
    @Builder.Default
    private String name = "";
    @Builder.Default
    private String executablePath = "";
    @Builder.Default
    private List<String> extraArgs = new ArrayList<>();
    @Builder.Default
    private boolean enabled = true;
    /**
     * Optional environment variables passed to the CLI process.
     * Useful for API keys, tokens, or tool-specific configuration
     * (e.g., GITHUB_TOKEN, ANTHROPIC_API_KEY).
     */
    @Builder.Default
    private Map<String, String> envVars = new LinkedHashMap<>();
    /**
     * CLI flag for passing the MCP config file path.
     * When set, the executor auto-generates a Backlog MCP config file
     * and appends [mcpConfigFlag, configPath] to the command.
     * Defaults are populated from {@link CliType#getDefaultMcpFlag()}.
     */
    @Builder.Default
    private String mcpConfigFlag = "";

    /**
     * Build the command list for ProcessBuilder.
     * The prompt is piped via stdin by the caller, so the prompt flag is not included.
     * Format: [executablePath, ...extraArgs]
     */
    public List<String> buildCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(executablePath);
        if (extraArgs != null) {
            cmd.addAll(extraArgs);
        }
        return cmd;
    }
}
