package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPServer;
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
import java.util.Map;

@Slf4j
public class MCPListenerService implements ChatModelListener {

    @Override
    public void onRequest(@NotNull ChatModelRequestContext requestContext) {
        log.debug("onRequest: {}", requestContext.chatRequest().toString());

        Map<String, MCPServer> mcpServers = DevoxxGenieStateService.getInstance().getMcpSettings().getMcpServers();
        int totalToolsCount = mcpServers.values().stream()
                .filter(MCPServer::isEnabled)
                .mapToInt(server -> server.getAvailableTools().size())
                .sum();

        if (totalToolsCount == 0) {
            return;
        }

        List<ChatMessage> messages = requestContext.chatRequest().messages();
        if (!messages.isEmpty() && messages.size() > 2) {
            ChatMessage chatMessage = messages.get(messages.size() - 2);

            if (chatMessage instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
                log.debug(">>> Tool args: {}", toolExecutionResultMessage.toolName());
            } else if (chatMessage instanceof AiMessage aiMessage) {
                if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
                    log.debug(">>> AI msg: {}", aiMessage.text());
                    postMessage(MCPMessage.builder()
                            .type(MCPType.AI_MSG)
                            .content(aiMessage.text())
                            .build());
                }
                if (aiMessage.hasToolExecutionRequests() && !aiMessage.toolExecutionRequests().isEmpty()) {
                    List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
                    if (toolExecutionRequests != null && !toolExecutionRequests.isEmpty()) {
                        ToolExecutionRequest toolExecutionRequest = toolExecutionRequests.get(0);
                        log.debug(">>> Tool msg: {}", toolExecutionRequest.arguments());
                        postMessage(MCPMessage.builder()
                                .type(MCPType.TOOL_MSG)
                                .content(toolExecutionRequest.arguments())
                                .build());
                    }
                }
            }
        }
    }

    private static void postMessage(MCPMessage mcpMessage) {
        if (mcpMessage != null) {
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(AppTopics.MCP_LOGGING_MSG)
                    .onMCPLoggingMessage(mcpMessage);
        }
    }
}
