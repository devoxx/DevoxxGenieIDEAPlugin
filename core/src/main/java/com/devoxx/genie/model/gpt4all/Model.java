package com.devoxx.genie.model.gpt4all;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Model {
    @JsonProperty("created")
    private long created;

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("owned_by")
    private String ownedBy;

    @JsonProperty("parent")
    private String parent;

    @JsonProperty("permissions")
    private List<ModelPermission> permissions;

    @JsonProperty("root")
    private String root;
}