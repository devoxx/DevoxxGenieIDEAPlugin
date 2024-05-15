package com.devoxx.genie.model.gemini;

import com.devoxx.genie.model.gemini.model.Content;
import com.devoxx.genie.model.gemini.model.GenerationConfig;
import com.devoxx.genie.model.gemini.model.SystemInstruction;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiMessageRequest {
    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("system_instruction")
    private SystemInstruction systemInstruction;

    @JsonProperty("generation_config")
    private GenerationConfig generationConfig;
}
