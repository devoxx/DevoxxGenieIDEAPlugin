package com.devoxx.genie.service.semanticsearch;

import com.devoxx.genie.service.chromadb.ChromaEmbeddingService;
import com.devoxx.genie.service.projectscanner.ProjectScannerService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.AppExecutorUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

import static com.devoxx.genie.service.semanticsearch.IndexerConstants.*;

@Service
public final class ProjectIndexerService {

    private static final Logger LOG = Logger.getLogger(ProjectIndexerService.class.getName());

    private final ChromaEmbeddingService embeddingService;
    private final ProjectScannerService projectScannerService;

    private DocumentSplitter documentSplitter;
    private Project project;

    @NotNull
    public static ProjectIndexerService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectIndexerService.class);
    }

    public ProjectIndexerService() {
        this.embeddingService = ChromaEmbeddingService.getInstance();
        this.projectScannerService = ProjectScannerService.getInstance();
    }

    public void init(Project project) {
        this.project = project;
        embeddingService.init(project);
        documentSplitter = DocumentSplitters.recursive(500, 0);
    }

    /**
     * Check if a project is already indexed by searching for a unique hash in metadata.
     *
     * @param projectPath Path to the project directory
     * @return true if project is indexed, false otherwise
     */
    private boolean isProjectIndexed(String projectPath) {
        try {
            // Create a unique project identifier (hash) based on path and last modified time
            String projectHash = createProjectHash(projectPath);

            // Search for the project hash in metadata
            Embedding hashEmbedding = embeddingService.getEmbeddingModel().embed(projectHash).content();
            var results = embeddingService.getEmbeddingStore().search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(hashEmbedding)
                    .maxResults(1)
                    .minScore(0.99) // High threshold for exact match
                    .build());

            return !results.matches().isEmpty();
        } catch (Exception e) {
            LOG.warning("Error checking project index status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a file is already indexed by comparing its embedding with stored embeddings.
     *
     * @param filePath Path to the file
     * @return true if file is indexed, false otherwise
     */
    private boolean isFileIndexed(Path filePath) {
        try {
            String fileIdentifier = filePath.toAbsolutePath().toString();
            Embedding fileEmbedding = embeddingService.getEmbeddingModel().embed(fileIdentifier).content();
            var results = embeddingService.getEmbeddingStore().search(EmbeddingSearchRequest
                    .builder()
                    .queryEmbedding(fileEmbedding)
                    .maxResults(1)
                    .minScore(0.99)
                    .build());
            if (results.matches().isEmpty()) {
                return false;
            }
            List<EmbeddingMatch<TextSegment>> matches = results.matches();
            TextSegment storedSegment = matches.get(0).embedded();
            return !hasFileChanged(filePath, storedSegment.metadata());
        } catch (Exception e) {
            LOG.warning("Error checking file index status: " + e.getMessage());
            return false;
        }
    }

    public void indexFiles(String basePath, boolean forceReindex, @NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setText("Scanning project files...");

        if (!forceReindex && isProjectIndexed(basePath)) {
            LOG.info("Project is already indexed, skipping indexing process");
            return;
        }

        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) {
            LOG.info("Could not find base directory: " + basePath);
            return;
        }


        projectScannerService.scanProject(project, baseDir, Integer.MAX_VALUE, false, indicator)
                .thenAcceptAsync(scanResult -> {
                    List<Path> filesToProcess = new ArrayList<>(scanResult.getFiles());
                    int totalFiles = filesToProcess.size();
                    int processed = 0;

                    for (Path path : filesToProcess) {
                        if (indicator.isCanceled()) {
                            break;
                        }

                        indicator.setFraction((double) processed / totalFiles);
                        indicator.setText(String.format("Indexing file %d of %d: %s",
                                processed + 1, totalFiles, path.getFileName()));

                        indexSingleFile(path);
                        processed++;
                    }

                    indicator.setFraction(1.0);
                    indicator.setText("Indexing complete - Processed " + processed + " files");

                }, AppExecutorUtil.getAppExecutorService())
                .exceptionally(throwable -> {
                    LOG.severe("Error during indexing: " + throwable.getMessage());
                    indicator.setText("Indexing failed: " + throwable.getMessage());
                    return null;
                });
    }

    /**
     * Index a single file by checking if it is already indexed and processing the content.
     * @param filePath Path to the file to index
     */
    private void indexSingleFile(Path filePath) {
        LOG.info("Indexing file: " + filePath);
        try {
            if (isFileIndexed(filePath)) {
                LOG.info("File already indexed: " + filePath);
                return;
            }

            processPath(filePath);
            LOG.info("File successfully indexed: " + filePath);
        } catch (Exception e) {
            LOG.severe("Error indexing file: " + filePath + " - " + e.getMessage());
        }
    }

    @NotNull
    private String createProjectHash(String projectPath) throws IOException {
        Path path = Paths.get(projectPath);
        long lastModified = Files.getLastModifiedTime(path).toMillis();
        return projectPath + "_" + lastModified;
    }

    private boolean hasFileChanged(Path filePath, @NotNull Metadata storedMetadata) {
        try {
            long currentLastModified = Files.getLastModifiedTime(filePath).toMillis();
            if (!storedMetadata.containsKey("lastModified")) {
                return true;
            }
            long storedLastModified = storedMetadata.getLong("lastModified");
            return currentLastModified > storedLastModified;
        } catch (IOException e) {
            return true; // If we can't check, assume it changed
        }
    }

    @NotNull
    private String createFileIdentifier(@NotNull Path filePath) {
        return filePath.toAbsolutePath().toString();
    }

    /**
     * Mark a file as indexed by storing its metadata and embedding in the database.
     * @param filePath Path to the file
     * @param segment  Text segment containing metadata
     */
    private void markFileAsIndexed(Path filePath, TextSegment segment) {
        try {
            String fileIdentifier = createFileIdentifier(filePath);
            Embedding embedding = embeddingService.getEmbeddingModel().embed(fileIdentifier).content();
            embeddingService.getEmbeddingStore().add(embedding, segment);
        } catch (Exception e) {
            LOG.warning("Error marking file as indexed: " + e.getMessage());
        }
    }

    private void processPath(Path path) {
        try {
            LOG.info("Processing file: " + path);

            String content = Files.readString(path);
            if (content.isBlank()) {
                return;
            }

            Document document = Document.from(content);
            List<TextSegment> segments = documentSplitter.split(document);

            for (TextSegment segment : segments) {
                Metadata metadata = new Metadata();
                metadata.put(FILE_PATH, path.toString());
                metadata.put(LAST_MODIFIED, Files.getLastModifiedTime(path).toMillis());
                metadata.put(INDEXED_AT, System.currentTimeMillis());
                markFileAsIndexed(path, new TextSegment(segment.text(), metadata));
            }

        } catch (IOException e) {
            LOG.warning("Error processing file: " + path);
        }
    }
}