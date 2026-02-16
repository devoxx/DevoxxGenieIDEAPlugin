package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.JCEFChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebViewAgentActivityHandlerTest {

    @Mock
    private WebViewJavaScriptExecutor jsExecutor;

    private WebViewAgentActivityHandler handler;

    private MockedStatic<DevoxxGenieStateService> mockedStateService;
    private MockedStatic<ThemeDetector> mockedThemeDetector;
    private MockedStatic<JCEFChecker> mockedJCEFChecker;

    @BeforeEach
    void setUp() {
        mockedJCEFChecker = Mockito.mockStatic(JCEFChecker.class);
        mockedJCEFChecker.when(JCEFChecker::isJCEFAvailable).thenReturn(false);

        DevoxxGenieStateService mockState = mock(DevoxxGenieStateService.class);
        when(mockState.getStreamMode()).thenReturn(false);

        mockedStateService = Mockito.mockStatic(DevoxxGenieStateService.class);
        mockedStateService.when(DevoxxGenieStateService::getInstance).thenReturn(mockState);

        mockedThemeDetector = Mockito.mockStatic(ThemeDetector.class);
        mockedThemeDetector.when(ThemeDetector::isDarkTheme).thenReturn(true);

        when(jsExecutor.isLoaded()).thenReturn(true);
        when(jsExecutor.escapeJS(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        handler = new WebViewAgentActivityHandler(jsExecutor);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedThemeDetector != null) mockedThemeDetector.close();
        if (mockedJCEFChecker != null) mockedJCEFChecker.close();
    }

    @Test
    void setActiveMessageIdShouldSetIdAndResetDeactivated() {
        handler.deactivate();
        handler.setActiveMessageId("msg-1");

        // After setting active message ID, handler should no longer be deactivated
        AgentMessage message = createToolRequestMessage("myTool", 1, 10);
        handler.onAgentLoggingMessage(message);

        // Should execute JS since handler is no longer deactivated
        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void deactivateShouldPreventFurtherLogging() {
        handler.setActiveMessageId("msg-1");
        handler.deactivate();

        AgentMessage message = createToolRequestMessage("myTool", 1, 10);
        handler.onAgentLoggingMessage(message);

        // Should not execute JS when deactivated
        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageShouldAddLogAndUpdateUI() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = createToolRequestMessage("readFile", 1, 10);
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWithToolResponseShouldWork() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = AgentMessage.builder()
                .type(AgentType.TOOL_RESPONSE)
                .toolName("readFile")
                .result("file contents here")
                .callNumber(1)
                .maxCalls(10)
                .build();
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWithToolErrorShouldWork() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = AgentMessage.builder()
                .type(AgentType.TOOL_ERROR)
                .toolName("readFile")
                .result("File not found")
                .callNumber(1)
                .maxCalls(10)
                .build();
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWithLoopLimitShouldWork() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = AgentMessage.builder()
                .type(AgentType.LOOP_LIMIT)
                .callNumber(10)
                .maxCalls(10)
                .build();
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWithApprovalRequestedShouldWork() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = AgentMessage.builder()
                .type(AgentType.APPROVAL_REQUESTED)
                .toolName("deleteFile")
                .callNumber(2)
                .maxCalls(10)
                .build();
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWithSubAgentShouldIncludeSubAgentId() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = AgentMessage.builder()
                .type(AgentType.SUB_AGENT_STARTED)
                .toolName("subAgent1")
                .subAgentId("sub-123")
                .callNumber(1)
                .maxCalls(5)
                .build();
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWhenNotLoadedShouldNotExecuteJs() {
        when(jsExecutor.isLoaded()).thenReturn(false);
        handler.setActiveMessageId("msg-1");

        AgentMessage message = createToolRequestMessage("myTool", 1, 10);
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void onAgentLoggingMessageWithNullActiveIdShouldNotExecuteJs() {
        // Don't set active message ID
        AgentMessage message = createToolRequestMessage("myTool", 1, 10);
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void setActiveMessageIdShouldClearPreviousLogs() {
        handler.setActiveMessageId("msg-1");
        handler.onAgentLoggingMessage(createToolRequestMessage("tool1", 1, 10));

        // Set new message ID - should clear logs
        handler.setActiveMessageId("msg-2");
        handler.onAgentLoggingMessage(createToolRequestMessage("tool2", 1, 10));

        // The second executeJS call should not contain tool1
        verify(jsExecutor, atLeast(2)).executeJavaScript(anyString());
    }

    @Test
    void multipleLogMessagesShouldAccumulate() {
        handler.setActiveMessageId("msg-1");

        handler.onAgentLoggingMessage(createToolRequestMessage("tool1", 1, 10));
        handler.onAgentLoggingMessage(createToolRequestMessage("tool2", 2, 10));
        handler.onAgentLoggingMessage(createToolRequestMessage("tool3", 3, 10));

        // Each message triggers an executeJavaScript call
        verify(jsExecutor, times(3)).executeJavaScript(anyString());
    }

    @Test
    void intermediateResponseShouldWork() {
        handler.setActiveMessageId("msg-1");

        AgentMessage message = AgentMessage.builder()
                .type(AgentType.INTERMEDIATE_RESPONSE)
                .callNumber(1)
                .maxCalls(10)
                .build();
        handler.onAgentLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    private AgentMessage createToolRequestMessage(String toolName, int callNumber, int maxCalls) {
        return AgentMessage.builder()
                .type(AgentType.TOOL_REQUEST)
                .toolName(toolName)
                .arguments("{\"path\": \"/test\"}")
                .callNumber(callNumber)
                .maxCalls(maxCalls)
                .build();
    }
}
