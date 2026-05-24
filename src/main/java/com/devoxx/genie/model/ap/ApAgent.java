package com.devoxx.genie.model.ap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApAgent(String id, String name, String description) {
}
