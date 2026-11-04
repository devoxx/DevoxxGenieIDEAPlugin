package com.devoxx.genie.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1374: the settings page showed the previous release's version because the bundled
 * application.properties was packaged before the build rewrote it. The installed plugin
 * descriptor is the source of truth; application.properties is only a fallback.
 */
class PropertiesServiceTest {

    @Test
    void getVersion_prefersInstalledPluginDescriptorVersion() {
        PropertiesService service = new PropertiesService(() -> "99.0.0");

        assertThat(service.getVersion()).isEqualTo("99.0.0");
    }

    @Test
    void getVersion_fallsBackToApplicationPropertiesWhenDescriptorUnavailable() {
        PropertiesService service = new PropertiesService(() -> null);

        assertThat(service.getVersion()).isNotBlank();
    }

    @Test
    void getVersion_fallsBackWhenDescriptorLookupThrows() {
        PropertiesService service = new PropertiesService(() -> {
            throw new IllegalStateException("plugin manager not ready");
        });

        assertThat(service.getVersion()).isNotBlank();
    }
}
