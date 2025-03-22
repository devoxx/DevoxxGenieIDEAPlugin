package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ResponseDTO {

    @JsonProperty("data")
    private List<Data> data;

    @JsonProperty("object")
    private String object;
}
