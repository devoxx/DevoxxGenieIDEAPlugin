package dev.langchain4j.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.langchain4j.mcp.client.protocol.ClientMethod;
import dev.langchain4j.mcp.client.protocol.McpClientMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class McpReadResourceRequest extends McpClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.RESOURCES_READ;

    @JsonInclude
    private Map<String, Object> params;

    public McpReadResourceRequest(Long id, String uri) {
        super(id);
        this.params = new HashMap<>();
        Objects.requireNonNull(uri);
        this.params.put("uri", uri);
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
