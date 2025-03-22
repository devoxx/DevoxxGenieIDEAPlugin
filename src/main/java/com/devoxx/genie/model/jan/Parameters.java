package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Parameters {
    @JsonProperty("temperature")
    private double temperature;

    @JsonProperty("top_p")
    private double topP;

    @JsonProperty("stream")
    private boolean stream;

    @JsonProperty("max_tokens")
    private int maxTokens;

    @JsonProperty("stop")
    private List<String> stop;

    @JsonProperty("frequency_penalty")
    private double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private double presencePenalty;
}
