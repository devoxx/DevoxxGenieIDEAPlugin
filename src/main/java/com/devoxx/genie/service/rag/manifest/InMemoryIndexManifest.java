package com.devoxx.genie.service.rag.manifest;

import com.devoxx.genie.service.rag.IndexerConstants;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    public boolean isCurrent(@NotNull Path file) {
        String key = file.toAbsolutePath().toString();
        IndexManifestEntry entry = entries.get(key);
        if (entry == null) return false;
        if (!IndexerConstants.CURRENT_EMBEDDING_SCHEMA_VERSION.equals(entry.schemaVersion())) {
            return false;
        }
        String currentHash = sha1OrNull(file);
        return currentHash != null && currentHash.equals(entry.contentHash());
    }

    @Override
    public void markIndexed(@NotNull Path file, int segmentCount) {
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
                IndexerConstants.CURRENT_EMBEDDING_SCHEMA_VERSION));
        onMutated();
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
