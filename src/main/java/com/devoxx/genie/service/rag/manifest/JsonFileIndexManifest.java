package com.devoxx.genie.service.rag.manifest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JSON-backed manifest persisted to the IDE's plugin data dir. Loads its state once on
 * construction; flushes lazily — mutations set a dirty flag and the actual disk write
 * happens on {@link #flush()}. Indexing pipelines should call {@code flush()} when a
 * batch completes; auto-reindex listeners should call it after their debounce fires.
 */
@Slf4j
public class JsonFileIndexManifest extends InMemoryIndexManifest {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final TypeReference<Map<String, IndexManifestEntry>> MAP_TYPE = new TypeReference<>() {};

    private final Path storagePath;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public JsonFileIndexManifest(@NotNull Path storagePath) {
        this.storagePath = storagePath;
        loadFromDisk();
    }

    @Override
    protected void onMutated() {
        dirty.set(true);
    }

    @Override
    public void flush() {
        if (!dirty.getAndSet(false)) return;
        try {
            Files.createDirectories(storagePath.getParent());
            // Snapshot to a plain Map so the serialized form is stable across JVM impls.
            Map<String, IndexManifestEntry> snapshot = Map.copyOf(entries);
            // Atomic write via tmp + move.
            Path tmp = storagePath.resolveSibling(storagePath.getFileName().toString() + ".tmp");
            JSON.writeValue(tmp.toFile(), snapshot);
            try {
                Files.move(tmp, storagePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, storagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Failed to flush index manifest to {}: {}", storagePath, e.getMessage());
            // Mark dirty again so a future flush retries.
            dirty.set(true);
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(storagePath)) return;
        try {
            Map<String, IndexManifestEntry> loaded = JSON.readValue(storagePath.toFile(), MAP_TYPE);
            entries.putAll(loaded);
        } catch (IOException e) {
            log.warn("Failed to load index manifest from {}; starting fresh: {}", storagePath, e.getMessage());
        }
    }
}
