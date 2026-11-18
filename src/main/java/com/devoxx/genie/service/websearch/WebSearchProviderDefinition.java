package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.FeatureId;
import dev.langchain4j.web.search.WebSearchEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * @param deprecation      set when the provider's API is being shut down, else null
 */
public record WebSearchProviderDefinition(@NotNull WebSearchProvider provider,
                                          @NotNull String displayName,
                                          @NotNull String signupUrl,
                                          @NotNull String hint,
                                          @NotNull List<WebSearchCredentialField> fields,
                                          @NotNull FeatureId analyticsFeature,
                                          @NotNull EngineFactory engineFactory,
                                          @Nullable WebSearchDeprecation deprecation) {

    public WebSearchProviderDefinition {
        fields = List.copyOf(fields);
    }

    /** A provider that is not deprecated. */
    public WebSearchProviderDefinition(@NotNull WebSearchProvider provider, @NotNull String displayName,
                                       @NotNull String signupUrl, @NotNull String hint,
                                       @NotNull List<WebSearchCredentialField> fields,
                                       @NotNull FeatureId analyticsFeature, @NotNull EngineFactory engineFactory) {
        this(provider, displayName, signupUrl, hint, fields, analyticsFeature, engineFactory, null);
    }

    /** The same definition with another engine factory (tests point engines at stub servers). */
    public @NotNull WebSearchProviderDefinition withEngineFactory(@NotNull EngineFactory factory) {
        return new WebSearchProviderDefinition(provider, displayName, signupUrl, hint, fields, analyticsFeature,
                factory, deprecation);
    }

    @FunctionalInterface
    public interface EngineFactory {
        @NotNull WebSearchEngine create(@NotNull WebSearchConfig config);
    }
}
