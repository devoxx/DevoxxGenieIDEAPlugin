package com.devoxx.genie.model.ap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApProject(String id, String name, String description) {
}
