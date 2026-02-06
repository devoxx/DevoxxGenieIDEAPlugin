package com.devoxx.genie.util;

/**
 * Utility class to escape LangChain4j template variables.
 * LangChain4j uses Mustache-style syntax: {{variable}} for escaped variables
 * and {{{variable}}} for unescaped variables. Both must be escaped to prevent
 * LangChain4j from interpreting user content as template expressions.
 */
public class TemplateVariableEscaper {

    private TemplateVariableEscaper() {
        // Utility class, no instantiation
    }

    /**
     * Escapes LangChain4j template variables in the given text.
     * Handles both triple braces ({{{...}}}) and double braces ({{...}}).
     * Triple braces are escaped first to avoid partial matches.
     *
     * @param text The text containing template variables to escape
     * @return The text with escaped template variables
     */
    public static String escape(String text) {
        if (text == null) {
            return null;
        }
        // Escape triple braces FIRST (Mustache unescaped variable syntax),
        // then double braces (Mustache escaped variable syntax).
        // Order matters: replacing {{ first would break {{{ handling.
        return text
                .replace("{{{", "\\{\\{\\{")
                .replace("}}}", "\\}\\}\\}")
                .replace("{{", "\\{\\{")
                .replace("}}", "\\}\\}");
    }
}
