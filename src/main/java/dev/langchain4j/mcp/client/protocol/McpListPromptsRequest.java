package dev.langchain4j.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.langchain4j.mcp.client.protocol.ClientMethod;
import dev.langchain4j.mcp.client.protocol.McpClientMessage;

public class McpListPromptsRequest extends McpClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.PROMPTS_LIST;

    public McpListPromptsRequest(Long id) {
        super(id);
    }
}
