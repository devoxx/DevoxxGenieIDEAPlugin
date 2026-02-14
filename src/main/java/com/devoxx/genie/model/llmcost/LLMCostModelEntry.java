package com.devoxx.genie.model.llmcost;

import lombok.Data;

@Data
public class LLMCostModelEntry {
    private String provider;
    private String modelName;
    private String displayName;
    private double inputCost;
    private double outputCost;
    private int inputMaxTokens;
    private int outputMaxTokens;
    private boolean apiKeyUsed;
}
