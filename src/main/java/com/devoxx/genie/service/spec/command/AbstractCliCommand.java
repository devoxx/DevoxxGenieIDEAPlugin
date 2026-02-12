package com.devoxx.genie.service.spec.command;

import com.devoxx.genie.model.spec.CliToolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation with shared behavior for most CLI tools:
 * builds the command from config, appends MCP config flag,
 * and pipes the prompt via stdin.
 */
public abstract class AbstractCliCommand implements CliCommand {

    @Override
    public @NotNull List<String> buildProcessCommand(@NotNull CliToolConfig config,
                                                      @NotNull String prompt,
                                                      @Nullable String mcpConfigPath) {
        List<String> command = new ArrayList<>(config.buildCommand());
        appendMcpConfig(command, config, mcpConfigPath);
        return command;
    }

    protected void appendMcpConfig(@NotNull List<String> command,
                                    @NotNull CliToolConfig config,
                                    @Nullable String mcpConfigPath) {
        String flag = config.getMcpConfigFlag();
        if (flag != null && !flag.isEmpty() && mcpConfigPath != null) {
            command.add(flag);
            command.add(formatMcpConfigPath(mcpConfigPath));
        }
    }

    protected @NotNull String formatMcpConfigPath(@NotNull String path) {
        return path;
    }

    @Override
    public void writePrompt(@NotNull Process process, @NotNull String prompt) throws IOException {
        try (var writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(prompt);
            writer.flush();
        }
    }

    @Override
    public @NotNull String mcpJsonKey() {
        return "mcpServers";
    }
}
