package com.devoxx.genie.service.analyzer.util;

import com.devoxx.genie.service.analyzer.tools.GlobTool;
import com.esotericsoftware.kryo.kryo5.minlog.Log;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses and handles .gitignore files for determining which files and directories should be excluded from scanning.
 * Supports both root .gitignore files and nested .gitignore files in subdirectories.
 */
public class GitignoreParser {
    private static final Logger LOG = Logger.getInstance(GitignoreParser.class);

    // Root patterns that apply to the entire project
    private final List<Pattern> rootExcludePatterns = new ArrayList<>();
    private final List<Pattern> rootIncludePatterns = new ArrayList<>();
    
    // Maps subdirectory paths to their own gitignore patterns
    private final Map<String, List<Pattern>> nestedExcludePatterns = new HashMap<>();
    private final Map<String, List<Pattern>> nestedIncludePatterns = new HashMap<>();
    
    // The project root directory
    private final VirtualFile baseDir;

    /**
     * Initializes the parser by reading and parsing .gitignore files from the given directory
     * and its subdirectories
     *
     * @param baseDir The base directory containing the .gitignore files
     */
    public GitignoreParser(@NotNull VirtualFile baseDir) {
        this.baseDir = baseDir;
        parseRootGitignore();
        parseNestedGitignores();
    }

    /**
     * Parses the root .gitignore file
     */
    private void parseRootGitignore() {
        VirtualFile gitignoreFile = baseDir.findChild(".gitignore");

        if (gitignoreFile == null) {
            LOG.info("No root .gitignore file found");
            return;
        }

        try {
            String content = VfsUtil.loadText(gitignoreFile);
            parseGitignoreContent(content, rootExcludePatterns, rootIncludePatterns, "");
        } catch (IOException e) {
            Log.error("Error reading root .gitignore file: " + e.getMessage());
        }
    }

