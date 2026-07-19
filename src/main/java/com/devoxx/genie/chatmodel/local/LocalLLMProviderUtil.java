package com.devoxx.genie.chatmodel.local;

import com.devoxx.genie.model.lmstudio.LMStudioModelEntryDTO;
import com.devoxx.genie.service.exception.UnsuccessfulRequestException;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class LocalLLMProviderUtil {

    private static final Gson gson = new Gson();

    public static <T> T getModels(String baseUrlConfigKey, String endpoint, Class<T> responseType) throws IOException {
        DevoxxGenieStateService stateService = DevoxxGenieStateService.getInstance();
        String configValue = stateService.getConfigValue(baseUrlConfigKey);

        if (configValue == null || configValue.trim().isEmpty()) {
            throw new IllegalStateException("Configuration value for " + baseUrlConfigKey + " is not set");
        }

        String baseUrl = ensureEndsWithSlash(Objects.requireNonNull(configValue));
        String url = endpoint == null || endpoint.isBlank() ? Objects.requireNonNull(configValue) : baseUrl + endpoint;

        return fetchModels(url, responseType, HttpClientProvider.getClient(), null);
    }

    /**
     * Fetches models from a fully-qualified URL.
     * Use this when the models endpoint URL differs from the chat base URL
     * (e.g., LMStudio uses /v1/ for chat but /api/v1/models for rich metadata).
     */
    public static <T> T getModelsFromUrl(String fullUrl, Class<T> responseType) throws IOException {
        return fetchModels(fullUrl, responseType, HttpClientProvider.getClient(), null);
    }

    /**
     * Fetches models from a fully-qualified URL using a caller-supplied HTTP client.
     * Use this when the default shared client's long connect timeout and retry/backoff
     * behaviour is undesirable (e.g. a best-effort model-list probe that must fail fast
     * so it never blocks the UI for a slow or unreachable endpoint).
     */
    public static <T> T getModelsFromUrl(String fullUrl, Class<T> responseType, OkHttpClient client) throws IOException {
        return fetchModels(fullUrl, responseType, client, null);
    }

    /**
     * Fetches models from a fully-qualified URL using a caller-supplied HTTP client and a bearer
     * token. Authenticated OpenAI-compatible gateways (e.g. Cloudflare AI Gateway) reject the
     * {@code /models} discovery call with 401 when no {@code Authorization} header is sent, so the
     * probe must carry the same API key the chat requests use.
     *
     * @param bearerToken API key to send as {@code Authorization: Bearer ...}; when {@code null} or
     *                    blank, no auth header is added (unauthenticated endpoint).
     */
    public static <T> T getModelsFromUrl(String fullUrl, Class<T> responseType, OkHttpClient client, String bearerToken) throws IOException {
        return fetchModels(fullUrl, responseType, client, bearerToken);
    }

    private static <T> T fetchModels(String url, Class<T> responseType, OkHttpClient client, String bearerToken) throws IOException {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (bearerToken != null && !bearerToken.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }
        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new UnsuccessfulRequestException("Unexpected code " + response);
            }

            if (response.body() == null) {
                throw new UnsuccessfulRequestException("Response body is null");
            }

            String json = response.body().string();

            // Special handling for LM Studio
            if (responseType.equals(LMStudioModelEntryDTO[].class)) {
                JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
                if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("data")) {
                    return gson.fromJson(jsonElement.getAsJsonObject().get("data"), responseType);
                } else if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("models")) {
                    return gson.fromJson(jsonElement.getAsJsonObject().get("models"), responseType);
                } else if (jsonElement.isJsonArray()) {
                    return gson.fromJson(jsonElement, responseType);
                } else {
                    return responseType.cast(new LMStudioModelEntryDTO[0]);
                }
            }

            return gson.fromJson(json, responseType);
        }
    }
}
