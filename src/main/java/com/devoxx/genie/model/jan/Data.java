package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Data {
    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("description")
    private String description;

    @JsonProperty("format")
    private String format;

    @JsonProperty("settings")
    private Settings settings;

    @JsonProperty("parameters")
    private Parameters parameters;

    @JsonProperty("metadata")
    private Metadata metadata;

    @JsonProperty("engine")
    private String engine;
}
