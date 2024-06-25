package com.devoxx.genie.service;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesService {

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
                System.out.println("Sorry, unable to find application.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getVersion() {
        return properties.getProperty("version");
    }
}
