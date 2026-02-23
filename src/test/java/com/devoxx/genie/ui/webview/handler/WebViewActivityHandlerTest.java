package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.webview.JCEFChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for WebViewActivityHandler — the unified handler for both MCP and Agent activity display.
 * Uses a capturing jsExecutor stub to inspect the JavaScript that would be executed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebViewActivityHandlerTest {

    private WebViewActivityHandler handler;
    private MockedStatic<JCEFChecker> mockedJCEFChecker;
    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private DevoxxGenieStateService stateService;

    /** Captures JavaScript strings passed to executeJavaScript */
    private final List<String> capturedJs = new ArrayList<>();
    private WebViewJavaScriptExecutor jsExecutor;

    @BeforeEach
    void setUp() {
        mockedJCEFChecker = Mockito.mockStatic(JCEFChecker.class);
        mockedJCEFChecker.when(JCEFChecker::isJCEFAvailable).thenReturn(false);

        stateService = mock(DevoxxGenieStateService.class);
        when(stateService.getStreamMode()).thenReturn(true);
        when(stateService.getShowToolActivityInChat()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(stateService);

        // Create a real executor with null browser (JCEF not available), then spy to capture calls
        jsExecutor = spy(new WebViewJavaScriptExecutor(null));
        jsExecutor.setLoaded(true);

        // Capture all JS executions
        doAnswer(invocation -> {
            String script = invocation.getArgument(0);
            if (script != null) capturedJs.add(script);
            return null;
        }).when(jsExecutor).executeJavaScript(anyString());

        handler = new WebViewActivityHandler(jsExecutor);
        capturedJs.clear();
    }

    @AfterEach
    void tearDown() {
        if (jsExecutor != null) jsExecutor.dispose();
        if (mockedStateService != null) mockedStateService.close();
        if (mockedJCEFChecker != null) mockedJCEFChecker.close();
    }

    // ─── Lifecycle tests ────────────────────────────────────────

    @Test
    void deactivate_ignoresSubsequentMessages() {
        handler.setActiveMessageId("msg-1");
        handler.deactivate();

        handler.onActivityMessage(agentIntermediate("hello"));

        assertThat(capturedJs).isEmpty();
    }

    @Test
    void setActiveMessageId_reactivatesAfterDeactivate() {
        handler.setActiveMessageId("msg-1");
        handler.deactivate();
        handler.setActiveMessageId("msg-2");

        handler.onActivityMessage(agentIntermediate("hello"));

        assertThat(capturedJs).hasSize(1);
    }

    @Test
    void setActiveMessageId_clearsOldState() {
        handler.setActiveMessageId("msg-1");
        handler.onActivityMessage(agentIntermediate("first"));
        capturedJs.clear();

        handler.setActiveMessageId("msg-2");
        handler.onActivityMessage(agentIntermediate("second"));

        // Only "second" should be in the JS, not "first" (accumulators were cleared)
        assertThat(capturedJs).hasSize(1);
        assertThat(capturedJs.get(0)).contains("second");
        assertThat(capturedJs.get(0)).doesNotContain("first");
    }

    // ─── Agent INTERMEDIATE_RESPONSE tests (showToolActivity=false) ─

    @Test
    void agentIntermediateResponse_appendsTextBelowThinking() {
        handler.setActiveMessageId("msg-100");

        handler.onActivityMessage(agentIntermediate("I'll help you implement this method."));

        assertThat(capturedJs).hasSize(1);
        String js = capturedJs.get(0);
        // Should target the loading indicator
        assertThat(js).contains("loading-msg-100");
        // Should create a sibling div with the intermediate text ID
        assertThat(js).contains("agent-intermediate-msg-100");
        // Should contain the intermediate text (HTML-escaped by CommonMark)
        assertThat(js).contains("implement this method");
    }

    @Test
    void agentIntermediateResponse_multiple_accumulates() {
        handler.setActiveMessageId("msg-200");

        handler.onActivityMessage(agentIntermediate("First response"));
        handler.onActivityMessage(agentIntermediate("Second response"));

        assertThat(capturedJs).hasSize(2);
        // The second JS should contain BOTH texts (accumulated)
        String js = capturedJs.get(1);
        assertThat(js).contains("First response");
        assertThat(js).contains("Second response");
    }

    @Test
    void agentIntermediateResponse_withNullResult_usesEmptyString() {
        handler.setActiveMessageId("msg-300");

        ActivityMessage message = ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.INTERMEDIATE_RESPONSE)
                .result(null)
                .build();
        handler.onActivityMessage(message);

        // Should still execute JS (with empty content)
        assertThat(capturedJs).hasSize(1);
    }

    @Test
    void agentIntermediateResponse_noActiveMessageId_skips() {
        // Don't set an active message ID
        handler.onActivityMessage(agentIntermediate("orphan message"));

        assertThat(capturedJs).isEmpty();
    }

    // ─── Agent INTERMEDIATE_RESPONSE tests (showToolActivity=true) ──

    @Test
    void agentIntermediateResponse_showToolActivity_usesAgentIndicator() {
        when(stateService.getShowToolActivityInChat()).thenReturn(true);
        handler.setActiveMessageId("msg-400");

        handler.onActivityMessage(agentIntermediate("reasoning text"));

        assertThat(capturedJs).hasSize(1);
        String js = capturedJs.get(0);
        // Should use the agent activity indicator (not the intermediate text div)
        assertThat(js).contains("agent-outer-container");
        assertThat(js).contains("Agent Activity");
    }

    // ─── Agent tool call tests ──────────────────────────────────

    @Test
    void agentToolRequest_showToolActivityDisabled_ignored() {
        handler.setActiveMessageId("msg-500");

        handler.onActivityMessage(agentToolRequest("read_file", "{\"path\":\"/foo\"}"));

        assertThat(capturedJs).isEmpty();
    }

    @Test
    void agentToolRequest_showToolActivityEnabled_rendersInChat() {
        when(stateService.getShowToolActivityInChat()).thenReturn(true);
        handler.setActiveMessageId("msg-600");

        handler.onActivityMessage(agentToolRequest("read_file", "{\"path\":\"/foo\"}"));

        assertThat(capturedJs).hasSize(1);
        String js = capturedJs.get(0);
        assertThat(js).contains("read_file");
        assertThat(js).contains("agent-outer-container");
    }

    @Test
    void agentToolResponse_showToolActivityEnabled_rendersResult() {
        when(stateService.getShowToolActivityInChat()).thenReturn(true);
        handler.setActiveMessageId("msg-700");

        handler.onActivityMessage(agentToolResponse("read_file", "file contents here"));

        assertThat(capturedJs).hasSize(1);
        assertThat(capturedJs.get(0)).contains("read_file");
    }

    // ─── MCP message tests ──────────────────────────────────────

    @Test
    void mcpToolMessage_rendersInChat() {
        handler.setActiveMessageId("msg-800");

        handler.onActivityMessage(mcpToolMessage("calling read_file tool"));

        assertThat(capturedJs).hasSize(1);
        assertThat(capturedJs.get(0)).contains("mcp-outer-container");
    }

    @Test
    void mcpAiMessage_skipped() {
        handler.setActiveMessageId("msg-900");

        ActivityMessage aiMsg = ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .mcpType(MCPType.AI_MSG)
                .content("AI response")
                .build();
        handler.onActivityMessage(aiMsg);

        assertThat(capturedJs).isEmpty();
    }

    @Test
    void mcpLogMessage_withoutToolActivity_skipped() {
        handler.setActiveMessageId("msg-1000");

        ActivityMessage logMsg = ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .mcpType(MCPType.LOG_MSG)
                .content("some log")
                .build();
        handler.onActivityMessage(logMsg);

        // LOG_MSG alone (without a TOOL_MSG first) doesn't trigger UI update
        assertThat(capturedJs).isEmpty();
    }

    @Test
    void mcpMessages_toolFollowedByLog_bothRendered() {
        handler.setActiveMessageId("msg-1100");

        // First send a tool message to trigger hasToolActivity
        handler.onActivityMessage(mcpToolMessage("tool call"));
        capturedJs.clear();

        // Now a log message should also trigger a UI update
        ActivityMessage logMsg = ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .mcpType(MCPType.LOG_MSG)
                .content("log entry")
                .build();
        handler.onActivityMessage(logMsg);

        assertThat(capturedJs).hasSize(1);
    }

    // ─── Deactivated handler tests ──────────────────────────────

    @Test
    void deactivated_mcpMessage_ignored() {
        handler.setActiveMessageId("msg-1200");
        handler.deactivate();

        handler.onActivityMessage(mcpToolMessage("tool call"));

        assertThat(capturedJs).isEmpty();
    }

    @Test
    void deactivated_agentToolMessage_ignored() {
        when(stateService.getShowToolActivityInChat()).thenReturn(true);
        handler.setActiveMessageId("msg-1300");
        handler.deactivate();

        handler.onActivityMessage(agentToolRequest("read_file", "{}"));

        assertThat(capturedJs).isEmpty();
    }

    // ─── JS content validation tests ────────────────────────────

    @Test
    void intermediateTextScript_containsFallbackToAssistantMessage() {
        handler.setActiveMessageId("msg-1400");
        handler.onActivityMessage(agentIntermediate("test text"));

        assertThat(capturedJs).hasSize(1);
        String js = capturedJs.get(0);
        // Should have fallback to .assistant-message if loading indicator not found
        assertThat(js).contains(".assistant-message");
        assertThat(js).contains("container.appendChild");
    }

    @Test
    void intermediateTextScript_escapesBackticks() {
        handler.setActiveMessageId("msg-1500");
        handler.onActivityMessage(agentIntermediate("code: `hello`"));

        assertThat(capturedJs).hasSize(1);
        // The backtick should be escaped in the JS template literal
        assertThat(capturedJs.get(0)).doesNotContain("code: `hello`");
    }

    @Test
    void intermediateTextScript_hasErrorHandler() {
        handler.setActiveMessageId("msg-1600");
        handler.onActivityMessage(agentIntermediate("text"));

        assertThat(capturedJs.get(0)).contains("catch (error)");
        assertThat(capturedJs.get(0)).contains("console.error");
    }

    // ─── Helper methods ─────────────────────────────────────────

    private ActivityMessage agentIntermediate(String text) {
        return ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.INTERMEDIATE_RESPONSE)
                .result(text)
                .build();
    }

    private ActivityMessage agentToolRequest(String toolName, String arguments) {
        return ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.TOOL_REQUEST)
                .toolName(toolName)
                .arguments(arguments)
                .callNumber(1)
                .maxCalls(25)
                .build();
    }

    private ActivityMessage agentToolResponse(String toolName, String result) {
        return ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(AgentType.TOOL_RESPONSE)
                .toolName(toolName)
                .result(result)
                .callNumber(1)
                .maxCalls(25)
                .build();
    }

    private ActivityMessage mcpToolMessage(String content) {
        return ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .mcpType(MCPType.TOOL_MSG)
                .content(content)
                .build();
    }
}
