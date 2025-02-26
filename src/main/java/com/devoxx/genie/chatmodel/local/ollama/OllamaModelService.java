package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.chatmodel.local.LocalLLMProviderUtil;
import com.devoxx.genie.model.ollama.OllamaModelDTO;
import com.devoxx.genie.model.ollama.OllamaModelEntryDTO;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class OllamaModelService implements LocalLLMProvider {

    private final OkHttpClient client = HttpClientProvider.getClient();
    private final Gson gson = new Gson();
    private final MediaType JSON = MediaType.parse("application/json");

    @NotNull
    public static OllamaModelService getInstance() {
        return ApplicationManager.getApplication().getService(OllamaModelService.class);
    }

    @Override
    public OllamaModelEntryDTO[] getModels() throws IOException {
        return LocalLLMProviderUtil
                .getModels("ollamaModelUrl", "api/tags", OllamaModelDTO.class)
                .getModels();
    }

    /**
     * Pulls the model from the Ollama server for RAG support.
     * @param modelName the name of the model to pull
     * @param statusCallback a callback to receive status updates
     * @throws IOException if an error occurs during the request
     */
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

    /**
     * Exception for unsuccessful requests.
     */
    public static class UnsuccessfulRequestException extends IOException {
        public UnsuccessfulRequestException(String message) {
            super(message);
        }
    }
}
