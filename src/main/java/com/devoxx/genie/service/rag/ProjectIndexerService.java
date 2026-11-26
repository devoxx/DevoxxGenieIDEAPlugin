package com.devoxx.genie.service.rag;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
import com.devoxx.genie.service.rag.chunking.RagChunker;
import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import com.devoxx.genie.service.rag.manifest.IndexManifest;
import com.devoxx.genie.service.rag.manifest.IndexManifestService;
import com.devoxx.genie.service.rag.manifest.InMemoryIndexManifest;
import com.devoxx.genie.ui.settings.ProjectScanSettingsService;
import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import com.devoxx.genie.service.rag.embedding.ProfiledEmbedder;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.service.rag.IndexerConstants.*;

@Slf4j
@Service
public final class ProjectIndexerService {

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

    /** Splitter selection + low-content filtering, shared with the standalone RAG benchmark. */
    private final RagChunker chunker = new RagChunker();

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

    @TestOnly
    public void setManifest(@NotNull IndexManifest manifest) {
        this.manifest = manifest;
    }

    /** What one full indexing run did: files embedded, skipped as unchanged, and removed from the index. */
    public record IndexRunSummary(int indexed, int unchanged, int removed, int failed) {
        public static final IndexRunSummary NOTHING = new IndexRunSummary(0, 0, 0, 0);

        public @NotNull String describe() {
            String text = indexed + (indexed == 1 ? " file indexed" : " files indexed")
                    + ", " + unchanged + " unchanged"
                    + ", " + removed + " removed";
            return failed == 0 ? text : text + ", " + failed + " failed (see idea.log)";
        }
    }

