package com.devoxx.genie.service.generator.file;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Handles file operations for the Devoxx Genie Generator.
 */
@Slf4j
public class FileManager {
    
    /**
     * Writes content to a file. Creates the file if it doesn't exist.
     * 
     * @param baseDir The base directory
     * @param fileName The file name
     * @param content The content to write
     */
    public void writeFile(VirtualFile baseDir, String fileName, String content) {
        try {
            // Get file reference
            VirtualFile file = baseDir.findChild(fileName);
            
            // Use invokeLater to avoid blocking the EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                // Run the write action on EDT
                ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        if (file == null) {
                            // Create new file
                            VirtualFile newFile = baseDir.createChildData(this, fileName);
                            VfsUtil.saveText(newFile, content);
                            log.info("Created new file: {}", fileName);
                        } else {
                            // Update existing file
                            VfsUtil.saveText(file, content);
                            log.info("Updated existing file: {}", fileName);
                        }
                    } catch (IOException e) {
                        log.error("Error writing to file: {}", fileName, e);
                        throw new RuntimeException("Failed to write file: " + fileName, e);
                    }
                });
            });
        } catch (Exception e) {
            log.error("Error in write operation for file: {}", fileName, e);
            throw new RuntimeException("Failed to process file operation: " + fileName, e);
        }
    }
    
    /**
     * Reads content from a file.
     * 
     * @param file The file to read
     * @return The file content or empty string if read fails
     */
    public String readFileContent(VirtualFile file) {
        return com.intellij.openapi.application.ReadAction.compute(() -> {
            try {
                return VfsUtilCore.loadText(file);
            } catch (IOException e) {
                log.error("Error reading file: {}", file.getName(), e);
                return "";
            }
        });
    }
    
    /**
     * Saves content to a file using EDT handling.
     * 
     * @param file The file to save to
     * @param content The content to save
     */
    public void saveContent(VirtualFile file, String content) {
        // Schedule the write action to run on the EDT without blocking
        ApplicationManager.getApplication().invokeLater(() -> {
            // Run the write action
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    VfsUtil.saveText(file, content);
                    log.info("Content saved to file: {}", file.getName());
                } catch (IOException e) {
                    log.error("Error saving to file: {}", file.getName(), e);
                }
            });
        });
    }
}
