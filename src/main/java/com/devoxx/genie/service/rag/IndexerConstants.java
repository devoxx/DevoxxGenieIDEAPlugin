package com.devoxx.genie.service.rag;

public final class IndexerConstants {
    public static final String INDEXED_AT = "indexedAt";
    public static final String FILE_PATH = "filePath";
    public static final String LAST_MODIFIED = "lastModified";

    /**
     * Stored on every segment so the indexer can detect — and ignore — entries written by
     * older, buggy versions of the pipeline. Bump this when the storage format or the
     * embedding semantics change in a non-backwards-compatible way.
     *
     * <p>History:
     * <ul>
     *   <li>(unversioned, pre-v2): embedded file paths instead of segment content — unusable for retrieval</li>
     *   <li>v2: embed segment content; per-chunk results; metadata filter for index lookup</li>
     * </ul>
     */
    public static final String EMBEDDING_SCHEMA_VERSION_KEY = "embeddingSchemaVersion";
    public static final String CURRENT_EMBEDDING_SCHEMA_VERSION = "v2";

    private IndexerConstants() {} // Prevent instantiation
}
