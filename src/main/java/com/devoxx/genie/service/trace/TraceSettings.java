package com.devoxx.genie.service.trace;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Immutable snapshot of the agent trace recording settings, taken once per turn so a settings
 * change mid-turn can't mix two configurations, and so the trace classes can be unit-tested
 * without the IntelliJ application.
 */
public record TraceSettings(
        boolean enabled,
        int maxToolResultChars,
        boolean redactHomeDirectory,
        @NotNull List<String> customPatterns,
        @NotNull TruffleHogMode truffleHogMode,
        @NotNull TruffleHogOnFindings truffleHogOnFindings,
        @NotNull String truffleHogPath,
        boolean truffleHogVerify) {

    public static final String DEFAULT_TRUFFLEHOG_BINARY = "trufflehog";

    public TraceSettings {
        customPatterns = List.copyOf(customPatterns);
        maxToolResultChars = Math.max(0, maxToolResultChars);
    }

    public static @NotNull TraceSettings fromState(@NotNull DevoxxGenieStateService state) {
        Integer max = state.getAgentTraceMaxToolResultChars();
        List<String> patterns = state.getAgentTraceCustomRedactionPatterns();
        String path = state.getAgentTraceTruffleHogPath();
        return new TraceSettings(
                Boolean.TRUE.equals(state.getAgentTraceRecordingEnabled()),
                max == null ? 50_000 : max,
                !Boolean.FALSE.equals(state.getAgentTraceRedactHomeDirectory()),
                patterns == null ? List.of() : patterns.stream().filter(p -> p != null && !p.isBlank()).toList(),
                TruffleHogMode.fromSetting(state.getAgentTraceTruffleHogMode()),
                TruffleHogOnFindings.fromSetting(state.getAgentTraceTruffleHogOnFindings()),
                path == null ? "" : path.trim(),
                Boolean.TRUE.equals(state.getAgentTraceTruffleHogVerify()));
    }

    /** Settings snapshot from the application state; recording is off when the state isn't available. */
    public static @NotNull TraceSettings current() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return state == null ? disabled() : fromState(state);
    }

    public static @NotNull TraceSettings disabled() {
        return new TraceSettings(false, 50_000, true, List.of(), TruffleHogMode.AUTO,
                TruffleHogOnFindings.REDACT, "", false);
    }

    public @NotNull String truffleHogBinary() {
        return truffleHogPath.isBlank() ? DEFAULT_TRUFFLEHOG_BINARY : truffleHogPath;
    }
}