    /**
     * Indexes the project's files for RAG.
     *
     * @param rebuild {@code false} for an incremental run (TASK-282): files whose content hash and
     *                embedding setup match the manifest are skipped, changed and new files are
     *                (re-)embedded, and files that are gone or now excluded are removed from the index.
     *                {@code true} empties the index first and embeds every file again.
     * @return what the run did; {@link IndexRunSummary#NOTHING} when it stopped before indexing
     */
    public @NotNull IndexRunSummary indexFiles(Project project,
                                               boolean rebuild,
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
            return IndexRunSummary.NOTHING;
        }

        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) {
            log.debug("Could not find base directory: {}", basePath);
            return IndexRunSummary.NOTHING;
        }

        // A full index always uses the configured embedding setup. If the existing index was
        // built with another one, its vectors can't be mixed with the new ones (and Chroma
        // fixes a collection's vector size on first write), so start from an empty index.
        // Only after the checks above, so an aborted run never leaves the user without an index.
        EmbeddingFingerprint fingerprint = chromaEmbeddingService.configuredFingerprint();
        if (!prepareIndexFor(fingerprint)) {
            SwingUtilities.invokeLater(() -> progressLabel.setText(
                    "Could not clear the index built with the previous embedding model; see idea.log"));
            return IndexRunSummary.NOTHING;
        }
        if (rebuild && !clearIndex()) {
            SwingUtilities.invokeLater(() -> progressLabel.setText("Could not clear the RAG index; see idea.log"));
            return IndexRunSummary.NOTHING;
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
        List<String> ragExcluded = ProjectScanSettingsService.getInstance(project).getRagExcludedDirectories();
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
        }

        // Drop files that are no longer part of the index: deleted since the last run, or now
        // excluded (task-220) or ignored by the scanner. Covers deletions the file watcher missed.
        // Never after a cancelled scan: a partial file list would delete most of the index.
        boolean scanCancelled = isIndexingCancelled() || (indicator != null && indicator.isCanceled());
        int removed = scanCancelled ? 0 : pruneFilesNotIn(filesToProcess);
        if (removed > 0) {
            log.info("RAG indexing: removed {} file(s) that are no longer part of the index", removed);
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
        java.util.concurrent.atomic.AtomicInteger indexedCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger unchangedCount = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger failedCount = new java.util.concurrent.atomic.AtomicInteger();
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
                    // Never forced: a rebuild has already emptied the manifest, so every file is
                    // embedded; an incremental run skips files whose content hash is unchanged.
                    switch (indexFile(path, false, fingerprint)) {
                        case INDEXED -> indexedCount.incrementAndGet();
                        case UNCHANGED -> unchangedCount.incrementAndGet();
                        case FAILED -> failedCount.incrementAndGet();
                        case SKIPPED -> { }
                    }
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
        }
        IndexRunSummary summary = new IndexRunSummary(indexedCount.get(), unchangedCount.get(), removed, failedCount.get());
        log.info("RAG indexing {}: {}", isIndexingCancelled() ? "cancelled" : "finished", summary.describe());
        // The cancellation flag is left set so the caller can tell a cancelled run apart; the next
        // run resets it on entry.
        return summary;
    }

    /**
     * Index a single file: skip if the content hash already matches what's recorded in the
     * manifest, otherwise split, embed, and store each chunk. Exposed for tests and for
     * future incremental flows (e.g. a {@code BulkFileListener}).
     */
    public void indexFile(Path filePath) {
        indexFile(filePath, false, indexFingerprint());
        manifest.flush();
    }

    /**
     * Get the index ready for a full run with {@code target}: when the manifest says the
     * project was indexed with a different embedding setup, drop every stored vector (Chroma
     * deletes and recreates the collection) and every manifest entry. Returns {@code false}
     * when the old vectors could not be dropped, in which case indexing must not proceed.
     *
     * @throws IllegalStateException when {@code target} cannot create embeddings (not installed,
     *                               or a chat model), checked first so the index stays intact
     */
    boolean prepareIndexFor(@NotNull EmbeddingFingerprint target) {
        try {
            // Through the profile, so an output dimension the model can't provide fails here too.
            new ProfiledEmbedder(chromaEmbeddingService.getEmbeddingModel(target), target.documentProfile())
                    .embedDocument("embedding model check");
        } catch (Exception e) {
            throw new IllegalStateException("Embedding model " + target.modelName()
                    + " cannot create embeddings (" + e.getMessage() + "). The existing RAG index was not changed.", e);
        }

        Optional<String> existing = manifest.indexFingerprint();
        if (existing.isEmpty() || existing.get().equals(target.id())) {
            return true;
        }
        log.info("RAG embedding setup changed from {} to {}; rebuilding the index from scratch",
                existing.get(), target.id());
        try {
            chromaEmbeddingService.getEmbeddingStore().removeAll();
        } catch (Exception e) {
            log.error("Could not clear the RAG index built with {}: {}", existing.get(), e.getMessage());
            return false;
        }
        manifest.clear();
        manifest.flush();
        return true;
    }

    /**
     * The embedding setup the project's current index was built with, falling back to the
     * configured one for an empty index. Search and incremental re-indexing use this so RAG
     * keeps working after a model change until the user rebuilds the index.
     */
    private @NotNull EmbeddingFingerprint indexFingerprint() {
        return manifest.indexFingerprint()
                .map(EmbeddingFingerprint::parse)
                .orElseGet(chromaEmbeddingService::configuredFingerprint);
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
        List<String> ragExcluded = ProjectScanSettingsService.getInstance(project).getRagExcludedDirectories();
        String basePath = project.getBasePath();
        Path projectBasePath = basePath != null ? Path.of(basePath) : null;
        // Keep embedding with the model the index was built with, even if settings now name
        // another one: mixing two models' vectors in one index would break search.
        EmbeddingFingerprint fingerprint = indexFingerprint();
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
                // indexFile replaces the file's existing chunks (it is tracked), so edits never
                // accumulate stale chunks alongside fresh ones.
                indexFile(file, true, fingerprint);
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

    /** What {@link #indexFile(Path, boolean, EmbeddingFingerprint)} did with one file. */
    enum FileOutcome {
        /** Embedded and stored (new or changed file, or forced). */
        INDEXED,
        /** Skipped: the content hash and embedding setup match the manifest. */
        UNCHANGED,
        /** Nothing stored: blank, unreadable, or every segment failed to embed. */
        SKIPPED,
        /** An error was logged. */
        FAILED
    }

    FileOutcome indexFile(Path filePath, boolean forceReindex, @NotNull EmbeddingFingerprint fingerprint) {
        log.debug("Indexing file: {}", filePath);
        try {
            if (!forceReindex && manifest.isCurrent(filePath, fingerprint.id())) {
                log.debug("File already indexed (content hash matches): {}", filePath);
                return FileOutcome.UNCHANGED;
            }
            List<TextSegment> segments = prepareSegments(filePath, fingerprint);
            if (segments == null) {
                return FileOutcome.FAILED; // Unreadable: keep whatever was indexed before.
            }
            boolean tracked = manifest.isTracked(filePath);
            if (segments.isEmpty()) {
                if (tracked) { // Now blank: its old chunks no longer describe the file.
                    removeChunksFromStore(filePath);
                    manifest.markRemoved(filePath);
                }
                return FileOutcome.SKIPPED;
            }
            EmbeddedSegments embedded = embedSegments(segments, fingerprint);
            if (embedded.segments().isEmpty()) {
                // Nothing could be embedded (e.g. the embedding server is down). Keep the old chunks
                // and manifest entry, so search still finds the file and a later run retries it.
                log.warn("Could not embed {}; its existing index entries were kept", filePath);
                return FileOutcome.FAILED;
            }
            // Replace, never append (TASK-282): chunks are stored under random ids, so a file that was
            // indexed before loses its old chunks, but only now that the new ones are ready.
            if (tracked) {
                removeChunksFromStore(filePath);
            }
            chromaEmbeddingService.getEmbeddingStore().addAll(embedded.embeddings(), embedded.segments());
            int segmentCount = embedded.segments().size();
            manifest.markIndexed(filePath, segmentCount, fingerprint.id(), queryPrefixFor(fingerprint));
            log.debug("File successfully indexed ({} segments): {}", segmentCount, filePath);
            return FileOutcome.INDEXED;
        } catch (Exception e) {
            log.warn("Error indexing file: {} - {}", filePath, e.getMessage());
            return FileOutcome.FAILED;
        }
    }

    /**
     * Removes the chunks and manifest entries of every indexed file that is not in
     * {@code indexedSet}, the files a full run covers after scanning and exclusions: files that
     * were deleted, or are now excluded or ignored. Returns how many files were removed. An empty
     * set prunes nothing, since it more likely means the scan failed than that every file is gone.
     */
    int pruneFilesNotIn(@NotNull Collection<Path> indexedSet) {
        if (indexedSet.isEmpty()) {
            return 0;
        }
        java.util.Set<Path> keep = new java.util.HashSet<>();
        for (Path p : indexedSet) {
            keep.add(p.toAbsolutePath().normalize());
        }
        int removed = 0;
        for (Path tracked : List.copyOf(manifest.trackedPaths())) {
            if (!keep.contains(tracked.toAbsolutePath().normalize())) {
                removeChunksFromStore(tracked);
                manifest.markRemoved(tracked);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Empties the index for a rebuild: deletes every stored chunk (Chroma drops and recreates the
     * collection) and every manifest entry. Returns {@code false} when the chunks could not be
     * deleted, in which case the manifest is left as it was.
     */
    boolean clearIndex() {
        try {
            chromaEmbeddingService.getEmbeddingStore().removeAll();
        } catch (Exception e) {
            log.error("Could not clear the RAG index: {}", e.getMessage());
            return false;
        }
        manifest.clear();
        manifest.flush();
        return true;
    }

    /** The query prefix to record with a file: the settings' for a current index, else the index's. */
    private @Nullable String queryPrefixFor(@NotNull EmbeddingFingerprint fingerprint) {
        return fingerprint.equals(chromaEmbeddingService.configuredFingerprint())
                ? chromaEmbeddingService.configuredQueryPrefix()
                : manifest.indexQueryPrefix();
    }

    /**
     * Embed and store a batch of segments in one shot. The previous implementation made
     * one HTTP call to Ollama per segment, turning a 200-chunk file into 200 sequential
     * roundtrips. Embedding in batches and using {@code addAll} on the store collapses
     * that to {@code ceil(N / EMBEDDING_BATCH_SIZE)} calls and one store write per batch.
     *
     * <p>The document prefix only goes to the model: the stored segments keep their original text.
     *
     * @return how many segments were actually stored, so failures can't be counted as indexed
     */
    /** Segments that were embedded, with their embeddings in the same order. */
    private record EmbeddedSegments(@NotNull List<Embedding> embeddings, @NotNull List<TextSegment> segments) {}

    /**
     * Embeds {@code segments} in batches without storing them. A batch the backend rejects is retried
     * per segment, so one malformed chunk doesn't lose the rest of the file; segments that still fail
     * are left out.
     */
    private @NotNull EmbeddedSegments embedSegments(@NotNull List<TextSegment> segments,
                                                    @NotNull EmbeddingFingerprint fingerprint) {
        ProfiledEmbedder embedder = new ProfiledEmbedder(
                chromaEmbeddingService.getEmbeddingModel(fingerprint), fingerprint.documentProfile());
        List<Embedding> embeddings = new ArrayList<>(segments.size());
        List<TextSegment> embedded = new ArrayList<>(segments.size());
        for (int from = 0; from < segments.size(); from += EMBEDDING_BATCH_SIZE) {
            int to = Math.min(from + EMBEDDING_BATCH_SIZE, segments.size());
            List<TextSegment> batch = segments.subList(from, to);
            try {
                embeddings.addAll(embedder.embedDocuments(batch));
                embedded.addAll(batch);
            } catch (Exception batchEx) {
                log.warn("Batch embed failed ({}); falling back to per-segment for this batch", batchEx.getMessage());
                for (TextSegment segment : batch) {
                    try {
                        embeddings.add(embedder.embedDocument(segment.text()));
                        embedded.add(segment);
                    } catch (Exception singleEx) {
                        log.warn("Skipping segment that failed to embed: {}", singleEx.getMessage());
                    }
                }
            }
        }
        return new EmbeddedSegments(embeddings, embedded);
    }

    /** The file's chunks with their metadata; empty for a blank file, {@code null} when it can't be read. */
    private @Nullable List<TextSegment> prepareSegments(Path path, @NotNull EmbeddingFingerprint fingerprint) {
        try {
            log.debug("Processing file: {}", path);

            String content = Files.readString(path);
            if (content.isBlank()) {
                return List.of();
            }

            List<TextSegment> chunks = chunker.chunk(path, content);
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            long indexedAt = System.currentTimeMillis();
            String absolutePath = path.toAbsolutePath().toString();

            List<TextSegment> segments = new ArrayList<>(chunks.size());
            for (TextSegment chunk : chunks) {
                Metadata metadata = new Metadata();
                metadata.put(FILE_PATH, absolutePath);
                metadata.put(LAST_MODIFIED, lastModified);
                metadata.put(INDEXED_AT, indexedAt);
                metadata.put(EMBEDDING_SCHEMA_VERSION_KEY, CURRENT_EMBEDDING_SCHEMA_VERSION);
                metadata.put(EMBEDDING_FINGERPRINT_KEY, fingerprint.id());
                segments.add(new TextSegment(chunk.text(), metadata));
            }

            return segments;
        } catch (IOException e) {
            log.warn("Error processing file: {}", path);
            return null;
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
