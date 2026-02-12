package com.devoxx.genie.service.cli.command;

import com.devoxx.genie.model.spec.CliToolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kimi CLI: prompt passed via --prompt flag.
 * <p>
 * Chat mode uses {@code --quiet} (= --print --output-format text --final-message-only)
 * which gives clean text output with only the final assistant message.
 * <p>
 * Task mode uses {@code --print} which outputs structured events
 * (TurnBegin, TextPart, etc.) — only TextPart text is extracted.
 */
public class KimiCliCommand extends AbstractCliCommand {

    /** Matches single-line: TextPart(type='text', text='...') */
    private static final Pattern TEXT_PART_INLINE =
            Pattern.compile("TextPart\\(type='text', text='(.*)'\\)");

    /** Matches the text field on its own line inside a multi-line TextPart. */
    private static final Pattern TEXT_FIELD =
            Pattern.compile("\\s*text='(.*)'\\s*");

    @Override
    public @NotNull List<String> buildProcessCommand(@NotNull CliToolConfig config,
                                                      @NotNull String prompt,
                                                      @Nullable String mcpConfigPath) {
        List<String> command = new ArrayList<>(config.buildCommand());
        appendMcpConfig(command, config, mcpConfigPath);
        command.add("--print");
        command.add("--prompt");
        command.add(prompt);
        return command;
    }

    @Override
    public @NotNull List<String> buildChatCommand(@NotNull CliToolConfig config,
                                                    @NotNull String prompt) {
        // Chat mode: --quiet gives clean text with only the final assistant message
        List<String> command = new ArrayList<>(config.buildCommand());
        command.add("--quiet");
        command.add("--prompt");
        command.add(prompt);
        return command;
    }

    @Override
    public void writePrompt(@NotNull Process process, @NotNull String prompt) throws IOException {
        // Prompt passed as --prompt argument — close stdin so process knows no more input is coming
        process.getOutputStream().close();
    }

    @Override
    public @Nullable String filterResponseLine(@NotNull String line) {
        String clean = ANSI_ESCAPE.matcher(line).replaceAll("");
        if (clean.trim().isEmpty()) return null;

        String trimmed = clean.trim();

        // --print mode (task execution): extract text from TextPart events
        Matcher m = TEXT_PART_INLINE.matcher(trimmed);
        if (m.matches()) return m.group(1);

        Matcher tf = TEXT_FIELD.matcher(trimmed);
        if (tf.matches()) return tf.group(1);

        // --quiet mode (chat): output is already clean text, pass through
        return clean;
    }

    @Override
    public boolean onTaskCompleted(@NotNull Process process) {
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
