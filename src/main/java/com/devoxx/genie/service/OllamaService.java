package com.devoxx.genie.service;

import com.devoxx.genie.model.ollama.OllamaModelDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.SettingsState;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class OllamaService {
    private final OkHttpClient client;

    public OllamaService(OkHttpClient client) {
        this.client = client;
    }

    public OllamaModelEntryDTO[] getModels() throws IOException {
        String baseUrl = ensureEndsWithSlash(SettingsState.getInstance().getOllamaModelUrl());

        Request request = new Request.Builder()
            .url(baseUrl + "api/tags")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            assert response.body() != null;

            OllamaModelDTO ollamaModelDTO = new Gson().fromJson(response.body().string(), OllamaModelDTO.class);
            return ollamaModelDTO != null && ollamaModelDTO.getModels() != null ? ollamaModelDTO.getModels() : new OllamaModelEntryDTO[0];
        }
    }

    private String ensureEndsWithSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
