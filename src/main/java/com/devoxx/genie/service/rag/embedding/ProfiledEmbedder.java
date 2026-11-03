package com.devoxx.genie.service.rag.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps an {@link EmbeddingModel} with an {@link EmbeddingProfile}, keeping queries and
 * documents on separate paths so each gets its own prefix.
 *
 * <p>Prefixes are applied to copies of the text sent to the model; callers' segments are
 * never modified, so stored chunk text and anything shown to the LLM stay prefix-free.
 *
 * <p>IDE-independent: used by the standalone retrieval benchmark today and intended for the
 * plugin's own indexing/search path (task-265).
 */
public final class ProfiledEmbedder {

    private final EmbeddingModel model;
    private final EmbeddingProfile profile;

    public ProfiledEmbedder(@NotNull EmbeddingModel model, @NotNull EmbeddingProfile profile) {
        this.model = model;
        this.profile = profile;
    }

    public @NotNull EmbeddingProfile profile() {
        return profile;
    }

    /** Embed a search query (with the query prefix). */
    public @NotNull Embedding embedQuery(@NotNull String query) {
        return shape(model.embed(profile.queryPrefix() + query).content());
    }

    /** Embed a single document chunk (with the document prefix). */
    public @NotNull Embedding embedDocument(@NotNull String text) {
        return shape(model.embed(profile.documentPrefix() + text).content());
    }

    /** Embed document chunks in one model call (with the document prefix); order is preserved. */
    public @NotNull List<Embedding> embedDocuments(@NotNull List<TextSegment> segments) {
        List<TextSegment> prefixed = new ArrayList<>(segments.size());
        for (TextSegment segment : segments) {
            prefixed.add(TextSegment.from(profile.documentPrefix() + segment.text()));
        }
        List<Embedding> raw = model.embedAll(prefixed).content();
        List<Embedding> shaped = new ArrayList<>(raw.size());
        for (Embedding e : raw) {
            shaped.add(shape(e));
        }
        return shaped;
    }

    private @NotNull Embedding shape(@NotNull Embedding embedding) {
        Integer k = profile.outputDimension();
        if (k == null) {
            return embedding;
        }
        return Embedding.from(truncateAndNormalize(embedding.vector(), k));
    }

    /**
     * Keep the first {@code k} values of {@code vector} and scale them to unit length.
     * Works whether or not the backend honours a requested dimension. An all-zero prefix is
     * returned as zeros rather than NaN. The input array is not modified.
     *
     * @throws IllegalArgumentException if {@code k} exceeds the vector's native dimension
     */
    public static float @NotNull [] truncateAndNormalize(float @NotNull [] vector, int k) {
        if (k > vector.length) {
            throw new IllegalArgumentException("Configured output dimension " + k
                    + " is larger than the model's native dimension " + vector.length);
        }
        float[] out = new float[k];
        double sumSquares = 0;
        for (int i = 0; i < k; i++) {
            out[i] = vector[i];
            sumSquares += (double) vector[i] * vector[i];
        }
        if (sumSquares == 0) {
            return out;
        }
        double norm = Math.sqrt(sumSquares);
        for (int i = 0; i < k; i++) {
            out[i] = (float) (out[i] / norm);
        }
        return out;
    }
}
