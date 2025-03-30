package dev.langchain4j.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

public class McpInitializeRequest extends McpClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.INITIALIZE;

    @Setter
    @Getter
    private InitializeParams params;

    public McpInitializeRequest(final Long id) {
        super(id);
    }

}
