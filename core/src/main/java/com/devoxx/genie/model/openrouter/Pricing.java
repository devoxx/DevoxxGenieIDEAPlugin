package com.devoxx.genie.model.openrouter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

@Setter
public class Pricing {

    // Input
    @JsonProperty("prompt")
    private String prompt;

    // Output
    @JsonProperty("completion")
    private String completion;

    public Float getPrompt() {
        return Float.parseFloat(prompt);
    }

    public Float getCompletion() {
        return Float.parseFloat(completion);
    }
}
