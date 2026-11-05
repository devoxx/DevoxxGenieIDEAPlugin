package com.devoxx.genie.chatmodel.catalog;

/**
 * Per-provider-kind fallbacks used by {@link ModelCatalogParser} when a catalog entry does not say.
 *
 * @param contextWindow     context window when no catalog path yields a positive number
 * @param apiKeyUsed        whether models of this provider kind need an API key
 * @param shortDisplayNames when no display name is mapped, drop the {@code org/} prefix of the model id
 */
public record CatalogDefaults(int contextWindow, boolean apiKeyUsed, boolean shortDisplayNames) {
}
