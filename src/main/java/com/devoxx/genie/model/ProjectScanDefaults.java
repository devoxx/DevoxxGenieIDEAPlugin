package com.devoxx.genie.model;

import java.util.List;

/**
 * Factory defaults for which files the project scanner (and therefore RAG indexing) picks up.
 *
 * <p>Kept in a plain class with no IntelliJ dependencies so tools that run outside the IDE,
 * such as the RAG retrieval benchmark, select files with the same defaults as a fresh plugin
 * install. {@code DevoxxGenieStateService} seeds its user-editable settings from these lists.
 */
public final class ProjectScanDefaults {

    /** Directory names skipped wherever they appear in a path. */
    public static final List<String> EXCLUDED_DIRECTORIES = List.of(
            "build", ".git", "bin", "out", "target", "node_modules", ".idea"
    );

    /** File extensions (without the dot) that are scanned. */
    public static final List<String> INCLUDED_FILE_EXTENSIONS = List.of(
            "java", "kt", "groovy", "scala", "xml", "json", "yaml", "yml", "properties", "txt", "md",
            "js", "ts", "css", "scss", "html"
    );

    private ProjectScanDefaults() {}
}
