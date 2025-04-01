package com.devoxx.genie.service.generator.tree;

import com.devoxx.genie.service.generator.file.FileManager;
import com.devoxx.genie.service.projectscanner.FileScanner;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Handles generation of project tree structure.
 */
@Slf4j
public class ProjectTreeGenerator {
    
    private final FileScanner fileScanner;
    private final FileManager fileManager;
    
    /**
     * Initializes the ProjectTreeGenerator with its dependencies.
     * 
     * @param fileScanner The FileScanner to use for tree generation
     */
    public ProjectTreeGenerator(FileScanner fileScanner) {
        this.fileScanner = fileScanner;
        this.fileManager = new FileManager();
    }
    
    /**
     * Appends a project tree to the specified file.
     * 
     * @param baseDir The base directory
     * @param fileName The file to append the tree to
     */
    public void appendProjectTree(VirtualFile baseDir, String fileName) {
        try {
            log.info("Generating project tree for file: {}", fileName);
            
            // Generate tree content
            String treeContent = generateTreeContent(baseDir);
            String formattedTree = formatTreeContent(treeContent);
            
            // Get file reference
            VirtualFile file = baseDir.findChild(fileName);
            if (file == null) {
                log.warn("Target file not found: {}", fileName);
                return;
            }
            
            // Read, update, and save content
            String currentContent = fileManager.readFileContent(file);
            String updatedContent = updateContentWithTree(currentContent, formattedTree);
            fileManager.saveContent(file, updatedContent);
            
            log.info("Project tree appended to file: {}", fileName);
        } catch (Exception e) {
            log.error("Error appending project tree to file: {}", fileName, e);
        }
    }
    
    /**
     * Generates tree content using the FileScanner.
     */
    private String generateTreeContent(VirtualFile baseDir) {
        return com.intellij.openapi.application.ReadAction.compute(() -> 
            fileScanner.generateSourceTreeRecursive(baseDir, 0)
        );
    }
    
    /**
     * Formats raw tree content as markdown.
     */
    private String formatTreeContent(String treeContent) {
        return "\n\n### Project Tree\n\n```\n" + treeContent + "\n```\n";
    }
    
    /**
     * Updates content by either replacing or appending the tree section.
     */
    private String updateContentWithTree(@NotNull String currentContent, String formattedTree) {
        // If there's no existing content, just return the tree
        if (currentContent.isEmpty()) {
            return formattedTree;
        }
        
        // Check if content already has a project tree section
        boolean hasTreeSection = currentContent.contains("### Project Tree");
        return hasTreeSection 
            ? replaceExistingTreeSection(currentContent, formattedTree) 
            : appendTreeSection(currentContent, formattedTree);
    }
    
    /**
     * Appends tree section to existing content.
     */
    private String appendTreeSection(String content, String treeSection) {
        return content + treeSection;
    }
    
    /**
     * Replaces an existing tree section in the content.
     */
    private @NotNull String replaceExistingTreeSection(@NotNull String content, String newTree) {
        int sectionStart = content.indexOf("### Project Tree");
        int codeStart = content.indexOf("```\n", sectionStart);
        
        // If we can't find the code block, just append
        if (codeStart == -1) {
            return appendTreeSection(content, newTree);
        }
        
        int codeEnd = content.indexOf("\n```", codeStart + 4);
        
        // If we can't find the end of code block, just append
        if (codeEnd <= sectionStart) {
            return appendTreeSection(content, newTree);
        }
        
        // Cut out the old section and insert the new one
        String beforeSection = content.substring(0, sectionStart - 2); // Account for newlines
        String afterSection = (codeEnd + 4 < content.length()) ? 
                              content.substring(codeEnd + 4) : "";
        
        return beforeSection + newTree + afterSection;
    }
}
