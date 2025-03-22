package com.devoxx.genie.service.projectscanner;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ContentExtractor {

    /**
     * Extracts the content of a file and formats it for inclusion in the project scan.
     */
    public String extractFileContent(@NotNull VirtualFile file) {
        StringBuilder content = new StringBuilder();
        String header = "\n--- " + file.getPath() + " ---\n";
        content.append(header);

        // Simplified approach - skip the ReadAction complexity for now
        try {
            extractFileContentInternal(file, content);
        } catch (Exception e) {
            // Just add the error message to the content
            content.append("Error reading file content: ").append(e.getMessage());
        }

        return content.toString();
    }

    /**
     * Internal implementation to extract file content - separated for easier testing
     */
    protected void extractFileContentInternal(VirtualFile file, StringBuilder content) throws IOException {
        String fileContent;

        try (InputStream is = file.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            fileContent = sb.toString();
        }

        // Process and append the file content
        content.append(processFileContent(fileContent));
    }

    public String combineContent(String directoryStructure, String fileContents) {
        return "Directory Structure:\n" +
                directoryStructure +
                "\n\nFile Contents:\n" +
                fileContents;
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
