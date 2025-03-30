package dev.langchain4j.mcp.client.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.langchain4j.mcp.client.protocol.ClientMethod;
import dev.langchain4j.mcp.client.protocol.McpClientMessage;

import java.util.HashMap;
import java.util.Map;

public class McpListToolsRequest extends McpClientMessage {

    @JsonInclude
    public final ClientMethod method = ClientMethod.TOOLS_LIST;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params;

    public McpListToolsRequest(final Long id) {
        super(id);
        this.params = new HashMap<>();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @JsonIgnore
    public void setCursor(String cursor) {
        this.params.put("cursor", cursor);
    }
}
