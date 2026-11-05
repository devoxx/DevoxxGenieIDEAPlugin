package com.devoxx.genie.ui.settings;

import com.devoxx.genie.chatmodel.cloud.router.RouterRegistry;
import com.devoxx.genie.model.enumarations.ModelProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderEnabledResolverTest {

    /** Every boolean on the state answers true, so a provider only resolves to false when nothing handles it. */
    private static DevoxxGenieStateService allEnabledState() {
        return Mockito.mock(DevoxxGenieStateService.class,
                invocation -> invocation.getMethod().getReturnType() == boolean.class ? Boolean.TRUE : null);
    }

    @Test
    void everyProvider_hasAnExplicitEnabledMapping() {
        DevoxxGenieStateService state = allEnabledState();

        for (ModelProvider provider : ModelProvider.values()) {
            assertThat(ProviderEnabledResolver.isEnabled(state, provider, true))
                    .as("provider %s must be handled by ProviderEnabledResolver", provider)
                    .isTrue();
        }
    }

    @Test
    void runnerProviders_followTheCallerChoice() {
        DevoxxGenieStateService state = allEnabledState();

        assertThat(ProviderEnabledResolver.isEnabled(state, ModelProvider.CLIRunners, false)).isFalse();
        assertThat(ProviderEnabledResolver.isEnabled(state, ModelProvider.ACPRunners, false)).isFalse();
        assertThat(ProviderEnabledResolver.isEnabled(state, ModelProvider.CLIRunners, true)).isTrue();
    }

    @Test
    void genericRouters_useTheRouterFlag() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        ModelProvider router = RouterRegistry.definitions().get(0).provider();

        assertThat(ProviderEnabledResolver.isEnabled(state, router, false)).isFalse();
        state.setRouterEnabled(router, true);
        assertThat(ProviderEnabledResolver.isEnabled(state, router, false)).isTrue();
    }
}
