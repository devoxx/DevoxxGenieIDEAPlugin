package com.devoxx.genie.service.spec.command;

import org.jetbrains.annotations.NotNull;

/**
 * Custom CLI tool with user-defined configuration.
 * Defaults to stdin prompt delivery and mcpServers JSON key.
 */
public class CustomCliCommand extends AbstractCliCommand {

    @Override
    public @NotNull String defaultExecutablePath() {
        return "";
    }

    @Override
    public @NotNull String defaultExtraArgs() {
        return "";
    }

    @Override
    public @NotNull String defaultMcpConfigFlag() {
        return "";
    }
}
