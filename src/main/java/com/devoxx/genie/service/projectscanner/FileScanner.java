package com.devoxx.genie.service.projectscanner;

import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.basjes.gitignore.GitIgnoreFileSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles file system traversal logic for project scanning.
 */
@Slf4j
public class FileScanner {
    
    private static final String GITIGNORE = ".gitignore";

    private GitIgnoreFileSet gitIgnoreFileSet;

    @Getter
    private final List<Path> includedFiles = new ArrayList<>();
    @Getter
    private int skippedFileCount = 0;
    @Getter
    private int skippedDirectoryCount = 0;
    @Getter
    private int fileCount = 0;

    protected DirectoryScannerService createDirectoryScanner() {
        return new DirectoryScannerService();
    }

    public void reset() {
        includedFiles.clear();
        skippedFileCount = 0;
        skippedDirectoryCount = 0;
        fileCount = 0;
    }

    /**
     * Initializes the GitignoreParser for the specified project and directory.
     *
     * @param project        the current project
     * @param startDirectory the starting directory for scanning
     */
    public void initGitignoreParser(Project project, VirtualFile startDirectory) {
        String projectBasePath = determineCorrectProjectBaseDir(project, startDirectory);
        if (projectBasePath == null) {
            log.error("Project base directory could not be determined. GitIgnore parsing was not initialized.");
            return;
        }

        log.info("Initializing GitIgnore parser with resolved project base directory: " + projectBasePath);
        this.gitIgnoreFileSet = new GitIgnoreFileSet(new File(projectBasePath), false);

        collectGitignoreFiles(startDirectory);
    }

    /**
     * Determines the most appropriate base directory, considering project modules (workspaces).
     */
    private @Nullable String determineCorrectProjectBaseDir(@NotNull Project project, @NotNull VirtualFile startDirectory) {
        // First check if startDirectory belongs to an explicit module's content root
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

        VirtualFile contentRoot = fileIndex.getContentRootForFile(startDirectory);
        if (contentRoot != null) {
            log.info("Content root determined from workspace (module): " + contentRoot.getPath());
            return contentRoot.getPath();
        }

        // Fall back to project's general base directory if module determination fails
        String generalProjectBase = project.getBasePath();

        if (generalProjectBase != null) {
            log.info("Using project's general base directory as fallback: " + generalProjectBase);
            return generalProjectBase;
        }

        // Attempt to guess directory using IntelliJ ProjectUtil as the final fallback
        VirtualFile guessedProjectDir = ProjectUtil.guessProjectDir(project);
        if (guessedProjectDir != null && guessedProjectDir.exists()) {
            log.warn("Fallback: Guessed project dir used: " + guessedProjectDir.getPath());
            return guessedProjectDir.getPath();
        }

        // Could not determine correct project base dir
        log.error("Failed to determine a valid projectBaseDir from module and project settings.");
        return null;
    }

