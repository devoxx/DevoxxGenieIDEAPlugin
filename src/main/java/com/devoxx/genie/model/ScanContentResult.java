package com.devoxx.genie.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ScanContentResult {
    private String content;
    private int tokenCount;
    private int fileCount;
    private int skippedFileCount;
    private int skippedDirectoryCount;
    @Getter
    private List<Path> files = new ArrayList<>();
    @Getter
    private Map<String, String> skippedFiles = new HashMap<>();

    public void incrementFileCount() {
        fileCount++;
    }

    public void incrementSkippedFileCount() {
        skippedFileCount++;
    }

    public void incrementSkippedDirectoryCount() {
        skippedDirectoryCount++;
    }

    public void addTokenCount(int tokenCount) {
        this.tokenCount += tokenCount;
    }

    public void addFile(Path file) {
        files.add(file);
    }
    
    /**
     * Add a skipped file with the reason why it was skipped
     * 
     * @param path The path of the skipped file
     * @param reason The reason why the file was skipped
     */
    public void addSkippedFile(String path, String reason) {
        skippedFiles.put(path, reason);
    }
}
