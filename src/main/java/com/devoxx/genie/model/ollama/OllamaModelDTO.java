package com.devoxx.genie.model.ollama;

public class OllamaModelDTO {
    private OllamaModelEntryDTO[] models;

    public OllamaModelEntryDTO[] getModels() {
        return models;
    }

    public void setModels(OllamaModelEntryDTO[] models) {
        this.models = models;
    }
}
