package com.devoxx.genie.service.semanticsearch.validator;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaValidator implements Validator {

    @Override
    public boolean isValid() {
        String ollamaModelUrl = DevoxxGenieStateService.getInstance().getOllamaModelUrl();
        if (ollamaModelUrl == null || ollamaModelUrl.isEmpty()) {
            return false;
        }

        try {
            // Try to connect to Ollama's default port
            URL url = new URL(ollamaModelUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);

            int responseCode = connection.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            // If any exception occurs (connection refused, timeout, etc.)
            // it means Ollama is not running
            return false;
        }
    }

    public String getName() {
        return "Ollama check";
    }

    public String getCommand() {
        return ValidatorType.OLLAMA.name().toLowerCase();
    }
}
