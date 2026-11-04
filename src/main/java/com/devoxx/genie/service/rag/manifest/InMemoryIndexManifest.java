package com.devoxx.genie.service.rag.manifest;

import com.devoxx.genie.service.rag.IndexerConstants;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Volatile, process-local manifest. Used by tests and as the default when no Project
 * context is available. Concrete persistent implementations subclass this and override
 * {@link #flush()} (and load their state in their constructor).
 */
@Slf4j
public class InMemoryIndexManifest implements IndexManifest {

    protected final ConcurrentMap<String, IndexManifestEntry> entries = new ConcurrentHashMap<>();

    @Override
    public boolean isTracked(@NotNull Path file) {
        return entries.containsKey(file.toAbsolutePath().toString());
    }

    @Override
    public @NotNull java.util.Collection<Path> trackedPaths() {
        // Snapshot via toList so callers can iterate without holding the lock and so concurrent
        // mutations during a sweep don't surprise them.
        return entries.keySet().stream()
                .map(Path::of)
                .toList();
    }

    @Override
    public boolean isCurrent(@NotNull Path file, @NotNull String fingerprint) {
        String key = file.toAbsolutePath().toString();
        IndexManifestEntry entry = entries.get(key);
        if (entry == null) return false;
        if (!IndexerConstants.CURRENT_EMBEDDING_SCHEMA_VERSION.equals(entry.schemaVersion())) {
            return false;
        }
        if (!fingerprintOf(entry).equals(fingerprint)) {
            return false;
        }
        String currentHash = sha1OrNull(file);
        return currentHash != null && currentHash.equals(entry.contentHash());
    }

    @Override
    public void markIndexed(@NotNull Path file, int segmentCount, @NotNull String fingerprint,
                            @org.jetbrains.annotations.Nullable String queryPrefix) {
        String key = file.toAbsolutePath().toString();
        String hash = sha1OrNull(file);
        if (hash == null) {
            log.debug("Skipping manifest entry for unreadable file: {}", file);
            return;
        }
        long lastModified;
        try {
            lastModified = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            lastModified = 0L;
        }
        entries.put(key, new IndexManifestEntry(
                hash,
                lastModified,
                System.currentTimeMillis(),
                segmentCount,
                IndexerConstants.CURRENT_EMBEDDING_SCHEMA_VERSION,
                fingerprint,
                queryPrefix));
        onMutated();
    }

    @Override
    public void clear() {
        if (!entries.isEmpty()) {
            entries.clear();
            onMutated();
        }
    }

    @Override
    public @NotNull Optional<String> indexFingerprint() {
        // A full re-index with a different fingerprint clears the manifest first, so all
        // entries share one fingerprint; any entry is representative.
        return entries.values().stream().findFirst().map(InMemoryIndexManifest::fingerprintOf);
    }

    @Override
    public @NotNull String indexQueryPrefix() {
        return entries.values().stream().findFirst()
                .map(IndexManifestEntry::queryPrefix)
                .orElse("");
    }

    /** Legacy (pre-TASK-212) entries carry no fingerprint; they were built with the legacy setup. */
    private static @NotNull String fingerprintOf(@NotNull IndexManifestEntry entry) {
        return EmbeddingFingerprint.parse(entry.embeddingFingerprint()).id();
    }

    @Override
    public void markRemoved(@NotNull Path file) {
        if (entries.remove(file.toAbsolutePath().toString()) != null) {
            onMutated();
        }
    }

    /** Hook for persistent subclasses; called after every mutation. Default is no-op. */
    protected void onMutated() {}

    static String sha1OrNull(@NotNull Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(file);
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }
}
