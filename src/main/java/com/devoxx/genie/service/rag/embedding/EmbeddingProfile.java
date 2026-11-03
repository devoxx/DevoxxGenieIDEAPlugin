package com.devoxx.genie.service.rag.embedding;

import org.jetbrains.annotations.Nullable;

/**
 * How texts are prepared for, and vectors taken from, an embedding model.
 *
 * <ul>
 *   <li>{@code queryPrefix} / {@code documentPrefix}: many embedding models are asymmetric and
 *       expect a short instruction in front of search queries and indexed documents (e.g.
 *       nomic-embed-text's {@code "search_query: "} / {@code "search_document: "}). Prefixes
 *       only change the text sent to the model; they are never stored or shown.</li>
 *   <li>{@code outputDimension}: when set, vectors are cut to their first {@code k} values and
 *       re-normalised to unit length (Matryoshka truncation). {@code null} keeps the model's
 *       vector exactly as returned.</li>
 * </ul>
 *
 * <p>{@code null} prefixes are accepted and normalised to {@code ""}, so the accessors never
 * return {@code null}. (The components are deliberately not annotated {@code @NotNull}: the
 * IntelliJ build instruments that annotation with a runtime check that would reject
 * {@code null} before the normalisation runs.)
 *
 * <p>Changing {@code documentPrefix} or {@code outputDimension} changes the stored vectors,
 * so an existing index must be rebuilt; changing {@code queryPrefix} does not.
 */
public record EmbeddingProfile(String queryPrefix,
                               String documentPrefix,
                               @Nullable Integer outputDimension) {

    /** No prefixes, no truncation: vectors identical to calling the model directly. */
    public static final EmbeddingProfile NONE = new EmbeddingProfile("", "", null);

    public EmbeddingProfile {
        queryPrefix = queryPrefix == null ? "" : queryPrefix;
        documentPrefix = documentPrefix == null ? "" : documentPrefix;
        if (outputDimension != null && outputDimension <= 0) {
            throw new IllegalArgumentException("outputDimension must be positive, was " + outputDimension);
        }
    }
}
