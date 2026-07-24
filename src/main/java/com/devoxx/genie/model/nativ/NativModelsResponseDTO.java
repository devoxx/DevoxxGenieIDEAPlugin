package com.devoxx.genie.model.nativ;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Envelope of Nativ's OpenAI-compatible {@code GET /v1/models} response:
 * {@code {"object": "list", "data": [ ... ]}}.
 */
@Getter
@Setter
public class NativModelsResponseDTO {

    @SerializedName("object")
    private String object;

    @SerializedName("data")
    private List<NativModelEntryDTO> data;
}
