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
 * @param schemaVersion  embedding schema version under which the file was indexed
 */
public record IndexManifestEntry(
        @JsonProperty("contentHash") String contentHash,
        @JsonProperty("lastModified") long lastModified,
        @JsonProperty("indexedAt") long indexedAt,
        @JsonProperty("segmentCount") int segmentCount,
        @JsonProperty("schemaVersion") String schemaVersion
) {
    @JsonCreator
    public IndexManifestEntry {}
}
