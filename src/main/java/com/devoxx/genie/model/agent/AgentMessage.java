package com.devoxx.genie.model.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    private AgentType type;
    private String toolName;
    private String arguments;
    private String result;
    private int callNumber;
    private int maxCalls;
    private String subAgentId;
    /** Provider · model label for sub-agent events (e.g. "Ollama · qwen3"); null otherwise. */
    private String agentModelLabel;
    /** Identifies the project that produced this message; used to filter cross-project noise. */
    private String projectLocationHash;
}
