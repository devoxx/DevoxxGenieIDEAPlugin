package com.devoxx.genie.service.prompt.websearch;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSearchEngineFactoryTest {

    @Test
    void notConfiguredWhenNoProviderEnabled() {
        DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
        when(state.isTavilySearchEnabled()).thenReturn(false);
        when(state.isGoogleSearchEnabled()).thenReturn(false);
        when(state.isKeenableSearchEnabled()).thenReturn(false);

        assertThat(WebSearchEngineFactory.isConfigured(state)).isFalse();
        assertThat(WebSearchEngineFactory.create(state)).isNull();
    }

    @Test
    void keenableIsUsedWhenEnabledAndOthersAreOff() {
        DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
        when(state.isTavilySearchEnabled()).thenReturn(false);
        when(state.isGoogleSearchEnabled()).thenReturn(false);
        when(state.isKeenableSearchEnabled()).thenReturn(true);
        when(state.getKeenableSearchKey()).thenReturn("");

        assertThat(WebSearchEngineFactory.isConfigured(state)).isTrue();
        assertThat(WebSearchEngineFactory.create(state)).isInstanceOf(KeenableWebSearchEngine.class);
    }

    @Test
    void tavilyStillWinsWhenBothTavilyAndKeenableAreEnabled() {
        DevoxxGenieStateService state = mock(DevoxxGenieStateService.class);
        when(state.isTavilySearchEnabled()).thenReturn(true);
        when(state.getTavilySearchKey()).thenReturn("tavily-key");
        when(state.isKeenableSearchEnabled()).thenReturn(true);
        when(state.getKeenableSearchKey()).thenReturn("");

        assertThat(WebSearchEngineFactory.create(state).getClass().getSimpleName())
                .isEqualTo("TavilyWebSearchEngine");
    }
}
