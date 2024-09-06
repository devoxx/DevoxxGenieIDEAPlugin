package com.devoxx.genie.model.openrouter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Data {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("context_length")
    private Integer contextLength;

    @JsonProperty("settings")
    private Pricing pricing;

    @JsonProperty("top_provider")
    private TopProvider topProvider;
}
