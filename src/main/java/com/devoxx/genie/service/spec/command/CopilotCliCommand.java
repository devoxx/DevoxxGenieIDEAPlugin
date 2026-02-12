package com.devoxx.genie.service.spec.command;

import org.jetbrains.annotations.NotNull;

/**
 * GitHub Copilot CLI: prompt piped via stdin, MCP config path prefixed with @.
 * Example: echo "prompt" | copilot --allow-all --additional-mcp-config @config.json
 */
public class CopilotCliCommand extends AbstractCliCommand {

    @Override
    protected @NotNull String formatMcpConfigPath(@NotNull String path) {
        return "@" + path;
    }

    @Override
    public @NotNull String defaultExecutablePath() {
        return "/opt/homebrew/bin/copilot";
    }

    @Override
    public @NotNull String defaultExtraArgs() {
        return "--allow-all";
    }

    @Override
    public @NotNull String defaultMcpConfigFlag() {
        return "--additional-mcp-config";
    }
}
