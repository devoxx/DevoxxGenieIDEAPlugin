package com.devoxx.genie.service.mcp;

import com.devoxx.genie.model.mcp.MCPMessage;

public interface MCPLoggingMessage {
    void onMCPLoggingMessage(MCPMessage message);
}
