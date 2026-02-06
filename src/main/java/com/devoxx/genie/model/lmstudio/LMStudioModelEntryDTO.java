package com.devoxx.genie.model.lmstudio;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Setter
@Getter
public class LMStudioModelEntryDTO {
    @SerializedName(value = "id", alternate = {"key"})
    private String id;
    @SerializedName("display_name")
    private String displayName;
    private String object;
    private String owned_by;

    @SerializedName(value = "max_context_length", alternate = {"maxContextLength"})
    private Integer max_context_length;
    private Integer context_length;
    private LoadedInstance[] loaded_instances;

    public String resolveModelName() {
        return id;
    }

    public String resolveDisplayName() {
        return displayName != null ? displayName : id;
    }

    public Integer resolveContextLength() {
        if (max_context_length != null) {
            return max_context_length;
        }

        if (context_length != null) {
            return context_length;
        }

        if (loaded_instances == null) {
            return null;
        }

        return Arrays.stream(loaded_instances)
                .map(LoadedInstance::getConfig)
                .filter(config -> config != null && config.getContext_length() != null)
                .map(Config::getContext_length)
                .findFirst()
                .orElse(null);
    }

    public int resolveContextLengthOrDefault(int defaultValue) {
        Integer resolvedContextLength = resolveContextLength();
        return resolvedContextLength != null ? resolvedContextLength : defaultValue;
    }

    @Setter
    @Getter
    public static class LoadedInstance {
        private Config config;
    }

    @Setter
    @Getter
    public static class Config {
        private Integer context_length;
    }
}
