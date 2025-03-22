package com.devoxx.genie.model.gpt4all;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelPermission {
    @JsonProperty("allow_create_engine")
    private boolean allowCreateEngine;

    @JsonProperty("allow_fine_tuning")
    private boolean allowFineTuning;

    @JsonProperty("allow_logprobs")
    private boolean allowLogprobs;

    @JsonProperty("allow_sampling")
    private boolean allowSampling;

    @JsonProperty("allow_search_indices")
    private boolean allowSearchIndices;

    @JsonProperty("allow_view")
    private boolean allowView;

    @JsonProperty("created")
    private long created;

    @JsonProperty("group")
    private String group;

    @JsonProperty("id")
    private String id;

    @JsonProperty("is_blocking")
    private boolean isBlocking;

    @JsonProperty("object")
    private String object;

    @JsonProperty("organization")
    private String organization;
}