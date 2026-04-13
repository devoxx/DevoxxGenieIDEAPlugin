package com.devoxx.genie.service.analytics;

import com.devoxx.genie.model.enumarations.ModelProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider-type bucket emitted on {@code feature_used} events (task-209, AC #16).
 *
 * <p>{@link ModelProvider.Type#OPTIONAL} (Azure OpenAI, Bedrock) folds into {@link #CLOUD}:
 * both are cloud-hosted enterprise endpoints, they just require extra setup locally.
 * The wire schema is strictly {@code local | cloud | none} — no {@code optional} bucket.
 */
public enum ProviderType {

    LOCAL("local"),
    CLOUD("cloud"),
    NONE("none");

    private final String wireValue;

    ProviderType(@NotNull String wireValue) {
        this.wireValue = wireValue;
    }

    @NotNull
    public String wireValue() {
        return wireValue;
    }

    @NotNull
    public static ProviderType fromModelProvider(@Nullable ModelProvider provider) {
        if (provider == null) {
            return NONE;
        }
        return switch (provider.getType()) {
            case LOCAL -> LOCAL;
            case CLOUD, OPTIONAL -> CLOUD;
        };
    }
}
