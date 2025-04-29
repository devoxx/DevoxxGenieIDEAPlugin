package com.devoxx.genie.ui.webview.template;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Utility class for loading resource files from the classpath.
 * Used for loading externalized HTML, CSS, and JavaScript files.
 */
public class ResourceLoader {

    /**
     * Private constructor to prevent instantiation.
     */
    private ResourceLoader() {
        // Utility class should not be instantiated
    }

    /**
     * Load a resource file from the classpath.
     *
     * @param resourcePath The path to the resource file
     * @return The content of the resource file as a string
     * @throws RuntimeException If the resource file cannot be loaded
     */
    public static @NotNull String loadResource(@NotNull String resourcePath) {
        try (InputStream is = ResourceLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }
}