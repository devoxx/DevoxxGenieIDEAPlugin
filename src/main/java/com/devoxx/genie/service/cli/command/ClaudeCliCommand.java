package com.devoxx.genie.service.cli.command;

import com.devoxx.genie.service.cli.ClaudeStreamJsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Filter stream-json output lines so only human-readable text is shown in the chat panel.
     * JSON event lines (system, assistant tool_use, user tool_result, etc.) are filtered out
     * since they are already parsed by {@link ClaudeStreamJsonParser} and shown in Activity Logs.
     * Only assistant text content is extracted and returned for display.
     */
    @Override
    public @Nullable String filterResponseLine(@NotNull String line) {
        String stripped = super.filterResponseLine(line);
        if (stripped == null) {
            return null;
        }
        // Non-JSON lines pass through (plain text output from non-stream-json mode)
        if (!stripped.startsWith("{")) {
            return stripped;
        }
        // Extract human-readable text; append extra newline so consecutive messages
        // get paragraph separation (\n\n) when the accumulator adds its own \n.
        String text = ClaudeStreamJsonParser.extractHumanReadableText(stripped);
        return text != null ? text + "\n" : null;
    }
}
