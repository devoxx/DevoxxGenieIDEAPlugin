package com.devoxx.genie.service.cli;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudeStreamJsonParserTest {

    private static final String PROJECT_HASH = "test-project-hash";

    // ── non-JSON lines ──────────────────────────────────────────────────────

    @Test
    void parse_emptyLine_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("", PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_plainTextLine_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("some plain text output", PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_ansiLine_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("\u001B[32mGreen text\u001B[0m", PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_invalidJson_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("{not valid json", PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_jsonWithoutTypeField_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("{\"foo\":\"bar\"}", PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_unknownEventType_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("{\"type\":\"unknown_event\"}", PROJECT_HASH)).isEmpty();
    }

    // ── system/init ─────────────────────────────────────────────────────────

    @Test
    void parse_systemInit_returnsIntermediateResponse() {
        String line = """
                {"type":"system","subtype":"init","cwd":"/project","session_id":"sess123",\
                "model":"claude-opus-4-5","permissionMode":"bypassPermissions"}""";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(msg.getToolName()).isEqualTo("system/init");
        assertThat(msg.getResult()).contains("CLI session started");
        assertThat(msg.getResult()).contains("claude-opus-4-5");
        assertThat(msg.getResult()).contains("sess123");
        assertThat(msg.getProjectLocationHash()).isEqualTo(PROJECT_HASH);
    }

    @Test
    void parse_systemInitMissingModel_summaryStillWorks() {
        String line = "{\"type\":\"system\",\"subtype\":\"init\",\"session_id\":\"s1\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getResult()).startsWith("CLI session started");
    }

    @Test
    void parse_systemNonInit_returnsEmpty() {
        String line = "{\"type\":\"system\",\"subtype\":\"something_else\"}";
        assertThat(ClaudeStreamJsonParser.parse(line, PROJECT_HASH)).isEmpty();
    }

    // ── assistant text ───────────────────────────────────────────────────────

    @Test
    void parse_assistantTextContent_returnsIntermediateResponse() {
        String line = """
                {"type":"assistant","message":{"id":"msg1","type":"message","role":"assistant",\
                "content":[{"type":"text","text":"I'll help you with that."}],\
                "model":"claude-opus-4-5","stop_reason":"end_turn"}}""";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(msg.getToolName()).isEqualTo("assistant/text");
        assertThat(msg.getResult()).isEqualTo("I'll help you with that.");
        assertThat(msg.getArguments()).isNull();
    }

    @Test
    void parse_assistantBlankText_returnsEmpty() {
        String line = """
                {"type":"assistant","message":{"content":[{"type":"text","text":"   "}]}}""";
        assertThat(ClaudeStreamJsonParser.parse(line, PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_assistantToolUse_returnsToolRequest() {
        String line = """
                {"type":"assistant","message":{"id":"msg2","type":"message","role":"assistant",\
                "content":[{"type":"tool_use","id":"toolu_abc","name":"Bash",\
                "input":{"command":"ls -la"}}],\
                "model":"claude-opus-4-5","stop_reason":"tool_use"}}""";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.TOOL_REQUEST);
        assertThat(msg.getToolName()).isEqualTo("Bash");
        assertThat(msg.getArguments()).contains("ls -la");
        assertThat(msg.getResult()).isNull();
    }

    @Test
    void parse_assistantToolUseNoName_usesUnknown() {
        String line = """
                {"type":"assistant","message":{"content":[{"type":"tool_use","input":{}}]}}""";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getToolName()).isEqualTo("unknown");
    }

    @Test
    void parse_assistantMixedContent_returnsMultipleMessages() {
        String line = """
                {"type":"assistant","message":{"content":[\
                {"type":"text","text":"Let me run a command."},\
                {"type":"tool_use","name":"Bash","input":{"command":"pwd"}}\
                ]}}""";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
        assertThat(result.get().get(0).getType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(result.get().get(1).getType()).isEqualTo(AgentType.TOOL_REQUEST);
    }

    @Test
    void parse_assistantNoMessageField_returnsEmpty() {
        String line = "{\"type\":\"assistant\"}";
        assertThat(ClaudeStreamJsonParser.parse(line, PROJECT_HASH)).isEmpty();
    }

    // ── tool result ──────────────────────────────────────────────────────────

    @Test
    void parse_toolResult_returnsToolResponse() {
        String line = "{\"type\":\"tool\",\"tool_use_id\":\"toolu_abc\",\"content\":\"file1.txt\\nfile2.txt\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.TOOL_RESPONSE);
        assertThat(msg.getToolName()).isEqualTo("tool/toolu_abc");
        assertThat(msg.getResult()).isEqualTo("file1.txt\nfile2.txt");
    }

    @Test
    void parse_toolResultNoToolUseId_fallbackName() {
        String line = "{\"type\":\"tool\",\"content\":\"output\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getToolName()).isEqualTo("tool/result");
    }

    // ── result/success ───────────────────────────────────────────────────────

    @Test
    void parse_resultSuccess_returnsIntermediateResponse() {
        String line = """
                {"type":"result","subtype":"success","duration_ms":12345,\
                "total_cost_usd":0.0512,"result":"Task completed","session_id":"sess1"}""";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.INTERMEDIATE_RESPONSE);
        assertThat(msg.getToolName()).isEqualTo("result/success");
        assertThat(msg.getResult()).contains("CLI task completed");
        assertThat(msg.getResult()).contains("Task completed");
        assertThat(msg.getResult()).contains("12345ms");
        assertThat(msg.getResult()).contains("$0.0512");
    }

    @Test
    void parse_resultSuccessNoCost_summaryOmitsCost() {
        String line = "{\"type\":\"result\",\"subtype\":\"success\",\"result\":\"done\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getResult()).doesNotContain("cost");
    }

    // ── result/error ─────────────────────────────────────────────────────────

    @Test
    void parse_resultError_returnsToolError() {
        String line = "{\"type\":\"result\",\"subtype\":\"error\",\"error\":\"Permission denied\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.TOOL_ERROR);
        assertThat(msg.getToolName()).isEqualTo("result/error");
        assertThat(msg.getResult()).isEqualTo("Permission denied");
    }

    @Test
    void parse_resultErrorMissingErrorField_returnsUnknownError() {
        String line = "{\"type\":\"result\",\"subtype\":\"error\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getResult()).isEqualTo("Unknown error");
    }

    // ── project location hash ────────────────────────────────────────────────

    @Test
    void parse_nullProjectHash_hashIsNull() {
        String line = "{\"type\":\"system\",\"subtype\":\"init\",\"model\":\"m1\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, null);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getProjectLocationHash()).isNull();
    }

    @Test
    void parse_projectHashPropagated() {
        String line = "{\"type\":\"system\",\"subtype\":\"init\",\"model\":\"m1\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, "my-hash");

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getProjectLocationHash()).isEqualTo("my-hash");
    }

    // ── user events (tool results) ───────────────────────────────────────────

    @Test
    void parse_userWithToolResult_returnsToolResponse() {
        String line = """
                {"type":"user","message":{"role":"user","content":[
                  {"type":"tool_result","tool_use_id":"toolu_abc","content":"file contents here"}
                ]}}""".replace("\n", "").replace("  ", "");

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.TOOL_RESPONSE);
        assertThat(msg.getToolName()).isEqualTo("tool_result/toolu_abc");
        assertThat(msg.getResult()).isEqualTo("file contents here");
    }

    @Test
    void parse_userWithToolResultArrayContent_returnsToolResponse() {
        String line = """
                {"type":"user","message":{"role":"user","content":[
                  {"type":"tool_result","tool_use_id":"toolu_xyz",
                   "content":[{"type":"text","text":"line1"},{"type":"text","text":"line2"}]}
                ]}}""".replace("\n", "").replace("  ", "");

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        AgentMessage msg = result.get().getFirst();
        assertThat(msg.getType()).isEqualTo(AgentType.TOOL_RESPONSE);
        assertThat(msg.getResult()).contains("line1");
        assertThat(msg.getResult()).contains("line2");
    }

    @Test
    void parse_userWithMultipleToolResults_returnsMultipleMessages() {
        String line = """
                {"type":"user","message":{"role":"user","content":[
                  {"type":"tool_result","tool_use_id":"t1","content":"result1"},
                  {"type":"tool_result","tool_use_id":"t2","content":"result2"}
                ]}}""".replace("\n", "").replace("  ", "");

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
        assertThat(result.get().get(0).getToolName()).isEqualTo("tool_result/t1");
        assertThat(result.get().get(1).getToolName()).isEqualTo("tool_result/t2");
    }

    @Test
    void parse_userWithNoToolResultBlocks_returnsEmpty() {
        String line = """
                {"type":"user","message":{"role":"user","content":[
                  {"type":"text","text":"hello"}
                ]}}""".replace("\n", "").replace("  ", "");

        assertThat(ClaudeStreamJsonParser.parse(line, PROJECT_HASH)).isEmpty();
    }

    @Test
    void parse_userWithNoMessage_returnsEmpty() {
        assertThat(ClaudeStreamJsonParser.parse("{\"type\":\"user\"}", PROJECT_HASH)).isEmpty();
    }

    // ── tool event (legacy format) ───────────────────────────────────────────

    @Test
    void parse_toolEventWithArrayContent_returnsToolResponse() {
        String line = """
                {"type":"tool","tool_use_id":"toolu_abc",
                 "content":[{"type":"text","text":"tool output"}]}
                """.replace("\n", "").replace("  ", "");

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getResult()).isEqualTo("tool output");
    }

    @Test
    void parse_toolEventWithEmptyContent_returnsToolResponseWithEmptyResult() {
        String line = "{\"type\":\"tool\",\"tool_use_id\":\"toolu_abc\",\"content\":\"\"}";

        Optional<List<AgentMessage>> result = ClaudeStreamJsonParser.parse(line, PROJECT_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getFirst().getResult()).isEmpty();
    }

    // ── stream-json flag constant ────────────────────────────────────────────

    @Test
    void streamJsonFlag_containsExpectedSubstring() {
        assertThat(ClaudeStreamJsonParser.STREAM_JSON_FLAG).isEqualTo("stream-json");
    }

    // ── extractHumanReadableText ─────────────────────────────────────────────

    @Test
    void extractHumanReadableText_systemInit_returnsNull() {
        String line = """
                {"type":"system","subtype":"init","cwd":"/project","session_id":"sess123",\
                "model":"claude-opus-4-5","tools":["Task","Bash"]}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }

    @Test
    void extractHumanReadableText_assistantText_returnsText() {
        String line = """
                {"type":"assistant","message":{"content":[{"type":"text","text":"Hello, I can help with that."}]}}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line))
                .isEqualTo("Hello, I can help with that.");
    }

    @Test
    void extractHumanReadableText_assistantToolUse_returnsNull() {
        String line = """
                {"type":"assistant","message":{"content":[{"type":"tool_use","name":"Read",\
                "input":{"file_path":"/some/file.java"}}]}}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }

    @Test
    void extractHumanReadableText_assistantMixedContent_returnsOnlyText() {
        String line = """
                {"type":"assistant","message":{"content":[\
                {"type":"text","text":"Let me read that file."},\
                {"type":"tool_use","name":"Read","input":{"file_path":"/foo.java"}}\
                ]}}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line))
                .isEqualTo("Let me read that file.");
    }

    @Test
    void extractHumanReadableText_assistantThinkingOnly_returnsNull() {
        String line = """
                {"type":"assistant","message":{"content":[{"type":"thinking","thinking":"Let me think..."}]}}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }

    @Test
    void extractHumanReadableText_userToolResult_returnsNull() {
        String line = """
                {"type":"user","message":{"role":"user","content":[
                  {"type":"tool_result","tool_use_id":"toolu_abc","content":"file contents"}
                ]}}""".replace("\n", "").replace("  ", "");
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }

    @Test
    void extractHumanReadableText_resultSuccess_returnsResultText() {
        String line = """
                {"type":"result","subtype":"success","result":"Task completed successfully.",\
                "duration_ms":5000,"total_cost_usd":0.03}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line))
                .isEqualTo("Task completed successfully.");
    }

    @Test
    void extractHumanReadableText_resultError_returnsError() {
        String line = "{\"type\":\"result\",\"subtype\":\"error\",\"error\":\"Something went wrong\"}";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line))
                .isEqualTo("Error: Something went wrong");
    }

    @Test
    void extractHumanReadableText_rateLimitEvent_returnsNull() {
        String line = "{\"type\":\"rate_limit_event\",\"rate_limit_info\":{\"status\":\"allowed\"}}";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }

    @Test
    void extractHumanReadableText_invalidJson_returnsLineAsIs() {
        String line = "{not valid json at all";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isEqualTo(line);
    }

    @Test
    void extractHumanReadableText_plainText_returnsLineAsIs() {
        // Non-JSON should not reach this method normally (caller checks for "{"),
        // but if it does, it should pass through
        String line = "some plain text";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isEqualTo(line);
    }

    @Test
    void extractHumanReadableText_assistantBlankText_returnsNull() {
        String line = """
                {"type":"assistant","message":{"content":[{"type":"text","text":"   "}]}}""";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }

    @Test
    void extractHumanReadableText_resultSuccessNoResult_returnsNull() {
        String line = "{\"type\":\"result\",\"subtype\":\"success\"}";
        assertThat(ClaudeStreamJsonParser.extractHumanReadableText(line)).isNull();
    }
}
