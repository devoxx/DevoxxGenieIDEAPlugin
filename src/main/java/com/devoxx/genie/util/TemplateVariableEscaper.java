package com.devoxx.genie.util;

/**
 * Utility class to escape LangChain4j template variables.
 * LangChain4j uses the syntax {{variable_name}} for template variables,
 * which can conflict with other template systems like Angular.
 */
public class TemplateVariableEscaper {

    private TemplateVariableEscaper() {
        // Utility class, no instantiation
    }

    /**
     * Escapes LangChain4j template variables by replacing {{ with \\{{ and }} with \\}}
     * This prevents LangChain4j from attempting to substitute variables that aren't meant for it.
     *
     * @param text The text containing template variables to escape
     * @return The text with escaped template variables
     */
    public static String escape(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("{{", "\\{\\{").replace("}}", "\\}\\}");
    }
}
