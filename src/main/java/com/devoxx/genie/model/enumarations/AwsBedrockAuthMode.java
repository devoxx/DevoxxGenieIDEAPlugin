package com.devoxx.genie.model.enumarations;

import org.jetbrains.annotations.NotNull;

public enum AwsBedrockAuthMode {
    ACCESS_KEY("Access Key / Secret Key"),
    PROFILE("AWS Profile"),
    BEARER_TOKEN("Bearer Token");

    private final String displayName;

    AwsBedrockAuthMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static @NotNull AwsBedrockAuthMode defaultMode() {
        return ACCESS_KEY;
    }
}
