package com.devoxx.genie.service.rag.embedding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifies the embedding setup that produced a project's vectors. Vectors from different
 * setups can't be compared, so a RAG index is bound to exactly one fingerprint: files are
 * only "current" under the fingerprint they were embedded with, and a full re-index with a
 * different fingerprint rebuilds the index from scratch.
 *
 * <p>Stored as {@link #id()} (e.g. {@code ollama:nomic-embed-text}) on every manifest entry.
 * TASK-265 extends it with the provider URL kind, output dimension and document prefix.
 *
 * <p>No IntelliJ dependencies: used by the indexer, the manifest and the standalone CLI.
 */
public record EmbeddingFingerprint(String provider, String modelName) {

    public static final String OLLAMA = "ollama";
    /** The model every index was built with before the model became configurable. */
    public static final String DEFAULT_MODEL_NAME = "nomic-embed-text";
    public static final EmbeddingFingerprint DEFAULT = new EmbeddingFingerprint(OLLAMA, DEFAULT_MODEL_NAME);

    private static final String LATEST_TAG = ":latest";

    // Components deliberately not @NotNull: the build instruments it into a runtime check
    // that would fire before this constructor can reject the value with a clear message.
    public EmbeddingFingerprint {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Embedding provider must not be blank");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Embedding model name must not be blank");
        }
        provider = provider.trim();
        modelName = normalizeModelName(modelName);
    }

    /** Fingerprint for an Ollama model; a blank or null name means {@link #DEFAULT_MODEL_NAME}. */
    public static @NotNull EmbeddingFingerprint ollama(@Nullable String modelName) {
        return modelName == null || modelName.isBlank()
                ? DEFAULT
                : new EmbeddingFingerprint(OLLAMA, modelName);
    }

    /** Stable string form, {@code <provider>:<model>}, as stored in the manifest. */
    public @NotNull String id() {
        return provider + ":" + modelName;
    }

    /**
     * Inverse of {@link #id()}. A null or blank id is an entry written before fingerprints
     * existed, which was always built with {@link #DEFAULT}.
     */
    public static @NotNull EmbeddingFingerprint parse(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return DEFAULT;
        }
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            throw new IllegalArgumentException("Not an embedding fingerprint: '" + id + "'");
        }
        return new EmbeddingFingerprint(id.substring(0, colon), id.substring(colon + 1));
    }

    /** Trims and drops a {@code :latest} tag, which Ollama treats as the same model as no tag. */
    public static @NotNull String normalizeModelName(@NotNull String modelName) {
        String trimmed = modelName.trim();
        return trimmed.endsWith(LATEST_TAG)
                ? trimmed.substring(0, trimmed.length() - LATEST_TAG.length())
                : trimmed;
    }

    /** True when two Ollama model names refer to the same model (ignoring {@code :latest}). */
    public static boolean sameOllamaModel(@NotNull String a, @NotNull String b) {
        return normalizeModelName(a).equals(normalizeModelName(b));
    }

    @Override
    public String toString() {
        return id();
    }
}