    private void collectGitignoreFiles(@NotNull VirtualFile directory) {
        VirtualFile gitignore = directory.findChild(GITIGNORE);
        if (gitignore != null && gitignore.exists()) {
            if (gitIgnoreFileSet == null) {
                gitIgnoreFileSet = new GitIgnoreFileSet(new File(directory.getPath()));
            }
        }

        // Recursively check subdirectories
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                collectGitignoreFiles(child);
            }
        }
    }

    /**
     * Scans project modules to find the highest common root directory.
     *
     * @param project the current project
     * @return the highest common root directory
     */
    public VirtualFile scanProjectModules(Project project) {
        // Create the scanner using our factory method
        DirectoryScannerService uniqueDirectoryScanner = createDirectoryScanner();

        // Collect all content roots from modules
        VirtualFile[] contentRootsFromAllModules =
                ProjectRootManager.getInstance(project).getContentRootsFromAllModules();

        // Add all content roots to the unique directory scanner
        Arrays.stream(contentRootsFromAllModules)
                .distinct()
                .forEach(uniqueDirectoryScanner::addDirectory);

        // Get the highest root directory
        return uniqueDirectoryScanner
                .getHighestCommonRoot()
                .orElseThrow();
    }

    /**
     * Scans a directory recursively to find all relevant files.
     *
     * @param fileIndex the project file index
     * @param directory the directory to scan
     * @param scanContentResult the result object to populate
     * @return list of files that should be included
     */
    public List<VirtualFile> scanDirectory(ProjectFileIndex fileIndex,
                                           VirtualFile directory,
                                           ScanContentResult scanContentResult) {
        List<VirtualFile> relevantFiles = new ArrayList<>();
        log.info("Starting directory scan for: " + directory.getPath());

        VfsUtilCore.visitChildrenRecursively(directory, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory()) {
                    log.info("Visiting directory: " + file.getPath());
                    if (shouldExcludeDirectory(file)) {
                        log.info("Excluding directory: " + file.getPath());
                        skippedDirectoryCount++;
                        return false;
                    }
                } else {
                    log.info("Checking file: " + file.getPath());
                    boolean isInContent = fileIndex.isInContent(file);
                    boolean shouldNotExclude = !shouldExcludeFile(file);
                    boolean shouldInclude = shouldIncludeFile(file);
                    log.info("File checks: isInContent=" + isInContent + ", shouldNotExclude=" + shouldNotExclude + ", shouldInclude=" + shouldInclude);
                    
                    if (isInContent && shouldNotExclude && shouldInclude) {
                        log.info("Including file: " + file.getPath());
                        relevantFiles.add(file);
                        fileCount++;
                        includedFiles.add(Paths.get(file.getPath()));
                    } else {
                        skippedFileCount++;
                        String reason = determineSkipReason(file, fileIndex);
                        log.info("Skipping file: " + file.getPath() + " (" + reason + ")");
                        scanContentResult.addSkippedFile(file.getPath(), reason);
                    }
                }
                return true;
            }
        });
        log.info("Scan completed. Found " + relevantFiles.size() + " relevant files, skipped " + skippedFileCount + " files");

        return relevantFiles;
    }
    
    /**
     * Determines the reason why a file was skipped.
     *
     * @param file the file that was skipped
     * @param fileIndex the project file index
     * @return the reason for skipping the file
     */
    private String determineSkipReason(VirtualFile file, ProjectFileIndex fileIndex) {
        if (!fileIndex.isInContent(file)) {
            return "not in project content";
        }
        
        // Check if file is excluded by settings or gitignore
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        List<String> excludedFiles = settings.getExcludedFiles();
        if (!excludedFiles.isEmpty() && excludedFiles.contains(file.getName())) {
            return "file explicitly excluded in settings";
        }
        
        // Check gitignore if enabled
        if (Boolean.TRUE.equals(settings.getUseGitIgnore()) && gitIgnoreFileSet != null) {
            try {
                if (gitIgnoreFileSet.ignoreFile(file.getPath())) {
                    return "excluded by .gitignore";
                }
            } catch (IllegalArgumentException e) {
                // Ignore the exception
            }
        }
        
        // Check file extension
        String extension = file.getExtension();
        if (extension == null) {
            return "no file extension";
        }
        
        extension = extension.toLowerCase();
        List<String> includedExtensions = settings.getIncludedFileExtensions();
        if (includedExtensions == null || includedExtensions.isEmpty()) {
            return "no file extensions configured for inclusion";
        }
        
        boolean isExtensionIncluded = includedExtensions.contains(extension);
        log.info("Extension check for " + file.getName() + ": extension=" + extension + ", included=" + isExtensionIncluded + ", includedList=" + includedExtensions);
        
        if (!isExtensionIncluded) {
            return "extension '" + extension + "' not in included list";
        }
        
        // If we reach here, there must be some other reason
        return "unknown reason";
    }

    /**
     * Generates a source tree representation of a directory.
     *
     * @param virtualFile the directory to represent
     * @param depth       the current recursion depth
     * @return a string representation of the source tree
     */
    public String generateSourceTreeRecursive(VirtualFile virtualFile, int depth) {
        StringBuilder result = new StringBuilder();
        String indent = "  ".repeat(depth);

        boolean excludeFile = shouldExcludeFile(virtualFile);
        boolean excludeDirectory = shouldExcludeDirectory(virtualFile);
        if (excludeFile || excludeDirectory) {
            return "";
        }

        result.append(indent).append(virtualFile.getName()).append("/\n");

        VirtualFile[] children = virtualFile.getChildren();
        if (children != null) {
            for (VirtualFile child : children) {
                if (child.isDirectory()) {
                    result.append(generateSourceTreeRecursive(child, depth + 1));
                } else if (shouldIncludeFile(child)) {
                    result.append(indent).append("  ").append(child.getName()).append("\n");
                }
            }
        }

        return result.toString();
    }

    /**
     * Checks if a directory should be excluded from scanning.
     *
     * @param file the directory to check
     * @return true if the directory should be excluded
     */
    public boolean shouldExcludeDirectory(@NotNull VirtualFile file) {
        if (!file.isDirectory()) {
            return false;
        }

        List<String> excludedDirectories = DevoxxGenieStateService.getInstance().getExcludedDirectories();

        // Add null check for excludedDirectories
        if (excludedDirectories == null || excludedDirectories.isEmpty()) {
            return false; // If excludedDirectories is null or empty, don't exclude any directories
        }

        // Check if the directory name or path is in the excluded list
        return excludedDirectories.contains(file.getName()) ||
               excludedDirectories.contains(file.getPath()) ||
               shouldExcludeFile(file);
    }

    /**
     * Checks if a file should be excluded from scanning.
     *
     * @param file the file to check
     * @return true if the file should be excluded
     */
    public boolean shouldExcludeFile(@NotNull VirtualFile file) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

        List<String> excludedFiles = settings.getExcludedFiles();

        // Check if file is in excluded files list
        if (!excludedFiles.isEmpty() && excludedFiles.contains(file.getName())) {
            return true;
        }

        // Check gitignore if enabled
        if (Boolean.TRUE.equals(settings.getUseGitIgnore()) && gitIgnoreFileSet != null) {
            try {
                // Try to use the gitignore check and catch the specific IllegalArgumentException
                return gitIgnoreFileSet.ignoreFile(file.getPath());
            } catch (IllegalArgumentException e) {
                // If the file is not within the project directory, just ignore the error
                // and don't apply gitignore rules to this file
                log.debug("File outside project directory, skipping gitignore check: " + file.getPath());
                return false;
            }
        }
        return false;
    }

    public boolean shouldIncludeFile(@NotNull VirtualFile file) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

        // First check if file should be excluded
        if (shouldExcludeFile(file)) {
            log.info("Skipping file: " + file.getPath() + " (excluded by settings or .gitignore)");
            return false;
        }

        // Then check if extension is included
        String extension = file.getExtension();
        if (extension == null) {
            return false;
        }

        // Convert to lowercase for case-insensitive comparison
        extension = extension.toLowerCase();
        
        List<String> includedExtensions = settings.getIncludedFileExtensions();
        
        // Debug log to check the actual extension and included list
        log.info("File: " + file.getPath() + ", Extension: " + extension + ", Included extensions: " + includedExtensions);

        boolean includeFile = includedExtensions != null &&
                !includedExtensions.isEmpty() &&
                includedExtensions.contains(extension);

        log.info("Decision for file: " + file.getPath() + ", Include: " + includeFile + ", Extension: " + extension);
        return includeFile;
    }
}
