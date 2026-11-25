package com.devoxx.genie.service.trace.hf;

import com.devoxx.genie.model.conversation.ChatMessage;
import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.debug.SecretRedactor.TraceRedactionOptions;
import com.devoxx.genie.service.trace.StoredTraceEvent;
import com.devoxx.genie.service.trace.TraceEvent;
import com.devoxx.genie.service.trace.TraceScanStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StsConverterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final StsConverter.Options OPTIONS =
            new StsConverter.Options(TraceRedactionOptions.SECRETS_ONLY, 0, "my-project");

    private static Conversation conversation() {
        Conversation c = new Conversation();
        c.setId("1764");
        c.setTitle("Fix the failing test");
        c.setTimestamp("2026-11-25T10:00:00");
        c.setModelName("claude-x");
        c.setLlmProvider("Anthropic");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(true, "Fix the failing test", null));
        messages.add(new ChatMessage(false, "Fixed.", null));
        c.setMessages(messages);
        return c;
    }

    private static StoredTraceEvent row(TraceEvent e) {
        return new StoredTraceEvent(0, "1764", "turn", e, TraceScanStatus.CLEAN);
    }

    private static List<JsonNode> lines(String jsonl) throws Exception {
        List<JsonNode> out = new ArrayList<>();
        for (String line : jsonl.split("\n")) {
            out.add(JSON.readTree(line));
        }
        return out;
    }

    @Test
    void agentTrace_becomesHeaderPlusUserAssistantToolMessages() throws Exception {
        List<StoredTraceEvent> events = List.of(
                row(TraceEvent.user("Fix the failing test", null, 1)),
                row(TraceEvent.assistant(null, "Read the file first",
                        List.of(new TraceEvent.ToolCall("call_1", "read_file", "{\"path\":\"A.java\"}")), "claude-x", null, 2)),
                row(TraceEvent.tool("call_1", "read_file", "class A {}", false, null, 3)),
                row(TraceEvent.tool("call_2", "run_command", "Error: boom", true, null, 4)),
                row(TraceEvent.assistant("Fixed.", null, List.of(), "claude-x", null, 5)));

        StsConverter.Result result = StsConverter.convert(conversation(), events, OPTIONS);
        List<JsonNode> lines = lines(result.jsonl());

        assertThat(result.fromTraceEvents()).isTrue();
        JsonNode header = lines.get(0);
        assertThat(header.get("type").asText()).isEqualTo("session");
        assertThat(header.get("harness").asText()).isEqualTo("devoxxgenie");
        assertThat(header.get("id").asText()).isEqualTo("1764");
        assertThat(header.get("name").asText()).isEqualTo("Fix the failing test");
        assertThat(header.get("mode").asText()).isEqualTo("agent");
        assertThat(header.get("models").get(0).asText()).isEqualTo("claude-x");
        assertThat(header.get("workspace").asText()).isEqualTo("my-project");

        assertThat(lines).hasSize(6);
        JsonNode assistant = lines.get(2).get("message");
        assertThat(assistant.get("role").asText()).isEqualTo("assistant");
        assertThat(assistant.get("content").asText()).isEmpty();
        assertThat(assistant.get("reasoningContent").asText()).isEqualTo("Read the file first");
        JsonNode call = assistant.get("toolCalls").get(0);
        assertThat(call.get("id").asText()).isEqualTo("call_1");
        assertThat(call.get("type").asText()).isEqualTo("function");
        assertThat(call.get("function").get("name").asText()).isEqualTo("read_file");
        assertThat(call.get("function").get("arguments").asText()).isEqualTo("{\"path\":\"A.java\"}");

        JsonNode tool = lines.get(3).get("message");
        assertThat(tool.get("role").asText()).isEqualTo("tool");
        assertThat(tool.get("toolCallId").asText()).isEqualTo("call_1");
        assertThat(tool.get("name").asText()).isEqualTo("read_file");
        assertThat(tool.get("content").asText()).isEqualTo("class A {}");
        assertThat(tool.has("isError")).isFalse();
        assertThat(lines.get(4).get("message").get("isError").asBoolean()).isTrue();
        assertThat(lines.get(5).get("message").get("timestamp").asLong()).isEqualTo(5);
    }

    @Test
    void subAgentSteps_areLeftOut() throws Exception {
        List<StoredTraceEvent> events = List.of(
                row(TraceEvent.user("explore", null, 1)),
                row(TraceEvent.user("Investigate X", "sub-agent-1", 2)),
                row(TraceEvent.assistant("found", null, List.of(), "m", "sub-agent-1", 3)),
                row(TraceEvent.assistant("Done", null, List.of(), "m", null, 4)));

        List<JsonNode> lines = lines(StsConverter.convert(conversation(), events, OPTIONS).jsonl());

        assertThat(lines).hasSize(3);
        assertThat(lines.get(1).get("message").get("content").asText()).isEqualTo("explore");
        assertThat(lines.get(2).get("message").get("content").asText()).isEqualTo("Done");
    }

    @Test
    void withoutTraceEvents_exportsPlainChatPairs() throws Exception {
        StsConverter.Result result = StsConverter.convert(conversation(), List.of(), OPTIONS);
        List<JsonNode> lines = lines(result.jsonl());

        assertThat(result.fromTraceEvents()).isFalse();
        assertThat(lines.get(0).get("mode").asText()).isEqualTo("chat");
        assertThat(lines.get(0).get("models").get(0).asText()).isEqualTo("claude-x");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1).get("message").get("role").asText()).isEqualTo("user");
        assertThat(lines.get(2).get("message").get("role").asText()).isEqualTo("assistant");
    }

    @Test
    void longToolResults_areTruncated_andTextIsRedactedAgain() throws Exception {
        StsConverter.Options options = new StsConverter.Options(TraceRedactionOptions.SECRETS_ONLY, 4, null);
        List<StoredTraceEvent> events = List.of(
                row(TraceEvent.user("key AKIA" + "ABCDEFGHIJKLMNOP", null, 1)),
                row(TraceEvent.tool("c", "read_file", "0123456789", false, null, 2)));

        List<JsonNode> lines = lines(StsConverter.convert(conversation(), events, options).jsonl());

        assertThat(lines.get(0).has("workspace")).isFalse();
        assertThat(lines.get(1).get("message").get("content").asText()).isEqualTo("key [REDACTED_AWS_ACCESS_KEY]");
        assertThat(lines.get(2).get("message").get("content").asText()).isEqualTo("0123\n…[truncated 6 characters]");
    }

    @Test
    void everyLine_isASingleLineOfJson() {
        List<StoredTraceEvent> events = List.of(row(TraceEvent.user("multi\nline\nprompt", null, 1)));
        String jsonl = StsConverter.convert(conversation(), events, OPTIONS).jsonl();
        assertThat(jsonl.split("\n")).hasSize(2);
        assertThat(jsonl).endsWith("\n");
    }
}
