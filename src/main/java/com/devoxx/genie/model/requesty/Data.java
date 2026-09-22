package com.devoxx.genie.model.requesty;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * One entry of the Requesty {@code /v1/models} or {@code /v1/models/managed} catalog.
 * Prices are USD per token; {@code context_window} is the input window in tokens.
 */
@Getter
@Setter
public class Data {

    @JsonProperty("id")
    private String id;

    @JsonProperty("api")
    private String api;

    @JsonProperty("description")
    private String description;

    @JsonProperty("context_window")
    private Integer contextWindow;

    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    @JsonProperty("input_price")
    private Double inputPrice;

    @JsonProperty("output_price")
    private Double outputPrice;

    public boolean isChatModel() {
        return api == null || "chat".equals(api);
    }
}
