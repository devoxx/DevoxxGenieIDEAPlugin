package com.devoxx.genie.model.ollama;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OllamaModelDTO {
    private OllamaModelEntryDTO[] models;
}
