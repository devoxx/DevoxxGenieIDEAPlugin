package com.devoxx.genie.model.openrouter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopProvider {

    @JsonProperty("context_length")
    private Integer contextLength;
}
