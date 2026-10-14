package com.devoxx.genie.chatmodel.local.ollama;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

public class OllamaApiService {

    private static final Gson gson = new Gson();
    public static final int DEFAULT_CONTEXT_LENGTH = 4096;

    /**
     * Fail-fast client for the best-effort {@code /api/show} context probe. It deliberately
     * does NOT use the shared {@link HttpClientProvider} client, whose retry/backoff
     * (2s/4s/8s) plus 30s read timeout would make model loading appear to hang when the
     * probe is slow or the endpoint errors. The probe result is optional — a failure simply
     * falls back to {@link #DEFAULT_CONTEXT_LENGTH} — so it must never block the UI for long.
     */
    private static final OkHttpClient PROBE_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
            .build();

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

        try (Response response = PROBE_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            if (response.body() == null) {
                return DEFAULT_CONTEXT_LENGTH;
            }
            String json = response.body().string();
            // Ollama answers 200 with an EMPTY body for some models (observed with
            // gemma3n:e4b). gson.fromJson("") returns null, which used to NPE in
            // findContextLength and — escaping the per-model task — hide the entire
            // model list. Treat an empty/unparseable body as "context unknown".
            if (json == null || json.isBlank()) {
                return DEFAULT_CONTEXT_LENGTH;
            }
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            if (jsonObject == null) {
                return DEFAULT_CONTEXT_LENGTH;
            }
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
