package com.devoxx.genie.service.rag.manifest;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

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

    /** True iff {@code file} is recorded and its content hash matches what's on disk. */
    boolean isCurrent(@NotNull Path file);

    /** Record that {@code file} has been indexed; computes and stores its current content hash. */
    void markIndexed(@NotNull Path file, int segmentCount);

    /** Drop {@code file} from the manifest (e.g. file deleted). */
    void markRemoved(@NotNull Path file);

    /** Persist any in-memory state. No-op for in-memory implementations. */
    default void flush() {}
}
