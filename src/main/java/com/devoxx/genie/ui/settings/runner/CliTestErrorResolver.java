package com.devoxx.genie.ui.settings.runner;

import com.intellij.openapi.diagnostic.Logger;
import org.jspecify.annotations.NonNull;

import java.util.regex.Pattern;

/**
 * Resolves and formats error messages from CLI tool test connections.
 * Strips ANSI escapes, control characters, detects common failure patterns
 * (auth, permissions), and provides actionable guidance.
 */
public final class CliTestErrorResolver {

    private static final Logger LOG = Logger.getInstance(CliTestErrorResolver.class);

    /** Matches ANSI escape sequences (CSI and OSC). */
    static final Pattern ANSI_ESCAPE =
            Pattern.compile("\\x1B(?:\\[[0-?]*[ -/]*[@-~]|\\].*?(?:\\x07|\\x1B\\\\))");

    /** Matches remaining control characters (except newline/tab) after ANSI stripping. */
    static final Pattern CONTROL_CHARS =
            Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    private CliTestErrorResolver() {}

    /**
     * Build a user-friendly error message from CLI process output.
     * Sanitizes output, detects common failure patterns, and logs the full error.
     */
    public static @NonNull String resolve(int exitCode, String stdout, @NonNull String stderr) {
        String fullOutput = sanitizeFullOutput(stderr);
        if (fullOutput.isEmpty()) fullOutput = sanitizeFullOutput(stdout);
        if (fullOutput.isEmpty()) fullOutput = "Exit code " + exitCode;

        LOG.warn("CLI test failed (exit " + exitCode + "): " + fullOutput);

        String lowerOutput = fullOutput.toLowerCase();
        if (isAuthError(lowerOutput)) {
            return "Authentication failed. Check CLI login status and env vars. Details: " + fullOutput;
        }
        if (isPermissionError(lowerOutput)) {
            return "Permission denied. Check file permissions on the executable. Details: " + fullOutput;
        }

        return fullOutput;
    }

    /**
     * Sanitize the full output: strip ANSI escapes, control characters,
     * and join all meaningful lines separated by " | ".
     */
    static @NonNull String sanitizeFullOutput(String output) {
        if (output == null || output.isBlank()) return "";

        String[] lines = output.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String cleaned = sanitizeOutputLine(line);
            if (!cleaned.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append(cleaned);
            }
        }
        return sb.toString();
    }

    static @NonNull String sanitizeOutputLine(String line) {
        if (line == null || line.isBlank()) return "";
        String cleaned = ANSI_ESCAPE.matcher(line).replaceAll("");
        cleaned = CONTROL_CHARS.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    private static boolean isAuthError(String lowerOutput) {
        return lowerOutput.contains("auth") || lowerOutput.contains("token")
                || lowerOutput.contains("login") || lowerOutput.contains("credential")
                || lowerOutput.contains("unauthorized") || lowerOutput.contains("403")
                || lowerOutput.contains("401");
    }

    private static boolean isPermissionError(String lowerOutput) {
        return lowerOutput.contains("permission denied") || lowerOutput.contains("eacces");
    }
}
