package com.devoxx.genie.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatModel {

    private String baseUrl;
    private String modelName; // the model name to use
    private Double temperature = 0.7;
    private Double topP = 0.7;
    private int maxTokens = 2_000;
    private int maxRetries = 5;
    private int timeout = 60;

}
