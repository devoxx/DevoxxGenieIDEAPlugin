package com.devoxx.genie.model.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationConfig {
    @JsonProperty("maxOutputTokens")
    private int maxOutputTokens;

    @JsonProperty("temperature")
    private double temperature;

    // Lets only use temperature for now

    // @JsonProperty("topP")
    // private int topP;

    // @JsonProperty("topK")
    // private int topK;
}
