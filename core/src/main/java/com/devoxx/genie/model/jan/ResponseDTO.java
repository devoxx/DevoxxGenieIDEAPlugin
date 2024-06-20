package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ResponseDTO {

    @JsonProperty("object")
    private String object;

    @JsonProperty("data")
    private List<Data> data;

}
