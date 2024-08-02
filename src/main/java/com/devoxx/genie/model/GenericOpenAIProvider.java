package com.devoxx.genie.model;

import lombok.Data;

@Data
public class GenericOpenAIProvider {
    private String name;
    private String baseUrl;
    private String modelName;
    private String apiKey;
    private Double inputCost;
    private Double outputCost;
    private Integer contextWindow;
}
