package com.devoxx.genie.ui.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for operations related to code language mappings and syntax highlighting.
 */
public class CodeLanguageUtil {

    /**
     * Maps language identifier to PrismJS language class.
     *
     * @param languageInfo Language info from markdown code block
     * @return PrismJS language class
     */
    public static @NotNull String mapLanguageToPrism(@Nullable String languageInfo) {
        if (languageInfo == null || languageInfo.isEmpty()) {
            return "plaintext";
        }

        String lang = languageInfo.trim().toLowerCase();

        // Map common language identifiers to PrismJS language classes
        return switch (lang) {
            case "js", "javascript" -> "javascript";
            case "ts", "typescript" -> "typescript";
            case "py", "python" -> "python";
            case "java" -> "java";
            case "c#", "csharp", "cs" -> "csharp";
            case "c++" -> "cpp";
            case "go" -> "go";
            case "rust" -> "rust";
            case "rb", "ruby" -> "ruby";
            case "kt", "kotlin" -> "kotlin";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "html" -> "markup";
            case "css" -> "css";
            case "sh", "bash" -> "bash";
            case "md", "markdown" -> "markdown";
            case "sql" -> "sql";
            case "docker", "dockerfile" -> "docker";
            case "dart" -> "dart";
            case "graphql" -> "graphql";
            case "hcl" -> "hcl";
            case "nginx" -> "nginx";
            case "powershell", "ps" -> "powershell";
            // Add more language mappings as needed
            default -> "plaintext";
        };
    }
}
