package com.devoxx.genie.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

<<<<<<< HEAD
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

=======
>>>>>>> master
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
<<<<<<< HEAD
    @Getter
    private List<Path> files = new ArrayList<>();  // Add this field
=======
>>>>>>> master

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
<<<<<<< HEAD

    public void addFile(Path file) {
        files.add(file);
    }
=======
>>>>>>> master
}
