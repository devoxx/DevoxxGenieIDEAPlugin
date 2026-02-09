package com.devoxx.genie.model.acp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings for an ACP (Agent Client Protocol) agent configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ACPSettings {

    /** Display name for the agent (e.g., "Gemini CLI") */
    @Builder.Default
    private String agentName = "Gemini CLI";

    /** Path to the agent command (e.g., "/usr/local/bin/gemini") */
    @Builder.Default
    private String agentCommand = "";

    /** Arguments passed to the agent command (e.g., ["--experimental-acp"]) */
    @Builder.Default
    private List<String> agentArgs = new ArrayList<>(List.of("--experimental-acp"));

    /** Environment variables for the agent process */
    @Builder.Default
    private Map<String, String> env = new HashMap<>();

    /** Whether the ACP agent is enabled */
    @Builder.Default
    private boolean enabled = false;
}
