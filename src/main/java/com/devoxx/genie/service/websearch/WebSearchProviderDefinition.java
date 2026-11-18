package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.FeatureId;
import dev.langchain4j.web.search.WebSearchEngine;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Everything DevoxxGenie needs to know about one web search provider.
 *
 * @param provider         the provider this definition describes
 * @param displayName      name shown in settings and messages
 * @param signupUrl        where to sign up / get credentials
 * @param hint             settings help text (HTML allowed)
 * @param fields           the credentials the provider needs, in display order
 * @param analyticsFeature feature id emitted when this provider is enabled/used (must be in the analytics allowlist)
 * @param engineFactory    builds the langchain4j engine from a config whose credentials are known to be present
 */
public record WebSearchProviderDefinition(@NotNull WebSearchProvider provider,
                                          @NotNull String displayName,
                                          @NotNull String signupUrl,
                                          @NotNull String hint,
                                          @NotNull List<WebSearchCredentialField> fields,
                                          @NotNull FeatureId analyticsFeature,
                                          @NotNull EngineFactory engineFactory) {

    public WebSearchProviderDefinition {
        fields = List.copyOf(fields);
    }

    @FunctionalInterface
    public interface EngineFactory {
        @NotNull WebSearchEngine create(@NotNull WebSearchConfig config);
    }
}
