package com.devoxx.genie.service;

import com.devoxx.genie.model.lmstudio.LMStudioModelDTO;
import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.devoxx.genie.service.OllamaApiService.ensureEndsWithSlash;

public class LMStudioService {

    private final OkHttpClient client = new OkHttpClient();

    @NotNull
    public static LMStudioService getInstance() {
        return ApplicationManager.getApplication().getService(LMStudioService.class);
    }

    /**
     * Get the models from the LMStudio service.
     *
     * @return array of model names
     * @throws IOException if there is an error
     */
    public LMStudioModelEntryDTO[] getModels() throws IOException {
        String baseUrl = ensureEndsWithSlash(DevoxxGenieSettingsServiceProvider.getInstance().getLmstudioModelUrl());

        Request request = new Request.Builder()
            .url(baseUrl + "models")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            assert response.body() != null;

            LMStudioModelDTO lmStudioModelDTO = new Gson().fromJson(response.body().string(), LMStudioModelDTO.class);
            return lmStudioModelDTO != null && lmStudioModelDTO.getData() != null ? lmStudioModelDTO.getData() : new LMStudioModelEntryDTO[0];
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
