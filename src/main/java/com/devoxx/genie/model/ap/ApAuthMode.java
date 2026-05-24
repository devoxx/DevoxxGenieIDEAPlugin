package com.devoxx.genie.model.ap;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Authentication mode for the Docker Agentic Platform CLI.
 * Maps to the {@code AP_AGENTIC_PLATFORM_AUTH_MODE} environment variable
 * understood by the {@code ap} binary.
 */
@Getter
public enum ApAuthMode {

    /**
     * Use whatever credentials the {@code ap} binary finds on its own — the OAuth tokens
     * cached by a prior TUI login (under {@code ~/.ap}) or Docker Desktop, whichever it
     * picks up first. This means <b>no</b> {@code AP_AGENTIC_PLATFORM_*} env vars are set
     * by the plugin, so the binary's natural detection runs unimpeded.
     */
    CACHED_LOGIN(null),

    /** Force the binary to use Docker Desktop's keychain by setting {@code AUTH_MODE=docker-desktop}. */
    DOCKER_DESKTOP("docker-desktop"),

    /** Access and refresh tokens are provided explicitly via env vars. */
    MANUAL_TOKENS("manual");

    /** Value to write into {@code AP_AGENTIC_PLATFORM_AUTH_MODE}, or {@code null} to leave it unset. */
    private final String envValue;

    ApAuthMode(@Nullable String envValue) {
        this.envValue = envValue;
    }

    public static @NotNull ApAuthMode fromName(@Nullable String name) {
        if (name == null) return CACHED_LOGIN;
        try {
            return ApAuthMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return CACHED_LOGIN;
        }
    }
}
