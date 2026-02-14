package com.devoxx.genie.model.llmcost;

import lombok.Data;

import java.util.List;

@Data
public class LLMCostData {
    private int schemaVersion;
    private String lastUpdated;
    private List<LLMCostModelEntry> models;
}
