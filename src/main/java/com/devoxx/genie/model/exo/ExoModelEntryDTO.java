package com.devoxx.genie.model.exo;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExoModelEntryDTO {
    private String id;
    private String name;
    @SerializedName("context_length")
    private int contextLength;
    @SerializedName("storage_size_megabytes")
    private int storageSizeMegabytes;
    @SerializedName("supports_tensor")
    private boolean supportsTensor;
    private String family;
    private String quantization;
    @SerializedName("base_model")
    private String baseModel;
    private String[] capabilities;
}
