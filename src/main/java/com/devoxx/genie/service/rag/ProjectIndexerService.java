package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /** Constant string used as the query for metadata-filter-only lookups. We embed it once
     *  per (model instance) and reuse, since EmbeddingSearchRequest requires a query embedding
     *  even when only the filter does the work. */
    private static final String LOOKUP_PROBE_TEXT = "__devoxxgenie_lookup__";

    private final ChromaEmbeddingService chromaEmbeddingService;
    private final ProjectScannerService projectScannerService;

    private DocumentSplitter documentSplitter;
    private Embedding cachedLookupProbe;
    private EmbeddingModel cachedLookupProbeOwner;

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

    private DocumentSplitter splitter() {
        if (documentSplitter == null) {
            documentSplitter = DocumentSplitters.recursive(CHUNK_SIZE_TOKENS, CHUNK_OVERLAP_TOKENS);
        }
        return documentSplitter;
    }

    /**
     * Check whether the given file already has at least one segment stored under the current
     * schema, and that the stored mtime matches the file on disk. Uses a metadata filter on
     * {@link IndexerConstants#FILE_PATH} rather than vector similarity — the latter only worked
     * by accident under the pre-v2 schema that (incorrectly) embedded file paths.
     */
    private boolean isFileIndexed(Path filePath) {
        try {
            String absolutePath = filePath.toAbsolutePath().toString();
            Filter filter = MetadataFilterBuilder.metadataKey(FILE_PATH).isEqualTo(absolutePath)
                    .and(MetadataFilterBuilder.metadataKey(EMBEDDING_SCHEMA_VERSION_KEY)
                            .isEqualTo(CURRENT_EMBEDDING_SCHEMA_VERSION));
            var results = chromaEmbeddingService.getEmbeddingStore().search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(lookupProbe())
                    .filter(filter)
                    .maxResults(1)
                    .minScore(0.0)
                    .build());
            if (results.matches().isEmpty()) {
                return false;
            }
            return !hasFileChanged(filePath, results.matches().get(0).embedded().metadata());
        } catch (Exception e) {
            log.warn("Error checking file index status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * EmbeddingSearchRequest requires a non-null query embedding even when results are
     * narrowed exclusively by metadata filter. We cache the probe embedding per embedding-model
     * instance so the lookup cost is O(1) rather than one Ollama roundtrip per file.
     */
    private Embedding lookupProbe() {
        EmbeddingModel current = chromaEmbeddingService.getEmbeddingModel();
        if (cachedLookupProbe == null || cachedLookupProbeOwner != current) {
            cachedLookupProbe = current.embed(LOOKUP_PROBE_TEXT).content();
            cachedLookupProbeOwner = current;
        }
        return cachedLookupProbe;
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

        for (int fileIndex = 0; fileIndex < totalFiles; fileIndex++) {
            if (isIndexingCancelled()) {
                log.info("Indexing cancelled after processing {} of {} files", fileIndex, totalFiles);
                final int processedFiles = fileIndex;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressLabel.setText(String.format("Indexing cancelled after processing %d of %d files",
                            processedFiles, totalFiles));
                });
                return;
            }

            Path path = filesToProcess.get(fileIndex);
            String fileName = path.getFileName().toString();
            int progress = (int) (((double) (fileIndex + 1) / totalFiles) * 100);
            final int currentFileIndex = fileIndex;

            SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(true);
                progressLabel.setVisible(true);
                progressBar.setValue(progress);
                progressLabel.setText(String.format("Processing %d of %d: %s", currentFileIndex + 1, totalFiles, fileName));
            });

            indexFile(path, forceReindex);
        }

        resetCancellationFlag();
    }

    /**
     * Index a single file: skip if already up-to-date under the current schema, otherwise
     * split, embed, and store each chunk. Exposed for tests and for future incremental flows
     * (e.g. a {@code BulkFileListener}).
     */
    public void indexFile(Path filePath) {
        indexFile(filePath, false);
    }

    private void indexFile(Path filePath, boolean forceReindex) {
        log.debug("Indexing file: {}", filePath);
        try {
            if (!forceReindex && isFileIndexed(filePath)) {
                log.debug("File already indexed: {}", filePath);
                return;
            }
            processPath(filePath);
            log.debug("File successfully indexed: {}", filePath);
        } catch (Exception e) {
            log.warn("Error indexing file: {} - {}", filePath, e.getMessage());
        }
    }

    private boolean hasFileChanged(Path filePath, @NotNull Metadata storedMetadata) {
        try {
            long currentLastModified = Files.getLastModifiedTime(filePath).toMillis();
            if (!storedMetadata.containsKey(LAST_MODIFIED)) {
                return true;
            }
            Long storedLastModified = storedMetadata.getLong(LAST_MODIFIED);
            if (storedLastModified == null) return false;
            return currentLastModified > storedLastModified;
        } catch (IOException e) {
            return true; // If we can't check, assume it changed
        }
    }

    /** Max segments per Ollama {@code embedAll} call. Keeps individual requests small enough
     *  to avoid timeouts while still avoiding the N+1 cost of one-call-per-segment. */
    static final int EMBEDDING_BATCH_SIZE = 64;

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

    private void processPath(Path path) {
        try {
            log.debug("Processing file: {}", path);

            String content = Files.readString(path);
            if (content.isBlank()) {
                return;
            }

            Document document = Document.from(content);
            List<TextSegment> rawSegments = splitter().split(document);
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            long indexedAt = System.currentTimeMillis();
            String absolutePath = path.toAbsolutePath().toString();

            List<TextSegment> segments = new ArrayList<>(rawSegments.size());
            for (TextSegment segment : rawSegments) {
                Metadata metadata = new Metadata();
                metadata.put(FILE_PATH, absolutePath);
                metadata.put(LAST_MODIFIED, lastModified);
                metadata.put(INDEXED_AT, indexedAt);
                metadata.put(EMBEDDING_SCHEMA_VERSION_KEY, CURRENT_EMBEDDING_SCHEMA_VERSION);
                segments.add(new TextSegment(segment.text(), metadata));
            }

            storeSegments(segments);
        } catch (IOException e) {
            log.warn("Error processing file: {}", path);
        }
    }
}
