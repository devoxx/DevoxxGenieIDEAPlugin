package com.devoxx.genie.service.semanticsearch.validator;

public enum ValidatorType {
    OLLAMA,
    NOMIC,
    DOCKER,
    CHROMADB;

    public String getName() {
        return name().toLowerCase();
    }
}
