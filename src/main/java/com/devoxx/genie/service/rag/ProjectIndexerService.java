package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
import com.devoxx.genie.service.rag.manifest.IndexManifest;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.service.rag.manifest.InMemoryIndexManifest;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.service.rag.IndexerConstants.*;

@Slf4j
@Service
public final class ProjectIndexerService {

    /** Chunk size in tokens for the recursive splitter. */
    static final int CHUNK_SIZE_TOKENS = 500;
    /** Chunk overlap in tokens; non-zero so a symbol straddling a boundary isn't lost. */
    static final int CHUNK_OVERLAP_TOKENS = 50;

    /** Max segments per Ollama {@code embedAll} call. Keeps individual requests small enough
     *  to avoid timeouts while still avoiding the N+1 cost of one-call-per-segment. */
    static final int EMBEDDING_BATCH_SIZE = 64;

    /** Number of files processed in parallel during bulk indexing. Conservative default —
     *  Ollama serializes embed requests by default and the embedding model also competes
     *  for CPU, so going above 4 typically regresses throughput. */
    static final int INDEXING_PARALLELISM = 2;

    private final ChromaEmbeddingService chromaEmbeddingService;
    private final ProjectScannerService projectScannerService;

    private DocumentSplitter documentSplitter;

    /**
     * Source of truth for which files are indexed under the current schema. Defaults to a
     * process-local in-memory manifest so tests work without filesystem setup; production
     * flows swap in the per-project JSON-backed manifest via {@link #indexFiles}.
     */
    private IndexManifest manifest = new InMemoryIndexManifest();

    // Flag to indicate if indexing should be cancelled
    private final AtomicBoolean cancelIndexing = new AtomicBoolean(false);

