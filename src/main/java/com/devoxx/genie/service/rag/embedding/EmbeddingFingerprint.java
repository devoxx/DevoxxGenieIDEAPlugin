package com.devoxx.genie.service.rag.embedding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Identifies the embedding setup that produced a project's vectors: everything that changes
 * the vector space. Vectors from different setups can't be compared, so a RAG index is bound
 * to exactly one fingerprint: files are only "current" under the fingerprint they were embedded
 * with, and a full re-index with a different fingerprint rebuilds the index from scratch.
 *
 * <p>The query prefix is deliberately left out: changing it doesn't change the stored vectors.
 * The provider's URL is too, since the same model behind a new address gives the same vectors.
 *
 * <p>Stored as {@link #id()} on every manifest entry, for example {@code ollama:bge-m3} or
 * {@code ollama:nomic-embed-text|doc=search_document%3A+}.
 *
 * <p>No IntelliJ dependencies: used by the indexer, the manifest and the standalone CLI.
 */
public record EmbeddingFingerprint(String provider,
                                   String modelName,
                                   @Nullable Integer outputDimension,
                                   String documentPrefix) {

    public static final String OLLAMA = EmbeddingProvider.OLLAMA.id();
    /** The default model, and the one every index was built with before it became configurable. */
    public static final String DEFAULT_MODEL_NAME = "nomic-embed-text";
    /**
     * What every index was built with before TASK-212: nomic-embed-text with no prefixes. Today's
     * default adds nomic's document prefix, so an upgraded install's index is stale exactly once.
     */
    public static final EmbeddingFingerprint LEGACY = new EmbeddingFingerprint(OLLAMA, DEFAULT_MODEL_NAME);

    private static final String LATEST_TAG = ":latest";
    private static final String DIMENSION_OPTION = "dim=";
    private static final String DOCUMENT_PREFIX_OPTION = "doc=";

    // Components deliberately not @NotNull: the build instruments it into a runtime check
    // that would fire before this constructor can reject the value with a clear message.
    public EmbeddingFingerprint {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Embedding provider must not be blank");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Embedding model name must not be blank");
        }
        if (outputDimension != null && outputDimension <= 0) {
            throw new IllegalArgumentException("Output dimension must be positive, was " + outputDimension);
        }
        provider = provider.trim();
        modelName = OLLAMA.equals(provider) ? normalizeModelName(modelName) : modelName.trim();
        documentPrefix = documentPrefix == null ? "" : documentPrefix;
    }

    /** A setup with no prefix and the model's own vector size. */
    public EmbeddingFingerprint(String provider, String modelName) {
        this(provider, modelName, null, "");
    }

    /** Fingerprint for an Ollama model with no prefix; a blank or null name means the default model. */
    public static @NotNull EmbeddingFingerprint ollama(@Nullable String modelName) {
        return modelName == null || modelName.isBlank()
                ? LEGACY
                : new EmbeddingFingerprint(OLLAMA, modelName);
    }

    /** Stable string form, as stored in the manifest. See {@link #parse}. */
    public @NotNull String id() {
        StringBuilder id = new StringBuilder(provider).append(':').append(escapeModelName(modelName));
        if (outputDimension != null) {
            id.append('|').append(DIMENSION_OPTION).append(outputDimension);
        }
        if (!documentPrefix.isEmpty()) {
            id.append('|').append(DOCUMENT_PREFIX_OPTION)
                    .append(URLEncoder.encode(documentPrefix, StandardCharsets.UTF_8));
        }
        return id.toString();
    }

    /**
     * Inverse of {@link #id()}. A null or blank id is an entry written before fingerprints
     * existed, which was always built with {@link #LEGACY}.
     */
    public static @NotNull EmbeddingFingerprint parse(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return LEGACY;
        }
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            throw new IllegalArgumentException("Not an embedding fingerprint: '" + id + "'");
        }
        String[] parts = id.substring(colon + 1).split("\\|");
        Integer dimension = null;
        String documentPrefix = "";
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith(DIMENSION_OPTION)) {
                dimension = parseDimension(id, parts[i].substring(DIMENSION_OPTION.length()));
            } else if (parts[i].startsWith(DOCUMENT_PREFIX_OPTION)) {
                documentPrefix = URLDecoder.decode(
                        parts[i].substring(DOCUMENT_PREFIX_OPTION.length()), StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException("Not an embedding fingerprint: '" + id + "'");
            }
        }
        return new EmbeddingFingerprint(id.substring(0, colon), unescapeModelName(parts[0]), dimension, documentPrefix);
    }

    /** How documents are prepared for this setup. */
    public @NotNull EmbeddingProfile documentProfile() {
        return new EmbeddingProfile("", documentPrefix, outputDimension);
    }

    /** How queries are prepared for an index built with this setup. */
    public @NotNull EmbeddingProfile queryProfile(@Nullable String queryPrefix) {
        return new EmbeddingProfile(queryPrefix, "", outputDimension);
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

    private static int parseDimension(String id, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not an embedding fingerprint: '" + id + "'", e);
        }
    }

    private static String escapeModelName(String modelName) {
        return modelName.replace("%", "%25").replace("|", "%7C");
    }

    private static String unescapeModelName(String escaped) {
        return escaped.replace("%7C", "|").replace("%25", "%");
    }

    @Override
    public String toString() {
        return id();
    }
}
