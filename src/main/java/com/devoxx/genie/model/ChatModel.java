package com.devoxx.genie.model;

public class ChatModel {

    public String baseUrl;
    public String name; // the model name to use
    public Double temperature = 0.7;
    public Double topP = 0.7;
    public int maxTokens = 2_000;
    public int maxRetries = 5;
    public int timeout = 60;
}
