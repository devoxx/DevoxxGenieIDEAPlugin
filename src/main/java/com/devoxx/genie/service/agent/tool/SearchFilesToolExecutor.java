package com.devoxx.genie.service.agent.tool;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SearchFilesToolExecutor implements ToolExecutor {

    static final int MAX_RESULTS = 50;
    static final int MAX_LINE_LENGTH = 200;
    static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "build", "out", "target", ".idea", "bin", ".gradle"
    );

    private final Project project;

    public SearchFilesToolExecutor(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public String execute(ToolExecutionRequest request, Object memoryId) {
        try {
            String patternStr = ToolArgumentParser.getString(request.arguments(), "pattern");
            String path = ToolArgumentParser.getString(request.arguments(), "path");
            String filePattern = ToolArgumentParser.getString(request.arguments(), "file_pattern");

            if (patternStr == null || patternStr.isBlank()) {
                return "Error: 'pattern' parameter is required.";
            }

            Pattern regex = compilePattern(patternStr);
            if (regex == null) {
                return "Error: Invalid regex pattern: " + patternStr;
            }

            PathMatcher fileMatcher = filePattern != null
                    ? FileSystems.getDefault().getPathMatcher("glob:" + filePattern)
                    : null;

            return ReadAction.compute(() -> {
                VirtualFile projectBase = getProjectBaseDir();
                return searchFiles(patternStr, path, regex, fileMatcher, projectBase);
            });
        } catch (Exception e) {
            log.error("Error searching files", e);
            return "Error: Failed to search files - " + e.getMessage();
        }
    }

    VirtualFile getProjectBaseDir() {
        return ProjectUtil.guessProjectDir(project);
    }

    static Pattern compilePattern(String patternStr) {
        try {
            return Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            return null;
        }
    }

    @NotNull String searchFiles(String patternStr, String path,
                                Pattern regex, PathMatcher fileMatcher,
                                VirtualFile projectBase) {
        if (projectBase == null) {
            return "Error: Project base directory not found.";
        }

        VirtualFile searchDir = resolveSearchDir(path, projectBase);
        if (searchDir == null) {
            return "Error: Directory not found: " + (path != null ? path : ".");
        }
        if (!isAncestor(projectBase, searchDir)) {
            return "Error: Access denied - path is outside the project root.";
        }

        StringBuilder result = new StringBuilder();
        int[] count = {0};
        searchInDirectory(searchDir, projectBase, regex, fileMatcher, result, count);

        if (count[0] == 0) {
            return "No matches found for pattern: " + patternStr;
        }
        if (count[0] >= MAX_RESULTS) {
            result.append("\n... (truncated, showing first ").append(MAX_RESULTS).append(" results)");
        }
        return result.toString();
    }

    static VirtualFile resolveSearchDir(String path, @NotNull VirtualFile projectBase) {
        if (path == null || path.isBlank() || path.equals(".")) {
            return projectBase;
        }
        return projectBase.findFileByRelativePath(path);
    }

    void searchInDirectory(VirtualFile dir, VirtualFile projectBase,
                           Pattern regex, PathMatcher fileMatcher,
                           StringBuilder result, int[] count) {
        VirtualFile[] children = dir.getChildren();
        if (children == null) return;

        for (VirtualFile child : children) {
            if (count[0] >= MAX_RESULTS) return;

            if (child.isDirectory()) {
                if (SKIP_DIRS.contains(child.getName())) continue;
                searchInDirectory(child, projectBase, regex, fileMatcher, result, count);
            } else {
                if (isBinaryFile(child)) continue;
                if (fileMatcher != null && !fileMatcher.matches(java.nio.file.Path.of(child.getName()))) continue;

                searchInFile(child, projectBase, regex, result, count);
            }
        }
    }

    void searchInFile(VirtualFile file, VirtualFile projectBase,
                      Pattern regex, StringBuilder result, int[] count) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String relativePath = getRelativePath(file, projectBase);
            if (relativePath == null) return;

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (count[0] >= MAX_RESULTS) return;

                Matcher matcher = regex.matcher(line);
                if (matcher.find()) {
                    String displayLine = line.length() > MAX_LINE_LENGTH
                            ? line.substring(0, MAX_LINE_LENGTH) + "..."
                            : line;
                    result.append(relativePath).append(":").append(lineNumber)
                            .append(": ").append(displayLine.trim()).append("\n");
                    count[0]++;
                }
            }
        } catch (Exception e) {
            // Skip files that can't be read
        }
    }

    boolean isAncestor(VirtualFile ancestor, VirtualFile descendant) {
        return VfsUtil.isAncestor(ancestor, descendant, false);
    }

    String getRelativePath(VirtualFile file, VirtualFile ancestor) {
        return VfsUtil.getRelativePath(file, ancestor);
    }

    static boolean isBinaryFile(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        if (extension == null) return false;
        return Set.of("jar", "class", "png", "jpg", "jpeg", "gif", "ico", "svg",
                "zip", "gz", "tar", "bin", "exe", "dll", "so", "dylib",
                "pdf", "woff", "woff2", "ttf", "eot").contains(extension.toLowerCase());
    }
}
