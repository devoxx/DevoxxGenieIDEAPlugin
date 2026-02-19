package com.devoxx.genie.model.models;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelConfig {
    private int schemaVersion;
    private String lastUpdated;
    private Map<String, List<ModelConfigEntry>> providers;
}
