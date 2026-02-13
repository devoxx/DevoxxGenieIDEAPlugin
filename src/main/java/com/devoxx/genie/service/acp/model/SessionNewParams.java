package com.devoxx.genie.service.acp.model;

import java.util.List;

public class SessionNewParams {
    public String cwd;
    public List<Object> mcpServers;

    public SessionNewParams() {}

    public SessionNewParams(String cwd) {
        this.cwd = cwd;
        this.mcpServers = List.of();
    }
}
