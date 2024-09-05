package com.devoxx.genie.service.openrouter;

import com.devoxx.genie.model.jan.Data;
import com.devoxx.genie.model.jan.ResponseDTO;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import net.schmizz.sshj.connection.channel.OpenFailException;
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
                // throw new OpenRouterService.UnsuccessfulRequestException("Unexpected code " + response);
            }

            assert response.body() != null;

            ResponseDTO responseDTO = new Gson().fromJson(response.body().string(), ResponseDTO.class);
            return responseDTO != null && responseDTO.getData() != null ? responseDTO.getData() : List.of();
        }
    }
}
