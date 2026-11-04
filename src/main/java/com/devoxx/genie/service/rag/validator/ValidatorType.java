package com.devoxx.genie.service.rag.validator;

public enum ValidatorType {
    OLLAMA("ollama"),
    EMBEDDING_MODEL("embedding model"),
    DOCKER("docker"),
    CHROMADB("chromadb");

    private final String label;

    ValidatorType(String label) {
        this.label = label;
    }

    /** Label shown in the RAG settings validation panel. */
    public String getName() {
        return label;
    }
}
