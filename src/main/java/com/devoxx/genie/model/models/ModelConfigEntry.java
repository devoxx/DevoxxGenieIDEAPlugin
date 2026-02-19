package com.devoxx.genie.model.models;

import lombok.Data;

@Data
public class ModelConfigEntry {
    private String modelName;
    private String displayName;
    private double inputCost;
    private double outputCost;
    private int inputMaxTokens;
    private int outputMaxTokens;
    private boolean apiKeyUsed;
}
