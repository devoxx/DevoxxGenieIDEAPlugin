package com.devoxx.genie.model.ap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApSession(String id,
                        String title,
                        String status,
                        String agent,
                        String project) {
}
