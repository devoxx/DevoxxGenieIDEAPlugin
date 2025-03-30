package com.devoxx.genie.service.mcp;

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
        List<ChatMessage> messages = requestContext.chatRequest().messages();
        if (!messages.isEmpty() && messages.size() > 2) {
            ChatMessage chatMessage = messages.get(messages.size() - 2);

            if (chatMessage instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
                log.debug(">>> Tool args: {}", toolExecutionResultMessage.toolName());
            } else if (chatMessage instanceof AiMessage aiMessage) {
                if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
                    log.debug(">>> AI msg: {}", aiMessage.text());
                }
                if (aiMessage.hasToolExecutionRequests() && !aiMessage.toolExecutionRequests().isEmpty()) {
                    List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
                    if (toolExecutionRequests != null && !toolExecutionRequests.isEmpty()) {
                        ToolExecutionRequest toolExecutionRequest = toolExecutionRequests.get(0);
                        log.debug(">>> Tool msg: {}", toolExecutionRequest.arguments());
                    }
                }
            }
        }
    }
}
