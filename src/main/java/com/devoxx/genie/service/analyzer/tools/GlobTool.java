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
                    i = handleBackslash(glob, i, regex);
                    break;
                case '*':
                    i = handleAsterisk(glob, i, inClass, regex);
                    break;
                case '?':
                    handleQuestion(inClass, regex);
                    break;
                case '[':
                    inClass++;
                    i = handleOpenBracket(glob, i, regex);
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
                    handleComma(inGroup, regex);
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

    private static int handleBackslash(@NotNull String glob, int i, @NotNull StringBuilder regex) {
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
        return i;
    }

    private static int handleAsterisk(@NotNull String glob, int i, int inClass, @NotNull StringBuilder regex) {
        if (inClass == 0) {
            if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                regex.append(".*");
                i++;
            } else {
                regex.append("[^/]*");
            }
        } else {
            regex.append(".*");
        }
        return i;
    }

    private static void handleQuestion(int inClass, @NotNull StringBuilder regex) {
        if (inClass == 0) {
            regex.append("[^/]");
        } else {
            regex.append(".");
        }
    }

    private static int handleOpenBracket(@NotNull String glob, int i, @NotNull StringBuilder regex) {
        regex.append('[');
        if (i + 1 < glob.length() && glob.charAt(i + 1) == '!') {
            regex.append('^');
            i++;
        } else if (i + 1 < glob.length() && glob.charAt(i + 1) == '^') {
            regex.append('\\');
        }
        return i;
    }

    private static void handleComma(int inGroup, @NotNull StringBuilder regex) {
        if (inGroup > 0) {
            regex.append('|');
        } else {
            regex.append(',');
        }
    }
}
