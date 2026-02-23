package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class MCPListenerService implements ChatModelListener {

    private final Supplier<Boolean> agentModeSupplier;
    private final Supplier<Boolean> agentDebugLogsEnabledSupplier;
    private final Consumer<MCPMessage> mcpMessagePublisher;
    private final Consumer<AgentMessage> agentMessagePublisher;

    public MCPListenerService() {
        this(
                () -> Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled()),
                () -> Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled()),
                MCPListenerService::publishMcpMessage,
                MCPListenerService::publishAgentMessage
        );
    }

    MCPListenerService(Supplier<Boolean> agentModeSupplier,
                       Supplier<Boolean> agentDebugLogsEnabledSupplier,
                       Consumer<MCPMessage> mcpMessagePublisher,
                       Consumer<AgentMessage> agentMessagePublisher) {
        this.agentModeSupplier = agentModeSupplier;
        this.agentDebugLogsEnabledSupplier = agentDebugLogsEnabledSupplier;
        this.mcpMessagePublisher = mcpMessagePublisher;
        this.agentMessagePublisher = agentMessagePublisher;
    }

    @Override
    public void onRequest(@NotNull ChatModelRequestContext requestContext) {
        log.debug("onRequest: {}", requestContext.chatRequest().toString());

        List<ChatMessage> messages = requestContext.chatRequest().messages();
        if (messages.isEmpty() || messages.size() <= 2) {
            return;
        }

        ChatMessage chatMessage = messages.get(messages.size() - 2);
        boolean agentMode = agentModeSupplier.get();

        if (chatMessage instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            log.debug(">>> Tool args: {}", toolExecutionResultMessage.toolName());
        } else if (chatMessage instanceof AiMessage aiMessage) {
            handleAiMessage(aiMessage, agentMode);
        }
    }

    private void handleAiMessage(@NotNull AiMessage aiMessage, boolean agentMode) {
        if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
            log.debug(">>> AI msg: {}", aiMessage.text());
            if (agentMode) {
                // Route LLM intermediate reasoning to Agent Logs
                postAgentMessage(aiMessage.text());
            } else {
                postMcpMessage(MCPMessage.builder()
                        .type(MCPType.AI_MSG)
                        .content(aiMessage.text())
                        .build());
            }
        }
        // Tool execution requests: only log to MCP panel when NOT in agent mode.
        // In agent mode, AgentLoopTracker already logs tool calls to Agent Logs.
        if (!agentMode && aiMessage.hasToolExecutionRequests() && !aiMessage.toolExecutionRequests().isEmpty()) {
            ToolExecutionRequest toolExecutionRequest = aiMessage.toolExecutionRequests().get(0);
            log.debug(">>> Tool msg: {}", toolExecutionRequest.arguments());
            postMcpMessage(MCPMessage.builder()
                    .type(MCPType.TOOL_MSG)
                    .content(toolExecutionRequest.arguments())
                    .build());
        }
    }

    private void postMcpMessage(MCPMessage mcpMessage) {
        if (mcpMessage != null) {
            mcpMessagePublisher.accept(mcpMessage);
        }
    }

    private void postAgentMessage(@NotNull String text) {
        try {
            AgentMessage message = AgentMessage.builder()
                    .type(AgentType.INTERMEDIATE_RESPONSE)
                    .result(text)
                    .build();
            agentMessagePublisher.accept(message);
        } catch (Exception e) {
            log.debug("Failed to publish agent intermediate response", e);
        }
    }

    private static void publishMcpMessage(MCPMessage mcpMessage) {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(AppTopics.MCP_LOGGING_MSG)
                .onMCPLoggingMessage(mcpMessage);
    }

    private static void publishAgentMessage(AgentMessage message) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppTopics.AGENT_LOG_MSG)
                .onAgentLoggingMessage(message);
    }
}
