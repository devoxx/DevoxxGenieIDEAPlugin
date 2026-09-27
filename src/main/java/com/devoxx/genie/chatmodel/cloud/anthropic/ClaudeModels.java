package com.devoxx.genie.chatmodel.cloud.anthropic;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ClaudeModels {

    private static final List<String> MODELS_WITHOUT_TEMPERATURE = List.of(
            "claude-opus-4-7",
            "claude-opus-4-8",
            "claude-opus-5",
            "claude-sonnet-5",
            "claude-fable",
            "claude-mythos"
    );

    private ClaudeModels() {
    }

    public static boolean rejectsTemperature(@NotNull String modelName) {
        return MODELS_WITHOUT_TEMPERATURE.stream().anyMatch(modelName::contains);
    }
}
