package com.devoxx.genie.service.websearch;

import org.jetbrains.annotations.Nullable;

/**
 * The web search providers DevoxxGenie can use for {@code /search}, the chat Web toggle, the agent
 * {@code web_search} tool and the settings Test search. Each value has exactly one
 * {@link WebSearchProviderDefinition} in {@link WebSearchProviderRegistry}; the registry wiring test
 * enforces that.
 *
 * <p>The enum name is persisted in settings, so never rename a constant. The order is the order shown in
 * settings (ordinals are never persisted): deprecated providers go last.
 */
public enum WebSearchProvider {
    TAVILY,
    KEENABLE,
    PARALLEL,
    GOOGLE;

    /** Resolves a persisted name; unknown names (for example from a newer plugin version) resolve to null. */
    public static @Nullable WebSearchProvider fromName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (WebSearchProvider provider : values()) {
            if (provider.name().equals(name)) {
                return provider;
            }
        }
        return null;
    }
}
