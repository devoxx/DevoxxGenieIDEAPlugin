package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.debug.SecretRedactor;
import com.devoxx.genie.service.debug.SecretRedactor.TraceRedactionOptions;
import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Converts a stored conversation into Hugging Face's Session Trace Simple Format (STS) JSONL,
 * which the Hub's agent trace viewer renders (task-280; port of copilot-traces-hf's {@code converter.ts}).
 * See https://huggingface.co/docs/hub/en/session-traces-format.
 * <pre>
 * {"type":"session","harness":"devoxxgenie","id":"…","name":"…",…}
 * {"type":"message","message":{"role":"user","content":"…"}}
 * {"type":"message","message":{"role":"assistant","content":"","toolCalls":[{"id":"t1","type":"function","function":{"name":"x","arguments":"{}"}}]}}
 * {"type":"message","message":{"role":"tool","toolCallId":"t1","name":"x","content":"…"}}
 * </pre>
 * With recorded trace events (task-279) the full agent loop is exported; otherwise the plain
 * prompt/answer pairs from {@code chat_messages}. Sub-agent steps are left out — STS has no
 * nested agents — but their findings are in the parent's {@code parallel_explore} tool result.
 * Every string is redacted again on the way out.
 */
public final class StsConverter {

    public static final String HARNESS = "devoxxgenie";
    static final String AGENT = "DevoxxGenie";
    static final String SOURCE = "intellij-plugin";

    private static final ObjectMapper JSON = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * @param maxToolResultChars tool results longer than this are cut with a marker (0 = no limit)
     * @param projectName        written to the header when not {@code null}
     */
    public record Options(@NotNull TraceRedactionOptions redaction, int maxToolResultChars, @Nullable String projectName) {}

    /** The converted trace: the JSONL text and whether it came from recorded trace events. */
    public record Result(@NotNull String jsonl, boolean fromTraceEvents, int messageCount) {}

    private StsConverter() {}

    public static @NotNull Result convert(@NotNull Conversation conversation, @NotNull List<StoredTraceEvent> events,
                                          @NotNull Options options) {
        List<TraceEvent> mainAgent = events.stream()
                .map(StoredTraceEvent::event)
                .filter(e -> e.subAgentId() == null)
                .toList();
        boolean fromTrace = !mainAgent.isEmpty();

        List<Map<String, Object>> messages = fromTrace
                ? fromTraceEvents(mainAgent, options)
                : fromChatMessages(conversation.getMessages(), options);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("type", "session");
        header.put("harness", HARNESS);
        header.put("id", conversation.getId());
        header.put("name", redact(conversation.getTitle(), options));
        header.put("agent", AGENT);
        header.put("source", SOURCE);
        header.put("createdAt", conversation.getTimestamp());
        header.put("models", models(conversation, mainAgent));
        header.put("mode", fromTrace ? "agent" : "chat");
        if (conversation.getLlmProvider() != null && !conversation.getLlmProvider().isBlank()) {
            header.put("provider", conversation.getLlmProvider());
        }
        if (options.projectName() != null && !options.projectName().isBlank()) {
            header.put("workspace", options.projectName());
        }

        StringBuilder out = new StringBuilder(write(header)).append('\n');
        for (Map<String, Object> m : messages) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("type", "message");
            line.put("message", m);
            out.append(write(line)).append('\n');
        }
        return new Result(out.toString(), fromTrace, messages.size());
    }

    private static List<Map<String, Object>> fromTraceEvents(List<TraceEvent> events, Options options) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (TraceEvent e : events) {
            Map<String, Object> m = new LinkedHashMap<>();
            switch (e.role()) {
                case USER -> {
                    m.put("role", "user");
                    m.put("content", text(e.content(), options));
                }
                case ASSISTANT -> {
                    m.put("role", "assistant");
                    m.put("content", text(e.content(), options));
                    if (e.reasoning() != null && !e.reasoning().isBlank()) {
                        m.put("reasoningContent", redact(e.reasoning(), options));
                    }
                    if (!e.toolCalls().isEmpty()) {
                        List<Map<String, Object>> calls = new ArrayList<>();
                        for (TraceEvent.ToolCall c : e.toolCalls()) {
                            Map<String, Object> call = new LinkedHashMap<>();
                            call.put("id", c.id());
                            call.put("type", "function");
                            Map<String, Object> function = new LinkedHashMap<>();
                            function.put("name", c.name());
                            function.put("arguments", c.arguments() == null || c.arguments().isBlank()
                                    ? "{}" : redact(c.arguments(), options));
                            call.put("function", function);
                            calls.add(call);
                        }
                        m.put("toolCalls", calls);
                    }
                    m.put("model", e.model());
                }
                case TOOL -> {
                    m.put("role", "tool");
                    m.put("toolCallId", e.toolCallId());
                    m.put("name", e.toolName());
                    m.put("content", truncate(text(e.content(), options), options.maxToolResultChars()));
                    if (e.error()) {
                        m.put("isError", true);
                    }
                }
            }
            if (e.timestampMs() > 0) {
                m.put("timestamp", e.timestampMs());
            }
            messages.add(m);
        }
        return messages;
    }

    private static List<Map<String, Object>> fromChatMessages(@Nullable List<ChatMessage> chat, Options options) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (chat == null) {
            return messages;
        }
        for (ChatMessage c : chat) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", c.isUser() ? "user" : "assistant");
            m.put("content", text(c.getContent(), options));
            messages.add(m);
        }
        return messages;
    }

    private static List<String> models(Conversation conversation, List<TraceEvent> events) {
        TreeSet<String> models = new TreeSet<>();
        events.stream().map(TraceEvent::model).filter(Objects::nonNull).filter(s -> !s.isBlank()).forEach(models::add);
        if (models.isEmpty() && conversation.getModelName() != null && !conversation.getModelName().isBlank()
                && !"None".equals(conversation.getModelName())) {
            models.add(conversation.getModelName());
        }
        return List.copyOf(models);
    }

    private static @NotNull String text(@Nullable String s, Options options) {
        return s == null ? "" : Objects.requireNonNullElse(redact(s, options), "");
    }

    private static @Nullable String redact(@Nullable String s, Options options) {
        return SecretRedactor.redactForTrace(s, options.redaction()).text();
    }

    static @NotNull String truncate(@NotNull String s, int maxChars) {
        if (maxChars <= 0 || s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "\n…[truncated " + (s.length() - maxChars) + " characters]";
    }

    private static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize STS line", e);
        }
    }
}
