package com.devoxx.genie.model.mcp.registry;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Map;

@Data
public class MCPRegistryServerEntry {
    private MCPRegistryServerInfo server;

    @SerializedName("_meta")
    private Map<String, Object> meta;
}
