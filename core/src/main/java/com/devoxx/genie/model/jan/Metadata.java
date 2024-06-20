package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Metadata {
    @JsonProperty("author")
    private String author;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("size")
    private long size;

    @JsonProperty("cover")
    private String cover;
}
