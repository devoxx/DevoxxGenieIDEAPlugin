package com.devoxx.genie.model.jan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Settings {
    @JsonProperty("ctx_len")
    private int ctxLen;

    @JsonProperty("prompt_template")
    private String promptTemplate;

    @JsonProperty("llama_model_path")
    private String llamaModelPath;

    @JsonProperty("ngl")
    private int ngl;
}
