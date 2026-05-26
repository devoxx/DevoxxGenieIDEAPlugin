package com.devoxx.genie.service.rag;

/**
 * A single semantic-search hit: which chunk matched the query, where it came from, and how well.
 *
 * @param filePath absolute path of the source file the chunk was extracted from
 * @param score    similarity score (0.0–1.0) returned by the vector store
 * @param content  the chunk text as it was embedded and stored (NOT the full file contents)
 */
public record SearchResult(String filePath, Double score, String content) {
}
