package com.devoxx.genie.chatmodel.local.customopenai;

import com.devoxx.genie.chatmodel.local.LocalLLMProvider;
import com.devoxx.genie.model.customopenai.CustomOpenAIModelEntryDTO;
import com.devoxx.genie.service.exception.UnsuccessfulRequestException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intellij.openapi.application.ApplicationManager;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class CustomOpenAIModelService implements LocalLLMProvider {

    private static final Gson gson = new Gson();

    @NotNull
    public static CustomOpenAIModelService getInstance() {
        return ApplicationManager.getApplication().getService(CustomOpenAIModelService.class);
    }

    @Override
    public CustomOpenAIModelEntryDTO[] getModels() throws IOException {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        String baseUrl = stateService.getCustomOpenAIUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            return new CustomOpenAIModelEntryDTO[0];
        }

        String modelsUrl = buildModelsUrl(baseUrl);

        Request.Builder requestBuilder = new Request.Builder().url(modelsUrl);

        if (stateService.isCustomOpenAIApiKeyEnabled()) {
            String apiKey = stateService.getCustomOpenAIApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
            }
        }

        Request request = requestBuilder.build();

        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new UnsuccessfulRequestException("Response body is null");
            }

            String json = response.body().string();
            return parseModelsResponse(json);
        }
    }

    /**
     * Parses the models response JSON.
     * Handles the standard OpenAI format: {"object": "list", "data": [...]}
     * Also handles responses that are plain arrays or have a "models" key.
     */
    CustomOpenAIModelEntryDTO[] parseModelsResponse(@NotNull String json) {
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);

        if (jsonElement.isJsonObject()) {
            // Standard OpenAI format: {"data": [...]}
            if (jsonElement.getAsJsonObject().has("data")) {
                JsonArray data = jsonElement.getAsJsonObject().getAsJsonArray("data");
                return gson.fromJson(data, CustomOpenAIModelEntryDTO[].class);
            }
            // Alternative format: {"models": [...]}
            if (jsonElement.getAsJsonObject().has("models")) {
                JsonArray models = jsonElement.getAsJsonObject().getAsJsonArray("models");
                return gson.fromJson(models, CustomOpenAIModelEntryDTO[].class);
            }
        }

        if (jsonElement.isJsonArray()) {
            return gson.fromJson(jsonElement, CustomOpenAIModelEntryDTO[].class);
        }

        return new CustomOpenAIModelEntryDTO[0];
    }

    /**
     * Builds the models endpoint URL from the configured base URL.
     * Appends "/models" to the base URL path.
     *
     * @param baseUrl The configured Custom OpenAI base URL (e.g., http://localhost:3000/v1/)
     * @return The full models endpoint URL (e.g., http://localhost:3000/v1/models)
     */
    protected String buildModelsUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }

        try {
            URI uri = URI.create(ensureEndsWithSlash(baseUrl));
            return uri.resolve("models").toString();
        } catch (IllegalArgumentException e) {
            return ensureEndsWithSlash(baseUrl) + "models";
        }
    }
}
