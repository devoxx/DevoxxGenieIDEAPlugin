package com.devoxx.genie.service;

import java.lang.reflect.Constructor;
import java.util.ServiceLoader;

public interface DevoxxGenieSettingsServiceProvider {

    public static DevoxxGenieSettingsService getInstance() {

        try {
            // Workaround to load the SPI implementation in the IntelliJ plugin
            Class<?> cl = Class.forName("com.devoxx.genie.ui.settings.DevoxxGenieStateServiceProvider");
            Constructor<?> cons = cl.getConstructor();
            DevoxxGenieSettingsServiceProvider provider = (DevoxxGenieSettingsServiceProvider) cons.newInstance();
            return provider.getDevoxxGenieSettingsService();
        } catch (Exception e) {
            // Ignore
        }

        // This is the original code of the SPI implementation using ServiceLoader. Not working in the plugin for unknown reasons.
        ServiceLoader<DevoxxGenieSettingsServiceProvider> serviceLoader = ServiceLoader.load(DevoxxGenieSettingsServiceProvider.class);
        return serviceLoader.findFirst().orElseThrow().getDevoxxGenieSettingsService();
    }

    DevoxxGenieSettingsService getDevoxxGenieSettingsService();
}
