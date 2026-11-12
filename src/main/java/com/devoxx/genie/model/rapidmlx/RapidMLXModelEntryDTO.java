package com.devoxx.genie.model.rapidmlx;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/** One entry of Rapid-MLX's OpenAI-compatible {@code GET /v1/models} response. */
@Getter
@Setter
public class RapidMLXModelEntryDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String object;

    @SerializedName("created")
    private Long created;

    @SerializedName("context_window")
    private Integer contextWindow;

    public int resolveContextLength(int fallback) {
        return contextWindow != null && contextWindow > 0 ? contextWindow : fallback;
    }

    /**
     * Strips the Hugging Face org prefix so the model dropdown shows
     * {@code Qwen3.5-4B-MLX-4bit} rather than the full
     * {@code mlx-community/Qwen3.5-4B-MLX-4bit}. Falls back to the raw id when
     * there is no prefix (or nothing after it).
     */
    public @NotNull String resolveDisplayName() {
        if (id == null || id.isBlank()) {
            return "";
        }
        int slash = id.lastIndexOf('/');
        if (slash < 0 || slash == id.length() - 1) {
            return id;
        }
        return id.substring(slash + 1);
    }
}
