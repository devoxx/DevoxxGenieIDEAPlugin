package com.devoxx.genie.service.rag.manifest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Source of truth for "which files in this project are currently indexed under the active
 * embedding schema." Replaces the earlier reliance on probing the vector store with a fake
 * query to figure out whether a file had been processed.
 *
 * <p>Compared to mtime-only change detection, the manifest also stores a content hash so
 * branch switches, git operations, or formatter-driven re-saves that touch mtime without
 * changing the actual bytes do not trigger spurious re-indexing.
 */
public interface IndexManifest {

    /**
     * True iff {@code file} is recorded under the current storage schema, was embedded with
     * {@code fingerprint} (an {@link com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint#id()}),
     * and its content hash matches what's on disk.
     */
    boolean isCurrent(@NotNull Path file, @NotNull String fingerprint);

    /** True iff {@code file} is recorded at all, regardless of whether its hash is still current.
     *  Used by the file watcher to scope auto-reindex to files the user has explicitly indexed
     *  before — new files require a manual full index so the watcher can't accidentally bootstrap. */
    boolean isTracked(@NotNull Path file);

    /**
     * Record that {@code file} has been indexed with the embedding setup {@code fingerprint};
     * computes and stores its current content hash.
     */
    default void markIndexed(@NotNull Path file, int segmentCount, @NotNull String fingerprint) {
        markIndexed(file, segmentCount, fingerprint, null);
    }

    /** As above, also recording the query prefix that goes with the index. */
    void markIndexed(@NotNull Path file, int segmentCount, @NotNull String fingerprint, @Nullable String queryPrefix);

    /** Drop {@code file} from the manifest (e.g. file deleted). */
    void markRemoved(@NotNull Path file);

    /** Drop every entry (the index is being rebuilt from scratch). */
    void clear();

    /**
     * The embedding fingerprint the project's index was built with, or empty when nothing is
     * indexed. Entries written before fingerprints existed report the legacy default. Search
     * and incremental re-indexing use this, not the configured model, so RAG keeps working
     * after a model change until the user rebuilds the index.
     */
    @NotNull Optional<String> indexFingerprint();

    /**
     * The query prefix recorded with the index; empty string when none was recorded, as with an
     * index built before prefixes existed.
     */
    default @NotNull String indexQueryPrefix() {
        return "";
    }

    /** Persist any in-memory state. No-op for in-memory implementations. */
    default void flush() {}

    /**
     * Snapshot of every absolute file path currently tracked by the manifest. Used by the
     * RAG indexer (task-220) to retroactively drop chunks for files that fall under a newly-
     * added exclusion. Default impl returns an empty collection so test stubs that don't care
     * still satisfy the interface.
     */
    default @NotNull Collection<Path> trackedPaths() {
        return Collections.emptyList();
    }
}
