package com.devoxx.genie.service.gpt4all;

import com.devoxx.genie.model.gpt4all.Model;
import com.devoxx.genie.model.gpt4all.ResponseDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class GPT4AllService {
    private final OkHttpClient client = new OkHttpClient();

    @NotNull
    public static GPT4AllService getInstance() {
        return ApplicationManager.getApplication().getService(GPT4AllService.class);
    }

    /**
     * Get the models from the GPT4All service.
     * @return array of model names
     * @throws IOException if there is an error
     */
    public List<Model> getModels() throws IOException {
        String baseUrl = ensureEndsWithSlash(DevoxxGenieStateService.getInstance().getGpt4allModelUrl());

        Request request = new Request.Builder()
            .url(baseUrl + "models")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            assert response.body() != null;

            ResponseDTO modelResponse = new Gson().fromJson(response.body().string(), ResponseDTO.class);
            return modelResponse != null && modelResponse.getData() != null ? modelResponse.getData() : List.of();
        }
    }

    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
