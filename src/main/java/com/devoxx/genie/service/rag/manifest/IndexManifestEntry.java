package com.devoxx.genie.service.rag.manifest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row of the per-project index manifest.
 *
 * @param contentHash    hex SHA-1 of the file's bytes when it was indexed
 * @param lastModified   file's {@code lastModifiedTime} in millis when it was indexed (debug-only signal)
 * @param indexedAt      wall-clock time the entry was written
 * @param segmentCount   how many chunks the file was split into
 * @param schemaVersion  embedding schema (storage format) version under which the file was indexed
 * @param embeddingFingerprint {@link com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint#id()}
 *                       of the embedding setup that produced the file's vectors; {@code null} for
 *                       entries written before TASK-212, which were all built with nomic-embed-text
 * @param queryPrefix    the query prefix configured when the file was indexed; {@code null} means
 *                       none. Not part of the fingerprint, but a stale index still needs it, because
 *                       search must keep using the settings that built the index
 */
public record IndexManifestEntry(
        @JsonProperty("contentHash") String contentHash,
        @JsonProperty("lastModified") long lastModified,
        @JsonProperty("indexedAt") long indexedAt,
        @JsonProperty("segmentCount") int segmentCount,
        @JsonProperty("schemaVersion") String schemaVersion,
        @JsonProperty("embeddingFingerprint") String embeddingFingerprint,
        @JsonProperty("queryPrefix") String queryPrefix
) {
    @JsonCreator
    public IndexManifestEntry {}
}
