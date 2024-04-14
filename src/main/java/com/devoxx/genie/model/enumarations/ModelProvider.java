package com.devoxx.genie.model.enumarations;

public enum ModelProvider {
    Ollama("Ollama"),
    LMStudio("LMStudio"),
    GPT4All("GPT4All");
    // Add more

    private String name;

    ModelProvider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
