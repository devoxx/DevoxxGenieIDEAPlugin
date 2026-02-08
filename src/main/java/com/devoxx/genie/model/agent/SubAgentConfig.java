package com.devoxx.genie.model.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-sub-agent LLM configuration.
 * When modelProvider is empty, the sub-agent inherits the default provider/model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubAgentConfig {
    private String modelProvider = "";
    private String modelName = "";
}
