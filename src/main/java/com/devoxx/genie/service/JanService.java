package com.devoxx.genie.service;

import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.model.jan.ResponseDTO;
import com.devoxx.genie.ui.SettingsState;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class JanService {
    private final OkHttpClient client;

    public JanService(OkHttpClient client) {
        this.client = client;
    }

    public List<Data> getModels() throws IOException {
        String baseUrl = ensureEndsWithSlash(SettingsState.getInstance().getJanModelUrl());

        Request request = new Request.Builder()
            .url(baseUrl + "models")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            assert response.body() != null;

            ResponseDTO responseDTO = new Gson().fromJson(response.body().string(), ResponseDTO.class);
            return responseDTO != null && responseDTO.getData() != null ? responseDTO.getData() : List.of();
        }
    }

    @Contract(pure = true)
    private String ensureEndsWithSlash(@NotNull String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
