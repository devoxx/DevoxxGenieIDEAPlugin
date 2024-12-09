package com.devoxx.genie.ui.settings.rag;

public enum ActionButtonState {
    PULL_CHROMA_DB("Pull ChromaDB Image"),
    START_CHROMA_DB("Start ChromaDB"),
    START_INDEXING("Start Indexing");

    private final String text;

    ActionButtonState(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}