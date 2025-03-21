package com.devoxx.genie.service.mcp;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.devoxx.genie.ui.topic.AppTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

public class MCPCallbackLogger extends AppenderBase<ILoggingEvent> {

    public MCPCallbackLogger() {
        addFilter(new Filter<>() {
            @Override
            public FilterReply decide(ILoggingEvent event) {
                // Only capture messages that start with < or > (MCP protocol messages)
                String message = event.getFormattedMessage();
                if (message != null && (message.startsWith("<") || message.startsWith(">"))) {
                    return FilterReply.ACCEPT;
                }
                return FilterReply.DENY;
            }
        });
    }

    @Override
    protected void append(@NotNull ILoggingEvent eventObject) {
        MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        messageBus.syncPublisher(AppTopics.MCP_LOGGING_MSG)
                  .onMCPLoggingMessage(eventObject.getFormattedMessage());

    }
}
