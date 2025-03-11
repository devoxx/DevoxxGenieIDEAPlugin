package com.devoxx.genie.service.analyzer.tools;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for working with glob patterns
 */
public class GlobTool {
    /**
     * Converts a glob pattern to a regex pattern
     * @param glob The glob pattern to convert
     * @return A regex pattern that matches the same strings as the provided glob pattern
     */
    public static @NotNull String convertGlobToRegex(@NotNull String glob) {
        StringBuilder regex = new StringBuilder();
        int inGroup = 0;
        int inClass = 0;
        
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '\\':
                    if (++i >= glob.length()) {
                        regex.append('\\');
                    } else {
                        char next = glob.charAt(i);
                        if (next == ',') {
                            regex.append("\\,");
                        } else {
                            regex.append('\\');
                            regex.append(next);
                        }
                    }
                    break;
                case '*':
                    if (inClass == 0) {
                        if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                            // ** means any number of directories (including none)
                            regex.append(".*");
                            i++; // Skip the second *
                        } else {
                            regex.append("[^/]*");
                        }
                    } else {
                        regex.append(".*");
                    }
                    break;
                case '?':
                    if (inClass == 0) {
                        regex.append("[^/]");
                    } else {
                        regex.append(".");
                    }
                    break;
                case '[':
                    inClass++;
                    regex.append('[');
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '!') {
                        regex.append('^');
                        i++;
                    } else if (i + 1 < glob.length() && glob.charAt(i + 1) == '^') {
                        regex.append('\\');
                    }
                    break;
                case ']':
                    inClass--;
                    regex.append(']');
                    break;
                case '{':
                    inGroup++;
                    regex.append('(');
                    break;
                case '}':
                    inGroup--;
                    regex.append(')');
                    break;
                case ',':
                    if (inGroup > 0) {
                        regex.append('|');
                    } else {
                        regex.append(',');
                    }
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    regex.append('\\');
                    regex.append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        
        return regex.toString();
    }
}