    @NotNull
    public static ProjectIndexerService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectIndexerService.class);
    }

    public ProjectIndexerService() {
        this.chromaEmbeddingService = ChromaEmbeddingService.getInstance();
        this.projectScannerService = ProjectScannerService.getInstance();
    }

    public void cancelIndexing() {
        cancelIndexing.set(true);
        log.info("Indexing cancellation requested");
    }

    public boolean isIndexingCancelled() {
        return cancelIndexing.get();
    }

    public void resetCancellationFlag() {
        cancelIndexing.set(false);
    }

    /** Cached default (recursive) splitter — used for source code and unknown file types. */
    private synchronized DocumentSplitter defaultSplitter() {
        if (documentSplitter == null) {
            documentSplitter = DocumentSplitters.recursive(CHUNK_SIZE_TOKENS, CHUNK_OVERLAP_TOKENS);
        }
        return documentSplitter;
    }

    /**
     * Pick a splitter appropriate to the file's content type. Real wins come from honoring
     * obvious natural boundaries:
     * <ul>
     *   <li>Markdown: split on paragraphs first so headers, code blocks, and bullet lists stay
     *       together when they fit. Falls back to line/word for any single paragraph over the
     *       chunk-size budget.</li>
     *   <li>Source code: line splitting respects statement boundaries better than the generic
     *       recursive separator order does for code (which prefers paragraph breaks, then
     *       newlines, then sentence punctuation — the last of which is wrong for code).</li>
     *   <li>Everything else: the default recursive splitter.</li>
     * </ul>
     * Per-language AST chunking is Phase 3; this is a cheap, library-only first pass.
     */
    DocumentSplitter splitterFor(@NotNull Path path) {
        String ext = extensionOf(path);
        return switch (ext) {
            case "md", "mdx", "markdown" ->
                    new DocumentByParagraphSplitter(CHUNK_SIZE_TOKENS, CHUNK_OVERLAP_TOKENS,
                            defaultSplitter());
            case "java", "kt", "kts", "py", "js", "mjs", "cjs", "ts", "tsx", "jsx",
                 "go", "rs", "cpp", "cc", "cxx", "hpp", "h", "c", "php", "rb", "scala" ->
                    new DocumentByLineSplitter(CHUNK_SIZE_TOKENS, CHUNK_OVERLAP_TOKENS,
                            defaultSplitter());
            default -> defaultSplitter();
        };
    }

    private static String extensionOf(@NotNull Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase();
    }

    @TestOnly
    public void setManifest(@NotNull IndexManifest manifest) {
        this.manifest = manifest;
    }

    public void indexFiles(Project project,
                           boolean forceReindex,
                           JProgressBar progressBar,
                           JLabel progressLabel) {
        resetCancellationFlag();

        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setValue(0);
            progressBar.setVisible(true);
            progressLabel.setText("Initializing indexing process...");
            progressLabel.setVisible(true);
        } else {
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(0);
                progressBar.setVisible(true);
                progressLabel.setText("Initializing indexing process...");
                progressLabel.setVisible(true);
            });
        }

        chromaEmbeddingService.init(project);
        this.manifest = IndexManifestService.getInstance().forProject(project);

        String basePath = project.getBasePath();
        if (basePath == null) {
            log.warn("Project base path is null");
            return;
        }

        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) {
            log.debug("Could not find base directory: {}", basePath);
            return;
        }

        ScanContentResult scanResult = projectScannerService.scanProject(project, baseDir, Integer.MAX_VALUE, false);
        List<Path> filesToProcess = new ArrayList<>(scanResult.getFiles());
        int totalFiles = filesToProcess.size();

        // Bounded parallel pool: a handful of file-processing threads share access to the
        // batched embedAll / Chroma writes. We still cap progress + cancellation checks at the
        // file boundary, since per-file work is the meaningful unit.
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(
                INDEXING_PARALLELISM,
                r -> {
                    Thread t = new Thread(r, "DevoxxGenie-Indexer");
                    t.setDaemon(true);
                    return t;
                });
        java.util.concurrent.atomic.AtomicInteger processedCount = new java.util.concurrent.atomic.AtomicInteger();
        try {
            java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>(totalFiles);
            for (Path path : filesToProcess) {
                futures.add(pool.submit(() -> {
                    if (isIndexingCancelled()) return;
                    indexFile(path, forceReindex);
                    int done = processedCount.incrementAndGet();
                    int progress = (int) (((double) done / totalFiles) * 100);
                    String fileName = path.getFileName().toString();
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(true);
                        progressLabel.setVisible(true);
                        progressBar.setValue(progress);
                        progressLabel.setText(String.format("Processing %d of %d: %s", done, totalFiles, fileName));
                    });
                }));
            }
            for (java.util.concurrent.Future<?> f : futures) {
                if (isIndexingCancelled()) break;
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException ee) {
                    log.warn("Indexer task failed: {}", ee.getCause() == null ? ee.getMessage() : ee.getCause().getMessage());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (isIndexingCancelled()) {
                int processed = processedCount.get();
                log.info("Indexing cancelled after processing {} of {} files", processed, totalFiles);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressLabel.setText(String.format("Indexing cancelled after processing %d of %d files",
                            processed, totalFiles));
                });
            }
        } finally {
            pool.shutdownNow();
            manifest.flush();
            resetCancellationFlag();
        }
    }

    /**
     * Index a single file: skip if the content hash already matches what's recorded in the
     * manifest, otherwise split, embed, and store each chunk. Exposed for tests and for
     * future incremental flows (e.g. a {@code BulkFileListener}).
     */
    public void indexFile(Path filePath) {
        indexFile(filePath, false);
        manifest.flush();
    }

    /**
     * Re-index a set of files belonging to {@code project}. Wraps the manifest swap +
     * per-file processing + flush in one call so callers (notably the file watcher) don't
     * have to know about manifest plumbing. Skips files the manifest has never seen — new
     * files require a manual full re-index so a casual edit can't bootstrap an unindexed
     * tree of generated/binary/excluded content.
     */
    public void reindexFiles(@NotNull Project project, @NotNull Collection<Path> files) {
        if (files.isEmpty()) return;
        chromaEmbeddingService.init(project);
        this.manifest = IndexManifestService.getInstance().forProject(project);
        try {
            for (Path file : files) {
                if (!manifest.isTracked(file)) continue;
                // Drop any existing chunks for this file before re-embedding, otherwise edits
                // accumulate stale chunks alongside fresh ones.
                removeChunksFromStore(file);
                indexFile(file, true);
            }
        } finally {
            manifest.flush();
        }
    }

    /**
     * Remove the given files from the vector store and the manifest. Called by the file
     * watcher on delete events so the index doesn't keep returning chunks for files that
     * no longer exist on disk.
     */
    public void removeFiles(@NotNull Project project, @NotNull Collection<Path> files) {
        if (files.isEmpty()) return;
        chromaEmbeddingService.init(project);
        this.manifest = IndexManifestService.getInstance().forProject(project);
        try {
            for (Path file : files) {
                if (!manifest.isTracked(file)) continue;
                removeChunksFromStore(file);
                manifest.markRemoved(file);
            }
        } finally {
            manifest.flush();
        }
    }

    private void removeChunksFromStore(@NotNull Path file) {
        try {
            Filter filter = MetadataFilterBuilder.metadataKey(FILE_PATH)
                    .isEqualTo(file.toAbsolutePath().toString());
            chromaEmbeddingService.getEmbeddingStore().removeAll(filter);
        } catch (Exception e) {
            // EmbeddingStore.removeAll(Filter) is a default method and a few legacy stores
            // throw UnsupportedOperationException. Log and continue — at worst we leave stale
            // chunks until the next full reindex.
            log.warn("Could not remove existing chunks for {}: {}", file, e.getMessage());
        }
    }

    private void indexFile(Path filePath, boolean forceReindex) {
        log.debug("Indexing file: {}", filePath);
        try {
            if (!forceReindex && manifest.isCurrent(filePath)) {
                log.debug("File already indexed (content hash matches): {}", filePath);
                return;
            }
            int segmentCount = processPath(filePath);
            if (segmentCount > 0) {
                manifest.markIndexed(filePath, segmentCount);
                log.debug("File successfully indexed ({} segments): {}", segmentCount, filePath);
            }
        } catch (Exception e) {
            log.warn("Error indexing file: {} - {}", filePath, e.getMessage());
        }
    }

    /**
     * Embed and store a batch of segments in one shot. The previous implementation made
     * one HTTP call to Ollama per segment, turning a 200-chunk file into 200 sequential
     * roundtrips. Embedding in batches and using {@code addAll} on the store collapses
     * that to {@code ceil(N / EMBEDDING_BATCH_SIZE)} calls and one store write per batch.
     */
    private void storeSegments(@NotNull List<TextSegment> segments) {
        if (segments.isEmpty()) return;
        EmbeddingModel embeddingModel = chromaEmbeddingService.getEmbeddingModel();
        for (int from = 0; from < segments.size(); from += EMBEDDING_BATCH_SIZE) {
            int to = Math.min(from + EMBEDDING_BATCH_SIZE, segments.size());
            List<TextSegment> batch = segments.subList(from, to);
            try {
                List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                chromaEmbeddingService.getEmbeddingStore().addAll(embeddings, batch);
            } catch (Exception batchEx) {
                // Some embedding backends choke on a single bad segment and reject the whole
                // batch. Fall back to per-segment so one malformed chunk doesn't poison the
                // rest of the file.
                log.warn("Batch embed failed ({}); falling back to per-segment for this batch", batchEx.getMessage());
                for (TextSegment segment : batch) {
                    try {
                        Embedding embedding = embeddingModel.embed(segment.text()).content();
                        chromaEmbeddingService.getEmbeddingStore().add(embedding, segment);
                    } catch (Exception singleEx) {
                        log.warn("Skipping segment that failed to embed: {}", singleEx.getMessage());
                    }
                }
            }
        }
    }

    /** Returns the number of segments stored for this file (0 if blank / unreadable). */
    private int processPath(Path path) {
        try {
            log.debug("Processing file: {}", path);

            String content = Files.readString(path);
            if (content.isBlank()) {
                return 0;
            }

            Document document = Document.from(content);
            List<TextSegment> rawSegments = splitterFor(path).split(document);
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            long indexedAt = System.currentTimeMillis();
            String absolutePath = path.toAbsolutePath().toString();

            List<TextSegment> segments = new ArrayList<>(rawSegments.size());
            int dropped = 0;
            for (TextSegment segment : rawSegments) {
                if (ChunkQualityFilter.isLowContent(segment.text())) {
                    dropped++;
                    continue;
                }
                Metadata metadata = new Metadata();
                metadata.put(FILE_PATH, absolutePath);
                metadata.put(LAST_MODIFIED, lastModified);
                metadata.put(INDEXED_AT, indexedAt);
                metadata.put(EMBEDDING_SCHEMA_VERSION_KEY, CURRENT_EMBEDDING_SCHEMA_VERSION);
                segments.add(new TextSegment(segment.text(), metadata));
            }
            if (dropped > 0) {
                log.debug("Dropped {} low-content chunk(s) from {}", dropped, path);
            }

            storeSegments(segments);
            return segments.size();
        } catch (IOException e) {
            log.warn("Error processing file: {}", path);
            return 0;
        }
    }
}
