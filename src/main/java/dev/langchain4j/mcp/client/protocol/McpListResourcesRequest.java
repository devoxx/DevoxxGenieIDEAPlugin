package dev.langchain4j.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.langchain4j.mcp.client.protocol.ClientMethod;
import dev.langchain4j.mcp.client.protocol.McpClientMessage;

public class McpListResourcesRequest extends McpClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.RESOURCES_LIST;

    public McpListResourcesRequest(final Long id) {
        super(id);
    }
}
