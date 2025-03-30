package dev.langchain4j.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.langchain4j.mcp.client.protocol.ClientMethod;
import dev.langchain4j.mcp.client.protocol.McpClientMessage;

public class InitializationNotification extends McpClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.NOTIFICATION_INITIALIZED;

    public InitializationNotification() {
        super(null);
    }
}
