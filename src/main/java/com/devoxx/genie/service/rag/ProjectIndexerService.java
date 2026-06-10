package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
import com.devoxx.genie.service.rag.manifest.IndexManifest;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.service.rag.manifest.InMemoryIndexManifest;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
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
import org.jetbrains.annotations.Nullable;
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

    /**
     * Fraction of the shared progress bar consumed by the scan phase; the embedding phase
     * fills the rest. One indicator, one monotonic 0→100% sweep across both phases.
     */
    static final double SCAN_PHASE_END = 0.5;

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

        // When running under a progress context (e.g. Task.Backgroundable from the RAG
        // settings panel), surface determinate progress in the IDE progress UI as well.
        // The run is split into two phases sharing one indicator: scan fills 0.0→0.5,
        // embedding fills 0.5→1.0 — a single monotonic sweep instead of two 0→100% passes.
        ProgressIndicator indicator = ApplicationManager.getApplication() == null
                ? null
                : ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
            indicator.setIndeterminate(true);
            indicator.setText("Scanning project files (1/2)...");
        }

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

        // Phase 1 (scan): run under a scaled sub-indicator so the determinate progress the
        // scanner reports (ProjectScannerService.extractAllFileContents) lands in the 0.0→0.5
        // half of the shared bar instead of sweeping it 0→100% on its own.
        ScanContentResult scanResult = indicator == null
                ? projectScannerService.scanProject(project, baseDir, Integer.MAX_VALUE, false)
                : ProgressManager.getInstance().runProcess(
                        () -> projectScannerService.scanProject(project, baseDir, Integer.MAX_VALUE, false),
                        new PhaseScalingIndicator(indicator, 0.0, SCAN_PHASE_END, "Scanning (1/2): "));
        List<Path> filesToProcess = new ArrayList<>(scanResult.getFiles());

        // RAG-specific directory exclusion (task-220). Layered on top of the project-scanner
        // exclusion — users can keep project-context broad while keeping RAG narrow.
        List<String> ragExcluded = DevoxxGenieStateService.getInstance().getRagExcludedDirectories();
        log.info("RAG indexing start: basePath='{}', files scanned={}, RAG exclusion entries={}",
                basePath, filesToProcess.size(), ragExcluded);
        if (ragExcluded != null && !ragExcluded.isEmpty()) {
            Path projectBasePath = Path.of(basePath);
            int before = filesToProcess.size();
            final List<String> exclusions = ragExcluded;
            filesToProcess.removeIf(p -> isRagExcluded(p, exclusions, projectBasePath));
            int skipped = before - filesToProcess.size();
            log.info("RAG indexing: applied {} exclusion entries, skipped {} of {} files",
                    exclusions.size(), skipped, before);
            if (skipped == 0 && !filesToProcess.isEmpty()) {
                // Defensive diagnostic when the user reports "exclusions ignored". Shows the
                // first few scanned paths next to the configured entries so the mismatch
                // (case, separator, scope, etc.) is visible in idea.log.
                log.warn("RAG indexing: exclusion entries matched no files. Entries: {}. " +
                        "First 3 scanned files: {}", exclusions,
                        filesToProcess.stream().limit(3).map(Path::toString).toList());
            }

            // Sweep previously-indexed files that now match the exclusion list and drop their
            // chunks from the vector store (task-220). Without this, "Indexed Segments" stays
            // unchanged after a user adds an exclusion — the old chunks linger because the
            // indexer only ever adds chunks for files it re-walks, never removes for files it
            // chose to skip.
            int swept = 0;
            for (Path tracked : manifest.trackedPaths()) {
                if (isRagExcluded(tracked, exclusions, projectBasePath)) {
                    removeChunksFromStore(tracked);
                    manifest.markRemoved(tracked);
                    swept++;
                }
            }
            if (swept > 0) {
                log.info("RAG indexing: removed chunks for {} previously-indexed file(s) now " +
                        "covered by exclusions", swept);
            }
        }

        int totalFiles = filesToProcess.size();

        if (indicator != null) {
            // Phase 2 (embed): continue from where the scan phase left the bar.
            indicator.setIndeterminate(false);
            indicator.setFraction(SCAN_PHASE_END);
            indicator.setText("Indexing project files for RAG (2/2)");
            indicator.setText2("");
        }

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
        java.util.concurrent.atomic.AtomicInteger maxReported = new java.util.concurrent.atomic.AtomicInteger();
        try {
            java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>(totalFiles);
            for (Path path : filesToProcess) {
                futures.add(pool.submit(() -> {
                    // Bridge IDE progress-indicator cancellation into the indexer's own flag so
                    // pressing Cancel on the background task stops the loop just like the
                    // settings panel's Stop button does.
                    if (indicator != null && indicator.isCanceled()) {
                        cancelIndexing.set(true);
                    }
                    if (isIndexingCancelled()) return;
                    indexFile(path, forceReindex);
                    int done = processedCount.incrementAndGet();
                    int progress = (int) (((double) done / totalFiles) * 100);
                    String fileName = path.getFileName().toString();
                    if (indicator != null && !indicator.isCanceled()
                            && maxReported.accumulateAndGet(done, Math::max) == done) {
                        // ProgressIndicator API is thread-safe; no EDT hop needed. The max-guard
                        // keeps parallel workers from briefly rolling the bar/text backwards.
                        indicator.setFraction(SCAN_PHASE_END + (done / (double) totalFiles) * (1.0 - SCAN_PHASE_END));
                        indicator.setText2(String.format("Indexing (2/2): %d of %d: %s", done, totalFiles, fileName));
                    }
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(true);
                        progressLabel.setVisible(true);
                        progressBar.setValue(progress);
                        progressLabel.setText(String.format("Processing %d of %d: %s", done, totalFiles, fileName));
                    });
                }));
            }
            for (java.util.concurrent.Future<?> f : futures) {
                if (indicator != null && indicator.isCanceled()) {
                    cancelIndexing.set(true);
                }
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
        List<String> ragExcluded = DevoxxGenieStateService.getInstance().getRagExcludedDirectories();
        String basePath = project.getBasePath();
        Path projectBasePath = basePath != null ? Path.of(basePath) : null;
        try {
            for (Path file : files) {
                if (!manifest.isTracked(file)) continue;
                // RAG-specific exclusion (task-220): users may have added the file's parent dir
                // since it was first indexed. Skip and drop any prior chunks so the index
                // doesn't keep returning stale matches for a now-excluded path.
                if (ragExcluded != null && !ragExcluded.isEmpty()
                        && isRagExcluded(file, ragExcluded, projectBasePath)) {
                    removeChunksFromStore(file);
                    manifest.markRemoved(file);
                    continue;
                }
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
     * Returns true if {@code path} is covered by any entry in {@code excluded}. Each entry is
     * treated as a path prefix and matched against <em>either</em> the file's absolute path
     * <em>or</em> its project-relative path (whichever the entry looks like).
     *
     * <p>This dual matching lets the Browse... button insert an absolute path — the form the
     * user sees in the RAG settings list — while still accepting manually-typed project-
     * relative paths like {@code docs/book} for users who want the entry to survive moving
     * the project.
     *
     * <p>A file is excluded when, for some normalized entry {@code E}:
     * <ul>
     *   <li>the file's absolute path equals {@code E} or starts with {@code E + "/"}, or</li>
     *   <li>the file's project-relative path equals {@code E} or starts with {@code E + "/"}.</li>
     * </ul>
     * Both comparisons are case-sensitive and use forward-slash normalization.
     *
     * <p>Normalization: separators are converted to {@code /}; whitespace is trimmed; trailing
     * slashes are stripped. Leading slashes are <em>kept</em> so absolute Unix paths still
     * match. Blank entries are silently skipped.
     */
    static boolean isRagExcluded(@NotNull Path path,
                                  @NotNull List<String> excluded,
                                  @Nullable Path projectBase) {
        if (excluded.isEmpty()) return false;

        Path abs = path.toAbsolutePath().normalize();
        String absoluteStr = abs.toString().replace('\\', '/');
        String relativeStr = null;
        if (projectBase != null) {
            Path base = projectBase.toAbsolutePath().normalize();
            if (abs.startsWith(base)) {
                relativeStr = base.relativize(abs).toString().replace('\\', '/');
            }
        }

        for (String entry : excluded) {
            if (entry == null) continue;
            String norm = entry.replace('\\', '/').trim();
            while (norm.endsWith("/")) norm = norm.substring(0, norm.length() - 1);
            if (norm.isEmpty()) continue;
            if (matchesPrefix(absoluteStr, norm)) return true;
            if (relativeStr != null && matchesPrefix(relativeStr, norm)) return true;
            // Single-segment entries (e.g. typed "node_modules" or "obsidian") match the dir
            // anywhere in the path, matching the project-scanner's existing behavior. Browse-
            // inserted entries always contain a "/" so they stay strictly prefix-matched.
            if (!norm.contains("/") && containsSegment(path, norm)) return true;
        }
        return false;
    }

    private static boolean matchesPrefix(@NotNull String target, @NotNull String prefix) {
        return target.equals(prefix) || target.startsWith(prefix + "/");
    }

    private static boolean containsSegment(@NotNull Path path, @NotNull String segment) {
        for (Path p : path) {
            if (p.toString().equals(segment)) return true;
        }
        return false;
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

    /**
     * Maps a sub-phase's 0.0→1.0 progress into a [from, to] slice of the parent indicator
     * and prefixes its detail text with the phase label. Cancellation, text and all other
     * calls delegate straight through, so cancelling the parent task still cancels the phase.
     */
    private static final class PhaseScalingIndicator extends DelegatingProgressIndicator {
        private final double from;
        private final double to;
        private final String text2Prefix;

        private PhaseScalingIndicator(@NotNull ProgressIndicator delegate,
                                      double from,
                                      double to,
                                      @NotNull String text2Prefix) {
            super(delegate);
            this.from = from;
            this.to = to;
            this.text2Prefix = text2Prefix;
        }

        @Override
        public void setFraction(double fraction) {
            super.setFraction(from + fraction * (to - from));
        }

        @Override
        public double getFraction() {
            double parent = super.getFraction();
            return to == from ? parent : (parent - from) / (to - from);
        }

        @Override
        public void setText2(String text) {
            super.setText2(text == null ? null : text2Prefix + text);
        }
    }
}
