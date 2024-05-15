package com.devoxx.genie.model.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {

    private String role;

    @JsonProperty("parts")
    private List<Part> parts;
}
