package com.devoxx.genie.service.cli.command;

import com.devoxx.genie.model.spec.CliToolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Command pattern (GoF) interface for CLI tool execution.
 * Each CLI tool type (Claude, Copilot, Codex, Gemini) has its own
 * implementation that encapsulates tool-specific behavior:
 * command construction, prompt delivery, and MCP config format.
 */
public interface CliCommand {

    /**
     * Build the full command list for ProcessBuilder,
     * including MCP config flag and prompt (if passed as a trailing argument).
     * Used by task execution mode.
     */
    @NotNull List<String> buildProcessCommand(@NotNull CliToolConfig config,
                                               @NotNull String prompt,
                                               @Nullable String mcpConfigPath);

    /**
     * Build the command list for chat mode (no MCP config, may omit
     * task-specific flags like --print).
     * Default delegates to {@link #buildProcessCommand} with null mcpConfigPath.
     */
    default @NotNull List<String> buildChatCommand(@NotNull CliToolConfig config,
                                                    @NotNull String prompt) {
        return buildProcessCommand(config, prompt, null);
    }

    /**
     * Deliver the prompt to the running process.
     * Implementations either pipe it via stdin or close stdin
     * (when the prompt was already appended as a command-line argument).
     */
    void writePrompt(@NotNull Process process, @NotNull String prompt) throws IOException;

    /** MCP config JSON top-level key (e.g., "mcpServers" or "servers"). */
    @NotNull String mcpJsonKey();

    /** Default executable path for auto-population in settings dialog. */
    @NotNull String defaultExecutablePath();

    /** Default extra arguments for auto-population in settings dialog. */
    @NotNull String defaultExtraArgs();

    /** Default MCP config CLI flag (e.g., "--mcp-config"). */
    @NotNull String defaultMcpConfigFlag();

    /**
     * Filter a line of stdout from the CLI process before including it in the chat response.
     * Return null to exclude the line, or the (possibly cleaned) line to include.
     * Default: return the line as-is.
     */
    default @Nullable String filterResponseLine(@NotNull String line) {
        return line;
    }

    /**
     * Called when the backlog task is marked Done while the process is still running.
     * Most CLI tools (Claude, Copilot) exit on their own — default is no-op.
     * Override to forcibly kill the process for tools that don't self-exit (e.g., Codex).
     *
     * @return true if the process was killed, false if no action was taken
     */
    default boolean onTaskCompleted(@NotNull Process process) {
        return false;
    }
}
