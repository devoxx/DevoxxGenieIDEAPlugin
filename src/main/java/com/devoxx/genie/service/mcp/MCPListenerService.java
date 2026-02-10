package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.devoxx.genie.util.ProjectContextHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
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

@Slf4j
public class MCPListenerService implements ChatModelListener {

    @Override
    public void onRequest(@NotNull ChatModelRequestContext requestContext) {
        log.debug("onRequest: {}", requestContext.chatRequest().toString());

        boolean agentMode = Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled());

        List<ChatMessage> messages = requestContext.chatRequest().messages();
        if (messages.isEmpty() || messages.size() <= 2) {
            return;
        }

        ChatMessage chatMessage = messages.get(messages.size() - 2);

        if (chatMessage instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            log.debug(">>> Tool args: {}", toolExecutionResultMessage.toolName());
        } else if (chatMessage instanceof AiMessage aiMessage) {
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
                List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
                if (toolExecutionRequests != null && !toolExecutionRequests.isEmpty()) {
                    ToolExecutionRequest toolExecutionRequest = toolExecutionRequests.get(0);
                    log.debug(">>> Tool msg: {}", toolExecutionRequest.arguments());
                    postMcpMessage(MCPMessage.builder()
                            .type(MCPType.TOOL_MSG)
                            .content(toolExecutionRequest.arguments())
                            .build());
                }
            }
        }
    }

    private static void postMcpMessage(MCPMessage mcpMessage) {
        if (mcpMessage != null) {
            MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
            messageBus.syncPublisher(AppTopics.MCP_LOGGING_MSG)
                    .onMCPLoggingMessage(mcpMessage);
        }
    }

    private static void postAgentMessage(@NotNull String text) {
        if (!Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled())) {
            return;
        }
        try {
            // Get the current project context to properly scope the message
            Project project = ProjectContextHolder.getCurrentProject();
            String projectLocationHash = project != null ? project.getLocationHash() : null;

            AgentMessage message = AgentMessage.builder()
                    .type(AgentType.INTERMEDIATE_RESPONSE)
                    .result(text)
                    .projectLocationHash(projectLocationHash)
                    .build();
            ApplicationManager.getApplication().getMessageBus()
                    .syncPublisher(AppTopics.AGENT_LOG_MSG)
                    .onAgentLoggingMessage(message);
        } catch (Exception e) {
            log.debug("Failed to publish agent intermediate response", e);
        }
    }
}
