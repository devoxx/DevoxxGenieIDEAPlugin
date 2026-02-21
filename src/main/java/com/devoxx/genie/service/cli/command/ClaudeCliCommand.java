package com.devoxx.genie.service.cli.command;

import org.jetbrains.annotations.NotNull;

/**
 * Claude Code CLI: prompt piped via stdin with -p flag.
 * Example: echo "prompt" | claude -p --dangerously-skip-permissions --model opus --allowedTools Backlog.md --mcp-config config.json
 */
public class ClaudeCliCommand extends AbstractCliCommand {

    @Override
    public @NotNull String defaultExecutablePath() {
        return "/opt/homebrew/bin/claude";
    }

    @Override
    public @NotNull String defaultExtraArgs() {
        return "-p --verbose --dangerously-skip-permissions --model opus --allowedTools Backlog.md --output-format stream-json";
    }

    @Override
    public @NotNull String defaultMcpConfigFlag() {
        return "--mcp-config";
    }
}
