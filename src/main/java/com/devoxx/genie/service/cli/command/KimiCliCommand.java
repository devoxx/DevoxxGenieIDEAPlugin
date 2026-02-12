package com.devoxx.genie.service.cli.command;

import com.devoxx.genie.model.spec.CliToolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Kimi CLI: prompt passed via --prompt flag.
 * Example: kimi --yolo --mcp-config-file mcp.json --prompt "the prompt here"
 */
public class KimiCliCommand extends AbstractCliCommand {

    @Override
    public @NotNull List<String> buildProcessCommand(@NotNull CliToolConfig config,
                                                      @NotNull String prompt,
                                                      @Nullable String mcpConfigPath) {
        List<String> command = new ArrayList<>(config.buildCommand());
        appendMcpConfig(command, config, mcpConfigPath);
        // Kimi takes the prompt via --prompt flag
        command.add("--prompt");
        command.add(prompt);
        return command;
    }

    @Override
    public void writePrompt(@NotNull Process process, @NotNull String prompt) throws IOException {
        // Prompt passed as --prompt argument — keep stdin open to avoid BrokenPipeError
    }

    @Override
    public boolean onTaskCompleted(@NotNull Process process) {
        // Kimi enters interactive mode after processing — kill it when done
        if (process.isAlive()) {
            process.destroyForcibly();
            return true;
        }
        return false;
    }

    @Override
    public @NotNull String defaultExecutablePath() {
        return "/opt/homebrew/bin/kimi";
    }

    @Override
    public @NotNull String defaultExtraArgs() {
        return "--yolo";
    }

    @Override
    public @NotNull String defaultMcpConfigFlag() {
        return "--mcp-config-file";
    }
}
