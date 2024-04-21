package com.devoxx.genie.model;

public class ChatInteraction {
    private String llmProvider;
    private String modelName;
    private String question;
    private String response;

    public ChatInteraction(String llmProvider,
                           String modelName,
                           String question,
                           String response) {
        this.llmProvider = llmProvider;
        this.modelName = modelName;
        this.question = question;
        this.response = response;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
