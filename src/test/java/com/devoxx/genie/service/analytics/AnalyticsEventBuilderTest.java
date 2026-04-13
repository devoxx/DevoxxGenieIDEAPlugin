package com.devoxx.genie.service.analytics;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnalyticsEventBuilder} (task-209, ACs #2, #8, #15).
 *
 * <p>These assert the central allowlist + shape enforcement that protects all downstream
 * analytics events from accidental PII leakage.
 */
class AnalyticsEventBuilderTest {

    private static final String CLIENT_ID = "test-client-id";

    private static Map<String, String> common() {
        Map<String, String> c = new LinkedHashMap<>();
        c.put("app_name", "devoxxgenie-intellij");
        c.put("app_version", "1.2.3");
        c.put("ide_version", "2024.1");
        c.put("session_id", "1234567890");
        return c;
    }

    @Test
    void buildsValidPromptExecutedPayload() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "claude-3-5-sonnet");

        String json = AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common());

        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\":\"prompt_executed\"");
        assertThat(json).contains("\"provider_id\":\"anthropic\"");
        assertThat(json).contains("\"model_name\":\"claude-3-5-sonnet\"");
        assertThat(json).contains("\"engagement_time_msec\":1");
    }

    @Test
    void buildsValidFeatureEnabledPayload() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("feature_id", "rag");

        String json = AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_FEATURE_ENABLED, ev, common());

        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\":\"feature_enabled\"");
        assertThat(json).contains("\"feature_id\":\"rag\"");
    }

    @Test
    void buildsValidFeatureUsedPayload() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("feature_id", "mcp");
        ev.put("provider_type", "local");
        ev.put("tool_call_count", "2-5");

        String json = AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_FEATURE_USED, ev, common());

        assertThat(json).isNotNull();
        assertThat(json).contains("\"feature_id\":\"mcp\"");
        assertThat(json).contains("\"provider_type\":\"local\"");
        assertThat(json).contains("\"tool_call_count\":\"2-5\"");
    }

    @Test
    void unknownEventNameIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "claude");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, "made_up_event", ev, common())).isNull();
    }

    @Test
    void unknownParamKeyIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "claude");
        ev.put("prompt_text", "hello world"); // leakage attempt
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void disallowedEnumValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("feature_id", "not_a_real_feature");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_FEATURE_ENABLED, ev, common())).isNull();
    }

    @Test
    void disallowedBucketValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("feature_id", "agent");
        ev.put("provider_type", "cloud");
        ev.put("tool_call_count", "42"); // raw int — not allowed
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_FEATURE_USED, ev, common())).isNull();
    }

    @Test
    void disallowedProviderTypeIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("feature_id", "agent");
        ev.put("provider_type", "optional"); // collapsed into "cloud" upstream; schema forbids this literal
        ev.put("tool_call_count", "1");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_FEATURE_USED, ev, common())).isNull();
    }

    @Test
    void absolutePathShapedValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "/Users/stephan/secret");
        ev.put("model_name", "claude");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void windowsPathShapedValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "\\\\server\\share\\leak");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void windowsDriveLetterBackslashPathIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "C:\\Users\\me\\project");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void windowsDriveLetterForwardSlashPathIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "D:/Users/me/project");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void lowercaseWindowsDriveLetterIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "z:\\leak");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void urlShapedValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "https://evil.example.com/");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void newlineInValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "line1\nline2");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void overlyLongValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "x".repeat(200));
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void emptyValueIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "");
        ev.put("model_name", "claude");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common())).isNull();
    }

    @Test
    void unknownCommonParamKeyIsRejected() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "claude");
        Map<String, String> badCommon = common();
        badCommon.put("user_email", "leak@example.com");
        assertThat(AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, badCommon)).isNull();
    }

    @Test
    void payloadContainsNoForbiddenSubstringsForTypicalInputs() {
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "anthropic");
        ev.put("model_name", "claude-3-5-sonnet");
        String json = AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common());

        assertThat(json).isNotNull();
        assertThat(json).doesNotContain("/Users/");
        assertThat(json).doesNotContain("file:");
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("apiKey");
    }

    @Test
    void huggingFaceStyleModelNameWithMidSlashIsAccepted() {
        // Model names like "meta-llama/Llama-3.1-8B" must still go through — only leading slash / URL forms are blocked.
        Map<String, String> ev = new LinkedHashMap<>();
        ev.put("provider_id", "ollama");
        ev.put("model_name", "meta-llama/Llama-3.1-8B");
        String json = AnalyticsEventBuilder.build(CLIENT_ID, AnalyticsService.EVENT_PROMPT_EXECUTED, ev, common());
        assertThat(json).isNotNull();
        assertThat(json).contains("meta-llama/Llama-3.1-8B");
    }
}
