package com.devoxx.genie.service.spec.command;

import com.devoxx.genie.model.spec.CliToolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Codex CLI: prompt passed as a trailing positional argument (not stdin).
 * Example: codex exec --model gpt-5.3-codex --full-auto --mcp-config config.json "prompt"
 */
public class CodexCliCommand extends AbstractCliCommand {

    @Override
    public @NotNull List<String> buildProcessCommand(@NotNull CliToolConfig config,
                                                      @NotNull String prompt,
                                                      @Nullable String mcpConfigPath) {
        List<String> command = new ArrayList<>(config.buildCommand());
        appendMcpConfig(command, config, mcpConfigPath);
        // Codex takes the prompt as a trailing positional argument
        command.add(prompt);
        return command;
    }

    @Override
    public void writePrompt(@NotNull Process process, @NotNull String prompt) throws IOException {
        // Prompt already passed as command-line argument — close stdin
        process.getOutputStream().close();
    }

    @Override
    public @NotNull String defaultExecutablePath() {
        return "/opt/homebrew/bin/codex";
    }

    @Override
    public @NotNull String defaultExtraArgs() {
        return "exec --model gpt-5.3-codex --full-auto";
    }

    @Override
    public @NotNull String defaultMcpConfigFlag() {
        return "";
    }

    @Override
    public boolean onTaskCompleted(@NotNull Process process) {
        // Codex doesn't exit on its own after completing — kill it
        if (process.isAlive()) {
            process.destroyForcibly();
            return true;
        }
        return false;
    }
}
