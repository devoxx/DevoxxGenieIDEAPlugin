package com.devoxx.genie.service.cli.command;

import org.jetbrains.annotations.NotNull;

/**
 * Google Gemini CLI: prompt piped via stdin.
 * Example: echo "prompt" | gemini --mcp-config config.json
 */
public class GeminiCliCommand extends AbstractCliCommand {

    @Override
    public @NotNull String defaultExecutablePath() {
        return "/opt/homebrew/bin/gemini";
    }

    @Override
    public @NotNull String defaultExtraArgs() {
        return "";
    }

    @Override
    public @NotNull String defaultMcpConfigFlag() {
        return "--mcp-config";
    }
}
