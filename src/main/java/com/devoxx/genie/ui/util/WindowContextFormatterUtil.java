package com.devoxx.genie.ui.util;

import org.jetbrains.annotations.NotNull;

public class WindowContextFormatterUtil {

    public static @NotNull String format(int tokens, String suffix) {
        if (tokens >= 1_000_000_000) {
            return String.format("%dB %s", (tokens / 1_000_000_000), suffix);
        } else if (tokens >= 1_000_000) {
            return String.format("%dM %s", (tokens / 1_000_000), suffix);
        } else if (tokens >= 1_000) {
            return String.format("%dK %s", (tokens / 1_000), suffix);
        } else {
            return String.format("%d %s", tokens, suffix);
        }
    }

    public static @NotNull String format(int tokens) {
        return format(tokens, "");
    }
}
