package com.devoxx.genie.service.rag.validator;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class OllamaValidator implements Validator {
    private String message;

    @Override
    public boolean isValid() {
        String ollamaModelUrl = DevoxxGenieStateService.getInstance().getOllamaModelUrl();
        if (ollamaModelUrl == null || ollamaModelUrl.isEmpty()) {
            return false;
        }

        try {
            // Try to connect to Ollama's default port
            URI uri = new URI(ollamaModelUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return true;
            } else {
                this.message = "Ollama not responding, please check the URL";
                return false;
            }

        } catch (Exception e) {
            this.message = "Ollama is not running, please start Ollama";
            return false;
        }
    }

    @Override
    public String getErrorMessage() {
        return this.message;
    }

    @Override
    public String getMessage() {
        return "Ollama is running";
    }

    @Override
    public ValidatorType getCommand() {
        return ValidatorType.OLLAMA;
    }
}
