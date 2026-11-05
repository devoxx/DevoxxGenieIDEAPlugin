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

    @Test
    void setRouterEnabledMap_nullIsToleratedAndLeavesUsableState() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        state.setRouterEnabled(ModelProvider.Requesty, true);

        state.setRouterEnabled((java.util.Map<String, Boolean>) null);

        assertThat(state.isRouterEnabled(ModelProvider.Requesty)).isFalse();
        state.setRouterEnabled(ModelProvider.Requesty, true);
        assertThat(state.isRouterEnabled(ModelProvider.Requesty)).isTrue();
    }

    @Test
    void routerEnabled_isThreadSafeMap() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        assertThat(state.getRouterEnabled()).isInstanceOf(java.util.concurrent.ConcurrentMap.class);

        state.setRouterEnabled(new java.util.HashMap<>(java.util.Map.of("Requesty", true)));
        assertThat(state.getRouterEnabled()).isInstanceOf(java.util.concurrent.ConcurrentMap.class);
        assertThat(state.isRouterEnabled(ModelProvider.Requesty)).isTrue();
    }

    @Test
    void routerEnabled_allRoutersSurviveXmlRoundTripAndLoadState() {
        DevoxxGenieStateService original = new DevoxxGenieStateService();
        original.setRouterEnabled(ModelProvider.OpenRouter, true);
        original.setRouterEnabled(ModelProvider.Requesty, true);
        original.setRouterEnabled(ModelProvider.DemonRoute, false);

        Element xml = XmlSerializer.serialize(original);
        DevoxxGenieStateService deserialized = XmlSerializer.deserialize(xml, DevoxxGenieStateService.class);
        DevoxxGenieStateService loaded = new DevoxxGenieStateService();
        loaded.loadState(deserialized);

        assertThat(loaded.isRouterEnabled(ModelProvider.OpenRouter)).isTrue();
        assertThat(loaded.isRouterEnabled(ModelProvider.Requesty)).isTrue();
        assertThat(loaded.isRouterEnabled(ModelProvider.DemonRoute)).isFalse();
        assertThat(loaded.getRouterEnabled()).isInstanceOf(java.util.concurrent.ConcurrentMap.class);
    }

    @Test
    void loadState_withNullMap_stillSeedsLegacyOpenRouterFlag() {
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setOpenRouterEnabled(true);
        saved.setRouterEnabled((java.util.Map<String, Boolean>) null);
        DevoxxGenieStateService loaded = new DevoxxGenieStateService();

        loaded.loadState(saved);

        assertThat(loaded.isRouterEnabled(ModelProvider.OpenRouter)).isTrue();
    }
}
