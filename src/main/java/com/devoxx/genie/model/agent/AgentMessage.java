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
}
