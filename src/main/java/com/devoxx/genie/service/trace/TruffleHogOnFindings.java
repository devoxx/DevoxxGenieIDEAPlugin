package com.devoxx.genie.service.trace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** What to do with a recorded turn when TruffleHog finds a secret in it. */
public enum TruffleHogOnFindings {
    /** Replace each finding with {@code [REDACTED_TRUFFLEHOG_<DETECTOR>]}. */
    REDACT,
    /** Drop the whole turn's trace. */
    SKIP;

    public @NotNull String settingValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @NotNull TruffleHogOnFindings fromSetting(@Nullable String value) {
        if (value != null) {
            for (TruffleHogOnFindings v : values()) {
                if (v.settingValue().equalsIgnoreCase(value.trim())) {
                    return v;
                }
            }
        }
        return REDACT;
    }
}
