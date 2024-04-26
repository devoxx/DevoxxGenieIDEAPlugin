package com.devoxx.genie.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ChatInteraction {
    private String llmProvider;
    private String modelName;
    private String question;
    private String response;
    private Instant createdOn = Instant.now();

    public ChatInteraction(String llmProvider,
                           String modelName,
                           String question,
                           String response) {
        this.llmProvider = llmProvider;
        this.modelName = modelName;
        this.question = question;
        this.response = response;
    }
}
