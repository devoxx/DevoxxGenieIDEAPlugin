package com.devoxx.genie.service.analyzer;

import com.devoxx.genie.service.analyzer.util.CachedProjectScanner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * A utility class to generate a project tree using CachedProjectScanner and append it to the DEVOXXGENIE.md file.
 */
public class ProjectTreeGenerator {
    private static final Logger LOG = Logger.getInstance(ProjectTreeGenerator.class);
    private static final String SECTION_HEADER = "\n\n### Project Tree\n\n";
    private static final String TREE_PREFIX = "```\n";
    private static final String TREE_SUFFIX = "\n```\n";

    private static final Set<String> IGNORED_DIRS = new HashSet<>(Arrays.asList(
            ".git", ".idea", ".gradle", "build", "out", "target", "node_modules"
    ));

    /**
     * Generates a project tree and appends it to the DEVOXXGENIE.md file.
     *
     * @param baseDir The base directory of the project
     * @param treeDepth The maximum depth of the tree
     */
    public static void appendProjectTreeToDevoxxGenie(@NotNull VirtualFile baseDir,
                                                      Integer treeDepth) {

        long startTime = System.currentTimeMillis();
        LOG.info("Starting project tree generation for: " + baseDir.getPath());
        
        try {
            CachedProjectScanner scanner = new CachedProjectScanner(baseDir);
            List<VirtualFile> files = scanner.scanDirectoryWithCache();
            
            long scanDuration = System.currentTimeMillis() - startTime;
            LOG.info(String.format("Found %d files in project structure (scan took %d ms)", files.size(), scanDuration));

            String treeContent = generateTreeContent(baseDir, files, treeDepth);
            
            LOG.info("Generated tree content with length: " + treeContent.length());
            
            // Append to DEVOXXGENIE.md
            appendToGenieMd(baseDir, treeContent);
        } catch (Exception e) {
            LOG.error("Error in appendProjectTreeToDevoxxGenie", e);
        }
    }

    /**
     * Generates a tree-like text representation of the project structure.
     *
     * @param baseDir The base directory of the project
     * @param files List of files found by the scanner
     * @return A formatted string representation of the project tree
     */
    private static @NotNull String generateTreeContent(@NotNull VirtualFile baseDir,
                                                       @NotNull List<VirtualFile> files,
                                                       Integer treeDepth) {
        long startTime = System.currentTimeMillis();

        // Create a tree structure from the files
        TreeNode root = new TreeNode(baseDir.getName(), true);
        String basePath = baseDir.getPath();
        
        LOG.info("Base path for tree generation: " + basePath);
        
        // Sort files for consistent output
        files.sort(Comparator.comparing(VirtualFile::getPath));
        
        for (VirtualFile file : files) {
            // Skip ignored directories
            boolean shouldSkip = false;
            String path = file.getPath();
            for (String ignoredDir : IGNORED_DIRS) {
                if (path.contains("/" + ignoredDir + "/") || path.endsWith("/" + ignoredDir)) {
                    shouldSkip = true;
                    break;
                }
            }
            if (shouldSkip) continue;
            
            // Get the relative path
            String relativePath = path.substring(basePath.length());
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            
            // Skip empty paths (this is the root directory)
            if (relativePath.isEmpty()) continue;
            
            // Add to tree
            addFileToTree(root, relativePath, file.isDirectory(), treeDepth);
        }
        
        // Generate the string representation
        StringBuilder sb = new StringBuilder();
        sb.append(TREE_PREFIX);
        sb.append(root.name).append("/\n");
        printTree(root, "", sb);
        sb.append(TREE_SUFFIX);
        
        LOG.info("Tree content generated, sample: " + (sb.length() > 100 ? sb.substring(0, 100) + "..." : sb.toString()));
        
        return sb.toString();
    }
    
    /**
     * Helper method to add a file to the tree structure.
     *
     * @param root The root tree node
     * @param path The relative path of the file
     * @param isDirectory Whether the file is a directory
     */
    private static void addFileToTree(TreeNode root, @NotNull String path, boolean isDirectory, Integer treeDepth) {
        // Skip paths that are too long to avoid processing issues
        if (path.length() > 500) {
            LOG.warn("Skipping excessively long path: " + path.substring(0, 100) + "...");
            return;
        }
        String[] parts = path.split("/");
        TreeNode current = root;
        
        int depth = 0;

        // Protect against paths with too many parts
        int maxParts = Math.min(parts.length, 50);
        for (int i = 0; i < maxParts; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            
            depth++;
            if (depth > treeDepth && i < parts.length - 1) {
                // Skip deeper levels but still count directories
                current.incrementChildCount();
                return;
            }
            
            boolean isLeafDirectory = isDirectory && i == parts.length - 1;
            boolean isFile = !isDirectory && i == parts.length - 1;
            
            // Try to find existing child
            TreeNode child = null;
            for (TreeNode node : current.children) {
                if (node.name.equals(part)) {
                    child = node;
                    break;
                }
            }
            
            if (child == null) {
                // Create new node if not found
                child = new TreeNode(part, isLeafDirectory || (!isFile && i < parts.length - 1));
                current.children.add(child);
            }
            
            current = child;
        }
    }
    
