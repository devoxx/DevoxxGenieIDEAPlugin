package com.devoxx.genie.service.projectscanner;

import com.devoxx.genie.model.ScanContentResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public class ProjectScannerService {
    private static final Logger LOG = Logger.getInstance(ProjectScannerService.class.getName());

    protected FileScanner fileScanner;

    protected ContentExtractor contentExtractor;

    protected TokenCalculator tokenCalculator;

    protected ProjectFileIndex projectFileIndex;

    public ProjectScannerService() {
        this.fileScanner = new FileScanner();
        this.contentExtractor = new ContentExtractor();
        this.tokenCalculator = new TokenCalculator();
    }

    public static ProjectScannerService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectScannerService.class);
    }

    public ScanContentResult scanProject(Project project,
                                         VirtualFile startDirectory,
                                         int windowContextMaxTokens,
                                         boolean isTokenCalculation) {

        if (this.projectFileIndex == null) {
            this.projectFileIndex = ProjectFileIndex.getInstance(project);
        }

        ScanContentResult scanContentResult = new ScanContentResult();
        ReadAction.run(() -> {
            fileScanner.reset();
            fileScanner.initGitignoreParser(project, startDirectory);

            String content = scanContent(project, startDirectory, windowContextMaxTokens, isTokenCalculation);
            fileScanner.getIncludedFiles().forEach(scanContentResult::addFile);

            scanContentResult.setTokenCount(tokenCalculator.calculateTokens(content));
            scanContentResult.setContent(content);
            scanContentResult.setFileCount(fileScanner.getFileCount());
            scanContentResult.setSkippedFileCount(fileScanner.getSkippedFileCount());
            scanContentResult.setSkippedDirectoryCount(fileScanner.getSkippedDirectoryCount());
        });
        return scanContentResult;
    }

    // Changed from private to public for better testability
    public @NotNull String scanContent(Project project,
                                       VirtualFile startDirectory,
                                       int windowContextMaxTokens,
                                       boolean isTokenCalculation) {
        // Initialize projectFileIndex if it's null
        if (this.projectFileIndex == null) {
            this.projectFileIndex = ProjectFileIndex.getInstance(project);
        }

        StringBuilder directoryStructure = new StringBuilder();
        String fileContents;

        if (startDirectory == null) {
            // Case 1: No directory provided, scan all modules
            VirtualFile rootDirectory = fileScanner.scanProjectModules(project);
            directoryStructure.append(fileScanner.generateSourceTreeRecursive(rootDirectory, 0));
            // Use the stored projectFileIndex instead of getting it again
            List<VirtualFile> files = fileScanner.scanDirectory(projectFileIndex, rootDirectory);
            fileContents = extractAllFileContents(files);
        } else if (startDirectory.isDirectory()) {
            // Case 2: Directory provided
            directoryStructure.append(fileScanner.generateSourceTreeRecursive(startDirectory, 0));
            // Use the stored projectFileIndex instead of getting it again
            List<VirtualFile> files = fileScanner.scanDirectory(projectFileIndex, startDirectory);
            fileContents = extractAllFileContents(files);
        } else {
            // Case 3: Single file provided
            return handleSingleFile(startDirectory);
        }

        String fullContent = contentExtractor.combineContent(directoryStructure.toString(), fileContents);

        // Truncate if necessary
        return tokenCalculator.truncateToTokens(fullContent, windowContextMaxTokens, isTokenCalculation);
    }

    // Changed from private to public for better testability
    public @NotNull String handleSingleFile(@NotNull VirtualFile file) {
        StringBuilder result = new StringBuilder("File:\n");
        result.append(file.getName()).append("\n\nFile Contents:\n");

        if (fileScanner.shouldIncludeFile(file)) {
            result.append(contentExtractor.extractFileContent(file));
        } else {
            LOG.debug("Skipping file: " + file.getPath() + " (excluded by settings or .gitignore)");
        }

        return result.toString();
    }

    // Changed from private to public for better testability
    public @NotNull String extractAllFileContents(@NotNull List<VirtualFile> files) {
        return files.stream()
                .map(contentExtractor::extractFileContent)
                .collect(Collectors.joining());
    }
}
