package com.devoxx.genie.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesService {

    private static final Logger LOG = Logger.getInstance(PropertiesService.class);

    private final Properties properties = new Properties();

    public PropertiesService() {
        loadProperties();
    }

    @NotNull
    public static PropertiesService getInstance() {
        return ApplicationManager.getApplication().getService(PropertiesService.class);
    }

    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                LOG.warn("Sorry, unable to find application.properties"); // Use LOG.warn
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            LOG.error("Error loading properties", ex); // Use LOG.error with exception
        }
    }

    public String getVersion() {
        return properties.getProperty("version");
    }
}