package com.devoxx.genie.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
