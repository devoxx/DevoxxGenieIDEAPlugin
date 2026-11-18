package com.devoxx.genie.service.websearch;

import com.devoxx.genie.service.analytics.AnalyticsEventBuilderAccess;
import com.devoxx.genie.service.credentials.CredentialKey;
import dev.langchain4j.web.search.WebSearchEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WebSearchProviderRegistryTest {

    @ParameterizedTest
    @EnumSource(WebSearchProvider.class)
    void everyDefinition_isFullyWired(WebSearchProvider provider) {
        WebSearchProviderDefinition definition = WebSearchProviderRegistry.definition(provider);

        assertThat(definition).as("registry definition for %s", provider).isNotNull();
        assertThat(definition.provider()).isEqualTo(provider);
        assertThat(definition.displayName()).isNotBlank();
        assertThat(definition.signupUrl()).startsWith("https://");
        assertThat(definition.hint()).isNotBlank();
        assertThat(definition.analyticsFeature()).isNotNull();
        assertThat(AnalyticsEventBuilderAccess.allowedFeatureIds())
                .as("analytics allowlist contains %s", definition.analyticsFeature().wireValue())
                .contains(definition.analyticsFeature().wireValue());
        assertThat(definition.engineFactory()).isNotNull();

        Map<CredentialKey, String> credentials = new HashMap<>();
        for (WebSearchCredentialField field : definition.fields()) {
            assertThat(field.label()).isNotBlank();
            assertThat(field.key()).isNotNull();
            credentials.put(field.key(), "dummy-" + field.key().getSubKey());
        }
        WebSearchEngine engine = definition.engineFactory().create(new WebSearchConfig(provider, credentials, 3));
        assertThat(engine).as("engine built by %s factory", provider).isNotNull();
    }

    @Test
    void credentialKeys_areNotSharedBetweenProviders() {
        Set<CredentialKey> seen = new HashSet<>();
        for (WebSearchProviderDefinition definition : WebSearchProviderRegistry.all()) {
            for (WebSearchCredentialField field : definition.fields()) {
                assertThat(seen.add(field.key()))
                        .as("%s is used by more than one provider", field.key())
                        .isTrue();
            }
        }
    }

    @Test
    void all_listsEveryProviderOnceInEnumOrder() {
        assertThat(WebSearchProviderRegistry.all())
                .extracting(WebSearchProviderDefinition::provider)
                .containsExactly(WebSearchProvider.values());
    }

    @Test
    void fromName_resolvesKnownNamesAndRejectsUnknownOnes() {
        assertThat(WebSearchProvider.fromName("TAVILY")).isEqualTo(WebSearchProvider.TAVILY);
        assertThat(WebSearchProvider.fromName("GOOGLE")).isEqualTo(WebSearchProvider.GOOGLE);
        assertThat(WebSearchProvider.fromName("KEENABLE_FROM_A_NEWER_PLUGIN")).isNull();
        assertThat(WebSearchProvider.fromName("")).isNull();
        assertThat(WebSearchProvider.fromName(null)).isNull();
    }
}
