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

    public OllamaModelEntryDTO[] getModels() throws IOException {
        OkHttpClient client = new OkHttpClient();

        String baseUrl = SettingsState.getInstance().getOllamaModelUrl();

        // Ensure the base URL ends with exactly one slash
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        Request request = new Request.Builder()
            .url(baseUrl+"api/tags")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            assert response.body() != null;

            OllamaModelDTO ollamaModelDTO = new Gson().fromJson(response.body().string(), OllamaModelDTO.class);
            if (ollamaModelDTO != null && ollamaModelDTO.getModels() != null) {
                return ollamaModelDTO.getModels();
            }
        }
        return new OllamaModelEntryDTO[0];
    }
}
