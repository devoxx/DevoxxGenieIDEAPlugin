package com.devoxx.genie.service;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

public class PropertiesService {

    private static final Logger LOG = Logger.getInstance(PropertiesService.class);
    private static final String PLUGIN_ID = "com.devoxx.genie";

    private final Properties properties = new Properties();
    private final Supplier<String> pluginVersionSupplier;

    public PropertiesService() {
        this(PropertiesService::installedPluginVersion);
    }

    @VisibleForTesting
    PropertiesService(@NotNull Supplier<String> pluginVersionSupplier) {
        this.pluginVersionSupplier = pluginVersionSupplier;
        loadProperties();
    }

    @NotNull
    public static PropertiesService getInstance() {
        return ApplicationManager.getApplication().getService(PropertiesService.class);
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                LOG.warn("Unable to find application.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            LOG.warn("Failed to load application.properties", ex);
        }
    }

    /**
     * Returns the plugin version. The installed plugin descriptor is authoritative; the bundled
     * application.properties is only a fallback, because the build packages it before
     * updating it, so it can lag one release behind (issue #1374).
     */
    public String getVersion() {
        try {
            String descriptorVersion = pluginVersionSupplier.get();
            if (descriptorVersion != null && !descriptorVersion.isBlank()) {
                return descriptorVersion;
            }
        } catch (RuntimeException ex) {
            LOG.debug("Could not read version from plugin descriptor", ex);
        }
        return properties.getProperty("version");
    }

    private static @Nullable String installedPluginVersion() {
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        return descriptor != null ? descriptor.getVersion() : null;
    }
}
