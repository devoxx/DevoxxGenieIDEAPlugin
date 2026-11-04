package com.devoxx.genie.service.rag.embedding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

/**
 * Where RAG embeddings come from. Reuses the providers configured in the LLM settings: their
 * URL and credential, with no separate RAG endpoint settings.
 *
 * <p>Every provider except Ollama speaks the OpenAI {@code /embeddings} API.
 */
public enum EmbeddingProvider {
    OLLAMA("ollama", "Ollama", List.of(
            EmbeddingFingerprint.DEFAULT_MODEL_NAME,
            "nomic-embed-text-v2-moe",
            "embeddinggemma-2",
            "embeddinggemma",
            "qwen3-embedding",
            "bge-m3",
            "bge-large",
            "mxbai-embed-large",
            "snowflake-arctic-embed",
            "snowflake-arctic-embed2",
            "all-minilm",
            "granite-embedding",
            "paraphrase-multilingual")),
    CUSTOM_OPENAI("custom-openai", "Custom OpenAI", List.of()),
    OPENAI("openai", "OpenAI", List.of(
            "text-embedding-3-small",
            "text-embedding-3-large")),
    LM_STUDIO("lmstudio", "LM Studio", List.of(
            "text-embedding-nomic-embed-text-v1.5")),
    LLAMA_CPP("llamacpp", "llama.cpp", List.of());

    private final String id;
    private final String displayName;
    private final String defaultModel;
    private final List<String> suggestedModels;

    /** @param models the provider's models, default first; shown to the user alphabetically */
    EmbeddingProvider(String id, String displayName, List<String> models) {
        this.id = id;
        this.displayName = displayName;
        this.defaultModel = models.isEmpty() ? "" : models.get(0);
        this.suggestedModels = models.stream().sorted().toList();
    }

    /** Stable id, stored in settings and in the embedding fingerprint. */
    public @NotNull String id() {
        return id;
    }

    /** Alphabetical, for the model dropdown. */
    public @NotNull List<String> suggestedModels() {
        return suggestedModels;
    }

    /** The model preselected when switching to this provider; empty if it has none. */
    public @NotNull String defaultModel() {
        return defaultModel;
    }

    /** The provider with this id; null, blank or unknown means {@link #OLLAMA}. */
    public static @NotNull EmbeddingProvider fromId(@Nullable String id) {
        for (EmbeddingProvider provider : values()) {
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        return OLLAMA;
    }

    /**
     * True when embedding through this provider sends project source code off this machine:
     * always for OpenAI, and for any other provider whose URL isn't a loopback address.
     */
    public boolean sendsCodeOffMachine(@Nullable String baseUrl) {
        return this == OPENAI || !isLoopback(baseUrl);
    }

    private static boolean isLoopback(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return true; // Not configured yet, so nothing can be sent anywhere.
        }
        try {
            String host = URI.create(baseUrl.trim()).getHost();
            return host != null && (host.equalsIgnoreCase("localhost")
                    || host.startsWith("127.")
                    || host.equals("0.0.0.0")
                    || host.equals("[::1]"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
