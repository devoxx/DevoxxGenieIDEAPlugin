package com.devoxx.genie.model.automation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps an IDE event type to an agent that should be triggered.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAgentMapping {

    @Builder.Default
    private boolean enabled = true;

    private String eventType;

    private String agentType;

    /**
     * Custom agent name (used when agentType is CUSTOM).
     */
    @Builder.Default
    private String customAgentName = "";

    /**
     * The prompt to use when triggering the agent.
     * For built-in agents this defaults to the agent's default prompt.
     * For custom agents this is user-defined.
     */
    @Builder.Default
    private String prompt = "";

    /**
     * Whether to run this agent automatically or show a confirmation dialog first.
     */
    @Builder.Default
    private boolean autoRun = false;
}
