package com.devoxx.genie.model.spec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a backlog document (non-task markdown file in the backlog/docs directory).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacklogDocument {
    private String id;
    private String title;
    private String content;
    private String filePath;
    private long lastModified;
}
