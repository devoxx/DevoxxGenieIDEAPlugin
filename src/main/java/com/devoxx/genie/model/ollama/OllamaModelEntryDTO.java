package com.devoxx.genie.model.ollama;

public class OllamaModelEntryDTO {
    private String name;
    private String modified_at;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModified_at() {
        return modified_at;
    }

    public void setModified_at(String modified_at) {
        this.modified_at = modified_at;
    }
}
