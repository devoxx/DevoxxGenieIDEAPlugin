package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Settings {

    @SerializedName("ctx_len")
    @JsonProperty("ctx_len")
    private Integer ctxLen;

    @JsonProperty("prompt_template")
    private String promptTemplate;

    @JsonProperty("llama_model_path")
    private String llamaModelPath;

    @JsonProperty("ngl")
    private int ngl;
}
