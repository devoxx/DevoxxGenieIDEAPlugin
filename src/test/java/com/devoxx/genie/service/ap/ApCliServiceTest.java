package com.devoxx.genie.service.ap;

import com.devoxx.genie.model.ap.ApAgent;
import com.devoxx.genie.model.ap.ApRunEvent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApCliServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private DevoxxGenieStateService stateService;

    private ApCliService service;

    @BeforeEach
    void setUp() {
        service = new ApCliService();
    }

    // ---------- applyAuthEnv ----------

    @Test
    void applyAuthEnv_cachedLogin_setsNothing() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("CACHED_LOGIN");

            Map<String, String> env = new HashMap<>();
            service.applyAuthEnv(env);

            assertThat(env).isEmpty();
        }
    }

    @Test
    void applyAuthEnv_dockerDesktop_setsOnlyAuthModeEnv() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("DOCKER_DESKTOP");

            Map<String, String> env = new HashMap<>();
            service.applyAuthEnv(env);

            assertThat(env)
                    .containsEntry(ApCliService.ENV_AUTH_MODE, "docker-desktop")
                    .doesNotContainKey(ApCliService.ENV_ACCESS_TOKEN)
                    .doesNotContainKey(ApCliService.ENV_REFRESH_TOKEN);
        }
    }

    @Test
    void applyAuthEnv_manualTokens_setsAllThree() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("MANUAL_TOKENS");
            when(stateService.getApAccessToken()).thenReturn("access-123");
            when(stateService.getApRefreshToken()).thenReturn("refresh-456");

            Map<String, String> env = new HashMap<>();
            service.applyAuthEnv(env);

            assertThat(env)
                    .containsEntry(ApCliService.ENV_AUTH_MODE, "manual")
                    .containsEntry(ApCliService.ENV_ACCESS_TOKEN, "access-123")
                    .containsEntry(ApCliService.ENV_REFRESH_TOKEN, "refresh-456");
        }
    }

    @Test
    void applyAuthEnv_manualTokens_blankTokensSkipped() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("MANUAL_TOKENS");
            when(stateService.getApAccessToken()).thenReturn("");
            when(stateService.getApRefreshToken()).thenReturn("   ");

            Map<String, String> env = new HashMap<>();
            service.applyAuthEnv(env);

            assertThat(env)
                    .containsEntry(ApCliService.ENV_AUTH_MODE, "manual")
                    .doesNotContainKey(ApCliService.ENV_ACCESS_TOKEN)
                    .doesNotContainKey(ApCliService.ENV_REFRESH_TOKEN);
        }
    }

    @Test
    void applyAuthEnv_manualTokens_nullTokensSkipped() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("MANUAL_TOKENS");
            when(stateService.getApAccessToken()).thenReturn(null);
            when(stateService.getApRefreshToken()).thenReturn(null);

            Map<String, String> env = new HashMap<>();
            service.applyAuthEnv(env);

            assertThat(env)
                    .containsEntry(ApCliService.ENV_AUTH_MODE, "manual")
                    .doesNotContainKey(ApCliService.ENV_ACCESS_TOKEN)
                    .doesNotContainKey(ApCliService.ENV_REFRESH_TOKEN);
        }
    }

    @Test
    void applyAuthEnv_unknownAuthMode_fallsBackToCachedLogin() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("BOGUS_MODE");

            Map<String, String> env = new HashMap<>();
            service.applyAuthEnv(env);

            assertThat(env).isEmpty();
        }
    }

    @Test
    void applyAuthEnv_preExistingEnvEntries_arePreserved() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApAuthMode()).thenReturn("DOCKER_DESKTOP");

            Map<String, String> env = new HashMap<>();
            env.put("PATH", "/usr/bin");
            env.put("HOME", "/home/dev");
            service.applyAuthEnv(env);

            assertThat(env)
                    .containsEntry("PATH", "/usr/bin")
                    .containsEntry("HOME", "/home/dev")
                    .containsEntry(ApCliService.ENV_AUTH_MODE, "docker-desktop");
        }
    }

    // ---------- buildCommand ----------

    @Test
    void buildCommand_withoutWorkingDir_isJustBinaryAndArgs() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApCliPath()).thenReturn("/usr/local/bin/ap");

            List<String> cmd = service.buildCommand(List.of("agent", "ls", "--json"), null);

            assertThat(cmd).containsExactly("/usr/local/bin/ap", "agent", "ls", "--json");
        }
    }

    @Test
    void buildCommand_withWorkingDir_insertsWorkingDirFlagBeforeArgs() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApCliPath()).thenReturn("/usr/local/bin/ap");

            List<String> cmd = service.buildCommand(List.of("run", "do the thing"), "/repo");

            assertThat(cmd).containsExactly("/usr/local/bin/ap", "--working-dir", "/repo", "run", "do the thing");
        }
    }

    @Test
    void buildCommand_blankWorkingDir_treatedAsAbsent() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApCliPath()).thenReturn("/usr/local/bin/ap");

            List<String> cmd = service.buildCommand(List.of("version"), "   ");

            assertThat(cmd).containsExactly("/usr/local/bin/ap", "version");
        }
    }

    @Test
    void buildCommand_missingBinaryPath_throwsIllegalState() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApCliPath()).thenReturn(null);

            assertThatThrownBy(() -> service.buildCommand(List.of("version"), null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ap CLI binary path");
        }
    }

    @Test
    void buildCommand_blankBinaryPath_throwsIllegalState() {
        try (MockedStatic<DevoxxGenieStateService> stateMock = mockStatic(DevoxxGenieStateService.class)) {
            stateMock.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);
            when(stateService.getApCliPath()).thenReturn("   ");

            assertThatThrownBy(() -> service.buildCommand(List.of("version"), null))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ---------- parseList ----------

    @Test
    void parseList_blankInput_returnsEmpty() throws Exception {
        List<ApAgent> result = service.parseList("", new TypeReference<>() {});
        assertThat(result).isEmpty();
    }

    @Test
    void parseList_whitespaceOnlyInput_returnsEmpty() throws Exception {
        List<ApAgent> result = service.parseList("   \n  ", new TypeReference<>() {});
        assertThat(result).isEmpty();
    }

    @Test
    void parseList_validJsonArray_deserializes() throws Exception {
        String json = "[{\"id\":\"a1\",\"name\":\"Agent One\",\"description\":\"first\"}," +
                "{\"id\":\"a2\",\"name\":\"Agent Two\",\"description\":\"second\"}]";

        List<ApAgent> result = service.parseList(json, new TypeReference<>() {});

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("a1");
        assertThat(result.get(0).name()).isEqualTo("Agent One");
        assertThat(result.get(1).id()).isEqualTo("a2");
    }

    @Test
    void parseList_unknownFields_areIgnored() throws Exception {
        String json = "[{\"id\":\"a1\",\"name\":\"One\",\"description\":\"x\",\"createdAt\":\"2026-01-01\"}]";

        List<ApAgent> result = service.parseList(json, new TypeReference<>() {});

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("a1");
    }

    @Test
    void parseList_jsonNullLiteral_returnsEmpty() throws Exception {
        // Jackson maps the JSON literal `null` to a Java null list — the helper should
        // normalise that to an empty list so callers never have to null-check.
        List<ApAgent> result = service.parseList("null", new TypeReference<>() {});
        assertThat(result).isEmpty();
    }

    @Test
    void parseList_malformedJson_wrapsInApCliException() {
        assertThatThrownBy(() ->
                service.parseList("not-json-at-all", new TypeReference<List<ApAgent>>() {}))
                .isInstanceOf(ApCliException.class)
                .hasMessageContaining("Failed to parse ap JSON");
    }

    // ---------- parseEvent ----------

    @Test
    void parseEvent_agentOutput_parsesContentAgentAndReasoning() throws Exception {
        String raw = "{\"type\":\"agent_output\",\"agent\":\"alpha\",\"content\":\"hello\",\"reasoning\":true}";
        JsonNode node = MAPPER.readTree(raw);

        ApRunEvent event = service.parseEvent(node, raw);

        assertThat(event).isInstanceOf(ApRunEvent.AgentOutput.class);
        ApRunEvent.AgentOutput output = (ApRunEvent.AgentOutput) event;
        assertThat(output.agent()).isEqualTo("alpha");
        assertThat(output.content()).isEqualTo("hello");
        assertThat(output.reasoning()).isTrue();
    }

    @Test
    void parseEvent_agentOutput_reasoningDefaultsToFalse() throws Exception {
        String raw = "{\"type\":\"agent_output\",\"agent\":\"beta\",\"content\":\"data\"}";
        JsonNode node = MAPPER.readTree(raw);

        ApRunEvent event = service.parseEvent(node, raw);

        assertThat(event).isInstanceOf(ApRunEvent.AgentOutput.class);
        assertThat(((ApRunEvent.AgentOutput) event).reasoning()).isFalse();
    }

    @Test
    void parseEvent_streamStarted_parsesAgent() throws Exception {
        String raw = "{\"type\":\"stream_started\",\"agent\":\"alpha\"}";
        JsonNode node = MAPPER.readTree(raw);

        ApRunEvent event = service.parseEvent(node, raw);

        assertThat(event).isInstanceOf(ApRunEvent.StreamStarted.class);
        assertThat(((ApRunEvent.StreamStarted) event).agent()).isEqualTo("alpha");
    }

    @Test
    void parseEvent_streamStopped_returnsStreamStopped() throws Exception {
        String raw = "{\"type\":\"stream_stopped\"}";
        JsonNode node = MAPPER.readTree(raw);

        ApRunEvent event = service.parseEvent(node, raw);

        assertThat(event).isInstanceOf(ApRunEvent.StreamStopped.class);
    }

    @Test
    void parseEvent_unknownType_wrappedInOther() throws Exception {
        String raw = "{\"type\":\"token_usage\",\"input\":42,\"output\":17}";
        JsonNode node = MAPPER.readTree(raw);

        ApRunEvent event = service.parseEvent(node, raw);

        assertThat(event).isInstanceOf(ApRunEvent.Other.class);
        ApRunEvent.Other other = (ApRunEvent.Other) event;
        assertThat(other.type()).isEqualTo("token_usage");
        assertThat(other.rawJson()).isEqualTo(raw);
    }

    @Test
    void parseEvent_missingType_wrappedInOtherWithEmptyType() throws Exception {
        String raw = "{\"foo\":\"bar\"}";
        JsonNode node = MAPPER.readTree(raw);

        ApRunEvent event = service.parseEvent(node, raw);

        assertThat(event).isInstanceOf(ApRunEvent.Other.class);
        assertThat(((ApRunEvent.Other) event).type()).isEmpty();
    }

    // ---------- TestResult / RunCompletion record helpers ----------

    @Test
    void testResult_successFactory_setsOkTrue() {
        ApCliService.TestResult r = ApCliService.TestResult.success("all good");
        assertThat(r.ok()).isTrue();
        assertThat(r.message()).isEqualTo("all good");
    }

    @Test
    void testResult_failureFactory_setsOkFalse() {
        ApCliService.TestResult r = ApCliService.TestResult.failure("nope");
        assertThat(r.ok()).isFalse();
        assertThat(r.message()).isEqualTo("nope");
    }

    @Test
    void runCompletion_successFactory_hasNoError() {
        ApCliService.RunCompletion r = ApCliService.RunCompletion.success();
        assertThat(r.ok()).isTrue();
        assertThat(r.error()).isNull();
    }

    @Test
    void runCompletion_failureFactory_carriesError() {
        ApCliService.RunCompletion r = ApCliService.RunCompletion.failure("exit 1");
        assertThat(r.ok()).isFalse();
        assertThat(r.error()).isEqualTo("exit 1");
    }
}
