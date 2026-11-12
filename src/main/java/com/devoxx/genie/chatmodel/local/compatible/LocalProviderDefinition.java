package com.devoxx.genie.chatmodel.local.compatible;

import com.devoxx.genie.chatmodel.catalog.CatalogFields;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.jetbrains.annotations.Nullable;

/**
 * One local OpenAI-compatible provider served by the generic local provider.
 *
 * @param catalogFields         how to read the provider's {@code /models} entries, or null for the defaults
 * @param defaultContextLength  context window assumed when the provider does not report one
 * @param fallbackContextHint   settings hint for the optional fallback context field, or null for no such field
 */
public record LocalProviderDefinition(ModelProvider provider,
                                      String defaultUrl,
                                      String downloadUrl,
                                      String hint,
                                      @Nullable CatalogFields catalogFields,
                                      int defaultContextLength,
                                      @Nullable String fallbackContextHint) {
}
