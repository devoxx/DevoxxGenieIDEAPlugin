package com.devoxx.genie.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WindowsSkikoRenderApiInitializerTest {

    @Test
    void componentsInitialized_setsSoftwareRenderingOnWindows() {
        Map<String, String> properties = new HashMap<>();
        WindowsSkikoRenderApiInitializer initializer =
                new WindowsSkikoRenderApiInitializer(() -> true, properties::put);

        initializer.initialize();

        assertThat(properties)
                .containsEntry(
                        WindowsSkikoRenderApiInitializer.SKIKO_RENDER_API_PROPERTY,
                        WindowsSkikoRenderApiInitializer.SOFTWARE_RENDER_API
                );
    }

    @Test
    void componentsInitialized_doesNothingOutsideWindows() {
        Map<String, String> properties = new HashMap<>();
        WindowsSkikoRenderApiInitializer initializer =
                new WindowsSkikoRenderApiInitializer(() -> false, properties::put);

        initializer.initialize();

        assertThat(properties).isEmpty();
    }
}
