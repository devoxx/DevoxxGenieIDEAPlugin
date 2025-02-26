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

        Request request = new Request.Builder()
                .url(baseUrl + endpoint)
                .build();

        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
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
                } else {
                    return responseType.cast(new LMStudioModelEntryDTO[0]);
                }
            }

            return gson.fromJson(json, responseType);
        }
    }
}
