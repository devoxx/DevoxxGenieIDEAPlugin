package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class OllamaApiService {

    private static final Gson gson = new Gson();
    public static final int DEFAULT_CONTEXT_LENGTH = 4096;

    /**
     * Get the context length of the model.
     *
     * @param modelName the model name
     * @return the context length
     * @throws IOException if there is an error
     */
    public static int getModelContext(@NotNull String modelName) throws IOException {
        RequestBody body = RequestBody.create(
            "{\"name\":\"" + modelName + "\"}",
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(ensureEndsWithSlash(DevoxxGenieStateService.getInstance().getOllamaModelUrl()) + "api/show")
            .post(body)
            .build();

        try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);
            return findContextLength(jsonObject);
        }
    }

    private static int findContextLength(@NotNull JsonObject jsonObject) {
        JsonElement modelInfo = jsonObject.get("model_info");

        // If the model context length has been overridden with num_ctx param, use that instead of max supported length
        JsonElement parameters = jsonObject.get("parameters");
        if (parameters != null && parameters.isJsonPrimitive()) {
            for (String parameter : parameters.getAsString().split("\n")) {
                String[] parts = parameter.strip().split("\\s+", 2);
                if (parts.length == 2 && parts[0].equals("num_ctx")) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (NumberFormatException nfe) {
                        break;
                    }
            }
        }

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
}
