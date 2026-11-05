package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DevoxxGenieStateServiceRouterTest {

    @Test
    void isRouterEnabled_defaultsFalse() {
        assertThat(new DevoxxGenieStateService().isRouterEnabled(ModelProvider.OpenRouter)).isFalse();
    }

    @Test
    void loadState_seedsOpenRouterFromLegacyFlag() {
        DevoxxGenieStateService loaded = new DevoxxGenieStateService();
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setOpenRouterEnabled(true);

        loaded.loadState(saved);

        assertThat(loaded.isRouterEnabled(ModelProvider.OpenRouter)).isTrue();
    }

    @Test
    void loadState_keepsExistingMapValue() {
        DevoxxGenieStateService loaded = new DevoxxGenieStateService();
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setOpenRouterEnabled(true);
        saved.setRouterEnabled(ModelProvider.OpenRouter, false);

        loaded.loadState(saved);

        assertThat(loaded.isRouterEnabled(ModelProvider.OpenRouter)).isFalse();
    }

    @Test
    void routerEnabled_survivesXmlRoundTrip() {
        DevoxxGenieStateService original = new DevoxxGenieStateService();
        original.setRouterEnabled(ModelProvider.OpenRouter, true);

        Element xml = XmlSerializer.serialize(original);
        DevoxxGenieStateService restored = XmlSerializer.deserialize(xml, DevoxxGenieStateService.class);

        assertThat(restored.isRouterEnabled(ModelProvider.OpenRouter)).isTrue();
    }

    @Test
    void routerKey_roundTripsBySubKey() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        state.setRouterKey("router.acme", "abc");
        assertThat(state.getRouterKey("router.acme")).isEqualTo("abc");
    }
}
