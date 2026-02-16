package com.devoxx.genie.ui.webview.handler;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebViewMCPLogHandlerTest {

    @Mock
    private WebViewJavaScriptExecutor jsExecutor;

    private WebViewMCPLogHandler handler;

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
        when(jsExecutor.escapeHtml(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        handler = new WebViewMCPLogHandler(jsExecutor);
    }

    @AfterEach
    void tearDown() {
        if (mockedStateService != null) mockedStateService.close();
        if (mockedThemeDetector != null) mockedThemeDetector.close();
        if (mockedJCEFChecker != null) mockedJCEFChecker.close();
    }

    @Test
    void setActiveMessageIdShouldResetState() {
        handler.deactivate();
        handler.setActiveMessageId("msg-1");

        // After reactivation, handler should process messages again
        MCPMessage toolMsg = createToolMessage("Tool call result");
        handler.onMCPLoggingMessage(toolMsg);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void deactivateShouldPreventFurtherLogging() {
        handler.setActiveMessageId("msg-1");
        handler.deactivate();

        MCPMessage message = createToolMessage("Tool result");
        handler.onMCPLoggingMessage(message);

        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageShouldSkipAiMessages() {
        handler.setActiveMessageId("msg-1");

        MCPMessage aiMsg = MCPMessage.builder()
                .type(MCPType.AI_MSG)
                .content("AI response text")
                .build();
        handler.onMCPLoggingMessage(aiMsg);

        // AI_MSG should be skipped
        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageShouldRenderToolMessages() {
        handler.setActiveMessageId("msg-1");

        MCPMessage toolMsg = createToolMessage("Tool execution result");
        handler.onMCPLoggingMessage(toolMsg);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageShouldNotRenderLogMsgWithoutToolActivity() {
        handler.setActiveMessageId("msg-1");

        MCPMessage logMsg = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content("Some log message")
                .build();
        handler.onMCPLoggingMessage(logMsg);

        // LOG_MSG without prior TOOL_MSG should not trigger UI update
        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageShouldRenderLogMsgAfterToolActivity() {
        handler.setActiveMessageId("msg-1");

        // First send a TOOL_MSG to set hasToolActivity
        MCPMessage toolMsg = createToolMessage("Tool result");
        handler.onMCPLoggingMessage(toolMsg);

        // Now LOG_MSG should also trigger UI update
        MCPMessage logMsg = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content("Follow-up log message")
                .build();
        handler.onMCPLoggingMessage(logMsg);

        // Two calls total (one for tool, one for log)
        verify(jsExecutor, times(2)).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageWhenNotLoadedShouldNotExecuteJs() {
        when(jsExecutor.isLoaded()).thenReturn(false);
        handler.setActiveMessageId("msg-1");

        MCPMessage message = createToolMessage("Tool result");
        handler.onMCPLoggingMessage(message);

        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageWithNullActiveIdShouldNotExecuteJs() {
        // Don't set active message ID
        MCPMessage message = createToolMessage("Tool result");
        handler.onMCPLoggingMessage(message);

        verify(jsExecutor, never()).executeJavaScript(anyString());
    }

    @Test
    void setActiveMessageIdShouldClearPreviousLogs() {
        handler.setActiveMessageId("msg-1");
        handler.onMCPLoggingMessage(createToolMessage("Result 1"));

        // Set new message ID - should clear logs and hasToolActivity
        handler.setActiveMessageId("msg-2");

        // Sending a LOG_MSG first should not trigger UI update (hasToolActivity was reset)
        MCPMessage logMsg = MCPMessage.builder()
                .type(MCPType.LOG_MSG)
                .content("New log")
                .build();
        handler.onMCPLoggingMessage(logMsg);

        // Only the first TOOL_MSG should have triggered executeJavaScript
        verify(jsExecutor, times(1)).executeJavaScript(anyString());
    }

    @Test
    void multipleToolMessagesShouldAccumulate() {
        handler.setActiveMessageId("msg-1");

        handler.onMCPLoggingMessage(createToolMessage("Result 1"));
        handler.onMCPLoggingMessage(createToolMessage("Result 2"));
        handler.onMCPLoggingMessage(createToolMessage("Result 3"));

        verify(jsExecutor, times(3)).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageWithMarkdownContentShouldRender() {
        handler.setActiveMessageId("msg-1");

        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("**Bold text** and `code`")
                .build();
        handler.onMCPLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
    }

    @Test
    void onMCPLoggingMessageWithCodeBlockShouldRenderWithEscaping() {
        handler.setActiveMessageId("msg-1");

        MCPMessage message = MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content("```java\npublic class Test {}\n```")
                .build();
        handler.onMCPLoggingMessage(message);

        verify(jsExecutor, atLeastOnce()).executeJavaScript(anyString());
        verify(jsExecutor, atLeastOnce()).escapeHtml(anyString());
    }

    private MCPMessage createToolMessage(String content) {
        return MCPMessage.builder()
                .type(MCPType.TOOL_MSG)
                .content(content)
                .build();
    }
}