    /**
     * Recursively parses nested .gitignore files in subdirectories
     */
    private void parseNestedGitignores() {

        // Use a file visitor to find all nested .gitignore files
        VfsUtil.visitChildrenRecursively(baseDir, new com.intellij.openapi.vfs.VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                // Skip the root .gitignore as it's already processed
                if (file.getName().equals(".gitignore") && !file.getPath().equals(baseDir.getPath() + "/.gitignore")) {
                    try {
                        // Get the relative directory path where this .gitignore is located
                        String relativeDirPath = getRelativePath(baseDir, file.getParent());
                        
                        if (relativeDirPath != null) {
                            // Create pattern lists for this directory if they don't exist
                            nestedExcludePatterns.putIfAbsent(relativeDirPath, new ArrayList<>());
                            nestedIncludePatterns.putIfAbsent(relativeDirPath, new ArrayList<>());
                            
                            // Parse the content
                            String content = VfsUtil.loadText(file);
                            parseGitignoreContent(
                                    content, 
                                    nestedExcludePatterns.get(relativeDirPath), 
                                    nestedIncludePatterns.get(relativeDirPath),
                                    relativeDirPath
                            );
                        }
                    } catch (IOException e) {
                        LOG.error("Error reading nested .gitignore file: " + e.getMessage());
                    }
                }
                return true;
            }
        });
    }

    /**
     * Parses the content of a .gitignore file and adds patterns to the appropriate lists
     *
     * @param content The content of the .gitignore file
     * @param excludePatterns The list to add exclusion patterns to
     * @param includePatterns The list to add inclusion patterns to
     * @param relativeDir The relative directory path where the .gitignore is located
     */
    private void parseGitignoreContent(
            @NotNull String content, 
            @NotNull List<Pattern> excludePatterns, 
            @NotNull List<Pattern> includePatterns,
            @NotNull String relativeDir) {
        
        String[] lines = content.split("\\r?\\n");
        String logPrefix = relativeDir.isEmpty() ? "" : "[" + relativeDir + "] ";

        for (String line : lines) {
            // Skip empty lines and comments
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            // Check if it's a negation pattern (inclusion)
            boolean isNegation = line.startsWith("!");
            String pattern = isNegation ? line.substring(1).trim() : line.trim();
            
            // Store the original pattern for optimization checks
            String originalPattern = pattern;

            // Handle directory-only patterns that end with /
            boolean directoryOnly = pattern.endsWith("/");
            if (directoryOnly) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }
            
            // Special optimization for simple top-level directory patterns like "build/" or ".gradle/"
            // These are very common and can be matched more efficiently
            if (directoryOnly && !pattern.contains("/") && !isNegation && relativeDir.isEmpty()) {
                // For simple directory patterns like "build/" or ".gradle/", add a direct pattern
                // This allows for very fast filtering of entire directory trees
                String simpleRegex = "^" + pattern + "(/.*)?$";
                Pattern simplePattern = Pattern.compile(simpleRegex);
                excludePatterns.add(simplePattern);
                
                // Add this to our set of directly excluded top-level directories for ultra-fast lookups
                directlyExcludedDirs.add(pattern);
                continue; // Skip the standard pattern generation for these
            }

            // Convert the gitignore pattern to regex for standard cases
            String regex = convertGitignoreToRegex(pattern, directoryOnly, relativeDir);

            // Add the pattern to the appropriate list
            Pattern compiledPattern = Pattern.compile(regex);
            if (isNegation) {
                includePatterns.add(compiledPattern);
            } else {
                excludePatterns.add(compiledPattern);
            }
        }
    }

    /**
     * Converts a gitignore pattern to a regular expression
     *
     * @param pattern       The gitignore pattern
     * @param directoryOnly Whether the pattern applies only to directories
     * @param relativeDir   The relative directory path where the .gitignore is located
     * @return A regex pattern equivalent to the gitignore pattern
     */
    @NotNull
    private String convertGitignoreToRegex(@NotNull String pattern, boolean directoryOnly, @NotNull String relativeDir) {
        // Make a copy to preserve the original pattern for logging
        String modifiedPattern = pattern;
        
        // For nested .gitignore files, patterns are relative to that directory
        if (!relativeDir.isEmpty()) {
            // If it's an absolute path (starts with /), it's relative to project root instead
            if (!modifiedPattern.startsWith("/")) {
                // For nested .gitignore files, prepend the relative directory path
                modifiedPattern = relativeDir + "/" + modifiedPattern;
            }
        }
        
        // 1. Remove leading / if present - it represents the project root directory
        if (modifiedPattern.startsWith("/")) {
            modifiedPattern = modifiedPattern.substring(1);
        }
        
        // 2. Handle ** (matches any number of directories)
        modifiedPattern = modifiedPattern.replace("**/", "(.*/)*");
        modifiedPattern = modifiedPattern.replace("/**", "(/.*)*");
        
        // 3. Handle special cases for matching files with certain extensions
        if (modifiedPattern.startsWith("*.")) {
            // Pattern like "*.txt" should match in any directory
            modifiedPattern = "(.*/)*" + modifiedPattern;
        }
        
        // 4. Convert gitignore glob to regex using the existing GlobUtil
        String regex = GlobTool.convertGlobToRegex(modifiedPattern);
        
        // 5. If the pattern doesn't contain a / and isn't an absolute path, it matches files in any directory
        if (!pattern.contains("/") && !pattern.startsWith("/")) {
            // If we're in a nested .gitignore, the pattern only applies to that directory and below
            if (!relativeDir.isEmpty() && !regex.startsWith(relativeDir)) {
                regex = "(" + relativeDir + "/.*/)?" + regex;
            } else if (relativeDir.isEmpty()) {
                // For root .gitignore, it applies to any directory
                regex = "(.*/)*" + regex;
            }
        }
        
        // 6. If it's a directory-only pattern, ensure it matches only directories
        if (directoryOnly) {
            regex += "(/.*)*";
        }
        
        // 7. Anchor the pattern
        regex = "^" + regex + "$";
        
        return regex;
    }

    /**
     * Gets the relative path of a file or directory compared to the base directory
     *
     * @param baseDir The base directory
     * @param file The file or directory to get the relative path for
     * @return The relative path, or null if the file is not under the base directory
     */
    @Nullable
    private String getRelativePath(@NotNull VirtualFile baseDir, @NotNull VirtualFile file) {
        String basePath = baseDir.getPath();
        String filePath = file.getPath();
        
        if (filePath.startsWith(basePath)) {
            // Remove the base path and any leading slash to get the relative path
            String relativePath = filePath.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            return relativePath;
        }
        
        return null;
    }

    /**
     * Fast lookup map for top-level directories that are directly excluded
     * This avoids regex matching for common cases like "build/" and ".gradle/"
     */
    private final Set<String> directlyExcludedDirs = new HashSet<>();
    
    /**
     * Checks if a file or directory should be ignored based on gitignore rules
     *
     * @param path         The relative path to check
     * @param isDirectory  Whether the path represents a directory
     * @return true if the path should be ignored, false otherwise
     */
    public boolean shouldIgnore(@NotNull String path, boolean isDirectory) {
        // Normalize path - ensure it doesn't start with /
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        
        // Handle empty path
        if (normalizedPath.isEmpty()) {
            return false;
        }
        
        // Fast path: Check if this is or is under a directly excluded top-level directory
        // This handles common patterns like "build/" or ".gradle/" very efficiently
        int slashIndex = normalizedPath.indexOf('/');
        if (slashIndex > 0) {
            // This path has at least one directory level
            String topLevelDir = normalizedPath.substring(0, slashIndex);
            if (directlyExcludedDirs.contains(topLevelDir)) {
                return true; // This is under a directly excluded directory
            }
        } else if (!normalizedPath.contains("/") && directlyExcludedDirs.contains(normalizedPath)) {
            // This is a top-level directory that's directly excluded
            return true;
        }
            
        // If we didn't get a fast path match, check parent directories recursively
        List<String> parentDirs = getParentDirectories(normalizedPath);
        for (String parentDir : parentDirs) {
            // Don't check the path itself, just its parents
            if (!parentDir.equals(normalizedPath)) {
                // If the parent directory is ignored, then this path should also be ignored
                // We use true for isDirectory since we're checking parent directories
                if (isPathIgnored(parentDir, true)) {
                    return true;
                }
            }
        }
        
        // If no parent directory is ignored, check this path directly
        return isPathIgnored(normalizedPath, isDirectory);
    }
    
    /**
     * Internal method to check if a specific path should be ignored, without checking parent directories
     * 
     * @param normalizedPath The normalized path to check
     * @param isDirectory Whether the path represents a directory
     * @return true if the path should be ignored, false otherwise
     */
    private boolean isPathIgnored(@NotNull String normalizedPath, boolean isDirectory) {
        // First check against root .gitignore patterns
        
        // Check if the path matches any root include patterns (negations)
        for (Pattern pattern : rootIncludePatterns) {
            if (pattern.matcher(normalizedPath).matches()) {
                return false; // Explicitly included by root .gitignore
            }
        }
        
        // Check if the path matches any root exclude patterns
        for (Pattern pattern : rootExcludePatterns) {
            if (pattern.matcher(normalizedPath).matches()) {
                return true; // Explicitly excluded by root .gitignore
            }
        }
        
        // Then check nested .gitignore patterns, from the most specific to the least specific
        // Get all parent directories of this path
        List<String> parentDirs = getParentDirectories(normalizedPath);
        
        for (String parentDir : parentDirs) {
            List<Pattern> nestedIncludes = nestedIncludePatterns.get(parentDir);
            List<Pattern> nestedExcludes = nestedExcludePatterns.get(parentDir);
            
            if (nestedIncludes != null) {
                for (Pattern pattern : nestedIncludes) {
                    if (pattern.matcher(normalizedPath).matches()) {
                        return false; // Explicitly included by a nested .gitignore
                    }
                }
            }
            
            if (nestedExcludes != null) {
                for (Pattern pattern : nestedExcludes) {
                    if (pattern.matcher(normalizedPath).matches()) {
                        return true; // Explicitly excluded by a nested .gitignore
                    }
                }
            }
        }
        
        return false; // Not matched by any pattern, don't ignore
    }

    /**
     * Gets all parent directories of a path, from most specific to least specific
     *
     * @param path The path to get parent directories for
     * @return A list of parent directory paths
     */
    @NotNull
    private List<String> getParentDirectories(@NotNull String path) {
        List<String> parents = new ArrayList<>();
        
        // Split the path into parts
        String[] parts = path.split("/");
        
        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) { // -1 to exclude the file/directory name itself
            if (i > 0) {
                currentPath.append("/");
            }
            currentPath.append(parts[i]);
            parents.add(currentPath.toString());
        }
        
        // Sort from most specific (longest path) to least specific
        Collections.reverse(parents);
        return parents;
    }

    /**
     * Gets all patterns that are used to exclude files and directories at the root level
     *
     * @return A list of compiled regex patterns
     */
    @NotNull
    public List<Pattern> getRootExcludePatterns() {
        return Collections.unmodifiableList(rootExcludePatterns);
    }
    
    /**
     * Gets all nested exclusion patterns for a specific directory path
     * 
     * @param dirPath The relative directory path
     * @return A list of compiled regex patterns, or an empty list if none exist
     */
    @NotNull
    public List<Pattern> getNestedExcludePatterns(@NotNull String dirPath) {
        List<Pattern> patterns = nestedExcludePatterns.get(dirPath);
        return patterns != null ? Collections.unmodifiableList(patterns) : Collections.emptyList();
    }
    
    /**
     * Gets a map of all nested exclusion patterns
     *
     * @return A map of directory paths to their exclusion patterns
     */
    @NotNull
    public Map<String, List<Pattern>> getAllNestedExcludePatterns() {
        return Collections.unmodifiableMap(nestedExcludePatterns);
    }
}
