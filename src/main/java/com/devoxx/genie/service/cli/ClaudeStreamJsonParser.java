package com.devoxx.genie.service.cli;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Parses Claude CLI {@code --output-format stream-json} NDJSON lines into {@link AgentMessage} objects
 * so they can be published to the DevoxxGenie Activity Logs panel.
 *
 * <p>Each stdout line from Claude CLI (when stream-json mode is active) is a self-contained JSON object
 * with a {@code type} field. This parser maps those event types to the existing {@link AgentType} enum
 * values used by the in-process agent mode.
 */
@Slf4j
public final class ClaudeStreamJsonParser {

    /** CLI flag substring that activates stream-json output mode. */
    static final String STREAM_JSON_FLAG = "stream-json";

    private ClaudeStreamJsonParser() {
    }

    /**
     * Parse a single stdout line from Claude CLI {@code --output-format stream-json}.
     *
     * @param line                raw stdout line
     * @param projectLocationHash project hash used to filter messages in {@code AgentMcpLogPanel}
     * @return list of parsed messages, or empty if the line is not a recognised stream-json event
     */
    @NotNull
    public static Optional<List<AgentMessage>> parse(@NotNull String line,
                                                     @Nullable String projectLocationHash) {
        if (!line.startsWith("{")) {
            return Optional.empty();
        }
        try {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            String type = getStringOrNull(obj, "type");
            if (type == null) {
                return Optional.empty();
            }

            List<AgentMessage> messages = new ArrayList<>();
            switch (type) {
                case "system"    -> parseSystem(obj, projectLocationHash, messages);
                case "assistant" -> parseAssistant(obj, projectLocationHash, messages);
                case "user"      -> parseUser(obj, projectLocationHash, messages);
                case "tool"      -> parseTool(obj, projectLocationHash, messages);
                case "result"    -> parseResult(obj, projectLocationHash, messages);
                default          -> { /* unknown event type — ignore */ }
            }

            return messages.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(messages));

        } catch (Exception e) {
            log.debug("ClaudeStreamJsonParser: failed to parse line: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // --- event-type handlers ---

    private static void parseSystem(JsonObject obj, String hash, List<AgentMessage> out) {
        if (!"init".equals(getStringOrNull(obj, "subtype"))) return;

        String model = getStringOrNull(obj, "model");
        String sessionId = getStringOrNull(obj, "session_id");
        StringBuilder summary = new StringBuilder("CLI session started");
        if (model != null)     summary.append(" | model: ").append(model);
        if (sessionId != null) summary.append(" | session: ").append(sessionId);

        out.add(build(AgentType.INTERMEDIATE_RESPONSE, "system/init", null, summary.toString(), hash));
    }

    private static void parseAssistant(JsonObject obj, String hash, List<AgentMessage> out) {
        JsonObject message = getObjectOrNull(obj, "message");
        if (message == null) return;

        JsonArray content = getArrayOrNull(message, "content");
        if (content == null) return;

        for (JsonElement item : content) {
            if (item.isJsonObject()) {
                parseAssistantContentItem(item.getAsJsonObject(), hash, out);
            }
        }
    }

    private static void parseAssistantContentItem(JsonObject contentItem, String hash, List<AgentMessage> out) {
        String contentType = getStringOrNull(contentItem, "type");
        if (contentType == null) return;

        switch (contentType) {
            case "text" -> {
                String text = getStringOrNull(contentItem, "text");
                if (text != null && !text.isBlank()) {
                    out.add(build(AgentType.INTERMEDIATE_RESPONSE, "assistant/text", null, text.trim(), hash));
                }
            }
            case "tool_use" -> {
                String toolName = getStringOrNull(contentItem, "name");
                JsonElement input = contentItem.get("input");
                String arguments = (input != null && !input.isJsonNull()) ? input.toString() : "{}";
                out.add(build(AgentType.TOOL_REQUEST, toolName != null ? toolName : "unknown", arguments, null, hash));
            }
            default -> { /* other content types (e.g. image) — ignore */ }
        }
    }

    /**
     * Handles {@code "user"} events emitted by Claude CLI to represent tool results being
     * sent back to the model. Each {@code tool_result} content block in the message becomes
     * a {@link AgentType#TOOL_RESPONSE} message.
     *
     * <p>Example event structure:
     * <pre>{@code
     * {"type":"user","message":{"role":"user","content":[
     *   {"type":"tool_result","tool_use_id":"toolu_abc","content":"file contents here"}
     * ]}}
     * }</pre>
     */
    private static void parseUser(JsonObject obj, String hash, List<AgentMessage> out) {
        JsonObject message = getObjectOrNull(obj, "message");
        if (message == null) return;

        JsonArray content = getArrayOrNull(message, "content");
        if (content == null) return;

        for (JsonElement item : content) {
            if (!item.isJsonObject()) continue;
            JsonObject block = item.getAsJsonObject();
            if (!"tool_result".equals(getStringOrNull(block, "type"))) continue;

            String toolUseId = getStringOrNull(block, "tool_use_id");
            String result = extractContentText(block.get("content"));
            String toolName = toolUseId != null ? "tool_result/" + toolUseId : "tool_result";
            out.add(build(AgentType.TOOL_RESPONSE, toolName, null, result, hash));
        }
    }

    /**
     * Handles the legacy {@code "tool"} event (some older Claude CLI versions).
     * Content may be a plain string or an array of content blocks.
     */
    private static void parseTool(JsonObject obj, String hash, List<AgentMessage> out) {
        String toolUseId = getStringOrNull(obj, "tool_use_id");
        String result = extractContentText(obj.get("content"));
        // Use the tool_use_id to correlate with the request; fall back gracefully
        String toolName = toolUseId != null ? "tool/" + toolUseId : "tool/result";
        out.add(build(AgentType.TOOL_RESPONSE, toolName, null, result, hash));
    }

    /**
     * Extracts a plain-text string from a content element that may be:
     * <ul>
     *   <li>a primitive string</li>
     *   <li>an array of content blocks (each with a {@code "text"} or {@code "content"} field)</li>
     * </ul>
     */
    @NotNull
    private static String extractContentText(@Nullable JsonElement content) {
        if (content == null || content.isJsonNull()) return "";
        if (content.isJsonPrimitive()) return content.getAsString();
        if (content.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement item : content.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                JsonObject block = item.getAsJsonObject();
                // Try plain "text" field first, then nested "content" string
                String text = getStringOrNull(block, "text");
                if (text == null) text = getStringOrNull(block, "content");
                if (text != null) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(text);
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static void parseResult(JsonObject obj, String hash, List<AgentMessage> out) {
        String subtype = getStringOrNull(obj, "subtype");
        if ("error".equals(subtype)) {
            String error = getStringOrNull(obj, "error");
            out.add(build(AgentType.TOOL_ERROR, "result/error", null,
                    error != null ? error : "Unknown error", hash));
        } else {
            // success (or unknown subtype — treat as success)
            String result = getStringOrNull(obj, "result");
            Long durationMs = getLongOrNull(obj, "duration_ms");
            Double cost = getDoubleOrNull(obj, "total_cost_usd");

            StringBuilder summary = new StringBuilder("CLI task completed");
            if (result != null && !result.isBlank()) summary.append(": ").append(result.trim());
            if (durationMs != null) summary.append(" | duration: ").append(durationMs).append("ms");
            if (cost != null) summary.append(String.format(Locale.US, " | cost: $%.4f", cost));

            out.add(build(AgentType.INTERMEDIATE_RESPONSE, "result/success", null, summary.toString(), hash));
        }
    }

    // --- builder helper ---

    private static AgentMessage build(AgentType type, String toolName, String arguments,
                                      String result, String hash) {
        return AgentMessage.builder()
                .type(type)
                .toolName(toolName)
                .arguments(arguments)
                .result(result)
                .projectLocationHash(hash)
                .build();
    }

    // --- JSON access helpers ---

    @Nullable
    private static String getStringOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull() && el.isJsonPrimitive()) ? el.getAsString() : null;
    }

    @Nullable
    private static JsonObject getObjectOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
    }

    @Nullable
    private static JsonArray getArrayOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el != null && el.isJsonArray()) ? el.getAsJsonArray() : null;
    }

    @Nullable
    private static Long getLongOrNull(JsonObject obj, String key) {
        try {
            JsonElement el = obj.get(key);
            return (el != null && !el.isJsonNull()) ? el.getAsLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Double getDoubleOrNull(JsonObject obj, String key) {
        try {
            JsonElement el = obj.get(key);
            return (el != null && !el.isJsonNull()) ? el.getAsDouble() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
