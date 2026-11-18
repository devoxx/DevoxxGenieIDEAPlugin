package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.credentials.CredentialKey;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A snapshot of the web search settings: the selected provider, that provider's credentials and the
 * result limit. Built from saved state ({@link #fromState}) or from the unsaved settings form, so the
 * settings Test search exercises exactly what the user typed.
 *
 * @param provider    the selected provider, or null when none is selected
 * @param credentials credential values by key; only the selected provider's keys are expected
 * @param maxResults  maximum number of results to request
 */
public record WebSearchConfig(@Nullable WebSearchProvider provider,
                              @NotNull Map<CredentialKey, String> credentials,
                              int maxResults) {

    public static final int DEFAULT_MAX_RESULTS = 5;

    public WebSearchConfig {
        credentials = Map.copyOf(credentials);
    }

    /** The trimmed credential value, or "" when absent. */
    public @NotNull String credential(@NotNull CredentialKey key) {
        String value = credentials.get(key);
        return value == null ? "" : value.trim();
    }

    /**
     * Reads the selected provider and only that provider's credentials, so nothing is fetched from
     * PasswordSafe for providers that are not in use.
     */
    public static @NotNull WebSearchConfig fromState(@NotNull DevoxxGenieStateService state) {
        WebSearchProvider provider = state.getSelectedWebSearchProvider();
        Map<CredentialKey, String> credentials = new EnumMap<>(CredentialKey.class);
        if (provider != null) {
            for (WebSearchCredentialField field : WebSearchService.definition(provider).fields()) {
                credentials.put(field.key(), state.getCredential(field.key()));
            }
        }
        Integer max = state.getMaxSearchResults();
        return new WebSearchConfig(provider, credentials, max == null || max < 1 ? DEFAULT_MAX_RESULTS : max);
    }
}
