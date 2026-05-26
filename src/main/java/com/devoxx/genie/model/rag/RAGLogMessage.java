package com.devoxx.genie.model.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

/**
 * One RAG retrieval event surfaced to the unified log panel. Carries enough detail that a
 * developer can audit "did RAG actually pick the right chunks for this query?" without
 * tailing {@code idea.log}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGLogMessage {

    /** Identifies the project that produced this message; used to filter cross-project noise. */
    private String projectLocationHash;

    /** The user prompt that was sent through the retriever (already trimmed). */
    private String query;

    /** Embedding model the retriever used (e.g. {@code nomic-embed-text}). May be null in tests. */
    private String embeddingModel;

    /** Configured minimum similarity score for this search. */
    private Double minScore;

    /** Configured top-K (max results) for this search. */
    private Integer maxResults;

    /** One entry per retrieved chunk; preserves duplicates from the same file. */
    @Singular("hit")
    private List<Hit> hits;

    /** Total time the retrieval took, in milliseconds. */
    private long durationMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hit {
        private String filePath;
        private Double score;
        /** Truncated preview of the chunk content (full text remains in the prompt). */
        private String preview;
        /** Length of the original (un-truncated) chunk in characters. */
        private int chunkLength;
    }
}
