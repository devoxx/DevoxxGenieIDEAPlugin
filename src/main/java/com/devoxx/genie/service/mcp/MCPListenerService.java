package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.activity.ActivityMessage;
import com.devoxx.genie.model.activity.ActivitySource;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPType;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
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
    private final Consumer<ActivityMessage> activityMessagePublisher;

    public MCPListenerService() {
        this(
                () -> Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentModeEnabled()),
                () -> Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getAgentDebugLogsEnabled()),
                MCPListenerService::publishActivityMessage
        );
    }

    MCPListenerService(Supplier<Boolean> agentModeSupplier,
                       Supplier<Boolean> agentDebugLogsEnabledSupplier,
                       Consumer<ActivityMessage> activityMessagePublisher) {
        this.agentModeSupplier = agentModeSupplier;
        this.agentDebugLogsEnabledSupplier = agentDebugLogsEnabledSupplier;
        this.activityMessagePublisher = activityMessagePublisher;
    }

    @Override
    public void onRequest(@NotNull ChatModelRequestContext requestContext) {
        log.debug("onRequest: {}", requestContext.chatRequest().toString());

        List<ChatMessage> messages = requestContext.chatRequest().messages();
        if (messages.isEmpty() || messages.size() <= 2) {
            return;
        }

        // When the last message is a UserMessage, the second-to-last AI message is from a
        // previous conversation turn — not an intermediate agent response. Publishing it
        // would flash the old response under the new prompt's "Thinking..." indicator.
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        if (lastMessage instanceof UserMessage) {
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
                postMcpMessage(MCPType.AI_MSG, aiMessage.text());
            }
        }
        // Tool execution requests: only log to MCP panel when NOT in agent mode.
        // In agent mode, AgentLoopTracker already logs tool calls to Agent Logs.
        if (!agentMode && aiMessage.hasToolExecutionRequests() && !aiMessage.toolExecutionRequests().isEmpty()) {
            ToolExecutionRequest toolExecutionRequest = aiMessage.toolExecutionRequests().get(0);
            log.debug(">>> Tool msg: {}", toolExecutionRequest.arguments());
            postMcpMessage(MCPType.TOOL_MSG, toolExecutionRequest.arguments());
        }
    }

    private void postMcpMessage(@NotNull MCPType type, @NotNull String content) {
        ActivityMessage message = ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .mcpType(type)
                .content(content)
                .build();
        activityMessagePublisher.accept(message);
    }

    private void postAgentMessage(@NotNull String text) {
        try {
            ActivityMessage message = ActivityMessage.builder()
                    .source(ActivitySource.AGENT)
                    .agentType(AgentType.INTERMEDIATE_RESPONSE)
                    .result(text)
                    .build();
            activityMessagePublisher.accept(message);
        } catch (Exception e) {
            log.debug("Failed to publish agent intermediate response", e);
        }
    }

    private static void publishActivityMessage(ActivityMessage message) {
        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(AppTopics.ACTIVITY_LOG_MSG)
                .onActivityMessage(message);
    }
}
