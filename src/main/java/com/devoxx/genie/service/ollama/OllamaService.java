package com.devoxx.genie.service.ollama;

import com.devoxx.genie.model.ollama.OllamaModelDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
<<<<<<< HEAD
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;
=======
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
>>>>>>> master

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class OllamaService {

    private final OkHttpClient client = new OkHttpClient();
<<<<<<< HEAD
    private final Gson gson = new Gson();
    private final MediaType JSON = MediaType.parse("application/json");
=======
>>>>>>> master

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

<<<<<<< HEAD
    public void pullModel(String modelName, Consumer<String> statusCallback) throws IOException {
        String baseUrl = ensureEndsWithSlash(DevoxxGenieStateService.getInstance().getOllamaModelUrl());

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);
        requestBody.addProperty("stream", true);

        Request request = new Request.Builder()
                .url(baseUrl + "api/pull")
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new UnsuccessfulRequestException("Empty response body");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                JsonObject status = gson.fromJson(line, JsonObject.class);
                if (status.has("status")) {
                    statusCallback.accept(status.get("status").getAsString());

                    // If download progress is available
                    if (status.has("completed") && status.has("total")) {
                        long completed = status.get("completed").getAsLong();
                        long total = status.get("total").getAsLong();
                        double progress = (double) completed / total * 100;
                        statusCallback.accept(String.format("Downloading: %.1f%%", progress));
                    }
                }
            }
        }
    }

=======
>>>>>>> master
    /**
     * Exception for unsuccessful requests.
     */
    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
