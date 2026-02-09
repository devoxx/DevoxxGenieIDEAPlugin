package com.devoxx.genie.completion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
 * Sends Fill-in-the-Middle (FIM) requests to LM Studio's OpenAI-compatible
 * /v1/completions endpoint. Uses the "prompt" + "suffix" fields which
 * trigger FIM mode in LM Studio for models that support it.
 */
public class LMStudioFimProvider implements FimProvider {

    private static final Logger LOG = LoggerFactory.getLogger(LMStudioFimProvider.class);
    private static final Gson GSON = new Gson();
    private static final MediaType JSON = MediaType.parse("application/json");

    /** Shared client — connection pool and thread pool are reused across requests. */
    private static final OkHttpClient BASE_CLIENT = new OkHttpClient.Builder().build();

    private final AtomicReference<Call> activeCall = new AtomicReference<>();

    @Override
    public @Nullable FimResponse generate(@NotNull FimRequest request) {
        cancelActiveCall();

        String url = ensureEndsWithSlash(request.getBaseUrl()) + "completions";

        JsonObject body = new JsonObject();
        body.addProperty("model", request.getModelName());
        body.addProperty("prompt", request.getPrefix());
        body.addProperty("suffix", request.getSuffix());
        body.addProperty("max_tokens", request.getMaxTokens());
        body.addProperty("temperature", request.getTemperature());
        body.addProperty("stream", false);

        RequestBody requestBody = RequestBody.create(GSON.toJson(body), JSON);

        Request httpRequest = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

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
                LOG.debug("LM Studio FIM request failed with code: {}", response.code());
                return null;
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            JsonObject jsonResponse = GSON.fromJson(responseBody.string(), JsonObject.class);

            // OpenAI completions format: { "choices": [{ "text": "..." }] }
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }

            String completionText = choices.get(0).getAsJsonObject()
                    .get("text").getAsString();

            long durationMs = System.currentTimeMillis() - startTime;

            if (completionText.isEmpty()) {
                return null;
            }

            // Trim trailing whitespace but preserve leading whitespace
            completionText = trimTrailing(completionText);

            return new FimResponse(completionText, durationMs);

        } catch (IOException e) {
            if (call.isCanceled()) {
                LOG.debug("LM Studio FIM request was cancelled");
            } else {
                LOG.debug("LM Studio FIM request failed: {}", e.getMessage());
            }
            return null;
        } finally {
            activeCall.compareAndSet(call, null);
        }
    }

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
