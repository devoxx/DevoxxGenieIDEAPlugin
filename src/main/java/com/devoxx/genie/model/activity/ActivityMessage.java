package com.devoxx.genie.model.activity;

import com.devoxx.genie.model.agent.AgentMessage;
import com.devoxx.genie.model.agent.AgentType;
import com.devoxx.genie.model.mcp.MCPMessage;
import com.devoxx.genie.model.mcp.MCPType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityMessage {
    private ActivitySource source;

    // MCP-specific
    private MCPType mcpType;

    // Agent-specific
    private AgentType agentType;

    // Shared fields
    private String content;
    private String toolName;
    private String arguments;
    private String result;
    private int callNumber;
    private int maxCalls;
    private String subAgentId;

    /** Identifies the project that produced this message; used to filter cross-project noise. */
    private String projectLocationHash;

    /**
     * Creates an ActivityMessage from an MCPMessage.
     */
    public static @NotNull ActivityMessage fromMCP(@NotNull MCPMessage mcpMessage) {
        return ActivityMessage.builder()
                .source(ActivitySource.MCP)
                .mcpType(mcpMessage.getType())
                .content(mcpMessage.getContent())
                .projectLocationHash(mcpMessage.getProjectLocationHash())
                .build();
    }

    /**
     * Creates an ActivityMessage from an AgentMessage.
     */
    public static @NotNull ActivityMessage fromAgent(@NotNull AgentMessage agentMessage) {
        return ActivityMessage.builder()
                .source(ActivitySource.AGENT)
                .agentType(agentMessage.getType())
                .toolName(agentMessage.getToolName())
                .arguments(agentMessage.getArguments())
                .result(agentMessage.getResult())
                .callNumber(agentMessage.getCallNumber())
                .maxCalls(agentMessage.getMaxCalls())
                .subAgentId(agentMessage.getSubAgentId())
                .projectLocationHash(agentMessage.getProjectLocationHash())
                .build();
    }
}
