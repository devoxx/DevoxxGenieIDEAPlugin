package com.devoxx.genie.ui.trace.view;

import com.devoxx.genie.service.trace.TraceScanStatus;
import com.devoxx.genie.service.trace.view.AgentRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The text of the agent run view (TASK-283): one-line tree rows, the detail pane and the summary
 * header. Plain strings only; the renderer appends them as plain text (no HTML).
 */
final class AgentRunText {

    /** How a row is styled. */
    enum Style { NORMAL, ERROR, WARNING, MUTED }

    /** One tree row: the main text, a greyed suffix, and the style of the main text. */
    record Row(@NotNull String text, @NotNull String suffix, @NotNull Style style) {}

    static final int ROW_TEXT_LIMIT = 90;
    private static final int ARGS_LIMIT = 60;
    private static final ObjectMapper PRETTY = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private AgentRunText() {
    }

    static @NotNull Row row(@NotNull Object node) {
        if (node instanceof AgentRun.Turn turn) {
            return new Row("Turn " + turn.number() + "  " + oneLine(userPrompt(turn.prompt()), ROW_TEXT_LIMIT),
                    stepsLabel(turn.steps().size()), Style.NORMAL);
        }
        if (node instanceof AgentRun.Step step) {
            String text = oneLine(step.text(), ROW_TEXT_LIMIT);
            if (text.isEmpty()) {
                text = step.toolCalls().isEmpty() ? "(no text)" : callsLabel(step.toolCalls().size());
            }
            String suffix = "+" + duration(step.sincePreviousMs())
                    + (isBlank(step.reasoning()) ? "" : "  · reasoning");
            return new Row("Step " + step.number() + "  " + text, suffix, Style.NORMAL);
        }
        if (node instanceof AgentRun.ToolCallNode call) {
            StringBuilder suffix = new StringBuilder();
            if (call.durationMs() >= 0) suffix.append(duration(call.durationMs()));
            if (call.repeat()) suffix.append(suffix.isEmpty() ? "" : "  ").append("⟳ repeated");
            if (call.toolCallLimit()) suffix.append(suffix.isEmpty() ? "" : "  ").append("hit the tool-call limit");
            if (!call.hasResult()) suffix.append(suffix.isEmpty() ? "" : "  ").append("no result");
            Style style = call.error() ? Style.ERROR
                    : (call.toolCallLimit() || !call.hasResult()) ? Style.WARNING : Style.NORMAL;
            return new Row(call.name() + "(" + oneLine(call.arguments(), ARGS_LIMIT) + ")", suffix.toString(), style);
        }
        if (node instanceof AgentRun.SubAgentRun sub) {
            return new Row(sub.label(), stepsLabel(sub.steps().size()), Style.MUTED);
        }
        return new Row(String.valueOf(node), "", Style.NORMAL);
    }

    static @NotNull String detail(@NotNull Object node) {
        if (node instanceof AgentRun.Turn turn) {
            String prompt = userPrompt(turn.prompt());
            String detail = "Turn " + turn.number() + " · " + stepsLabel(turn.steps().size()) + section("Prompt", prompt);
            if (turn.prompt() != null && !turn.prompt().equals(prompt)) {
                detail += section("Sent to the model", turn.prompt());
            }
            return detail;
        }
        if (node instanceof AgentRun.Step step) {
            StringBuilder sb = new StringBuilder("Step " + step.number());
            if (!isBlank(step.model())) sb.append(" · ").append(step.model());
            sb.append(" · +").append(duration(step.sincePreviousMs()));
            sb.append(section("Text", isBlank(step.text()) ? "(none)" : step.text()));
            if (!isBlank(step.reasoning())) sb.append(section("Reasoning", step.reasoning()));
            if (!step.toolCalls().isEmpty()) {
                sb.append(section("Tool calls", step.toolCalls().stream()
                        .map(c -> "- " + c.name() + "(" + oneLine(c.arguments(), ARGS_LIMIT) + ")")
                        .collect(Collectors.joining("\n"))));
            }
            return sb.toString();
        }
        if (node instanceof AgentRun.ToolCallNode call) {
            StringBuilder sb = new StringBuilder(call.name());
            sb.append(call.error() ? " · failed" : call.hasResult() ? " · ok" : " · no result recorded");
            if (call.durationMs() >= 0) sb.append(" · ").append(duration(call.durationMs()));
            if (call.repeat()) sb.append(" · repeated: same tool and arguments as an earlier call");
            if (call.toolCallLimit()) sb.append(" · hit the tool-call limit");
            if (!call.subAgents().isEmpty()) sb.append(" · ").append(call.subAgents().size()).append(" sub-agent(s)");
            sb.append(section("Arguments", prettyJson(call.arguments())));
            sb.append(section("Result", call.hasResult() ? call.result() : "(no result recorded)"));
            return sb.toString();
        }
        if (node instanceof AgentRun.SubAgentRun sub) {
            return sub.label() + " · " + stepsLabel(sub.steps().size()) + section("Prompt", sub.prompt());
        }
        return "";
    }

