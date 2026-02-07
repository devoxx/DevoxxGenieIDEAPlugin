package com.devoxx.genie.service.agent;

import com.devoxx.genie.model.agent.AgentMessage;

public interface AgentLoggingMessage {
    void onAgentLoggingMessage(AgentMessage message);
}
