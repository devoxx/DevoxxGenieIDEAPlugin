package com.devoxx.genie.model.nativ;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * One entry of Nativ's OpenAI-compatible {@code GET /v1/models} response.
 * <p>
 * Nativ (built on MLX-VLM) reports only the bare OpenAI model shape — the model id is the
 * Hugging Face repo id of a locally downloaded MLX model, e.g.
 * {@code mlx-community/Qwen2.5-Coder-7B-Instruct-4bit}. There is deliberately no context-length
 * field: the server does not expose one, which is why
 * {@code NativChatModelFactory} falls back to a user-configurable value.
 */
@Getter
@Setter
public class NativModelEntryDTO {

    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String object;

    @SerializedName("created")
    private Long created;

    /**
     * Strips the Hugging Face org prefix so the model dropdown shows
     * {@code Qwen2.5-Coder-7B-Instruct-4bit} rather than the full
     * {@code mlx-community/Qwen2.5-Coder-7B-Instruct-4bit}. Falls back to the raw id when
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
