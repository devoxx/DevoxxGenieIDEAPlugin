package com.devoxx.genie.ui.panel.mcp;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for formatting JSON strings
 */
public class JsonFormatter {

    private static final int INDENT_WIDTH = 2;

    /**
     * Format a JSON string with proper indentation
     *
     * @param jsonString The JSON string to format
     * @return Formatted JSON string
     */
    public static String format(@NotNull String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean inQuotes = false;
        char prevChar = 0;

        for (char c : jsonString.toCharArray()) {
            if (c == '"' && prevChar != '\\') {
                inQuotes = !inQuotes;
                result.append(c);
            } else if (!inQuotes) {
                switch (c) {
                    case '{':
                    case '[':
                        result.append(c).append('\n').append(indent(++indentLevel));
                        break;
                    case '}':
                    case ']':
                        result.append('\n').append(indent(--indentLevel)).append(c);
                        break;
                    case ',':
                        result.append(c).append('\n').append(indent(indentLevel));
                        break;
                    case ':':
                        result.append(c).append(' ');
                        break;
                    default:
                        if (!Character.isWhitespace(c)) {
                            result.append(c);
                        }
                        break;
                }
            } else {
                result.append(c);
            }
            prevChar = c;
        }

        return result.toString();
    }

    private static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level * INDENT_WIDTH; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
