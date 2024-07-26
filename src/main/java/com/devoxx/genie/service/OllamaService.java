package com.devoxx.genie.service;

import com.devoxx.genie.model.ollama.OllamaModelDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.devoxx.genie.service.OllamaApiService.ensureEndsWithSlash;

public class OllamaService {

    private final OkHttpClient client = new OkHttpClient();

    @NotNull
    public static OllamaService getInstance() {
        return ApplicationManager.getApplication().getService(OllamaService.class);
    }

    /**
     * Get the models from the Ollama service.
     *
     * @return array of model names
     * @throws IOException if there is an error
     */
    public OllamaModelEntryDTO[] getModels() throws IOException {
        String baseUrl = ensureEndsWithSlash(DevoxxGenieStateService.getInstance().getOllamaModelUrl());

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

    /**
     * Exception for unsuccessful requests.
     */
    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
