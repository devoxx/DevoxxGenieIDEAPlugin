package com.devoxx.genie.model.spec;

import com.devoxx.genie.service.cli.command.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
     * Known CLI tool types. Each type maps to a {@link CliCommand}
     * implementation that encapsulates tool-specific execution behavior.
     */
    @Getter
    public enum CliType {
        COPILOT("Copilot"),
        CLAUDE("Claude"),
        CODEX("Codex"),
        GEMINI("Gemini"),
        KIMI("Kimi"),
        CUSTOM("Custom");

        private final String displayName;

        CliType(String displayName) {
            this.displayName = displayName;
        }

        /** Factory method — creates the Command (GoF) for this CLI type. */
        public @NotNull CliCommand createCommand() {
            return switch (this) {
                case COPILOT -> new CopilotCliCommand();
                case CLAUDE -> new ClaudeCliCommand();
                case CODEX -> new CodexCliCommand();
                case GEMINI -> new GeminiCliCommand();
                case KIMI -> new KimiCliCommand();
                case CUSTOM -> new CustomCliCommand();
            };
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
     */
    @Builder.Default
    private String mcpConfigFlag = "";

    /**
     * Build the base command list for ProcessBuilder.
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
