package com.devoxx.genie.model.rapidmlx;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Envelope of RapidMLX's OpenAI-compatible {@code GET /v1/models} response:
 * {@code {"object": "list", "data": [ ... ]}}.
 */
@Getter
@Setter
public class RapidMLXModelsResponseDTO {

    @SerializedName("object")
    private String object;

    @SerializedName("data")
    private List<RapidMLXModelEntryDTO> data;
}
