package com.devoxx.genie.ui.settings;

import com.devoxx.genie.model.enumarations.ModelProvider;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

class DevoxxGenieStateServiceLocalProviderTest {

    private static DevoxxGenieStateService upgrade(DevoxxGenieStateService saved) {
        Element xml = XmlSerializer.serialize(saved);
        DevoxxGenieStateService loaded = new DevoxxGenieStateService();
        loaded.loadState(XmlSerializer.deserialize(xml, DevoxxGenieStateService.class));
        return loaded;
    }

    @Test
    void upgrade_carriesNativLegacySettings() {
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setNativModelUrl("http://localhost:9090/v1/");
        saved.setNativEnabled(true);
        saved.setNativFallbackContextLength(131_072);
        DevoxxGenieStateService loaded = upgrade(saved);
        assertThat(loaded.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://localhost:9090/v1/");
        assertThat(loaded.isLocalProviderEnabled(ModelProvider.Nativ)).isTrue();
        assertThat(loaded.getLocalProviderFallbackContext(ModelProvider.Nativ)).isEqualTo(131_072);
    }

    @Test
    void upgrade_doesNotPinDefaultUrl_orFallback() {
        DevoxxGenieStateService loaded = upgrade(new DevoxxGenieStateService());
        assertThat(loaded.getLocalProviderUrls()).doesNotContainKey("Nativ");
        assertThat(loaded.getLocalProviderFallbackContext(ModelProvider.Nativ)).isNull();
        assertThat(loaded.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
    }

    @Test
    void savedMapValues_winOverLegacyFields() {
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setNativModelUrl("http://old:1/v1/");
        saved.setNativEnabled(true);
        saved.setLocalProviderUrl(ModelProvider.Nativ, "http://new:2/v1/");
        saved.setLocalProviderEnabled(ModelProvider.Nativ, false);
        DevoxxGenieStateService loaded = upgrade(saved);
        assertThat(loaded.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://new:2/v1/");
        assertThat(loaded.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
    }

    @Test
    void defaults_disabledOnDefinitionUrl_andBlankUrlFallsBack() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        assertThat(state.isLocalProviderEnabled(ModelProvider.Nativ)).isFalse();
        assertThat(state.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://localhost:8080/v1/");
        state.setLocalProviderUrl(ModelProvider.Nativ, "   ");
        assertThat(state.getLocalProviderUrls()).doesNotContainKey("Nativ");
        assertThat(state.getLocalProviderUrl(ModelProvider.Nativ)).isEqualTo("http://localhost:8080/v1/");
    }

    @Test
    void maps_areThreadSafe_andDropNullEntries() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        Map<String, String> urls = new HashMap<>();
        urls.put("Nativ", null);
        state.setLocalProviderUrls(urls);
        assertThat(state.getLocalProviderUrls()).isInstanceOf(ConcurrentMap.class).isEmpty();
    }

    @Test
    void fallbackContext_nullRemovesEntry() {
        DevoxxGenieStateService state = new DevoxxGenieStateService();
        state.setLocalProviderFallbackContext(ModelProvider.Nativ, 4096);
        assertThat(state.getLocalProviderFallbackContext(ModelProvider.Nativ)).isEqualTo(4096);
        state.setLocalProviderFallbackContext(ModelProvider.Nativ, null);
        assertThat(state.getLocalProviderFallbackContext(ModelProvider.Nativ)).isNull();
    }

    @Test
    void clearedFallback_staysClearedAfterReload() {
        DevoxxGenieStateService saved = new DevoxxGenieStateService();
        saved.setNativFallbackContextLength(131_072);
        DevoxxGenieStateService loaded = upgrade(saved);
        assertThat(loaded.getLocalProviderFallbackContext(ModelProvider.Nativ)).isEqualTo(131_072);
        loaded.setLocalProviderFallbackContext(ModelProvider.Nativ, null);
        assertThat(upgrade(loaded).getLocalProviderFallbackContext(ModelProvider.Nativ)).isNull();
    }
}
