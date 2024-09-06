package com.devoxx.genie.service.openrouter;

import com.devoxx.genie.model.openrouter.Data;
import com.devoxx.genie.model.openrouter.ResponseDTO;
import com.devoxx.genie.service.exception.UnsuccessfulRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class OpenRouterService {
    private final OkHttpClient client = new OkHttpClient();

    @NotNull
    public static OpenRouterService getInstance() {
        return ApplicationManager.getApplication().getService(OpenRouterService.class);
    }

    public List<Data> getModels() throws IOException {
        Request request = new Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                 throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new UnsuccessfulRequestException("Response is empty");
            }

            Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

            ResponseDTO responseDTO = gson.fromJson(response.body().string(), ResponseDTO.class); return responseDTO != null && responseDTO.getData() != null ? responseDTO.getData() : List.of();
        }
    }
}
