package com.devoxx.genie.model.gpt4all;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResponseDTO {
    @JsonProperty("data")
    private List<Model> data;

    @JsonProperty("object")
    private String object;
}