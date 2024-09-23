package com.devoxx.genie.service.jan;

import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.model.jan.ResponseDTO;
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

public class JanService {
    private final OkHttpClient client = new OkHttpClient();

    @NotNull
    public static JanService getInstance() {
        return ApplicationManager.getApplication().getService(JanService.class);
    }

    public List<Data> getModels() throws IOException {
        String baseUrl = ensureEndsWithSlash(DevoxxGenieStateService.getInstance().getJanModelUrl());

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

    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
