package com.devoxx.genie.completion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

/**
 * Sends Fill-in-the-Middle (FIM) requests to Ollama's /api/generate endpoint.
 * Uses the "suffix" parameter which triggers FIM mode in Ollama for models
 * that support it (e.g., starcoder2, deepseek-coder, qwen2.5-coder).
 */
public class OllamaFimProvider implements FimProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OllamaFimProvider.class);
    private static final Gson GSON = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json");

    /** Shared client — connection pool and thread pool are reused across requests. */
    private static final OkHttpClient BASE_CLIENT = new OkHttpClient.Builder().build();

    private final AtomicReference<Call> activeCall = new AtomicReference<>();

    /**
     * Generate a FIM completion from Ollama.
     *
     * @param request the FIM request containing prefix, suffix, model info
     * @return FIM response with completion text, or null if cancelled/failed
     */
    @Override
    public @Nullable FimResponse generate(@NotNull FimRequest request) {
        cancelActiveCall();

        String url = ensureEndsWithSlash(request.getBaseUrl()) + "api/generate";

        JsonObject body = new JsonObject();
        body.addProperty("model", request.getModelName());
        body.addProperty("prompt", request.getPrefix());
        body.addProperty("suffix", request.getSuffix());
        body.addProperty("stream", false);

        JsonObject options = new JsonObject();
        options.addProperty("num_predict", request.getMaxTokens());
        options.addProperty("temperature", request.getTemperature());
        body.add("options", options);

        RequestBody requestBody = RequestBody.create(GSON.toJson(body), JSON);

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        // Per-request timeouts, but shares connection pool with BASE_CLIENT
        OkHttpClient client = BASE_CLIENT.newBuilder()
                .callTimeout(Duration.ofMillis(request.getTimeoutMs()))
                .connectTimeout(Duration.ofMillis(Math.min(request.getTimeoutMs(), 3000)))
                .readTimeout(Duration.ofMillis(request.getTimeoutMs()))
                .build();

        long startTime = System.currentTimeMillis();
        Call call = client.newCall(httpRequest);
        activeCall.set(call);

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                LOG.debug("Ollama FIM request failed with code: {}", response.code());
                return null;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            JsonObject jsonResponse = GSON.fromJson(responseBody.string(), JsonObject.class);
            String completionText = jsonResponse.has("response")
                    ? jsonResponse.get("response").getAsString()
                    : "";

            long durationMs = System.currentTimeMillis() - startTime;

            if (completionText.isEmpty()) {
                return null;
            }

            // Trim trailing whitespace but preserve leading whitespace
            completionText = trimTrailing(completionText);

            return new FimResponse(completionText, durationMs);

        } catch (IOException e) {
            if (call.isCanceled()) {
                LOG.debug("Ollama FIM request was cancelled");
            } else {
                LOG.debug("Ollama FIM request failed: {}", e.getMessage());
            }
            return null;
        } finally {
            activeCall.compareAndSet(call, null);
        }
    }

    /**
     * Cancel any currently active FIM request.
     */
    @Override
    public void cancelActiveCall() {
        Call call = activeCall.getAndSet(null);
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private static @NotNull String trimTrailing(@NotNull String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}
