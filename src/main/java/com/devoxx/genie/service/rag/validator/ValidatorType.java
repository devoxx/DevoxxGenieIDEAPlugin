package com.devoxx.genie.service.rag.validator;

public enum ValidatorType {
    OLLAMA,
    NOMIC,
    DOCKER,
    CHROMADB;

    public String getName() {
        return name().toLowerCase();
    }
}
