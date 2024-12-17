package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Data {
    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("ctx_len")
    private Long ctxLen;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    @JsonProperty("description")
    private String description;

    @JsonProperty("format")
    private String format;

    @SerializedName("settings")
    @JsonProperty("settings")
    private Settings settings;

    @JsonProperty("parameters")
    private Parameters parameters;

    @JsonProperty("metadata")
    private Metadata metadata;

    @JsonProperty("engine")
    private String engine;
}
