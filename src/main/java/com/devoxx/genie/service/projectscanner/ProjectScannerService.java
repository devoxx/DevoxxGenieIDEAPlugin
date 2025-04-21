package com.devoxx.genie.service.projectscanner;

import com.devoxx.genie.model.ScanContentResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Setter
@Getter
public class ProjectScannerService {

    protected FileScanner fileScanner;

    protected ContentExtractor contentExtractor;

    protected TokenCalculator tokenCalculator;

    // Use a Map to store ProjectFileIndex instances per project
    private final Map<String, ProjectFileIndex> projectFileIndexMap = new HashMap<String, ProjectFileIndex>();

    public ProjectScannerService() {
        this.fileScanner = new FileScanner();
        this.contentExtractor = new ContentExtractor();
        this.tokenCalculator = new TokenCalculator();
    }

    public static ProjectScannerService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectScannerService.class);
    }

    /**
     * Get the ProjectFileIndex for a specific project
     * This ensures we always use the correct context for each project
     *
     * @param project The project to get the file index for
     * @return The ProjectFileIndex for the specified project
     */
    private ProjectFileIndex getProjectFileIndex(@NotNull Project project) {
        String projectLocationHash = project.getLocationHash();
        log.info("Getting ProjectFileIndex for project: " + project.getName() +
                " with location hash: " + projectLocationHash);

        return projectFileIndexMap.computeIfAbsent(projectLocationHash, hash -> {
            log.info("Creating new ProjectFileIndex for project: " + project.getName());
            return ProjectFileIndex.getInstance(project);
        });
    }

    public ScanContentResult scanProject(Project project,
                                         VirtualFile startDirectory,
                                         int windowContextMaxTokens,
                                         boolean isTokenCalculation) {

        // Always get the correct ProjectFileIndex for this project
        ProjectFileIndex projectFileIndex = getProjectFileIndex(project);
        log.debug("Scanning project: {} with directory: {}", project.getName(),
                (startDirectory != null ? startDirectory.getPath() : "null"));

        ScanContentResult scanContentResult = new ScanContentResult();
        ReadAction.run(() -> {
            fileScanner.reset();

            // Get a valid startDirectory if null
            VirtualFile resolvedDirectory = startDirectory;
            if (resolvedDirectory == null) {
                // Find the project root directory
                resolvedDirectory = fileScanner.scanProjectModules(project);
                log.info("Resolved null startDirectory to project root: {}",
                        (resolvedDirectory != null ? resolvedDirectory.getPath() : "null"));
            }

            fileScanner.initGitignoreParser(project, resolvedDirectory);

            log.info("Starting content scan for {}", (startDirectory != null ? startDirectory.getPath() : "entire project"));
            String content = scanContent(project, startDirectory, windowContextMaxTokens, isTokenCalculation, scanContentResult, projectFileIndex);

            // Log included files for debugging
            log.info("Files included after scan: {}", fileScanner.getIncludedFiles().size());
            fileScanner.getIncludedFiles().forEach(file -> {
                log.info("Including file in result: {}", file);
                scanContentResult.addFile(file);
            });

            int tokenCount = tokenCalculator.calculateTokens(content);
            log.info("FINAL TOKEN COUNT: {} tokens", tokenCount);
            log.info("Content length: {} characters", content.length());
            log.info("First 100 chars: {}", content.substring(0, Math.min(100, content.length())));
            log.info("Last 100 chars: {}", content.substring(Math.max(0, content.length() - 100)));
            scanContentResult.setTokenCount(tokenCount);
            scanContentResult.setContent(content);
            scanContentResult.setFileCount(fileScanner.getFileCount());
            scanContentResult.setSkippedFileCount(fileScanner.getSkippedFileCount());
            scanContentResult.setSkippedDirectoryCount(fileScanner.getSkippedDirectoryCount());

            log.info("Scan complete. Files: {}, Skipped files: {}, Skipped directories: {}",
                    fileScanner.getFileCount(),
                    fileScanner.getSkippedFileCount(),
                    fileScanner.getSkippedDirectoryCount());
        });
        return scanContentResult;
    }

    // Changed from private to public for better testability
    public @NotNull String scanContent(Project project,
                                       VirtualFile startDirectory,
                                       int windowContextMaxTokens,
                                       boolean isTokenCalculation,
                                       ScanContentResult scanContentResult,
                                       ProjectFileIndex projectFileIndex) {
        // We're now using the projectFileIndex parameter directly

        StringBuilder directoryStructure = new StringBuilder();
        String fileContents;

        if (startDirectory == null) {
            // Case 1: No directory provided, scan all modules
            VirtualFile rootDirectory = fileScanner.scanProjectModules(project);
            directoryStructure.append(fileScanner.generateSourceTreeRecursive(rootDirectory, 0));
            // Use the provided projectFileIndex
            List<VirtualFile> files = fileScanner.scanDirectory(projectFileIndex, rootDirectory, scanContentResult);
            fileContents = extractAllFileContents(files);
        } else if (startDirectory.isDirectory()) {
            // Case 2: Directory provided
            directoryStructure.append(fileScanner.generateSourceTreeRecursive(startDirectory, 0));
            // Use the provided projectFileIndex
            List<VirtualFile> files = fileScanner.scanDirectory(projectFileIndex, startDirectory, scanContentResult);
            fileContents = extractAllFileContents(files);
        } else {
            // Case 3: Single file provided
            return handleSingleFile(startDirectory);
        }

        String fullContent = contentExtractor.combineContent(directoryStructure.toString(), fileContents);

        // For token calculation (Calc tokens for directory), we want the ACTUAL token count
        // For content to be used in prompts, we need to truncate
        if (isTokenCalculation) {
            log.info("Not truncating content for token calculation only");
            return fullContent;
        } else {
            // Only truncate if the content will be used (not for token calculation only)
            log.info("Truncating content to {} tokens for usage in prompt", windowContextMaxTokens);
            return tokenCalculator.truncateToTokens(fullContent, windowContextMaxTokens);
        }
    }

    // Changed from private to public for better testability
    public @NotNull String handleSingleFile(@NotNull VirtualFile file) {
        StringBuilder result = new StringBuilder("File:\n");
        result.append(file.getName()).append("\n\nFile Contents:\n");

        if (fileScanner.shouldIncludeFile(file)) {
            result.append(contentExtractor.extractFileContent(file));
        } else {
            log.debug("Skipping file: {} (excluded by settings or .gitignore)", file.getPath());
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
