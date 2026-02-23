package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.service.activity.ActivityLoggingMessage;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import dev.langchain4j.mcp.client.logging.McpLogLevel;
import dev.langchain4j.mcp.client.logging.McpLogMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MCPLogMessageHandlerTest {

    @Mock
    private Application application;

    @Mock
    private MessageBus messageBus;

    @Mock
    private ActivityLoggingMessage activityPublisher;

    private MCPLogMessageHandler handler;
    private MockedStatic<ApplicationManager> mockedAppManager;
    private MockedStatic<MCPService> mockedMCPService;

    @BeforeEach
    void setUp() {
        mockedAppManager = Mockito.mockStatic(ApplicationManager.class);
        mockedAppManager.when(ApplicationManager::getApplication).thenReturn(application);
        when(application.getMessageBus()).thenReturn(messageBus);
        when(messageBus.syncPublisher(AppTopics.ACTIVITY_LOG_MSG)).thenReturn(activityPublisher);

        mockedMCPService = Mockito.mockStatic(MCPService.class);
        mockedMCPService.when(MCPService::isDebugLogsEnabled).thenReturn(true);

        handler = new MCPLogMessageHandler();
    }

    @AfterEach
    void tearDown() {
        mockedAppManager.close();
        mockedMCPService.close();
    }

    private McpLogMessage createLogMessage(McpLogLevel level, String text) {
        JsonNode data = new TextNode(text);
        return new McpLogMessage(level, "test-logger", data);
    }

    @Test
    void handleLogMessage_debugLogsDisabled_doesNotProcess() {
        mockedMCPService.when(MCPService::isDebugLogsEnabled).thenReturn(false);

        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "test message");
        handler.handleLogMessage(message);

        verify(activityPublisher, never()).onActivityMessage(any());
    }

    @Test
    void handleLogMessage_jsonWithContent_classifiedAsAiMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "{\"content\": \"hello\"}");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        ActivityMessage activityMsg = captor.getValue();
        assertThat(activityMsg.getSource()).isEqualTo(ActivitySource.MCP);
        assertThat(activityMsg.getMcpType()).isEqualTo(MCPType.AI_MSG);
    }

    @Test
    void handleLogMessage_jsonWithFunctionName_classifiedAsToolMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "{\"name\": \"read_file\"}");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.TOOL_MSG);
    }

    @Test
    void handleLogMessage_postRequest_classifiedAsToolMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "POST /api/tools/call");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.TOOL_MSG);
    }

    @Test
    void handleLogMessage_plainText_classifiedAsLogMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "Some plain log message");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        ActivityMessage activityMsg = captor.getValue();
        assertThat(activityMsg.getMcpType()).isEqualTo(MCPType.LOG_MSG);
        assertThat(activityMsg.getContent()).isEqualTo("Some plain log message");
    }

    @Test
    void handleLogMessage_genericJson_classifiedAsAiMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "{\"id\": 1, \"result\": \"ok\"}");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        // Default for JSON without specific content/name keys is AI_MSG
        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.AI_MSG);
    }

    @Test
    void handleLogMessage_jsonArray_classifiedAsAiMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "[{\"tool\": \"test\"}]");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.AI_MSG);
    }

    @Test
    void handleLogMessage_messageBusException_doesNotThrow() {
        when(messageBus.syncPublisher(AppTopics.ACTIVITY_LOG_MSG))
                .thenThrow(new RuntimeException("bus error"));

        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "test message");

        // Should not throw
        handler.handleLogMessage(message);
    }

    @Test
    void handleLogMessage_debugLevel_logsAtDebugLevel() {
        McpLogMessage message = createLogMessage(McpLogLevel.DEBUG, "debug message");

        // Should not throw
        handler.handleLogMessage(message);
        verify(activityPublisher).onActivityMessage(any());
    }

    @Test
    void handleLogMessage_errorLevel_logsAtErrorLevel() {
        McpLogMessage message = createLogMessage(McpLogLevel.ERROR, "error message");

        // Should not throw
        handler.handleLogMessage(message);
        verify(activityPublisher).onActivityMessage(any());
    }

    // ─── Additional classification branch tests ─────────────────

    @Test
    void handleLogMessage_getRequest_classifiedAsToolMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "GET /api/tools/list");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.TOOL_MSG);
    }

    @Test
    void handleLogMessage_functionCall_classifiedAsToolMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "calling function(arg1, arg2)");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.TOOL_MSG);
    }

    @Test
    void handleLogMessage_jsonWithResponseKey_classifiedAsAiMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "{\"response\": \"hello world\"}");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.AI_MSG);
    }

    @Test
    void handleLogMessage_jsonWithFunctionKey_classifiedAsToolMsg() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "{\"function\": \"read_file\", \"args\": {}}");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.TOOL_MSG);
    }

    // ─── publishToBus direction stripping tests ─────────────────

    @Test
    void handleLogMessage_aiMsg_contentStrippedOfIncomingPrefix() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "{\"content\": \"response data\"}");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        // The "< " prefix should be stripped by publishToBus
        assertThat(captor.getValue().getContent()).doesNotStartWith("< ");
        assertThat(captor.getValue().getContent()).isEqualTo("{\"content\": \"response data\"}");
    }

    @Test
    void handleLogMessage_toolMsg_contentStrippedOfOutgoingPrefix() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "POST /api/call");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        // The "> " prefix should be stripped by publishToBus
        assertThat(captor.getValue().getContent()).doesNotStartWith("> ");
        assertThat(captor.getValue().getContent()).isEqualTo("POST /api/call");
    }

    @Test
    void handleLogMessage_logMsg_contentPreservedWithoutMarkers() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "plain log with no markers");
        handler.handleLogMessage(message);

        ArgumentCaptor<ActivityMessage> captor = ArgumentCaptor.forClass(ActivityMessage.class);
        verify(activityPublisher).onActivityMessage(captor.capture());

        assertThat(captor.getValue().getContent()).isEqualTo("plain log with no markers");
        assertThat(captor.getValue().getMcpType()).isEqualTo(MCPType.LOG_MSG);
    }

    // ─── logAtLevel default case tests ──────────────────────────

    @Test
    void handleLogMessage_warningLevel_logsAtInfoDefault() {
        McpLogMessage message = createLogMessage(McpLogLevel.WARNING, "warning message");
        handler.handleLogMessage(message);

        // Should not throw, falls through to default (info)
        verify(activityPublisher).onActivityMessage(any());
    }

    @Test
    void handleLogMessage_infoLevel_logsAtInfoDefault() {
        McpLogMessage message = createLogMessage(McpLogLevel.INFO, "info message");
        handler.handleLogMessage(message);

        verify(activityPublisher).onActivityMessage(any());
    }

    @Test
    void handleLogMessage_criticalLevel_logsAtInfoDefault() {
        McpLogMessage message = createLogMessage(McpLogLevel.CRITICAL, "critical message");
        handler.handleLogMessage(message);

        verify(activityPublisher).onActivityMessage(any());
    }
}
