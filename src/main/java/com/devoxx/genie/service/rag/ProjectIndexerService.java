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
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.devoxx.genie.service.rag.IndexerConstants.*;

@Slf4j
@Service
public final class ProjectIndexerService {
    
    private final ChromaEmbeddingService chromaEmbeddingService;
    private final ProjectScannerService projectScannerService;

    private DocumentSplitter documentSplitter;
    
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
    
    /**
     * Cancels the current indexing process if one is running.
     * The indexing process will stop at the next file boundary.
     */
    public void cancelIndexing() {
        cancelIndexing.set(true);
        log.info("Indexing cancellation requested");
    }
    
    /**
     * Checks if indexing is currently being cancelled.
     * @return true if indexing is being cancelled, false otherwise
     */
    public boolean isIndexingCancelled() {
        return cancelIndexing.get();
    }
    
    /**
     * Resets the cancellation flag to allow future indexing operations.
     */
    public void resetCancellationFlag() {
        cancelIndexing.set(false);
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

            OllamaEmbeddingModel embeddingModel = chromaEmbeddingService.getEmbeddingModel();

            if (embeddingModel == null) {
                log.warn("Ollama embedding model is not available");
                return false;
            }

            // Search for the project hash in metadata
            Embedding hashEmbedding = embeddingModel.embed(projectHash).content();
            var results = chromaEmbeddingService.getEmbeddingStore().search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(hashEmbedding)
                    .maxResults(1)
                    .minScore(0.99) // High threshold for exact match
                    .build());

            return !results.matches().isEmpty();
        } catch (Exception e) {
            log.warn("Error checking project index status: {}", e.getMessage());
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
            Embedding fileEmbedding = chromaEmbeddingService.getEmbeddingModel().embed(fileIdentifier).content();
            var results = chromaEmbeddingService.getEmbeddingStore().search(EmbeddingSearchRequest
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
            log.warn("Error checking file index status: {}", e.getMessage());
            return false;
        }
    }

    public void indexFiles(Project project,
                           boolean forceReindex,
                           JProgressBar progressBar,
                           JLabel progressLabel) {
        // Reset cancellation flag at the start of indexing
        resetCancellationFlag();
        
        // Initialize progress UI
        if (SwingUtilities.isEventDispatchThread()) {
            // If we're already on the EDT, update directly
            progressBar.setValue(0);
            progressBar.setVisible(true);
            progressLabel.setText("Initializing indexing process...");
            progressLabel.setVisible(true);
        } else {
            // Otherwise, use invokeLater
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(0);
                progressBar.setVisible(true);
                progressLabel.setText("Initializing indexing process...");
                progressLabel.setVisible(true);
            });
        }
        
        chromaEmbeddingService.init(project);
        documentSplitter = DocumentSplitters.recursive(500, 0);

        String basePath = project.getBasePath();
        if (basePath == null) {
            log.warn("Project base path is null");
            return;
        }

        if (!forceReindex && isProjectIndexed(basePath)) {
            log.warn("Project is already indexed, skipping indexing process");
            return;
        }

        VirtualFile baseDir = LocalFileSystem.getInstance().findFileByPath(basePath);
        if (baseDir == null) {
            log.debug("Could not find base directory: {}", basePath);
            return;
        }

        // Use synchronous project scanning
        ScanContentResult scanResult = projectScannerService.scanProject(project, baseDir, Integer.MAX_VALUE, false);
        List<Path> filesToProcess = new ArrayList<>(scanResult.getFiles());
        int totalFiles = filesToProcess.size();

        // Process each file sequentially
        for (int fileIndex = 0; fileIndex < totalFiles; fileIndex++) {
            // Check if indexing has been cancelled
            if (isIndexingCancelled()) {
                log.info("Indexing cancelled after processing {} of {} files", fileIndex, totalFiles);
                
                // Update UI to show cancellation
                final int processedFiles = fileIndex;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100); // Set to 100% to indicate completion
                    progressLabel.setText(String.format("Indexing cancelled after processing %d of %d files", 
                                                       processedFiles, totalFiles));
                });
                
                return;
            }
            
            Path path = filesToProcess.get(fileIndex);
            String fileName = path.getFileName().toString();
            int progress = (int) (((double) (fileIndex + 1) / totalFiles) * 100);
            final int currentFileIndex = fileIndex;
            
            // Update progress UI and ensure it's visible
            SwingUtilities.invokeLater(() -> {
                progressBar.setVisible(true);
                progressLabel.setVisible(true);
                progressBar.setValue(progress);
                progressLabel.setText(String.format("Processing %d of %d: %s", currentFileIndex + 1, totalFiles, fileName));
            });
            
            // Process the file without waiting for UI update to complete
            indexSingleFile(path);
        }
        
        // Reset cancellation flag after successful completion
        resetCancellationFlag();
    }

    /**
     * Index a single file by checking if it is already indexed and processing the content.
     * @param filePath Path to the file to index
     */
    private void indexSingleFile(Path filePath) {
        log.debug("Indexing file: {}", filePath);
        try {
            if (isFileIndexed(filePath)) {
                log.debug("File already indexed: {}", filePath);
                return;
            }

            processPath(filePath);
            log.debug("File successfully indexed: {}", filePath);
        } catch (Exception e) {
            log.warn("Error indexing file: {} - {}",  filePath, e.getMessage());
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
            Long storedLastModified = storedMetadata.getLong("lastModified");
            if (storedLastModified == null) return false;
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
     *
     * @param filePath Path to the file
     * @param segment  Text segment containing metadata
     */
    private void markFileAsIndexed(Path filePath, TextSegment segment) {
        try {
            String fileIdentifier = createFileIdentifier(filePath);
            Embedding embedding = chromaEmbeddingService.getEmbeddingModel().embed(fileIdentifier).content();
            chromaEmbeddingService.getEmbeddingStore().add(embedding, segment);
        } catch (Exception e) {
            log.warn("Error marking file as indexed: {}", e.getMessage());
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
            List<TextSegment> segments = documentSplitter.split(document);

            for (TextSegment segment : segments) {
                log.debug("Segment: {}", segment.text());
                Metadata metadata = new Metadata();
                metadata.put(FILE_PATH, path.toString());
                metadata.put(LAST_MODIFIED, Files.getLastModifiedTime(path).toMillis());
                metadata.put(INDEXED_AT, System.currentTimeMillis());
                markFileAsIndexed(path, new TextSegment(segment.text(), metadata));
            }

        } catch (IOException e) {
            log.warn("Error processing file: {}", path);
        }
    }
}
