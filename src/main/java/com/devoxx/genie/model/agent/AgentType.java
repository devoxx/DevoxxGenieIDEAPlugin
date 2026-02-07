package com.devoxx.genie.model.agent;

public enum AgentType {
    TOOL_REQUEST,
    TOOL_RESPONSE,
    TOOL_ERROR,
    LOOP_LIMIT,
    APPROVAL_REQUESTED,
    APPROVAL_GRANTED,
    APPROVAL_DENIED,
    INTERMEDIATE_RESPONSE
}
