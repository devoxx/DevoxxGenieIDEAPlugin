package com.devoxx.genie.service.trace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** When the TruffleHog secret scan runs on recorded agent traces. */
public enum TruffleHogMode {
    /** Scan when the trufflehog binary is available; otherwise rely on regex redaction only. */
    AUTO,
    /** Only keep a trace when a scan succeeded; a missing binary or failed scan drops the trace. */
    REQUIRED,
    /** Never scan. */
    OFF;

    public @NotNull String settingValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static @NotNull TruffleHogMode fromSetting(@Nullable String value) {
        if (value != null) {
            for (TruffleHogMode mode : values()) {
                if (mode.settingValue().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
        }
        return AUTO;
    }
}
