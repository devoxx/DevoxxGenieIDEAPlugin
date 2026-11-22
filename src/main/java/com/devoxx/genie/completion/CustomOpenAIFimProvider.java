package com.devoxx.genie.completion;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

/**
 * Sends Fill-in-the-Middle (FIM) requests to any OpenAI-compatible
 * {@code /v1/completions} endpoint (llama.cpp, LiteLLM, ...).
 * <p>
 * Identical wire format to {@link LMStudioFimProvider} (prompt + suffix),
 * plus an optional {@code Authorization: Bearer} header so authenticated
 * gateways such as LiteLLM can be used. The API key is read from
 * {@link DevoxxGenieStateService#getInlineCompletionCustomApiKey()} at
 * request time, mirroring how {@code CustomOpenAIChatModelFactory} reads
 * its key.
 */
public class CustomOpenAIFimProvider implements FimProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CustomOpenAIFimProvider.class);
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
        body.addProperty("repeat_penalty", 1.1);
        JsonArray stop = new JsonArray();
        stop.add("\n\n\n");
        String suffixAnchor = firstMeaningfulSuffixLine(request.getSuffix());
        if (suffixAnchor != null) {
            stop.add(suffixAnchor);
        }
        body.add("stop", stop);
        body.addProperty("stream", false);

        RequestBody requestBody = RequestBody.create(GSON.toJson(body), JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

        String apiKey = resolveApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        OkHttpClient client = BASE_CLIENT.newBuilder()
                .callTimeout(Duration.ofMillis(request.getTimeoutMs()))
                .connectTimeout(Duration.ofMillis(Math.min(request.getTimeoutMs(), 3000)))
                .readTimeout(Duration.ofMillis(request.getTimeoutMs()))
                .build();

        long startTime = System.currentTimeMillis();
        Call call = client.newCall(requestBuilder.build());
        activeCall.set(call);

        LOG.debug("Custom OpenAI FIM request: url={}, model={}, apiKey={}",
                url, request.getModelName(), apiKey != null && !apiKey.isBlank() ? "set" : "absent");

        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                LOG.warn("Custom OpenAI FIM request failed with code {}: {}",
                        response.code(), errorBodySnippet(response));
                return null;
            }

            ResponseBody responseBody = Objects.requireNonNull(response.body());

            JsonObject jsonResponse = GSON.fromJson(responseBody.string(), JsonObject.class);

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

            completionText = trimTrailing(completionText);

            return new FimResponse(completionText, durationMs);

        } catch (IOException e) {
            if (call.isCanceled()) {
                LOG.debug("Custom OpenAI FIM request was cancelled");
            } else {
                LOG.warn("Custom OpenAI FIM request failed: {}", e.getMessage());
            }
            return null;
        } catch (RuntimeException e) {
            LOG.warn("Custom OpenAI FIM response could not be parsed: {}", e.getMessage());
            return null;
        } finally {
            activeCall.compareAndSet(call, null);
        }
    }

    /** Short body excerpt of an error response, for diagnostics; empty when unavailable. */
    private static @NotNull String errorBodySnippet(@NotNull Response response) {
        try (ResponseBody body = response.peekBody(512)) {
            String text = body.string();
            return text.isBlank() ? "" : text;
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public void cancelActiveCall() {
        Call call = activeCall.getAndSet(null);
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    @Nullable
    static String resolveApiKey() {
        return DevoxxGenieStateService.getInstance().getInlineCompletionCustomApiKey();
    }

    /**
     * First non-blank suffix line that is not a pure closing brace, returned as a stop anchor.
     * When the model starts reproducing the suffix (its most likely continuation after the gap),
     * the server stops generation at this anchor instead of typing out the file below the cursor.
     * Short lines are skipped: they would stop legitimate completions too often.
     */
    @Nullable
    static String firstMeaningfulSuffixLine(@Nullable String suffix) {
        if (suffix == null) {
            return null;
        }
        for (String line : suffix.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.equals("}") || trimmed.equals("});")) {
                continue;
            }
            return trimmed.length() >= 8 ? line : null;
        }
        return null;
    }

    private static @NotNull String trimTrailing(@NotNull String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }
}
