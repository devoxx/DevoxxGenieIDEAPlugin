package com.devoxx.genie.service.analytics;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Generic GA4 payload builder with strict per-event parameter allowlists (task-209, AC #2, #8, #15).
 *
 * <p>All analytics events flow through this single class. It enforces:
 * <ul>
 *   <li><strong>Closed per-event allowlist</strong> — unknown event-specific params are rejected
 *       and the whole payload is dropped. No "pass through unknown keys" path exists.</li>
 *   <li><strong>Closed enum value allowlist</strong> — params like {@code feature_id} /
 *       {@code provider_type} / bucketed counts may only take values this class knows about.</li>
 *   <li><strong>Shape rejection</strong> — values that look like absolute paths, URLs, or
 *       multi-line strings are rejected defensively, even for free-form params like
 *       {@code provider_id} and {@code model_name}.</li>
 *   <li><strong>Length cap</strong> — any param value over 128 chars is rejected.</li>
 * </ul>
 *
 * <p>On rejection, {@link #build} returns {@code null} and logs at debug level. Callers treat
 * {@code null} as "do not send" — consistent with the fire-and-forget privacy default.
 */
@Slf4j
public final class AnalyticsEventBuilder {

    /** Common params attached to every event. */
    static final Set<String> COMMON_PARAM_KEYS = Set.of(
            "app_name", "app_version", "ide_version", "session_id");

    /** Closed per-event allowlists of event-specific param keys. Common params are always allowed. */
    static final Map<String, Set<String>> EVENT_ALLOWLIST = Map.of(
            AnalyticsService.EVENT_PROMPT_EXECUTED, Set.of("provider_id", "model_name"),
            AnalyticsService.EVENT_MODEL_SELECTED,  Set.of("provider_id", "model_name"),
            AnalyticsService.EVENT_FEATURE_ENABLED, Set.of("feature_id"),
            AnalyticsService.EVENT_FEATURE_USED,    Set.of("feature_id", "provider_type", "tool_call_count"),
            AnalyticsService.EVENT_FEATURE_COUNTS,  Set.of("mcp_server_count", "custom_prompt_count", "chat_memory_bucket")
    );

    /** Closed enum-value allowlists keyed by param name. Params absent here are free-form (subject to shape/length checks). */
    static final Map<String, Set<String>> ENUM_VALUE_ALLOWLIST = Map.of(
            "feature_id", Set.of(
                    "rag", "semantic_search", "web_search_google", "web_search_tavily",
                    "agent", "mcp", "streaming", "project_context_full",
                    "project_context_selected", "devoxxgenie_md", "custom_prompt"),
            "provider_type", Set.of("local", "cloud", "none"),
            "tool_call_count", Set.of("0", "1", "2-5", "6-10", "11+"),
            "mcp_server_count", Set.of("0", "1", "2-5", "6-10", "11+"),
            "custom_prompt_count", Set.of("0", "1", "2-5", "6-10", "11+"),
            "chat_memory_bucket", Set.of("0", "1-5", "6-10", "11-20", "21+")
    );

    private static final int MAX_VALUE_LENGTH = 128;

    private AnalyticsEventBuilder() {
        // utility
    }

    /**
     * Builds a GA4 payload JSON string, or returns {@code null} if validation fails.
     *
     * @param clientId    stable anonymous client id
     * @param eventName   must be a known event (see {@link #EVENT_ALLOWLIST})
     * @param eventParams event-specific params; keys must be in the event's allowlist; values
     *                    subject to enum/shape/length checks
     * @param commonParams common params (app_name, app_version, ide_version, session_id)
     * @return payload JSON, or {@code null} if the caller must not send
     */
    @Nullable
    public static String build(@NotNull String clientId,
                               @NotNull String eventName,
                               @NotNull Map<String, String> eventParams,
                               @NotNull Map<String, String> commonParams) {

        Set<String> allowedEventKeys = EVENT_ALLOWLIST.get(eventName);
        if (allowedEventKeys == null) {
            log.debug("Analytics event rejected: unknown event name '{}'", eventName);
            return null;
        }

        if (!rejectShape(clientId)) {
            log.debug("Analytics event rejected: clientId failed shape check");
            return null;
        }

        // Validate event-specific params.
        for (Map.Entry<String, String> entry : eventParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (!allowedEventKeys.contains(key)) {
                log.debug("Analytics event '{}' rejected: unknown param '{}'", eventName, key);
                return null;
            }
            if (value == null || value.isEmpty()) {
                log.debug("Analytics event '{}' rejected: empty value for '{}'", eventName, key);
                return null;
            }
            if (!rejectShape(value)) {
                log.debug("Analytics event '{}' rejected: param '{}' failed shape/length check", eventName, key);
                return null;
            }
            Set<String> allowedValues = ENUM_VALUE_ALLOWLIST.get(key);
            if (allowedValues != null && !allowedValues.contains(value)) {
                log.debug("Analytics event '{}' rejected: param '{}' has disallowed value", eventName, key);
                return null;
            }
        }

        // Validate common params (keys must match the closed set; values pass shape check).
        for (Map.Entry<String, String> entry : commonParams.entrySet()) {
            if (!COMMON_PARAM_KEYS.contains(entry.getKey())) {
                log.debug("Analytics event '{}' rejected: unknown common param '{}'", eventName, entry.getKey());
                return null;
            }
            if (entry.getValue() == null || !rejectShape(entry.getValue())) {
                log.debug("Analytics event '{}' rejected: common param '{}' failed shape check", eventName, entry.getKey());
                return null;
            }
        }

        return encodeJson(clientId, eventName, eventParams, commonParams);
    }

    /**
     * Defensive shape filter: rejects values that look like absolute paths, URLs, or
     * multi-line text, or that exceed the max length. Applied to every string value.
     *
     * @return {@code true} if the value is acceptable, {@code false} otherwise
     */
    static boolean rejectShape(@NotNull String value) {
        if (value.length() > MAX_VALUE_LENGTH) return false;
        if (value.startsWith("/") || value.startsWith("\\")) return false;
        if (value.contains("://")) return false;
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return false;
        return true;
    }

    @NotNull
    private static String encodeJson(@NotNull String clientId,
                                     @NotNull String eventName,
                                     @NotNull Map<String, String> eventParams,
                                     @NotNull Map<String, String> commonParams) {
        StringBuilder sb = new StringBuilder(384);
        sb.append('{')
          .append("\"client_id\":\"").append(escape(clientId)).append("\",")
          .append("\"events\":[{")
          .append("\"name\":\"").append(escape(eventName)).append("\",")
          .append("\"params\":{");

        boolean first = true;
        // Event-specific params first (preserves existing test expectations for prompt_executed).
        for (Map.Entry<String, String> entry : eventParams.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(entry.getKey()).append("\":\"")
              .append(escape(entry.getValue())).append('"');
            first = false;
        }
        // Common params.
        for (Map.Entry<String, String> entry : commonParams.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(entry.getKey()).append("\":\"")
              .append(escape(entry.getValue())).append('"');
            first = false;
        }
        // engagement_time_msec is always an int literal (GA4 expectation).
        if (!first) sb.append(',');
        sb.append("\"engagement_time_msec\":1");

        sb.append("}}]}");
        return sb.toString();
    }

    @NotNull
    static String escape(@NotNull String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Convenience ordered-map factory for callers assembling event params. */
    @NotNull
    public static Map<String, String> params() {
        return new LinkedHashMap<>();
    }
}
