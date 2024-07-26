package com.devoxx.genie.service;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class OllamaApiService {
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    public static final int DEFAULT_CONTEXT_LENGTH = 4096;

    /**
     * Get the context length of the model.
     * @param modelName the model name
     * @return the context length
     * @throws IOException if there is an error
     */
    public static int getModelContext(@NotNull String modelName) throws IOException {
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"),
            "{\"name\":\"" + modelName + "\"}"
        );

        Request request = new Request.Builder()
            .url(ensureEndsWithSlash(DevoxxGenieStateService.getInstance().getOllamaModelUrl()) + "api/show")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);
            return findContextLength(jsonObject);
        }
    }

    private static int findContextLength(@NotNull JsonObject jsonObject) {
        JsonElement modelInfo = jsonObject.get("model_info");
        if (modelInfo != null && modelInfo.isJsonObject()) {
            JsonObject modelInfoObject = modelInfo.getAsJsonObject();
            for (String key : modelInfoObject.keySet()) {
                if (key.endsWith(".context_length")) {
                    return modelInfoObject.get(key).getAsInt();
                }
            }
        }

        // Fallback: check if context_length exists directly in the root
        JsonElement contextLength = jsonObject.get("context_length");
        if (contextLength != null) {
            return contextLength.getAsInt();
        }

        return DEFAULT_CONTEXT_LENGTH;
    }

    /**
     * Ensure the URL ends with a slash.
     *
     * @param url the URL
     * @return the URL with a slash at the end
     */
    @Contract(pure = true)
    public static String ensureEndsWithSlash(@NotNull String url) {
        return url.endsWith("/") ? url : url + "/";
    }
}
