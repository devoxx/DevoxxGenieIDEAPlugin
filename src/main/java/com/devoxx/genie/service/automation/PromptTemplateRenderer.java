package com.devoxx.genie.service.automation;

import com.devoxx.genie.model.automation.EventContext;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders template variables in agent prompts using data from {@link EventContext}.
 *
 * Supported variables:
 * <ul>
 *   <li>{@code {{context}}} — full rendered context block</li>
 *   <li>{@code {{content}}} — primary text payload (diff, errors, stack trace)</li>
 *   <li>{@code {{files}}} — newline-separated list of affected file paths</li>
 *   <li>{@code {{event}}} — display name of the IDE event</li>
 *   <li>{@code {{timestamp}}} — ISO-8601 timestamp</li>
 *   <li>{@code {{meta.KEY}}} — value from metadata map</li>
 * </ul>
 *
 * If the prompt contains no template variables, the full context block is appended.
 */
public final class PromptTemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+(?:\\.\\w+)?)}}");

    private PromptTemplateRenderer() {}

    /**
     * Renders the prompt template with values from the given event context.
     * If the template has no {{variables}}, the context block is appended after the prompt.
     */
    public static @NotNull String render(@NotNull String promptTemplate, @NotNull EventContext ctx) {
        if (!VARIABLE_PATTERN.matcher(promptTemplate).find()) {
            // No template variables — append the full context block
            return promptTemplate + "\n\n" + ctx.toPromptBlock();
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement = resolveVariable(variable, ctx);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static @NotNull String resolveVariable(@NotNull String variable, @NotNull EventContext ctx) {
        if (variable.startsWith("meta.")) {
            String key = variable.substring(5);
            return ctx.getMetadata().getOrDefault(key, "");
        }

        return switch (variable) {
            case "context" -> ctx.toPromptBlock();
            case "content" -> ctx.getContent();
            case "files" -> String.join("\n", ctx.getFilePaths());
            case "event" -> ctx.getEventType().getDisplayName();
            case "timestamp" -> ctx.getTimestamp();
            default -> "{{" + variable + "}}"; // leave unknown variables as-is
        };
    }
}
