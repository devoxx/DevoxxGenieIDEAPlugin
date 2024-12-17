package com.devoxx.genie.service.projectscanner;
import com.devoxx.genie.model.ScanContentResult;
import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.GitignoreParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ProjectScannerService {

    private static final Logger LOG = Logger.getInstance(ProjectScannerService.class.getName());

    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
    private static final String GITIGNORE = ".gitignore";

    private GitignoreParser gitignoreParser;

    public static ProjectScannerService getInstance() {
        return ApplicationManager.getApplication().getService(ProjectScannerService.class);
    }

    public CompletableFuture<ScanContentResult> scanProject(Project project,
                                                            VirtualFile startDirectory,
                                                            int windowContextMaxTokens,
                                                            boolean isTokenCalculation) {
        CompletableFuture<ScanContentResult> future = new CompletableFuture<>();
        ScanContentResult scanContentResult = new ScanContentResult();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ReadAction.nonBlocking(() -> {
                            StringBuilder result = new StringBuilder();
                            StringBuilder fullContent;

                            initGitignoreParser(project, startDirectory);

                            if (startDirectory == null) {
                                result.append("Directory Structure:\n");
                                fullContent = getContentFromModules(project, result, scanContentResult);
                            } else if (startDirectory.isDirectory()) {
                                result.append("Directory Structure:\n");
                                fullContent = processDirectory(project, startDirectory, result, scanContentResult);
                            } else {
                                // Handle the case where startDirectory is a file
                                result.append("File:\n");
                                fullContent = processSingleFile(startDirectory, result, scanContentResult);
                            }

                            String content = isTokenCalculation ? fullContent.toString() :
                                    truncateToTokens(fullContent.toString(), windowContextMaxTokens, isTokenCalculation);

                            scanContentResult.setTokenCount(ENCODING.countTokensOrdinary(content));
                            scanContentResult.setContent(content);

                            return scanContentResult;
                        })
                        .inSmartMode(project)
                        .finishOnUiThread(ModalityState.defaultModalityState(), future::complete)
                        .submit(AppExecutorUtil.getAppExecutorService());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    public ScanContentResult scanProjectSynchronously(Project project, VirtualFile startDirectory, int windowContextMaxTokens, boolean isTokenCalculation) {
        ScanContentResult scanContentResult = new ScanContentResult();
        ReadAction.run(() -> {
            StringBuilder result = new StringBuilder();
            StringBuilder fullContent;
            initGitignoreParser(project, startDirectory);

            if (startDirectory == null) {
                result.append("Directory Structure:\n");
                fullContent = getContentFromModules(project, result, scanContentResult);
            } else if (startDirectory.isDirectory()) {
                result.append("Directory Structure:\n");
                fullContent = processDirectory(project, startDirectory, result, scanContentResult);
            } else {
                // Handle the case where startDirectory is a file
                result.append("File:\n");
                fullContent = processSingleFile(startDirectory, result, scanContentResult);
            }

            String content = isTokenCalculation ? fullContent.toString() : truncateToTokens(fullContent.toString(), windowContextMaxTokens, isTokenCalculation);
            scanContentResult.setTokenCount(ENCODING.countTokens(content));
            scanContentResult.setContent(content);
        });
        return scanContentResult;
    }

    private void initGitignoreParser(Project project, VirtualFile startDirectory) {

        VirtualFile gitignoreFile = null;
        if (startDirectory != null && startDirectory.isDirectory()) {
            gitignoreFile = startDirectory.findChild(GITIGNORE);
        } else if (project != null) {
            gitignoreFile = Objects.requireNonNull(ProjectUtil.guessProjectDir(project)).findChild(GITIGNORE);
        } else {
            LOG.error("Error initializing GitignoreParser: .gitignore file could not be found or startDirectory is not a directory");
        }

        if (gitignoreFile != null && gitignoreFile.exists()) {
            try {
                gitignoreParser = new GitignoreParser(Paths.get(gitignoreFile.getPath()));
            } catch (IOException e) {
                LOG.error("Error initializing GitignoreParser: " + e.getMessage());
            }
        }
    }

    private StringBuilder getContentFromModules(Project project,
                                                StringBuilder result,
                                                ScanContentResult scanContentResult) {

        UniqueDirectoryScannerService uniqueDirectoryScanner = new UniqueDirectoryScannerService();

        // Collect all content roots from modules
        VirtualFile[] contentRootsFromAllModules =
                ProjectRootManager.getInstance(project).getContentRootsFromAllModules();

        // Add all content roots to the unique directory scanner
        Arrays.stream(contentRootsFromAllModules)
                .distinct()
                .forEach(uniqueDirectoryScanner::addDirectory);

        // Get the highest root directory and process the content
        return uniqueDirectoryScanner
                .getHighestCommonRoot()
                .map(highestCommonRoot -> processDirectory(project,
                        highestCommonRoot,
                        result,
                        scanContentResult))
                .orElseThrow();
    }

    private @NotNull StringBuilder processDirectory(Project project,
                                                    VirtualFile startDirectory,
                                                    @NotNull StringBuilder result,
                                                    ScanContentResult scanContentResult) {
        result.append(generateSourceTreeRecursive(startDirectory, 0));

        result.append("\n\nFile Contents:\n");

        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

        StringBuilder fullContent = new StringBuilder(result);

        walkThroughDirectory(startDirectory, fileIndex, fullContent, scanContentResult);

        return fullContent;
    }

    private @NotNull StringBuilder processSingleFile(@NotNull VirtualFile file,
                                                     @NotNull StringBuilder result,
                                                     ScanContentResult scanContentResult) {
        result.append(file.getName()).append("\n");
        result.append("\n\nFile Contents:\n");

        StringBuilder fullContent = new StringBuilder(result);
        if (shouldIncludeFile(file)) {
            readFileContent(file, fullContent, scanContentResult);
        } else {
            scanContentResult.incrementSkippedFileCount();
            LOG.debug("Skipping file: " + file.getPath() + " (excluded by settings or .gitignore)");
        }
        return fullContent;
    }

    private void walkThroughDirectory(@NotNull VirtualFile directory,
                                      @NotNull ProjectFileIndex fileIndex,
                                      @NotNull StringBuilder fullContent,
                                      @NotNull ScanContentResult scanContentResult) {

        VfsUtilCore.visitChildrenRecursively(directory, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (file.isDirectory()) {
                    if (shouldExcludeDirectory(file)) {
                        scanContentResult.incrementSkippedDirectoryCount();
                        return false;
                    }
                } else {
                    if (fileIndex.isInContent(file) && !shouldExcludeFile(file) && shouldIncludeFile(file)) {
                        readFileContent(file, fullContent, scanContentResult);
                    } else {
                        scanContentResult.incrementSkippedFileCount();
                        LOG.debug("Skipping file: " + file.getPath() + " (not in content, excluded by settings, or .gitignore)");
                    }
                }
                return true;
            }
        });
    }

    private void readFileContent(@NotNull VirtualFile file,
                                 @NotNull StringBuilder fullContent,
                                 @NotNull ScanContentResult scanContentResult) {
        scanContentResult.incrementFileCount();
        scanContentResult.addFile(Paths.get(file.getPath()));

        String header = "\n--- " + file.getPath() + " ---\n";
        fullContent.append(header);

        try {
            String content = ReadAction.compute(() -> {
                try {
                    return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    LOG.error("Error reading file: " + file.getPath(), e);
                    return "Error reading file content: " + e.getMessage();
                }
            });

            content = processFileContent(content);
            fullContent.append(content).append("\n");
        } catch (Exception e) {
            String errorMsg = "Error processing file: " + e.getMessage() + "\n";
            fullContent.append(errorMsg);
        }
    }

    private String truncateToTokens(@NotNull String text,
                                    int windowContext,
                                    boolean isTokenCalculation) {

        IntArrayList tokens = ENCODING.encodeOrdinary(text);
        if (tokens.size() <= windowContext) {
            return text;
        }

        IntArrayList truncatedTokens = new IntArrayList(windowContext);
        for (int i = 0; i < windowContext; i++) {
            truncatedTokens.add(tokens.get(i));
        }

        String truncatedContent = ENCODING.decode(truncatedTokens);
        return isTokenCalculation ? truncatedContent :
                truncatedContent + "\n--- Project context truncated due to token limit ---\n";
    }

    private @NotNull String generateSourceTreeRecursive(VirtualFile virtualFile, int depth) {
        StringBuilder result = new StringBuilder();
        String indent = "  ".repeat(depth);

        boolean excludeFile = shouldExcludeFile(virtualFile);
        boolean excludeDirectory = shouldExcludeDirectory(virtualFile);
        if (excludeFile || excludeDirectory) {
            return "";
        }

        result.append(indent).append(virtualFile.getName()).append("/\n");

        for (VirtualFile child : virtualFile.getChildren()) {
            if (child.isDirectory()) {
                result.append(generateSourceTreeRecursive(child, depth + 1));
            } else if (shouldIncludeFile(child)) {
                result.append(indent).append("  ").append(child.getName()).append("\n");
            }
        }

        return result.toString();
    }

    private boolean shouldExcludeDirectory(@NotNull VirtualFile file) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();
        return file.isDirectory() &&
                (settings.getExcludedDirectories().contains(file.getName()) || shouldExcludeFile(file));
    }

    private boolean shouldExcludeFile(@NotNull VirtualFile file) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

        // Check if file is in excluded files list
        if (settings.getExcludedFiles().contains(file.getName())) {
            return true;
        }

        // Check gitignore if enabled
        if (Boolean.TRUE.equals(settings.getUseGitIgnore()) &&
                gitignoreParser != null) {
            Path path = Paths.get(file.getPath());
            return gitignoreParser.matches(path);
        }
        return false;
    }

    private boolean shouldIncludeFile(@NotNull VirtualFile file) {
        DevoxxGenieSettingsService settings = DevoxxGenieStateService.getInstance();

        // First check if file should be excluded
        if (shouldExcludeFile(file)) {
            return false;
        }

        // Then check if extension is included
        String extension = file.getExtension();
        return extension != null && settings.getIncludedFileExtensions().contains(extension.toLowerCase());
    }

    private String processFileContent(String content) {
        if (Boolean.TRUE.equals(DevoxxGenieStateService.getInstance().getExcludeJavaDoc())) {
            return removeJavadoc(content);
        }
        return content;
    }

    private @NotNull String removeJavadoc(String content) {
        // Remove block comments (which include Javadoc)
        content = content.replaceAll("/\\*{1,2}[\\s\\S]*?\\*/", "");
        // Remove single-line comments that start with '///'
        content = content.replaceAll("^\\s*///.*$", "");
        return content;
    }
}