    static @NotNull String summary(@NotNull AgentRun.Summary s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.models().isEmpty() ? "unknown model" : String.join(", ", s.models()));
        sb.append(" · ").append(count(s.turns(), "turn"));
        sb.append(" · ").append(count(s.steps(), "step"));
        sb.append(" · ").append(count(s.toolCalls(), "tool call"));
        sb.append(" · ").append(s.failedToolCalls()).append(" failed");
        if (s.repeatedToolCalls() > 0) sb.append(" · ").append(s.repeatedToolCalls()).append(" repeated");
        sb.append(" · ").append(duration(s.durationMs()));
        if (s.hitToolCallLimit()) sb.append(" · hit the tool-call limit");
        if (!s.scanStatuses().isEmpty()) {
            sb.append(" · scan: ").append(s.scanStatuses().stream()
                    .sorted().map(TraceScanStatus::name).collect(Collectors.joining(", ")));
        }
        return sb.toString();
    }

    private static final java.util.regex.Pattern USER_PROMPT =
            java.util.regex.Pattern.compile("<UserPrompt>(.*?)</UserPrompt>", java.util.regex.Pattern.DOTALL);

    /**
     * What the user typed: the recorded prompt is the full message sent to the model, which wraps it
     * in {@code <UserPrompt>} next to context such as {@code <ProjectPath>}. Unwrapped prompts are returned as is.
     */
    static @Nullable String userPrompt(@Nullable String recorded) {
        if (recorded == null) return null;
        java.util.regex.Matcher m = USER_PROMPT.matcher(recorded);
        return m.find() ? m.group(1).strip() : recorded;
    }

    /** {@code 450 ms}, {@code 3.2 s}, {@code 2 m 14 s}. */
    static @NotNull String duration(long ms) {
        if (ms < 1_000) return ms + " ms";
        if (ms < 60_000) return String.format(Locale.ROOT, "%.1f s", ms / 1000.0);
        long seconds = ms / 1000;
        return (seconds / 60) + " m " + (seconds % 60) + " s";
    }

    /** The first line of {@code text}, whitespace-collapsed and cut to {@code max} characters with an ellipsis. */
    static @NotNull String oneLine(@Nullable String text, int max) {
        if (text == null) return "";
        String line = text.strip().replaceAll("\\s+", " ");
        return line.length() <= max ? line : line.substring(0, max - 1) + "…";
    }

    private static String prettyJson(@Nullable String json) {
        if (isBlank(json)) return "(none)";
        try {
            return PRETTY.writeValueAsString(PRETTY.readTree(json));
        } catch (Exception e) {
            return json;
        }
    }

    private static String section(String title, @Nullable String body) {
        return "\n\n── " + title + " ──\n" + (body == null ? "" : body);
    }

    private static String stepsLabel(int n) {
        return count(n, "step");
    }

    private static String callsLabel(int n) {
        return "(" + count(n, "tool call") + ")";
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static boolean isBlank(@Nullable String s) {
        return s == null || s.isBlank();
    }
}