    /**
     * Helper method to print the tree recursively.
     *
     * @param node The current tree node
     * @param prefix The prefix for the current line
     * @param sb The StringBuilder to append to
     */
    private static void printTree(TreeNode node, String prefix, @NotNull StringBuilder sb) {
        // Protect against excessively large trees by limiting total nodes processed
        if (sb.length() > 100000) {
            sb.append(prefix).append("... (truncated due to size)\n");
            return;
        }
        List<TreeNode> children = new ArrayList<>(node.children);
        
        // Sort directories first, then files, both alphabetically
        children.sort((a, b) -> {
            if (a.isDirectory && !b.isDirectory) return -1;
            if (!a.isDirectory && b.isDirectory) return 1;
            return a.name.compareTo(b.name);
        });
        
        for (int i = 0; i < children.size(); i++) {
            TreeNode child = children.get(i);
            boolean isLast = i == children.size() - 1;
            
            sb.append(prefix)
              .append(isLast ? "└── " : "├── ")
              .append(child.name)
              .append(child.isDirectory ? "/" : "")
              .append("\n");
            
            if (child.isDirectory) {
                if (child.children.isEmpty() && child.childCount > 0) {
                    // Show a placeholder for directories with children beyond MAX_DEPTH
                    sb.append(prefix)
                      .append(isLast ? "    " : "│   ")
                      .append("└── ")
                      .append("... (")
                      .append(child.childCount)
                      .append(" more items)")
                      .append("\n");
                } else {
                    printTree(child, prefix + (isLast ? "    " : "│   "), sb);
                }
            }
        }
    }
    
    /**
     * Appends the project tree to the DEVOXXGENIE.md file.
     *
     * @param baseDir The base directory of the project
     * @param treeContent The generated tree content
     */
    private static void appendToGenieMd(VirtualFile baseDir, String treeContent) {
        try {
            VirtualFile devoxxGenieMdFile = baseDir.findChild("DEVOXXGENIE.md");
            
            if (devoxxGenieMdFile == null) {
                LOG.warn("DEVOXXGENIE.md file not found, cannot append project tree");
                return;
            }
            
            // Read current content
            String currentContent = VfsUtil.loadText(devoxxGenieMdFile);
            LOG.info("Read current DEVOXXGENIE.md content, length: " + currentContent.length());
            
            // Check if it already has a project tree section
            if (currentContent.contains(SECTION_HEADER)) {
                // Remove existing project tree section
                int sectionStart = currentContent.indexOf(SECTION_HEADER);
                int sectionEnd = currentContent.indexOf(TREE_SUFFIX, sectionStart);
                
                LOG.info("Found existing project tree section: start=" + sectionStart + ", end=" + sectionEnd);
                
                if (sectionEnd > sectionStart) {
                    // Replace the existing section
                    String beforeSection = currentContent.substring(0, sectionStart);
                    String afterSection = sectionEnd + TREE_SUFFIX.length() < currentContent.length() ? 
                                          currentContent.substring(sectionEnd + TREE_SUFFIX.length()) : "";
                    
                    currentContent = beforeSection + SECTION_HEADER + treeContent + afterSection;
                    LOG.info("Replaced existing project tree section");
                } else {
                    // If section is malformed, just append
                    currentContent += SECTION_HEADER + treeContent;
                    LOG.info("Existing project tree section appears malformed, appending new section");
                }
            } else {
                // Append new section
                currentContent += SECTION_HEADER + treeContent;
                LOG.info("Appending new project tree section");
            }
            
            // Update the file
            final String finalContent = currentContent;
            
            // Run in a write action on the EDT
            LOG.info("Attempting to save updated content to DEVOXXGENIE.md");

            // Using invokeAndWait to ensure we switch to the EDT thread
            try {
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        try {
                            VfsUtil.saveText(devoxxGenieMdFile, finalContent);
                            LOG.info("Project tree appended to DEVOXXGENIE.md successfully");
                        } catch (IOException e) {
                            LOG.error("Error updating DEVOXXGENIE.md with project tree", e);
                        }
                    });
                });
            } catch (Exception e) {
                LOG.error("Error while waiting for EDT to save DEVOXXGENIE.md", e);
            }
        } catch (Exception e) {
            LOG.error("Error appending project tree to DEVOXXGENIE.md", e);
        }
    }
    
    /**
     * A simple tree node class for building the directory structure.
     */
    private static class TreeNode {
        String name;
        boolean isDirectory;
        List<TreeNode> children;
        int childCount;
        
        TreeNode(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.children = new ArrayList<>();
            this.childCount = 0;
        }
        
        void incrementChildCount() {
            childCount++;
        }
    }
}
